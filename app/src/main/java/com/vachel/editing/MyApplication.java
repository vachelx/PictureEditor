package com.vachel.editing;

import android.app.Application;
import android.graphics.Color;

import com.vachel.editor.PictureEditor;
import com.vachel.editor.util.Utils;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PictureEditor.getInstance()
                .setMaxScale(10)
//                .setBtnColor(Color.GREEN)
                .setMaxStickerScale(3)
                .setRelativeSavePath("edit/test")
//                .setDoodleColors(new int[]{Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.DKGRAY, Color.CYAN})
//                .setDefaultCheckColor(Color.BLUE)
                .setClipRectMarginBottom(Utils.dip2px(this, 44))
                .setClipRectMarginNormal(Utils.dip2px(this, 16));
        ;
    }

}
