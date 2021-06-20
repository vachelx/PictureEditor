package com.vachel.editor.sticker;

public interface IStickerView {

    float getRotation();

    float getPivotX();

    float getPivotY();

    void setX(float x);

    void setY(float y);

    void setRotation(float rotate);

    float getScale();

    void setScale(float scale);

    void addScale(float scale);
}
