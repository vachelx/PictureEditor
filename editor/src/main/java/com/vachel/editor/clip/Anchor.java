package com.vachel.editor.clip;

import android.graphics.RectF;

import com.vachel.editor.PictureEditor;

public enum Anchor {
    LEFT(1),
    RIGHT(2),
    TOP(4),
    BOTTOM(8),
    LEFT_TOP(5),
    RIGHT_TOP(6),
    LEFT_BOTTOM(9),
    RIGHT_BOTTOM(10);

    int v;

    final static int[] PN = {1, -1};

    Anchor(int v) {
        this.v = v;
    }

    public void move(RectF win, RectF frame, float dx, float dy, EditClipWindow.IClipRender render) {
        float[] maxFrame = cohesion(win, render.getClipMargin());
        float[] minFrame = cohesion(frame, render.getCornerSize() * 3.14f);
        float[] theFrame = cohesion(frame, 0);

        float[] dxy = {dx, 0, dy};
        for (int i = 0; i < 4; i++) {
            if (((1 << i) & v) != 0) {

                int pn = PN[i & 1];

                theFrame[i] = pn * revise(pn * (theFrame[i] + dxy[i & 2]),
                        pn * maxFrame[i], pn * minFrame[i + PN[i & 1]]);
            }
        }

        float maxBottom = win.bottom - render.getClipMargin() - PictureEditor.getInstance().getClipRectMarginBottom();
        frame.set(theFrame[0], theFrame[2], theFrame[1], Math.min(theFrame[3], maxBottom));
    }

    public static float revise(float v, float min, float max) {
        return Math.min(Math.max(v, min), max);
    }

    public static float[] cohesion(RectF win, float v) {
        return new float[]{
                win.left + v, win.right - v,
                win.top + v, win.bottom - v
        };
    }

    public static boolean isCohesionContains(RectF frame, float v, float x, float y) {
        return frame.left + v < x && frame.right - v > x
                && frame.top + v < y && frame.bottom - v > y;
    }

    public static Anchor valueOf(int v) {
        Anchor[] values = values();
        for (Anchor anchor : values) {
            if (anchor.v == v) {
                return anchor;
            }
        }
        return null;
    }
}
