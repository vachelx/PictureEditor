package com.vachel.editor.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import com.vachel.editor.PictureEditor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm == null || wm.getDefaultDisplay() == null) {
            return 0;
        }
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm == null || wm.getDefaultDisplay() == null) {
            return 0;
        }
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    // 调用的地方记得要关流
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void closeAll(Closeable... closeables){
        if(closeables == null){
            return;
        }
        for (Closeable closeable : closeables) {
            if(closeable!=null){
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            return true;
        }
        return false;
    }

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static String saveBitmap(Context context, Bitmap bitmap) {
        boolean saveSuccess;
        String path;
        String displayName = "image_" + getCurrentFormatTime() + ".jpg";
        if (Build.VERSION.SDK_INT >= 29) {
            saveSuccess = BitmapUtil.saveBitmapWithAndroidQ(context, bitmap, displayName);
            path = getSaveImagePathByName(displayName);
        } else {
            path = getSaveFilePath(context, displayName);
            saveSuccess = BitmapUtil.saveBitmapFile(bitmap, path);
        }
        return saveSuccess ? path : "";
    }

    private static String getCurrentFormatTime() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static String getSaveFilePath(Context context,String fileName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File outputDirectory = new File(sdCard.getAbsolutePath() + File.separator + PictureEditor.getInstance().getRelativePath());
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        return outputDirectory.getAbsolutePath()  + File.separator + fileName;
    }

    /**
     * 根据图片名称返回保存的图片路径
     * @param displayName 图片名称
     * @return 保存的图片路径 /DCIM/edit/displayName
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String getSaveImagePathByName(String displayName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                + File.separator + PictureEditor.getInstance().getRelativePath() + File.separator + displayName);
        return file.getPath();
    }
}
