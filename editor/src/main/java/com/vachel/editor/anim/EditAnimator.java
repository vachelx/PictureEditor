package com.vachel.editor.anim;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import com.vachel.editor.bean.EditState;

public class EditAnimator extends ValueAnimator {
    private boolean isRotate = false;
    private EditEvaluator mEvaluator;
    private static final float INTERPOLATOR_RATIO = 1.5f; //回弹系数

    public EditAnimator() {
        setInterpolator(new DecelerateInterpolator(INTERPOLATOR_RATIO));
    }

    @Override
    public void setObjectValues(Object... values) {
        super.setObjectValues(values);
        if (mEvaluator == null) {
            mEvaluator = new EditEvaluator();
        }
        setEvaluator(mEvaluator);
    }

    public void setValues(EditState sState, EditState eState) {
        setObjectValues(sState, eState);
        isRotate = EditState.isRotate(sState, eState);
    }

    public boolean isRotate() {
        return isRotate;
    }

    static class EditEvaluator implements TypeEvaluator<EditState> {
        private EditState state;

        public EditEvaluator() {

        }

        @Override
        public EditState evaluate(float fraction, EditState startValue, EditState endValue) {
            float x = startValue.x + fraction * (endValue.x - startValue.x);
            float y = startValue.y + fraction * (endValue.y - startValue.y);
            float scale = startValue.scale + fraction * (endValue.scale - startValue.scale);
            float rotate = startValue.rotate + fraction * (endValue.rotate - startValue.rotate);
            if (state == null) {
                state = new EditState(x, y, scale, rotate);
            } else {
                state.set(x, y, scale, rotate);
            }
            return state;
        }
    }

}
