package com.vachel.editor.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.vachel.editor.PictureEditor;
import com.vachel.editor.R;
import com.vachel.editor.ui.widget.ColorGroup;
import com.vachel.editor.ui.widget.ColorRadio;

/**
 * 定制的颜色组控件，根据PictureEditor提供的颜色动态的添加colorRadio
 * <p>
 * 默认选中的颜色为第一个
 */
public class CustomColorGroup extends ColorGroup {
    public CustomColorGroup(Context context) {
        this(context, null);
    }

    public CustomColorGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        initColors(context, attrs);
    }

    private void initColors(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomColorGroup);
        boolean hasExtraType = a.getBoolean(R.styleable.CustomColorGroup_hasExtraType, false);
        a.recycle();

        int size = getContext().getResources().getDimensionPixelSize(R.dimen.image_color);
        int margin = getContext().getResources().getDimensionPixelSize(R.dimen.image_color_margin);
        if (hasExtraType) {
            addColorRadio(size, margin, ColorRadio.TYPE_MOSAIC_COLOR);
        }
        int[] doodleColors = PictureEditor.getInstance().getDoodleColors(getContext());
        for (int color : doodleColors) {
            addColorRadio(size, margin, color);
        }
        setCheckColor(doodleColors[0]);
    }

    private void addColorRadio(int size, int margin, int color) {
        ColorRadio colorRadio = new ColorRadio(getContext());
        colorRadio.setColor(color);
        colorRadio.setClickable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = margin;
        params.bottomMargin = margin;
        addView(colorRadio, params);
    }
}
