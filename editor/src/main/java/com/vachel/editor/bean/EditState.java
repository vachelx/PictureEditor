package com.vachel.editor.bean;

import androidx.annotation.NonNull;

public class EditState {
    public float x, y;
    public float scale;
    public float rotate;

    public EditState(float x, float y, float scale, float rotate) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.rotate = rotate;
    }

    public void set(float x, float y, float scale, float rotate) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.rotate = rotate;
    }

    public void concat(EditState state) {
        this.scale *= state.scale;
        this.x += state.x;
        this.y += state.y;
    }

    public void rConcat(EditState state) {
        this.scale *= state.scale;
        this.x -= state.x;
        this.y -= state.y;
    }

    public static boolean isRotate(EditState sState, EditState eState) {
        return Float.compare(sState.rotate, eState.rotate) != 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "scale = " + scale + "; scrollX = " + x + "; scrollY = " + y;
    }
}
