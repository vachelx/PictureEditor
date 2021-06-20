package com.vachel.editing;

import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import com.vachel.editing.emoji.Emoji;
import com.vachel.editing.emoji.EmojiDrawer;
import com.vachel.editing.emoji.IEmojiCallback;
import com.vachel.editor.bean.StickerText;
import com.vachel.editor.PictureEditActivity;
import com.vachel.editor.util.Utils;

public class MyPicEditActivity extends PictureEditActivity implements IEmojiCallback {

    @Override
    public void initData() {
        mSupportEmoji = true;
    }

    @Override
    public View getStickerLayout() {
            return new EmojiDrawer(this).bindCallback(this);
    }

    @Override
    public void onEmojiClick(String emoji) {
        StickerText stickerText = new StickerText(emoji, Color.WHITE);
        onText(stickerText, false); // emoji其实也是text文本
        Utils.dismissDialog(mStickerImageDialog);
    }

    @Override
    public void onBackClick() {
        mStickerImageDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Emoji.recycleAllEmoji();
    }

    @Override
    public void onSaveSuccess(String savePath) {
        Toast.makeText(this, savePath, Toast.LENGTH_LONG).show();
        finish();
    }
}
