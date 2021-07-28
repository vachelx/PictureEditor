package com.vachel.editor.bean;

import android.graphics.Path;

public class DrawPen extends PaintPath {
    private static final float VALID_VALUE = 2;
    private int identity = Integer.MIN_VALUE;
    private float startX, startY, offset; // 用于判断当前绘制path是否有效
    private boolean isValid = false;

    public void reset() {
        this.path.reset();
        this.identity = Integer.MIN_VALUE;
        isValid = false;
        offset = 0;
    }

    public void reset(float x, float y) {
        this.path.reset();
        this.path.moveTo(x, y);
        startX = x;
        startY = y;
        this.identity = Integer.MIN_VALUE;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    public boolean isIdentity(int identity) {
        return this.identity == identity;
    }

    public void lineTo(float x, float y) {
        this.path.lineTo(x, y);
        if (!isValid) {
            offset = Math.max(Math.max(Math.abs(x - startX), Math.abs(y - startY)), this.offset);
            if (offset > VALID_VALUE) {
                isValid = true;
            }
        }
    }

    public boolean isEmpty() {
        return this.path.isEmpty();
    }

    public boolean isValidPath() {
        return isValid;
    }

    public PaintPath toPath() {
        return new PaintPath(new Path(this.path), getMode(), getColor());
    }
}
