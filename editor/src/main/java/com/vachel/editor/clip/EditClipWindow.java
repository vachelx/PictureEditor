package com.vachel.editor.clip;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

import com.vachel.editor.PictureEditor;
import com.vachel.editor.util.EditUtils;

public class EditClipWindow {
    /**
     * 裁剪区域
     */
    private final RectF mFrame = new RectF();
    private final RectF mBaseFrame = new RectF();
    private final RectF mTargetFrame = new RectF();
    /**
     * 裁剪窗口
     */
    private final RectF mWinFrame = new RectF();
    private final RectF mWin = new RectF();

    private boolean isClipping = false;
    private boolean isResetting = true;
    private boolean isShowShade = false;
    private boolean isEditAnimRunning = false;

    private final Matrix M = new Matrix();

    private final Path mShadePath = new Path();

    /**
     * 垂直窗口比例
     */
    private static final float VERTICAL_RATIO = 0.8f;
    private IClipRender mWindowRender;

    public EditClipWindow() {
    }

    /**
     * 计算裁剪窗口区域
     */
    public void setClipWinSize(float width, float height) {
        mWin.set(0, 0, width, height);
        mWinFrame.set(0, 0, width, height * VERTICAL_RATIO);

        if (!mFrame.isEmpty()) {
            EditUtils.fitFrameCenter(mWinFrame, mFrame);
            mTargetFrame.set(mFrame);
        }
    }

    public void reset(RectF clipImage, float rotate) {
        RectF imgRect = new RectF();
        M.setRotate(rotate, clipImage.centerX(), clipImage.centerY());
        M.mapRect(imgRect, clipImage);
        reset(imgRect.width(), imgRect.height());
    }

    /**
     * 重置裁剪
     */
    private void reset(float clipWidth, float clipHeight) {
        setResetting(true);
        mFrame.set(0, 0, clipWidth, clipHeight);
        EditUtils.fitCenter(mWin, mFrame, getRender().getClipMargin());
        mTargetFrame.set(mFrame);
    }

    public boolean onClipSteady() {
        mBaseFrame.set(mFrame);
        mTargetFrame.set(mFrame);
        EditUtils.fitCenter(mWin, mTargetFrame, getRender().getClipMargin());
        return isEditAnimRunning = !mTargetFrame.equals(mBaseFrame);
    }

    public void onEditAnimationUpdate(float fraction) {
        if (isEditAnimRunning) {
            mFrame.set(
                    mBaseFrame.left + (mTargetFrame.left - mBaseFrame.left) * fraction,
                    mBaseFrame.top + (mTargetFrame.top - mBaseFrame.top) * fraction,
                    mBaseFrame.right + (mTargetFrame.right - mBaseFrame.right) * fraction,
                    mBaseFrame.bottom + (mTargetFrame.bottom - mBaseFrame.bottom) * fraction
            );
        }
    }

    public boolean isEditAnimRunning() {
        return isEditAnimRunning;
    }

    public void setEditAnimRunning(boolean editAnimRunning) {
        isEditAnimRunning = editAnimRunning;
    }

    public boolean isClipping() {
        return isClipping;
    }

    public void setClipping(boolean clipping) {
        isClipping = clipping;
    }

    public boolean isResetting() {
        return isResetting;
    }

    public void setResetting(boolean resetting) {
        isResetting = resetting;
    }

    public RectF getFrame() {
        return mFrame;
    }

    public RectF getWinFrame() {
        return mWinFrame;
    }

    public RectF getOffsetFrame(float offsetX, float offsetY) {
        RectF frame = new RectF(mFrame);
        frame.offset(offsetX, offsetY);
        return frame;
    }

    public RectF getTargetFrame() {
        return mTargetFrame;
    }

    public RectF getOffsetTargetFrame(float offsetX, float offsetY) {
        RectF targetFrame = new RectF(mFrame);
        targetFrame.offset(offsetX, offsetY);
        return targetFrame;
    }

    public boolean isShowShade() {
        return isShowShade;
    }

    public void setShowShade(boolean showShade) {
        isShowShade = showShade;
    }

    public void onDraw(Canvas canvas) {

        if (isResetting) {
            return;
        }

        getRender().onDraw(canvas, mFrame);
    }

//    public void onDrawShade(Canvas canvas) {
//        if (!isShowShade) return;
//
//        // 计算遮罩图形
//        mShadePath.reset();
//
//        mShadePath.setFillType(Path.FillType.WINDING);
//        mShadePath.addRect(mFrame.left + 100, mFrame.top + 100, mFrame.right - 100, mFrame.bottom - 100, Path.Direction.CW);
//
//        mPaint.setColor(COLOR_SHADE);
//        mPaint.setStyle(Paint.Style.FILL);
//        canvas.drawPath(mShadePath, mPaint);
//    }

    public Anchor getAnchor(float x, float y) {
        float cornerSize = getRender().getCornerSize();
        if (Anchor.isCohesionContains(mFrame, -cornerSize, x, y)
                && !Anchor.isCohesionContains(mFrame, cornerSize, x, y)) {
            int v = 0;
            float[] cohesion = Anchor.cohesion(mFrame, 0);
            float[] pos = {x, y};
            for (int i = 0; i < cohesion.length; i++) {
                if (Math.abs(cohesion[i] - pos[i >> 1]) < cornerSize) {
                    v |= 1 << i;
                }
            }

            Anchor anchor = Anchor.valueOf(v);
            if (anchor != null) {
                isEditAnimRunning = false;
            }
            return anchor;
        }
        return null;
    }

    public void onScroll(Anchor anchor, float dx, float dy) {
        anchor.move(mWin, mFrame, dx, dy, getRender());
    }

    /**
     * 优先使用全局设置的自定义设置的render，其次是全局设置的，都没有时使用默认的render
     */
    private IClipRender getRender() {
        if (mWindowRender == null) {
            mWindowRender = PictureEditor.getInstance().getGlobalClipWindowRender();
            if (mWindowRender == null) {
                mWindowRender = new DefaultClipWindowRender();
            }
        }
        return mWindowRender;
    }

    public void setRender(IClipRender clipRender) {
        mWindowRender = clipRender;
    }

    public interface IClipRender {
        float getCornerSize();

        float getClipMargin();

        void onDraw(Canvas canvas, RectF frame);
    }
}
