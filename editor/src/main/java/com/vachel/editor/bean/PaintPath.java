package com.vachel.editor.bean;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import com.vachel.editor.EditMode;
import com.vachel.editor.PictureEditor;

/**
 * 涂鸦/马赛克线条路径
 */

public class PaintPath {

    protected Path path;

    private int color;

    private float width;

    private EditMode mode;

    public PaintPath() {
        this(new Path());
    }

    public PaintPath(Path path) {
        this(path, EditMode.DOODLE);
    }

    public PaintPath(Path path, EditMode mode) {
        this(path, mode, Color.RED);
    }

    public PaintPath(Path path, EditMode mode, int color) {
        this(path, mode, color, mode == EditMode.DOODLE ?
                PictureEditor.getInstance().getDefaultDoodleWidth() :
                PictureEditor.getInstance().getDefaultMosaicWidth());
    }

    public PaintPath(Path path, EditMode mode, int color, float width) {
        this.path = path;
        this.mode = mode;
        this.color = color;
        this.width = width;
        if (mode == EditMode.MOSAIC) {
            path.setFillType(Path.FillType.EVEN_ODD);
        }
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public EditMode getMode() {
        return mode;
    }

    public void setMode(EditMode mode) {
        this.mode = mode;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getWidth() {
        return width;
    }

    public void onDrawDoodle(Canvas canvas, Paint paint) {
        if (mode == EditMode.DOODLE) {
            paint.setColor(color);
            paint.setStrokeWidth(width);
            canvas.drawPath(path, paint);
        }
    }

    public void onDrawMosaic(Canvas canvas, Paint paint) {
        if (mode == EditMode.MOSAIC) {
            paint.setStrokeWidth(width);
            canvas.drawPath(path, paint);
        }
    }

    public void transform(Matrix matrix) {
        path.transform(matrix);
    }
}
