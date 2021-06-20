package com.vachel.editor.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.vachel.editor.EditMode;
import com.vachel.editor.PictureEditor;
import com.vachel.editor.bean.EditState;
import com.vachel.editor.bean.PaintPath;
import com.vachel.editor.clip.Anchor;
import com.vachel.editor.clip.EditClipWindow;
import com.vachel.editor.sticker.ISticker;
import com.vachel.editor.util.EditUtils;
import com.vachel.editor.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class EditPresenter {
    private Bitmap mDoodleLayer, mMosaicLayer;
   // 底图区域
    private final RectF mFrame = new RectF();
    // 裁剪图片边框（显示的图片区域）
    private final RectF mClipFrame = new RectF();
    private final RectF mTempClipFrame = new RectF();

    // 裁剪模式前状态备份
    private final RectF mBackupClipFrame = new RectF();
    private float mBackupClipRotate = 0;

    // 裁剪模式时当前触摸锚点
    private Anchor mAnchor;

    private boolean isSteady = true;

    private float mRotate = 0, mTargetRotate = 0;
    private boolean isRequestToBaseFitting = false;
    private boolean isAnimCanceled = false;
    private final Path mShade = new Path();

    // 裁剪窗口
    private final EditClipWindow mClipWin = new EditClipWindow();

    private EditMode mMode = EditMode.NONE;
    private boolean isClipMode = mMode == EditMode.CLIP;

    // 可视区域，无Scroll偏移区域
    private final RectF mWindow = new RectF();

    private boolean isInitial = false;

    private ISticker mForeSticker;
    private final List<ISticker> mBackStickers = new ArrayList<>();

    // 涂鸦路径
    private final List<PaintPath> mDoodleList = new ArrayList<>();
    //马赛克路径
    private final List<PaintPath> mMosaicList = new ArrayList<>();
    // 需保证与mDoodleList mHistoryList的数据一致性，用于undo操作
    private final List<PaintPath> mHistoryList = new ArrayList<>();

    private static final int MIN_SIZE = 360;

    private static final int MAX_SIZE = 10000;

    private final Paint mPaint;
    private Paint mMosaicPaint;
    private Paint mShadePaint;

    private final Matrix M = new Matrix();
    private static final boolean DEBUG = false;
    private static Bitmap DEFAULT_IMAGE;
    private static final int COLOR_SHADE = 0xCC000000;

    public EditPresenter() {
        mDoodleLayer = getDefaultImage();
        if (mMode == EditMode.CLIP) {
            initShadePaint();
        }
        mShade.setFillType(Path.FillType.WINDING);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(PictureEditor.getInstance().getDefaultDoodleWidth());
        mPaint.setColor(Color.RED);
        mPaint.setPathEffect(new CornerPathEffect(PictureEditor.getInstance().getDefaultDoodleWidth()));
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setClipWindowRender(EditClipWindow.IClipRender clipRender) {
        mClipWin.setRender(clipRender);
    }

    private Bitmap getDefaultImage() {
        if (DEFAULT_IMAGE == null || DEFAULT_IMAGE.isRecycled()) {
            DEFAULT_IMAGE = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        }
        return DEFAULT_IMAGE;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        this.mDoodleLayer = bitmap;
        // 清空马赛克图层
        if (mMosaicLayer != null) {
            mMosaicLayer.recycle();
        }
        this.mMosaicLayer = null;
        makeMosaicBitmap();
        onImageChanged();
    }

    public EditMode getMode() {
        return mMode;
    }

    public void setMode(EditMode mode) {
        if (this.mMode == mode) return;
        moveToBackground(mForeSticker);
        if (mode == EditMode.CLIP) {
            setClipMode(true);
        }
        this.mMode = mode;
        if (mMode == EditMode.CLIP) {
            // 初始化Shade 画刷
            initShadePaint();
            // 备份裁剪前Clip 区域
            mBackupClipRotate = getRotate();
            mBackupClipFrame.set(mClipFrame);

            float scale = 1 / getScale();
            M.setTranslate(-mFrame.left, -mFrame.top);
            M.postScale(scale, scale);
            M.mapRect(mBackupClipFrame);

            // 重置裁剪区域
            mClipWin.reset(mClipFrame, getTargetRotate());
        } else {
            if (mMode == EditMode.MOSAIC) {
                makeMosaicBitmap();
            }
            mClipWin.setClipping(false);
        }
    }

    private void rotateStickers(float rotate) {
        M.setRotate(rotate, mClipFrame.centerX(), mClipFrame.centerY());
        for (ISticker sticker : mBackStickers) {
            M.mapRect(sticker.getFrame());
            sticker.setRotation(sticker.getRotation() + rotate);
            sticker.setX(sticker.getFrame().centerX() - sticker.getPivotX());
            sticker.setY(sticker.getFrame().centerY() - sticker.getPivotY());
        }
    }

    private void initShadePaint() {
        if (mShadePaint == null) {
            mShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mShadePaint.setColor(COLOR_SHADE);
            mShadePaint.setStyle(Paint.Style.FILL);
        }
    }

    public boolean isMosaicEmpty() {
        return mMosaicList.isEmpty();
    }

    public boolean isDoodleEmpty() {
        return mDoodleList.isEmpty();
    }

    public void undo() {
        if (!mHistoryList.isEmpty()) {
            PaintPath removePath = mHistoryList.remove(mHistoryList.size() - 1);
            if (removePath.getMode() == EditMode.DOODLE) {
                mDoodleList.remove(removePath);
            } else if (removePath.getMode() == EditMode.MOSAIC) {
                mMosaicList.remove(removePath);
            }
        }
    }

    public RectF getClipFrame() {
        return mClipFrame;
    }

    /**
     * 裁剪区域旋转回原始角度后形成新的裁剪区域，旋转中心发生变化，
     * 因此需要将视图窗口平移到新的旋转中心位置。
     */
    public EditState clip(float scrollX, float scrollY) {
        RectF frame = mClipWin.getOffsetFrame(scrollX, scrollY);

        M.setRotate(-getRotate(), mClipFrame.centerX(), mClipFrame.centerY());
        M.mapRect(mClipFrame, frame);

        return new EditState(
                scrollX + (mClipFrame.centerX() - frame.centerX()),
                scrollY + (mClipFrame.centerY() - frame.centerY()),
                getScale(), getRotate()
        );
    }

    public void toBackupClip() {
        M.setScale(getScale(), getScale());
        M.postTranslate(mFrame.left, mFrame.top);
        M.mapRect(mClipFrame, mBackupClipFrame);
        setTargetRotate(mBackupClipRotate);
        isRequestToBaseFitting = true;
    }

    public void resetClip() {
        // TODO 就近旋转
        setTargetRotate(getRotate() - getRotate() % 360);
        mClipFrame.set(mFrame);
        mClipWin.reset(mClipFrame, getTargetRotate());
    }

    private void makeMosaicBitmap() {
        if (mMosaicLayer != null || mDoodleLayer == null) {
            return;
        }
        if (mMode == EditMode.MOSAIC) {
            int w = Math.round(mDoodleLayer.getWidth() / 12f);
            int h = Math.round(mDoodleLayer.getHeight() / 12f);
            w = Math.max(w, 8);
            h = Math.max(h, 8);
            // 马赛克画刷
            if (mMosaicPaint == null) {
                mMosaicPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mMosaicPaint.setFilterBitmap(false);
                mMosaicPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            }
            mMosaicLayer = Bitmap.createScaledBitmap(mDoodleLayer, w, h, false);
        }
    }

    private void onImageChanged() {
        isInitial = false;
        onWindowChanged(mWindow.width(), mWindow.height());
        if (mMode == EditMode.CLIP) {
            mClipWin.reset(mClipFrame, getTargetRotate());
        }
    }

    public RectF getFrame() {
        return mFrame;
    }

    public boolean onClipSteady() {
        return mClipWin.onClipSteady();
    }

    public EditState getStartEditState(float scrollX, float scrollY) {
        return new EditState(scrollX, scrollY, getScale(), getRotate());
    }

    public EditState getEndEditState(float scrollX, float scrollY) {
        EditState state = new EditState(scrollX, scrollY, getScale(), getTargetRotate());

        if (mMode == EditMode.CLIP) {
            RectF frame = new RectF(mClipWin.getTargetFrame());
            frame.offset(scrollX, scrollY);
            if (mClipWin.isResetting()) {

                RectF clipFrame = new RectF();
                M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY());
                M.mapRect(clipFrame, mClipFrame);

                state.rConcat(EditUtils.fill(frame, clipFrame));
            } else {
                RectF cFrame = new RectF();

                if (mClipWin.isEditAnimRunning()) {
                    // TODO 偏移中心
                    M.setRotate(getTargetRotate() - getRotate(), mClipFrame.centerX(), mClipFrame.centerY());
                    M.mapRect(cFrame, mClipWin.getOffsetFrame(scrollX, scrollY));
                    state.rConcat(EditUtils.fitHoming(frame, cFrame, mClipFrame.centerX(), mClipFrame.centerY()));
                } else {
                    M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY());
                    M.mapRect(cFrame, mFrame);
                    state.rConcat(EditUtils.fillHoming(frame, cFrame, mClipFrame.centerX(), mClipFrame.centerY()));
                }
            }
        } else {
            RectF clipFrame = new RectF();
            M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY());
            M.mapRect(clipFrame, mClipFrame);

            RectF win = new RectF(mWindow);
            win.offset(scrollX, scrollY);
            state.rConcat(EditUtils.fitHoming(win, clipFrame, isRequestToBaseFitting));
            isRequestToBaseFitting = false;
        }
        return state;
    }

    public void addSticker(ISticker sticker) {
        if (sticker != null) {
            moveToForeground(sticker);
        }
    }

    public void addPath(PaintPath path, float sx, float sy) {
        if (path == null) return;
        float scale = 1f / getScale();

        M.setTranslate(sx, sy);
        M.postRotate(-getRotate(), mClipFrame.centerX(), mClipFrame.centerY());
        M.postTranslate(-mFrame.left, -mFrame.top);
        M.postScale(scale, scale);
        path.transform(M);
        switch (path.getMode()) {
            case DOODLE:
                path.setWidth(path.getWidth() * scale);
                mDoodleList.add(path);
                mHistoryList.add(path);
                break;
            case MOSAIC:
                path.setWidth(path.getWidth() * scale);
                mMosaicList.add(path);
                mHistoryList.add(path);
                break;
        }
    }

    private void moveToForeground(ISticker sticker) {
        if (sticker == null) return;

        moveToBackground(mForeSticker);

        if (sticker.isShowing()) {
            mForeSticker = sticker;
            // 从BackStickers中移除
            mBackStickers.remove(sticker);
        } else sticker.show();
    }

    private void moveToBackground(ISticker sticker) {
        if (sticker == null) return;

        if (!sticker.isShowing()) {
            // 加入BackStickers中
            if (!mBackStickers.contains(sticker)) {
                mBackStickers.add(sticker);
            }

            if (mForeSticker == sticker) {
                mForeSticker = null;
            }
        } else sticker.dismiss();
    }

    public void stickAll() {
        moveToBackground(mForeSticker);
    }

    public void onDismiss(ISticker sticker) {
        moveToBackground(sticker);
    }

    public void onShowing(ISticker sticker) {
        if (mForeSticker != sticker) {
            moveToForeground(sticker);
        }
    }

    public void onRemoveSticker(ISticker sticker) {
        if (mForeSticker == sticker) {
            mForeSticker = null;
        } else {
            mBackStickers.remove(sticker);
        }
    }

    public void onWindowChanged(float width, float height) {
        if (width == 0 || height == 0) {
            return;
        }

        mWindow.set(0, 0, width, height);

        if (!isInitial) {
            onInitialFrame(width, height);
        } else {
            // Pivot to fit window.
            M.setTranslate(mWindow.centerX() - mClipFrame.centerX(), mWindow.centerY() - mClipFrame.centerY());
            M.mapRect(mFrame);
            M.mapRect(mClipFrame);
        }
        mClipWin.setClipWinSize(width, height);
    }

    private void onInitialFrame(float width, float height) {
        mFrame.set(0, 0, mDoodleLayer.getWidth(), mDoodleLayer.getHeight());
        mClipFrame.set(mFrame);
        mClipWin.setClipWinSize(width, height);

        if (mClipFrame.isEmpty()) {
            return;
        }
        toBaseFrame();
        isInitial = true;
        onInitialFrameDone();
    }

    private void toBaseFrame() {
        if (mClipFrame.isEmpty()) {
            // Bitmap invalidate.
            return;
        }

        float scale = Math.min(
                mWindow.width() / mClipFrame.width(),
                mWindow.height() / mClipFrame.height()
        );

        // Scale to fit window.
        M.setScale(scale, scale, mClipFrame.centerX(), mClipFrame.centerY());
        M.postTranslate(mWindow.centerX() - mClipFrame.centerX(), mWindow.centerY() - mClipFrame.centerY());
        M.mapRect(mFrame);
        M.mapRect(mClipFrame);
    }

    private void onInitialFrameDone() {
        if (mMode == EditMode.CLIP) {
            mClipWin.reset(mClipFrame, getTargetRotate());
        }
    }

    public void onDrawImage(Canvas canvas) {
        if (mDoodleLayer == null){
            return;
        }
        // 裁剪区域
        canvas.clipRect(mClipWin.isClipping() ? mFrame : mClipFrame);
        // 绘制图片
        canvas.drawBitmap(mDoodleLayer, null, mFrame, null);
        if (DEBUG) {
            // Clip 区域
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(6);
            canvas.drawRect(mFrame, mPaint);
            canvas.drawRect(mClipFrame, mPaint);
        }
    }

    public int onDrawMosaicsPath(Canvas canvas) {
        int layerCount = canvas.saveLayer(mFrame, null, Canvas.ALL_SAVE_FLAG);

        if (!isMosaicEmpty()) {
            canvas.save();
            float scale = getScale();
            canvas.translate(mFrame.left, mFrame.top);
            canvas.scale(scale, scale);
            for (PaintPath path : mMosaicList) {
                path.onDrawMosaic(canvas, mPaint);
            }
            canvas.restore();
        }

        return layerCount;
    }

    public void onDrawMosaic(Canvas canvas, int layerCount) {
        canvas.drawBitmap(mMosaicLayer, null, mFrame, mMosaicPaint);
        canvas.restoreToCount(layerCount);
    }

    public void onDrawDoodles(Canvas canvas) {
        if (!isDoodleEmpty()) {
            canvas.save();
            float scale = getScale();
            canvas.translate(mFrame.left, mFrame.top);
            canvas.scale(scale, scale);
            for (PaintPath path : mDoodleList) {
                path.onDrawDoodle(canvas, mPaint);
            }
            canvas.restore();
        }
    }

    public void onDrawStickerClip(Canvas canvas) {
        M.setRotate(getRotate(), mClipFrame.centerX(), mClipFrame.centerY());
        M.mapRect(mTempClipFrame, mClipWin.isClipping() ? mFrame : mClipFrame);
        canvas.clipRect(mTempClipFrame);
    }

    public void onDrawStickers(Canvas canvas, boolean drawOutFrame) {
        if (mBackStickers.isEmpty() && mForeSticker == null) {
            return;
        }
        if (!drawOutFrame) {
            canvas.save();
            onDrawStickerClip(canvas);
        }
        Rect bounds = canvas.getClipBounds();
        final int canvasUsefulHeight = bounds.height();
        for (ISticker sticker : mBackStickers) {
            if (!sticker.isShowing() && !sticker.equals(mForeSticker)) {
                sticker.onDrawSticker(canvas, canvasUsefulHeight);
            }
        }
        if (!drawOutFrame) {
            canvas.restore();
        }
        if (mForeSticker != null) {// 被选中的sticker可以绘制在bitmap区域以外
            mForeSticker.onDrawSticker(canvas, canvasUsefulHeight);
        }
    }

    public void onDrawShade(Canvas canvas) {
        if (mMode == EditMode.CLIP && isSteady) {
            mShade.reset();
            mShade.addRect(mFrame.left - 2, mFrame.top - 2, mFrame.right + 2, mFrame.bottom + 2, Path.Direction.CW);
            mShade.addRect(mClipFrame, Path.Direction.CCW);
            canvas.drawPath(mShade, mShadePaint);
        }
    }

    public void onDrawClip(Canvas canvas, float scrollX, float scrollY) {
        if (mMode == EditMode.CLIP) {
            mClipWin.onDraw(canvas);
        }
    }

    public void onTouchDown(float x, float y) {
        isSteady = false;
        moveToBackground(mForeSticker);
        if (mMode == EditMode.CLIP) {
            mAnchor = mClipWin.getAnchor(x, y);
        }
    }

    public void onTouchUp(float scrollX, float scrollY) {
        if (mAnchor != null) {
            mAnchor = null;
        }
    }

    public void onSteady(float scrollX, float scrollY) {
        isSteady = true;
        onClipSteady();
        mClipWin.setShowShade(true);
    }

    public void onScaleBegin() {

    }

    public EditState onScroll(float scrollX, float scrollY, float dx, float dy) {
        if (mMode == EditMode.CLIP) {
            mClipWin.setShowShade(false);
            if (mAnchor != null) {
                mClipWin.onScroll(mAnchor, dx, dy);

                RectF clipFrame = new RectF();
                M.setRotate(getRotate(), mClipFrame.centerX(), mClipFrame.centerY());
                M.mapRect(clipFrame, mFrame);

                RectF frame = mClipWin.getOffsetFrame(scrollX, scrollY);
                EditState state = new EditState(scrollX, scrollY, getScale(), getTargetRotate());
                state.rConcat(EditUtils.fillHoming(frame, clipFrame, mClipFrame.centerX(), mClipFrame.centerY()));
                return state;
            }
        }
        return null;
    }

    public float getTargetRotate() {
        return mTargetRotate;
    }

    public void setTargetRotate(float targetRotate) {
        this.mTargetRotate = targetRotate;
    }

    /**
     * 在当前基础上旋转
     */
    public void rotate(int rotate) {
        mTargetRotate = Math.round((mRotate + rotate) / 90f) * 90;
        mClipWin.reset(mClipFrame, getTargetRotate());
    }

    public float getRotate() {
        return mRotate;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
    }

    public float getScale() {
        return 1f * mFrame.width() / mDoodleLayer.getWidth();
    }

    public void setScale(float scale) {
        setScale(scale, mClipFrame.centerX(), mClipFrame.centerY());
    }

    public float getBaseScale() {
        return mWindow.width() / mDoodleLayer.getWidth();
    }

    public void setScale(float scale, float focusX, float focusY) {
        onScale(scale / getScale(), focusX, focusY);
    }

    public void onScale(float factor, float focusX, float focusY) {

        if (factor == 1f) return;

        if (Math.max(mClipFrame.width(), mClipFrame.height()) >= MAX_SIZE
                || Math.min(mClipFrame.width(), mClipFrame.height()) <= MIN_SIZE) {
            factor += (1 - factor) / 2;
        }
        // 限制最大缩放倍数
        float maxScale = PictureEditor.getInstance().getMaxScale();
        if (getScale() * factor > maxScale * getBaseScale()) {
            factor = maxScale * getBaseScale() / getScale();
            if (factor == 1f) {
                return;
            }
        }
        M.setScale(factor, factor, focusX, focusY);
        M.mapRect(mFrame);
        M.mapRect(mClipFrame);

        // 修正clip 窗口
        if (!mFrame.contains(mClipFrame)) {
            // TODO
//            mClipFrame.intersect(mFrame);
        }

        for (ISticker sticker : mBackStickers) {
            if (!sticker.equals(mForeSticker)) {
                sticker.onGestureScale(M, factor);
            }
        }
        if (mForeSticker != null) {
            mForeSticker.onGestureScale(M, factor);
        }
    }

    public void onScaleEnd() {

    }

    public void onEditAnimStart(boolean isRotate) {
        isAnimCanceled = false;
    }

    public void onEditAnimationUpdate(float fraction) {
        mClipWin.onEditAnimationUpdate(fraction);
    }

    public boolean onEditAnimEnd(float scrollX, float scrollY, boolean isRotate) {
        if (mMode == EditMode.CLIP) {
            // 开启裁剪模式

            boolean clip = !isAnimCanceled;

            mClipWin.setEditAnimRunning(false);
            mClipWin.setClipping(true);
            mClipWin.setResetting(false);

            return clip;
        } else {
            if (isClipMode && !isAnimCanceled) {
                setClipMode(false);
            }
        }
        return false;
    }

    public boolean isClipMode() {
        return isClipMode;
    }

    private void setClipMode(boolean clipMode) {
        if (clipMode != isClipMode) {
            rotateStickers(clipMode ? -getRotate() : getTargetRotate());
            isClipMode = clipMode;
        }
    }

    public void onEditAnimCancel(boolean isRotate) {
        isAnimCanceled = true;
    }

    public void release() {
        Utils.recycleBitmap(mDoodleLayer);
        Utils.recycleBitmap(mMosaicLayer);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (DEFAULT_IMAGE != null) {
            DEFAULT_IMAGE.recycle();
        }
    }
}
