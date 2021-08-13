package com.vachel.editor.clip;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.vachel.editor.PictureEditor;

public class DefaultClipWindowRender implements EditClipWindow.IClipRender {
    float CLIP_MARGIN = PictureEditor.getInstance().getClipRectMarginNormal(); // 裁剪区域的边距
    float CLIP_CORNER_SIZE = 42f; // 角尺寸
    float CLIP_THICKNESS_CELL = 2f; // 内边厚度
    float CLIP_THICKNESS_FRAME = 4f; //外边框厚度
    float CLIP_THICKNESS_SEWING = 8f; //角边厚度
    int COLOR_CELL = Color.WHITE;
    int COLOR_FRAME = Color.WHITE;
    int COLOR_CORNER = Color.WHITE;
    int COLOR_SHADE = 0xCC000000;

    /**
     * 比例尺，用于计算出 {0, width, 1/3 width, 2/3 width} & {0, height, 1/3 height, 2/3 height}
     */
    private float[] CLIP_SIZE_RATIO = {0, 1, 0.33f, 0.66f};
    private float[] CLIP_CORNER_STEPS = {0, 3, -3};
    private float[] CLIP_CORNER_SIZES = {0, CLIP_CORNER_SIZE, -CLIP_CORNER_SIZE};
    private byte[] CLIP_CORNERS = {
            0x8, 0x8, 0x9, 0x8,
            0x6, 0x8, 0x4, 0x8,
            0x4, 0x8, 0x4, 0x1,
            0x4, 0xA, 0x4, 0x8,
            0x4, 0x4, 0x6, 0x4,
            0x9, 0x4, 0x8, 0x4,
            0x8, 0x4, 0x8, 0x6,
            0x8, 0x9, 0x8, 0x8
    };
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int CLIP_CELL_STRIDES = 0x7362DC98;
    int CLIP_CORNER_STRIDES = 0x0AAFF550;
    private final float[] mCells = new float[16];
    private final float[] mCorners = new float[32];
    private final float[][] mBaseSizes = new float[2][4];

    public DefaultClipWindowRender() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public float getCornerSize() {
        return CLIP_CORNER_SIZE;
    }

    @Override
    public float getClipMargin() {
        return CLIP_MARGIN;
    }

    @Override
    public void onDraw(Canvas canvas, RectF frame) {
        float[] size = {frame.width(), frame.height()};
        for (int i = 0; i < mBaseSizes.length; i++) {
            for (int j = 0; j < mBaseSizes[i].length; j++) {
                mBaseSizes[i][j] = size[i] * CLIP_SIZE_RATIO[j];
            }
        }

        for (int i = 0; i < mCells.length; i++) {
            mCells[i] = mBaseSizes[i & 1][CLIP_CELL_STRIDES >>> (i << 1) & 3];
        }
        for (int i = 0; i < mCorners.length; i++) {
            mCorners[i] = mBaseSizes[i & 1][CLIP_CORNER_STRIDES >>> i & 1]
                    + CLIP_CORNER_SIZES[CLIP_CORNERS[i] & 3] + CLIP_CORNER_STEPS[CLIP_CORNERS[i] >> 2];
        }

        canvas.translate(frame.left, frame.top);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(COLOR_CELL);
        mPaint.setStrokeWidth(CLIP_THICKNESS_CELL);
        canvas.drawLines(mCells, mPaint);

        canvas.translate(-frame.left, -frame.top);
        mPaint.setColor(COLOR_FRAME);
        mPaint.setStrokeWidth(CLIP_THICKNESS_FRAME);
        canvas.drawRect(frame, mPaint);

        canvas.translate(frame.left, frame.top);
        mPaint.setColor(COLOR_CORNER);
        mPaint.setStrokeWidth(CLIP_THICKNESS_SEWING);
        canvas.drawLines(mCorners, mPaint);
    }
}
