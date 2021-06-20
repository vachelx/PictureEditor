package com.vachel.editor.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RadioButton;

import com.vachel.editor.R;

public class ColorRadio extends RadioButton implements ValueAnimator.AnimatorUpdateListener {
    public static final int TYPE_MOSAIC_COLOR = 0; // 这里将透明定义为马赛克
    private int mColor = Color.WHITE;
    private int mStrokeColor = Color.WHITE;
    private float mRadiusRatio = 0f;
    private ValueAnimator mAnimator;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float RADIUS_BASE = 0.7f;
    private static final float RADIUS_RING = 0.9f;
    private static final float RADIUS_BALL = 0.75f;

    public ColorRadio(Context context) {
        this(context, null, 0);
    }

    public ColorRadio(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public ColorRadio(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorRadio);

        mColor = a.getColor(R.styleable.ColorRadio_image_color, Color.WHITE);
        mStrokeColor = a.getColor(R.styleable.ColorRadio_image_stroke_color, Color.WHITE);

        a.recycle();

        setButtonDrawable(null);

        mPaint.setColor(mColor);
        mPaint.setStrokeWidth(5f);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private ValueAnimator getAnimator() {
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofFloat(0f, 1f);
            mAnimator.addUpdateListener(this);
            mAnimator.setDuration(200);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        }
        return mAnimator;
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(mColor);
    }

    public int getColor() {
        return mColor;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float hw = getWidth() / 2f, hh = getHeight() / 2f;
        float radius = Math.min(hw, hh);

        canvas.save();
        mPaint.setStyle(Paint.Style.FILL);
        if (mColor == TYPE_MOSAIC_COLOR) {
            // 绘制马赛克
            drawMosaic(canvas, hw, hh, radius);
        } else {
            mPaint.setStrokeWidth(5);
            mPaint.setColor(mColor);
            canvas.drawCircle(hw, hh, getBallRadius(radius), mPaint);
        }

        mPaint.setColor(mStrokeColor);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(hw, hh, getRingRadius(radius), mPaint);
        canvas.restore();
    }

    private void drawMosaic(Canvas canvas, float hw, float hh, float radius) {
        float ringRadius = getRingRadius(radius);
        float perWidth = ringRadius * 2 / 5;
        mPaint.setStrokeWidth(perWidth);
        float startX = hw - ringRadius;
        float startY = hh - ringRadius;
        float currentX, currentY;
        for (int i = 0; i < 5; i++) {
            currentY = (i + 0.5f) * perWidth + startY;
            for (int j = 0; j < 5; j++) {
                currentX = j * perWidth + startX;
                mPaint.setColor((i + j) % 2 == 0 ? Color.WHITE : Color.BLACK);
                canvas.drawLine(currentX, currentY, currentX + perWidth, currentY, mPaint);
            }
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(hw);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(hw, hh, ringRadius + hw/2f, mPaint);
        // 复原
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mPaint.setStrokeWidth(5);
    }

    private float getBallRadius(float radius) {
        return radius * ((RADIUS_BALL - RADIUS_BASE) * mRadiusRatio + RADIUS_BASE);
    }

    private float getRingRadius(float radius) {
        return radius * ((RADIUS_RING - RADIUS_BASE) * mRadiusRatio + RADIUS_BASE);
    }

    @Override
    public void setChecked(boolean checked) {
        boolean isChanged = checked != isChecked();

        super.setChecked(checked);

        if (isChanged) {
            ValueAnimator animator = getAnimator();

            if (checked) {
                animator.start();
            } else {
                animator.reverse();
            }
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mRadiusRatio = (float) animation.getAnimatedValue();
        invalidate();
    }

}
