package com.vachel.editing;

import android.app.Application;
import android.graphics.Color;

import com.vachel.editor.PictureEditor;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PictureEditor.getInstance()
                .setMaxScale(3)
//                .setBtnColor(Color.GREEN)
                .setMaxStickerScale(3)
                .setRelativeSavePath("edit/test")
//                .setDoodleColors(new int[]{Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.DKGRAY, Color.CYAN})
//                .setDefaultCheckColor(Color.BLUE)
        ;
    }

}
