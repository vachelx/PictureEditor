package com.vachel.editor.sticker;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

public interface IStickerLayer {

    boolean show();

    boolean remove();

    boolean dismiss();

    boolean isShowing();

    RectF getFrame();

    void onDrawSticker(Canvas canvas, int frameHeight);

    void onGestureScale(Matrix matrix, float factor);

    void registerCallback(IStickerParent IStickerParent);

    void unregisterCallback(IStickerParent IStickerParent);

}
