package com.vachel.editor.sticker;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.vachel.editor.ui.sticker.StickerView;

/**
 * 移动和双指缩放旋转
 */
public class StickerFingerTouchHelper {
    private final StickerView container;
    private float translationX;
    private float translationY;
    private float baseScale = 1;
    private float rotation;

    private float actionX;
    private float actionY;
    private float spacing;
    private float degree;
    private int moveType;
    private float downTransX, downTransY; // 手指按下时的初始偏移

    public StickerFingerTouchHelper(StickerView view){
        container = view;
    }

    public boolean onTouch(final View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                moveType = 1;
                actionX = event.getRawX();
                actionY = event.getRawY();
                baseScale = container.getScale();
                downTransX = v.getTranslationX();
                downTransY = v.getTranslationY();
                translationX = downTransX;
                translationY = downTransY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                moveType = 2;
                spacing = getSpacing(event);
                degree = getDegree(event);
                break;
            case MotionEvent.ACTION_MOVE:
                container.invalidateParent();
                if (moveType == 1) {
                    translationX = translationX + event.getRawX() - actionX;
                    translationY = translationY + event.getRawY() - actionY;
                    v.setTranslationX(translationX);
                    v.setTranslationY(translationY);
                    actionX = event.getRawX();
                    actionY = event.getRawY();
                } else if (moveType == 2) {
                    float currScale = baseScale * getSpacing(event) / spacing;
                    container.setScale(currScale);
                    rotation = rotation + getDegree(event) - degree;
                    if (rotation > 360) {
                        rotation = rotation - 360;
                    }
                    if (rotation < -360) {
                        rotation = rotation + 360;
                    }
                    container.setRotation(rotation);
                }
                break;
            case MotionEvent.ACTION_UP:
                // 超出底图范围了，还原到初始位置
                if (container.isMoveToOutside()) {
                    translationX = downTransX;
                    translationY = downTransY;
                    startResetTransAnim(v, translationX, translationY);
                }
            case MotionEvent.ACTION_POINTER_UP:
                moveType = 0;
        }
        return true;
    }

    // 触碰两点间距离
    private float getSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取旋转角度
    private float getDegree(MotionEvent event) {
        double delta_x = event.getX(0) - event.getX(1);
        double delta_y = event.getY(0) - event.getY(1);
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private void startResetTransAnim(final View view, final float translationX, final float translationY) {
        final float startY = view.getTranslationY();
        final float startX = view.getTranslationX();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(view.getTranslationY(), translationY);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                container.invalidateParent();
                view.setTranslationY(animatedValue);
                float currentTransX = (animatedValue - startY) * (translationX - startX) / (translationY - startY) + startX;
                view.setTranslationX(currentTransX);
            }
        });
        valueAnimator.setDuration(200);
        valueAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        valueAnimator.start();
    }

}
