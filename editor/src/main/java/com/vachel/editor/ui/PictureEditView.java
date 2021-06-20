package com.vachel.editor.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.vachel.editor.EditMode;
import com.vachel.editor.PictureEditor;
import com.vachel.editor.anim.EditAnimator;
import com.vachel.editor.bean.DrawPen;
import com.vachel.editor.bean.EditState;
import com.vachel.editor.bean.StickerText;
import com.vachel.editor.clip.EditClipWindow;
import com.vachel.editor.IEditSave;
import com.vachel.editor.sticker.IStickerParent;
import com.vachel.editor.ui.sticker.ImageStickerView;
import com.vachel.editor.ui.sticker.StickerView;
import com.vachel.editor.ui.sticker.TextStickerView;
import com.vachel.editor.ui.widget.ColorRadio;
import com.vachel.editor.util.Utils;

public class PictureEditView extends FrameLayout implements Runnable, ScaleGestureDetector.OnScaleGestureListener,
        ValueAnimator.AnimatorUpdateListener, IStickerParent, Animator.AnimatorListener {

    private EditMode mPreMode = EditMode.NONE;
    private final EditPresenter mPicPresenter = new EditPresenter();
    private final Paint mDoodlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mMosaicPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DrawPen mDrawPen = new DrawPen();
    private IOnPathListener mPathListener;
    private GestureDetector mGDetector;
    private ScaleGestureDetector mSGDetector;

    private EditAnimator mEditAnimator;
    private int mPointerCount = 0;

    public PictureEditView(Context context) {
        this(context, null, 0);
    }

    public PictureEditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PictureEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        mDrawPen.setMode(mPicPresenter.getMode());
        mGDetector = new GestureDetector(context, new MoveAdapter());
        mSGDetector = new ScaleGestureDetector(context, this);

        // 涂鸦画刷
        mDoodlePaint.setStyle(Paint.Style.STROKE);
        mDoodlePaint.setStrokeWidth(PictureEditor.getInstance().getDefaultDoodleWidth());
        mDoodlePaint.setColor(Color.RED);
        mDoodlePaint.setPathEffect(new CornerPathEffect(PictureEditor.getInstance().getDefaultDoodleWidth()));
        mDoodlePaint.setStrokeCap(Paint.Cap.ROUND);
        mDoodlePaint.setStrokeJoin(Paint.Join.ROUND);

        // 马赛克画刷
        mMosaicPaint.setStyle(Paint.Style.STROKE);
        mMosaicPaint.setStrokeWidth(PictureEditor.getInstance().getDefaultMosaicWidth());
        mMosaicPaint.setColor(Color.BLACK);
        mMosaicPaint.setPathEffect(new CornerPathEffect(PictureEditor.getInstance().getDefaultMosaicWidth()));
        mMosaicPaint.setStrokeCap(Paint.Cap.ROUND);
        mMosaicPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setImageBitmap(Bitmap image) {
        mPicPresenter.setBitmap(image);
        invalidate();
    }

    public void setMode(EditMode mode) {
        mPreMode = mPicPresenter.getMode();
        mPicPresenter.setMode(mode);
        mDrawPen.setMode(mode);
        // 矫正区域
        onClipFrameChanged();
    }

    /**
     * 是否真正修正归位
     */
    boolean isEditAnimRunning() {
        return mEditAnimator != null
                && mEditAnimator.isRunning();
    }

    private void onClipFrameChanged() {
        invalidate();
        stopAnim();
        startAnim(mPicPresenter.getStartEditState(getScrollX(), getScrollY()),
                mPicPresenter.getEndEditState(getScrollX(), getScrollY()));
    }

    private void startAnim(EditState sState, EditState eState) {
        if (mEditAnimator == null) {
            mEditAnimator = new EditAnimator();
            mEditAnimator.addUpdateListener(this);
            mEditAnimator.addListener(this);
        }
        mEditAnimator.setValues(sState, eState);
        mEditAnimator.start();
    }

    private void stopAnim() {
        if (mEditAnimator != null) {
            mEditAnimator.cancel();
        }
    }

    public void doRotate() {
        if (!isEditAnimRunning()) {
            mPicPresenter.rotate(-90);
            onClipFrameChanged();
        }
    }

    public void resetClip() {
        mPicPresenter.resetClip();
        onClipFrameChanged();
    }

    public void doClip() {
        mPicPresenter.clip(getScrollX(), getScrollY());
        setMode(mPreMode);
        onClipFrameChanged();
    }

    public void cancelClip() {
        mPicPresenter.toBackupClip();
        setMode(mPreMode);
    }

    public void setPenColor(int color) {
        boolean isMosaicType = color == ColorRadio.TYPE_MOSAIC_COLOR;
        if (isMosaicType) {
            if (getMode() == EditMode.DOODLE) {
                setMode(EditMode.MOSAIC);
            }
        } else {
            if (getMode() == EditMode.MOSAIC) {
                setMode(EditMode.DOODLE);
            }
            mDrawPen.setColor(color);
        }
    }

    public void undo() {
        mPicPresenter.undo();
        invalidate();
    }

    public EditMode getMode() {
        return mPicPresenter.getMode();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawImages(canvas);
    }

    private void onDrawImages(Canvas canvas) {
        canvas.save();
        // 旋转指定角度
        RectF clipFrame = mPicPresenter.getClipFrame();
        canvas.rotate(mPicPresenter.getRotate(), clipFrame.centerX(), clipFrame.centerY());
        // 图片
        mPicPresenter.onDrawImage(canvas);

        // 马赛克
        if (!mPicPresenter.isMosaicEmpty() || (mPicPresenter.getMode() == EditMode.MOSAIC && !mDrawPen.isEmpty())) {
            int count = mPicPresenter.onDrawMosaicsPath(canvas);
            if (mPicPresenter.getMode() == EditMode.MOSAIC && !mDrawPen.isEmpty()) {
                mMosaicPaint.setStrokeWidth(PictureEditor.getInstance().getDefaultMosaicWidth());
                onDrawTouchPath(canvas, mDrawPen.getPath(), mMosaicPaint);
            }
            mPicPresenter.onDrawMosaic(canvas, count);
        }

        // 涂鸦
        mPicPresenter.onDrawDoodles(canvas);
        if (mPicPresenter.getMode() == EditMode.DOODLE && !mDrawPen.isEmpty()) {// 手指正在画的涂鸦
            mDoodlePaint.setColor(mDrawPen.getColor());
            mDoodlePaint.setStrokeWidth(PictureEditor.getInstance().getDefaultDoodleWidth());
            onDrawTouchPath(canvas, mDrawPen.getPath(), mDoodlePaint);
        }

        if (mPicPresenter.isClipMode()) {
            mPicPresenter.onDrawStickers(canvas, true);   // 文字贴片
            mPicPresenter.onDrawShade(canvas);
            canvas.restore();

            // 裁剪窗口
            canvas.save();
            canvas.translate(getScrollX(), getScrollY());
            mPicPresenter.onDrawClip(canvas, getScrollX(), getScrollY());
            canvas.restore();
        } else {
            canvas.restore();
            // 贴图绘制范围不同，区别于上面个onDrawStickers
            mPicPresenter.onDrawStickers(canvas, false);
        }
    }

    public void onDrawTouchPath(Canvas canvas, Path path, Paint paint) {
        canvas.save();
        RectF frame = mPicPresenter.getClipFrame();
        canvas.rotate(-mPicPresenter.getRotate(), frame.centerX(), frame.centerY());
        canvas.translate(getScrollX(), getScrollY());
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void setClipWindowRender(EditClipWindow.IClipRender clipRender) {
        mPicPresenter.setClipWindowRender(clipRender);
    }

    public boolean saveEdit(@NonNull IEditSave callback) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            String savePath = Utils.saveBitmap(getContext(), bitmap);
            Utils.recycleBitmap(bitmap);
            if (!TextUtils.isEmpty(savePath)) {
                callback.onSaveSuccess(savePath);
                return true;
            }
        }
        callback.onSaveFailed();
        return false;
    }

    @NonNull
    public Bitmap getBitmap() {
        mPicPresenter.stickAll();
        float scale = 1f / mPicPresenter.getScale();
        RectF frame = new RectF(mPicPresenter.getClipFrame());

        // 旋转基画布
        Matrix m = new Matrix();
        m.setRotate(mPicPresenter.getRotate(), frame.centerX(), frame.centerY());
        m.mapRect(frame);

        // 缩放基画布
        m.setScale(scale, scale, frame.left, frame.top);
        m.mapRect(frame);
        Bitmap bitmap = Bitmap.createBitmap(Math.round(frame.width()),
                Math.round(frame.height()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // 平移到基画布原点&缩放到原尺寸
        canvas.translate(-frame.left, -frame.top);
        canvas.scale(scale, scale, frame.left, frame.top);
        onDrawImages(canvas);
        return bitmap;
    }

    public void addStickerView(StickerView stickerView, LayoutParams params) {
        if (stickerView != null) {
            addView(stickerView, params);
            stickerView.registerCallback(this);
            mPicPresenter.addSticker(stickerView);
        }
    }

    public void addStickerImage() {
        ImageStickerView imageView = new ImageStickerView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        imageView.setX(getScrollX());
        imageView.setY(getScrollY());
        addStickerView(imageView, layoutParams);
    }

    public void addStickerText(StickerText text, boolean enableEdit) {
        TextStickerView textView = new TextStickerView(getContext());
        textView.onText(text, enableEdit);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        textView.setX(getScrollX());
        textView.setY(getScrollY());
        addStickerView(textView, layoutParams);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mPicPresenter.onWindowChanged(right - left, bottom - top);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return onInterceptTouch(ev) || super.onInterceptTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean onInterceptTouch(MotionEvent event) {
        if (isEditAnimRunning()) {
            stopAnim();
            return true;
        } else {
            return mPicPresenter.getMode() == EditMode.CLIP;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(this);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                postDelayed(this, 800);
                break;
        }
        return onTouch(event);
    }

    boolean onTouch(MotionEvent event) {
        if (isEditAnimRunning()) {
            return false;
        }
        mPointerCount = event.getPointerCount();
        boolean handled = mSGDetector.onTouchEvent(event);
        EditMode mode = mPicPresenter.getMode();
        if (mode == EditMode.NONE || mode == EditMode.CLIP) {
            handled |= onTouchNONE(event);
        } else if (mPointerCount > 1) {
            onPathDone();
            handled |= onTouchNONE(event);
        } else {
            handled |= onTouchPath(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPicPresenter.onTouchDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mPicPresenter.onTouchUp(getScrollX(), getScrollY());
                onClipFrameChanged();
                break;
        }
        return handled;
    }

    private boolean onTouchNONE(MotionEvent event) {
        return mGDetector.onTouchEvent(event);
    }

    private boolean onTouchPath(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDrawPen.reset(event.getX(), event.getY());
                mDrawPen.setIdentity(event.getPointerId(0));
                if (mPathListener != null) {
                    mPathListener.onPathStart();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mDrawPen.isIdentity(event.getPointerId(0))) {
                    mDrawPen.lineTo(event.getX(), event.getY());
                    invalidate();
                    return true;
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mPathListener != null) {
                    mPathListener.onPathEnd();
                }
                return onPathDone();
        }
        return false;
    }

    private boolean onPathDone() {
        if (!mDrawPen.isValidPath()) {
            return false;
        }
        mPicPresenter.addPath(mDrawPen.toPath(), getScrollX(), getScrollY());
        mDrawPen.reset();

        invalidate();
        return true;
    }

    public void setOnPathListener(IOnPathListener listener) {
        mPathListener = listener;
    }

    public interface IOnPathListener {
        void onPathStart();

        void onPathEnd();
    }

    @Override
    public void run() {
        // 稳定触发
        if (!onSteady()) {
            postDelayed(this, 500);
        }
    }

    boolean onSteady() {
        if (!isEditAnimRunning()) {
            mPicPresenter.onSteady(getScrollX(), getScrollY());
            onClipFrameChanged();
            return true;
        }
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
        mPicPresenter.release();
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mPointerCount > 1) {
            mPicPresenter.onScale(detector.getScaleFactor(),
                    getScrollX() + detector.getFocusX(),
                    getScrollY() + detector.getFocusY());
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (mPointerCount > 1) {
            mPicPresenter.onScaleBegin();
            return true;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mPicPresenter.onScaleEnd();
    }

    private void toApplyEditState(EditState state) {
        mPicPresenter.setScale(state.scale);
        mPicPresenter.setRotate(state.rotate);
        if (!onScrollTo(Math.round(state.x), Math.round(state.y))) {
            invalidate();
        }
    }

    private boolean onScrollTo(int x, int y) {
        if (getScrollX() != x || getScrollY() != y) {
            scrollTo(x, y);
            return true;
        }
        return false;
    }

    @Override
    public void onDismiss(StickerView stickerView) {
        mPicPresenter.onDismiss(stickerView);
        invalidate();
    }

    @Override
    public void onShowing(StickerView stickerView) {
        mPicPresenter.onShowing(stickerView);
        invalidate();
    }

    @Override
    public boolean onRemove(StickerView stickerView) {
        if (mPicPresenter != null) {
            mPicPresenter.onRemoveSticker(stickerView);
        }
        stickerView.unregisterCallback(this);
        ViewParent parent = stickerView.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(stickerView);
        }
        return true;
    }

    @Override
    public void onLayerChanged(final StickerView stickerView) {
        int childCount = getChildCount();
        if (childCount > 1) {
            View topChild = getChildAt(childCount - 1);
            // 编辑中的图片不在最顶层时需要挪到最上层
            if (topChild != stickerView) {
                stickerView.bringToFront();
                stickerView.requestLayout();
            }
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mPicPresenter.onEditAnimationUpdate(animation.getAnimatedFraction());
        toApplyEditState((EditState) animation.getAnimatedValue());
    }

    @Override
    public void onAnimationStart(Animator animation) {
        mPicPresenter.onEditAnimStart(mEditAnimator.isRotate());
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mPicPresenter.onEditAnimEnd(getScrollX(), getScrollY(), mEditAnimator.isRotate())) {
            toApplyEditState(mPicPresenter.clip(getScrollX(), getScrollY()));
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        mPicPresenter.onEditAnimCancel(mEditAnimator.isRotate());
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // empty implementation.
    }

    private boolean onScroll(float dx, float dy) {
        EditState state = mPicPresenter.onScroll(getScrollX(), getScrollY(), -dx, -dy);
        if (state != null) {
            toApplyEditState(state);
            return true;
        }
        return onScrollTo(getScrollX() + Math.round(dx), getScrollY() + Math.round(dy));
    }

    private class MoveAdapter extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return PictureEditView.this.onScroll(distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
