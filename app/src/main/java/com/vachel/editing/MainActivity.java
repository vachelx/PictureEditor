package com.vachel.editing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.vachel.editor.PictureEditActivity;
import com.vachel.editor.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        copyTestImageToLocalIfNeed();
        findViewById(R.id.edit_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()){
                    editImage();
                }
            }
        });
        checkPermission();
    }

    public void copyTestImageToLocalIfNeed() {
        String directory = getCacheDir().getAbsolutePath();
        final File file = new File(directory + File.separator + "test_image.jpg");
        if (file.exists() && file.length() > 0) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = getAssets().open("test_image.jpg");
                    outputStream = new FileOutputStream(file);
                    Utils.copyStream(inputStream, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Utils.closeAll(inputStream, outputStream);
                }
            }
        }.start();
    }


    private boolean checkPermission() {
        int hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        }
        return true;
    }

    private void editImage() {
        String directory = getCacheDir().getAbsolutePath();
        final File file = new File(directory + File.separator + "test_image.jpg");
        Uri uri = Uri.fromFile(file);
        Intent editIntent = new Intent(this, MyPicEditActivity.class);
        editIntent.putExtra(PictureEditActivity.EXTRA_IMAGE_URI, uri);
        startActivity(editIntent);
    }
}