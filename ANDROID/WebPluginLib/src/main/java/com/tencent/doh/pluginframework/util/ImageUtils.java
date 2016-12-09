package com.tencent.doh.pluginframework.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tencent.doh.pluginframework.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by shibinhuang on 2016/9/7.
 */
public class ImageUtils {

    public static final int WIDTH = 720;
    public static final int HEIGHT = 1280;

    // 根据路径获得图片并压缩，返回压缩后的图片
    public static Bitmap getCompressedImage(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, WIDTH, HEIGHT);
        options.inJustDecodeBounds = false;

        Bitmap bm =  BitmapFactory.decodeFile(filePath, options);
        return  bm ;
    }


    //计算图片的缩放值
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height/ (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
}
