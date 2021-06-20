package com.vachel.editor.ui.sticker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.vachel.editor.R;
import com.vachel.editor.ui.sticker.StickerView;

public class ImageStickerView extends StickerView {

    private ImageView mImageView;
    private @DrawableRes int mImageResource = R.mipmap.ic_doodle_checked;

    public ImageStickerView(Context context) {
        super(context);
    }

    public ImageStickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageStickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View onCreateContentView(Context context) {
        mImageView = new ImageView(context);
        mImageView.setImageResource(mImageResource);
        return mImageView;
    }

    public void setImage(@DrawableRes int image){
        mImageResource = image;
        mImageView.setImageResource(image);
    }
}
