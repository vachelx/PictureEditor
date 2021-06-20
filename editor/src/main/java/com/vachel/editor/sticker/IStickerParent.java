package com.vachel.editor.sticker;

import com.vachel.editor.ui.sticker.StickerView;

public interface IStickerParent {
    void onDismiss(StickerView stickerView);

    void onShowing(StickerView stickerView);

    boolean onRemove(StickerView stickerView);

    void onLayerChanged(StickerView stickerView);

    void invalidate();

    int getScrollY();
}
