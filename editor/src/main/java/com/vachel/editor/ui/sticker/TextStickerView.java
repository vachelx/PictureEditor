package com.vachel.editor.ui.sticker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.vachel.editor.bean.StickerText;
import com.vachel.editor.ui.widget.TextEditDialog;
import com.vachel.editor.util.Utils;

public class TextStickerView extends StickerView implements TextEditDialog.ITextChangedListener {
    private static final int PADDING = 26;
    private static final int PADDING_BG = 20;
    private static final float TEXT_SIZE_SP = 30f;

    private TextView mTextView;
    private StickerText mText;
    private TextEditDialog mDialog;
    private boolean mAllowEditText = true;

    public TextStickerView(Context context) {
        this(context, null, 0);
    }

    public TextStickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextStickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View onCreateContentView(Context context) {
        mTextView = new TextView(context);
        mTextView.setTextSize(TEXT_SIZE_SP);
        mTextView.setPadding(PADDING, PADDING, PADDING, PADDING);
        mTextView.setTextColor(Color.WHITE);
        GradientDrawable gd = new GradientDrawable();//创建drawable
        gd.setColor(Color.TRANSPARENT);
        gd.setCornerRadius(Utils.dip2px(context, 10));
        mTextView.setBackground(gd);
        return mTextView;
    }

    public StickerText getText() {
        return mText;
    }

    @Override
    public void onContentTap() {
        if (mAllowEditText){
            TextEditDialog dialog = getDialog();
            dialog.setText(mText);
            dialog.show();
        }
    }

    public void enableEditText(boolean enable){
        mAllowEditText = enable;
    }

    private TextEditDialog getDialog() {
        if (mDialog == null) {
            mDialog = new TextEditDialog(getContext(), this);
        }
        return mDialog;
    }

    @Override
    public void onText(StickerText text, boolean enableEdit) {
        mText = text;
        mAllowEditText = enableEdit;
        if (mText != null && mTextView != null) {
            mTextView.setText(mText.getText());
            GradientDrawable myGrad = (GradientDrawable) mTextView.getBackground();
            if (mText.isDrawBackground()) {
                int color = text.getColor();
                boolean isWhite = color == Color.WHITE;
                mTextView.setTextColor(isWhite ? Color.BLACK : Color.WHITE);
                myGrad.setColor(text.getColor());
                setPadding(PADDING_BG, PADDING_BG, PADDING_BG, PADDING_BG);
            } else {
                mTextView.setTextColor(text.getColor());
                myGrad.setColor(Color.TRANSPARENT);
                setPadding(0, 0, 0, 0);
            }
        }
    }
}