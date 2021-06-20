package com.vachel.editor.bean;

import android.text.TextUtils;

/**
 * 粘贴文本
 */

public class StickerText {
    private boolean drawBackground;

    private String text;

    private int color;

    public StickerText(String text, int color) {
        this(text, color, false);
    }

    public StickerText(String text, int color, boolean drawBackground) {
        this.text = text;
        this.color = color;
        this.drawBackground = drawBackground;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isDrawBackground() {
        return drawBackground;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(text);
    }

    public int length() {
        return isEmpty() ? 0 : text.length();
    }
}
