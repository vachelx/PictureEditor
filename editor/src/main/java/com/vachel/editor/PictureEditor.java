package com.vachel.editor;

import android.content.Context;
import android.content.res.Resources;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.vachel.editor.clip.EditClipWindow;
import com.vachel.editor.ui.sticker.StickerView;

/**
 * 自定义全局配置参数
 *
 * 在使用前配置，也可以不配置全都使用默认值
 */
public class PictureEditor {
    private static volatile PictureEditor sInstance;

    private float maxScale = 5; // 编辑的图片编辑过程中双指最大放大倍数
    private int loadPicSizeLimit = 1080 * 5;//编辑的图片过大时，根据最大宽高限制值截取一部分来编辑
    // 编辑后的图片保存的相对路径： AndroidQ以上在DCIM+relativePath下， AndroidQ以下保存在SDCard+relativePath下
    // 注意前后不要加斜杠/
    private String relativePath = "edit";
    private float maxStickerScale = 4; // 贴图的最大放大倍数
    private float defaultDoodleWidth = 14f; // 涂鸦线条默认宽度
    private float defaultMosaicWidth = 25f; // 马赛克线条默认宽度
    private @DrawableRes int deleteStickerIcon = R.mipmap.ic_delete; // 删除按钮
    private @DrawableRes int adjustStickerIcon = R.mipmap.ic_adjust; // 调整按钮
    private EditClipWindow.IClipRender globalClipWindowRender = null; // 剪裁窗口渲染器，用于自定义剪裁窗口
    private StickerView.IRectRender globalStickerRectRender = null; // 贴图边框渲染器
    private @ColorInt int btnColor = 0;  // 页面中所有Button的颜色
    // 涂鸦画笔支持的颜色组， checkColor为默认选中颜色（doodleColors中的一个）
    private int[] doodleColors;
    private @ColorInt int checkColor = 0;

    private PictureEditor() {
    }

    public static PictureEditor getInstance() {
        if (sInstance == null) {
            synchronized (PictureEditor.class) {
                if (sInstance == null) {
                    sInstance = new PictureEditor();
                }
            }
        }
        return sInstance;
    }

    public PictureEditor setMaxScale(float maxScale) {
        this.maxScale = maxScale;
        return this;
    }

    public PictureEditor setMaxStickerScale(float maxScale) {
        this.maxStickerScale = maxScale;
        return this;
    }

    public PictureEditor setPicSizeLimit(int sizeLimit) {
        this.loadPicSizeLimit = sizeLimit;
        return this;
    }

    public PictureEditor setRelativeSavePath(String path){
        this.relativePath = path;
        return this;
    }

    public PictureEditor setDefaultDoodleWidth(int width) {
        this.defaultDoodleWidth = width;
        return this;
    }

    public PictureEditor setDefaultMosaicWidth(int width) {
        this.defaultMosaicWidth = width;
        return this;
    }

    public PictureEditor setGlobalClipWindowRender(EditClipWindow.IClipRender clipRender){
        this.globalClipWindowRender = clipRender;
        return this;
    }

    public PictureEditor setDeleteStickerIcon(@DrawableRes int removeStickerIcon) {
        this.deleteStickerIcon = removeStickerIcon;
        return this;
    }

    public PictureEditor setAdjustStickerIcon(@DrawableRes int adjustStickerIcon) {
        this.adjustStickerIcon = adjustStickerIcon;
        return this;
    }

    public PictureEditor setGlobalStickerRectRender(StickerView.IRectRender rectRender){
        this.globalStickerRectRender = rectRender;
        return this;
    }

    public PictureEditor setBtnColor(@ColorInt int color) {
        btnColor = color;
        return this;
    }

    public PictureEditor setDoodleColors(int[] colors){
        doodleColors = colors;
        return this;
    }

    public PictureEditor setDefaultCheckColor(@ColorInt int color){
        checkColor = color;
        return this;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public float getMaxStickerScale() {
        return maxStickerScale;
    }

    public int getPicSizeLimit() {
        return loadPicSizeLimit;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public float getDefaultDoodleWidth() {
        return defaultDoodleWidth;
    }

    public float getDefaultMosaicWidth() {
        return defaultMosaicWidth;
    }

    public ImageView getDeleteStickerIcon(Context context) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        view.setImageResource(deleteStickerIcon);
        return view;
    }

    public ImageView getAdjustStickerIcon(Context context) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        view.setImageResource(adjustStickerIcon);
        return view;
    }

    public EditClipWindow.IClipRender getGlobalClipWindowRender(){
        return globalClipWindowRender;
    }

    public StickerView.IRectRender getGlobalStickerRectRender(){
        return globalStickerRectRender;
    }

    public @ColorInt int getBtnColor(Context context) {
        if (btnColor == 0) {
            return context.getResources().getColor(R.color.image_color_accent);
        }
        return btnColor;
    }

    public int[] getDoodleColors(Context context) {
        if (doodleColors == null) {
            Resources resources = context.getResources();
            int[] defaultColors = new int[7];
            defaultColors[0] = resources.getColor(R.color.image_color_white);
            defaultColors[1] = resources.getColor(R.color.image_color_black);
            defaultColors[2] = resources.getColor(R.color.image_color_red);
            defaultColors[3] = resources.getColor(R.color.image_color_cyan);
            defaultColors[4] = resources.getColor(R.color.image_color_yellow);
            defaultColors[5] = resources.getColor(R.color.image_color_blue);
            defaultColors[6] = resources.getColor(R.color.image_color_purple);
            return defaultColors;
        }
        return doodleColors;
    }

    public int getDefaultCheckedColor(Context context) {
        if (checkColor == 0) {
            if (doodleColors != null) {
                return doodleColors[0];
            }
            return context.getResources().getColor(R.color.image_color_red);
        }
        return checkColor;
    }

}
