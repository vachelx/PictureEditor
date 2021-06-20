package com.vachel.editor.ui.sticker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.vachel.editor.PictureEditor;
import com.vachel.editor.sticker.ISticker;
import com.vachel.editor.sticker.IStickerParent;
import com.vachel.editor.sticker.StickerFingerTouchHelper;
import com.vachel.editor.sticker.StickerMatrixTouchHelper;

public abstract class StickerView extends ViewGroup implements ISticker, View.OnClickListener {
    private static final int ANCHOR_SIZE = 48;
    private static final int ANCHOR_SIZE_HALF = ANCHOR_SIZE >> 1;

    private View mContentView;
    private float mScale = 1f;
    private int mDownShowing = 0;
    private ImageView mRemoveView, mAdjustView;
    private final Matrix mMatrix = new Matrix();
    private final RectF mFrame = new RectF();
    private final Rect mTempFrame = new Rect();
    private StickerFingerTouchHelper mTouchHelper;
    private final Matrix M = new Matrix();
    private int mUsefulHeight;
    private RectF mDrawFrame;
    private IStickerParent mIStickerParent;
    private boolean isShowing = false;
    private IRectRender mRectRender;

    public StickerView(Context context) {
        this(context, null, 0);
    }

    public StickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onInitialize(context);
    }

    public void onInitialize(Context context) {
        setBackgroundColor(Color.TRANSPARENT);
        mContentView = onCreateContentView(context);
        addView(mContentView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mRemoveView = PictureEditor.getInstance().getDeleteStickerIcon(context);
        addView(mRemoveView, getAnchorLayoutParams());
        mRemoveView.setOnClickListener(this);

        mAdjustView = PictureEditor.getInstance().getAdjustStickerIcon(context);
        addView(mAdjustView, getAnchorLayoutParams());

        new StickerMatrixTouchHelper(this, mAdjustView);
        mTouchHelper = new StickerFingerTouchHelper(this);
         // 设置padding后mRemoveView和mAdjustView会被截断，允许超出绘制
        setClipChildren(false);
        setClipToPadding(false);
    }

    public abstract View onCreateContentView(Context context);

    @Override
    public float getScale() {
        return mScale;
    }

    @Override
    public void setScale(float scale) {
        float maxScale = PictureEditor.getInstance().getMaxStickerScale();
        if (scale > maxScale) {
            scale = maxScale;
        }
        mScale = scale;

        mContentView.setScaleX(mScale);
        mContentView.setScaleY(mScale);

        int pivotX = (getLeft() + getRight()) >> 1;
        int pivotY = (getTop() + getBottom()) >> 1;

        mFrame.set(pivotX, pivotY, pivotX, pivotY);
        mFrame.inset(-(mContentView.getMeasuredWidth() >> 1), -(mContentView.getMeasuredHeight() >> 1));

        mMatrix.setScale(mScale, mScale, mFrame.centerX(), mFrame.centerY());
        mMatrix.mapRect(mFrame);

        mFrame.round(mTempFrame);

        layout(mTempFrame.left, mTempFrame.top, mTempFrame.right, mTempFrame.bottom);
        requestLayout();
    }

    @Override
    public void addScale(float scale) {
        setScale(getScale() * scale);
    }

    private LayoutParams getAnchorLayoutParams() {
        return new LayoutParams(ANCHOR_SIZE, ANCHOR_SIZE);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isShowing()) {
            getRectRender().onDraw(canvas, getWidth(), getHeight());
        }
        super.draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        int paddingHor = getPaddingLeft() + getPaddingRight();
        int paddingVer = getPaddingBottom() + getPaddingTop();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.measure(widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.round(Math.max(maxWidth, (child.getMeasuredWidth() + paddingHor) * child.getScaleX() + ANCHOR_SIZE));
                maxHeight = Math.round(Math.max(maxHeight, (child.getMeasuredHeight() + paddingVer) * child.getScaleY() + ANCHOR_SIZE));
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(maxWidth, maxHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mFrame.set(left, top, right, bottom);

        int count = getChildCount();
        if (count == 0) {
            return;
        }

        mRemoveView.layout(0, 0, mRemoveView.getMeasuredWidth(), mRemoveView.getMeasuredHeight());
        mAdjustView.layout(
                right - left - mAdjustView.getMeasuredWidth(),
                bottom - top - mAdjustView.getMeasuredHeight(),
                right - left, bottom - top
        );

        int centerX = (right - left) >> 1, centerY = (bottom - top) >> 1;
        int hw = mContentView.getMeasuredWidth() >> 1;
        int hh = mContentView.getMeasuredHeight() >> 1;

        mContentView.layout(centerX - hw, centerY - hh, centerX + hw, centerY + hh);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return isShowing() && super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isShowing() && ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownShowing = 0;
            show();
            return true;
        }
        return isShowing() && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mTouchHelper.onTouch(this, event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownShowing++;
                break;
            case MotionEvent.ACTION_UP:
                if (mDownShowing > 1 && event.getEventTime() - event.getDownTime() < ViewConfiguration.getTapTimeout()) {
                    onContentTap();
                    return true;
                }
                if (isShowing() && mIStickerParent != null) {
                    // 将当前编辑的sticker移动到顶层
                    mIStickerParent.onLayerChanged(this);
                }
                break;
        }

        return handled | super.onTouchEvent(event);
    }

    @Override
    public void onClick(View v) {
        if (v == mRemoveView) {
            remove();
        }
    }

    public void onContentTap() {

    }

    @Override
    public boolean show() {
        if (!isShowing()) {
            isShowing = true;
            invalidate();
            if (mIStickerParent != null) {
                mIStickerParent.onShowing(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove() {
        if (mIStickerParent != null) {
            return mIStickerParent.onRemove(this);
        }
        return false;
    }

    @Override
    public boolean dismiss() {
        if (isShowing()) {
            isShowing = false;
            mDrawFrame = null;
            invalidate();
            if (mIStickerParent != null) {
                mIStickerParent.onDismiss(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isShowing() {
        return isShowing;
    }

    @Override
    public RectF getFrame() {
        if (mDrawFrame == null) {
            mDrawFrame = new RectF(0, 0, getWidth(), getHeight());
            float pivotX = getX() + getPivotX();
            float pivotY = getY() + getPivotY();
            Matrix matrix = new Matrix();
            matrix.setTranslate(getX(), getY());
            matrix.postScale(getScaleX(), getScaleY(), pivotX, pivotY);
            matrix.mapRect(mDrawFrame);
        }
        return mDrawFrame;
    }

    @Override
    public void onDrawSticker(Canvas canvas, int frameHeight) {
        mUsefulHeight = frameHeight; // 可见区域高度
        float tPivotX = getX() + getPivotX();
        float tPivotY = getY() + getPivotY();
        canvas.save();
        M.setTranslate(getX(), getY());
        M.postScale(getScale(), getScale(), tPivotX, tPivotY);
        M.postRotate(getRotation(), tPivotX, tPivotY);
        canvas.concat(M);
        canvas.translate(mContentView.getX(), mContentView.getY());
        mContentView.draw(canvas);
        canvas.restore();
    }

    @Override
    public void onGestureScale(Matrix matrix, float factor) {
        matrix.mapRect(getFrame());
        float tPivotX = getX() + getPivotX();
        float tPivotY = getY() + getPivotY();
        addScale(factor);
        setX(getX() + getFrame().centerX() - tPivotX);
        setY(getY() + getFrame().centerY() - tPivotY);
    }

    @Override
    public void registerCallback(IStickerParent parent) {
        mIStickerParent = parent;
    }

    @Override
    public void unregisterCallback(IStickerParent IStickerParent) {
        mIStickerParent = null;
    }

    public boolean isMoveToOutside() {
        float currTransY = getTranslationY() - getParentScrollY();
        // 超出底图范围了，还原到初始位置
        if (currTransY < -mUsefulHeight / 2f || currTransY > mUsefulHeight / 2f) {
            return true;
        }
        return false;
    }

    public void invalidateParent() {
        if (mIStickerParent != null) {
            mIStickerParent.invalidate();
        }
    }

    private int getParentScrollY() {
        if (mIStickerParent != null) {
            return mIStickerParent.getScrollY();
        }
        return 0;
    }

    @NonNull
    private IRectRender getRectRender() {
        if (mRectRender == null) {
            mRectRender = PictureEditor.getInstance().getGlobalStickerRectRender();
            if (mRectRender == null) {
                mRectRender = new DefaultRectRender();
            }
        }
        return mRectRender;
    }

    public interface IRectRender {
        void onDraw(Canvas canvas, int width, int height);
    }

    public static class DefaultRectRender implements StickerView.IRectRender {
        private static final float STROKE_WIDTH = 3f;
        private Paint mPaint;

        public DefaultRectRender() {
            initPaint();
        }

        @Override
        public void onDraw(Canvas canvas, int width, int height) {
            canvas.drawRect(ANCHOR_SIZE_HALF, ANCHOR_SIZE_HALF,
                    width - ANCHOR_SIZE_HALF,
                    height - ANCHOR_SIZE_HALF, mPaint);
        }

        private void initPaint() {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(STROKE_WIDTH);
        }
    }
}
