package com.vachel.editor.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.vachel.editor.R;

public class StickerImageDialog extends Dialog {

    private View mContentView;

    public StickerImageDialog(@NonNull Context context, View contentView) {
        this(context);
        mContentView = contentView;
    }

    public StickerImageDialog(@NonNull Context context) {
        this(context, R.style.BottomDialog);
    }

    public StickerImageDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sticker_layout);
        Window window = getWindow();
        if (window != null) {
            LinearLayout.LayoutParams attachLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 900);
            LinearLayout rootView = findViewById(R.id.root_view);
            rootView.addView(mContentView, attachLp);
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        setCanceledOnTouchOutside(true);
    }
}
