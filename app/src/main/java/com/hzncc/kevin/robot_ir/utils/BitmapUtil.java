package com.hzncc.kevin.robot_ir.utils;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtil {
    /**
     * 保存方法
     */
    public static void saveBitmap(String path, String fileName, Bitmap bitmap) {
        saveBitmap(path + fileName, bitmap);
    }

    public static void saveBitmap(String pathName, Bitmap bitmap) {
        File f = new File(pathName);
        if (f.exists()) {
            if(!f.delete()){
                Log.e("BitmapUtil","delete old file failed");
            }
        } else {
            if (null != f.getParentFile() && !f.getParentFile().exists()) {
                if(!f.getParentFile().mkdirs()){
                    Log.e("BitmapUtil","make dirs failed");
                }
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] getRGBByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int pixels[] = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] data = convertColorToByte(pixels);
        return data;
    }

    public static byte[] convertColorToByte(int color[]) {
        if (color == null) {
            return null;
        }
        byte[] data = new byte[color.length * 3];
        for (int i = 0; i < color.length; i++) {
            data[i * 3] = (byte) (color[i] >> 16 & 0xff);
            data[i * 3 + 1] = (byte) (color[i] >> 8 & 0xff);
            data[i * 3 + 2] = (byte) (color[i] & 0xff);
        }
        return data;
    }

    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }
        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }

        int[] color = new int[size / 3 + arg];
        if (arg == 0) {
            for (int i = 0; i < color.length; ++i) {
                color[i] = (data[i * 3] << 16 & 0x00FF0000)
                        | (data[i * 3 + 1] << 8 & 0x0000FF00)
                        | (data[i * 3 + 2] & 0x000000FF)
                        | 0xFF000000;
            }
        } else {
            for (int i = 0; i < color.length - 1; ++i) {
                color[i] = (data[i * 3] << 16 & 0x00FF0000)
                        | (data[i * 3 + 1] << 8 & 0x0000FF00)
                        | (data[i * 3 + 2] & 0x000000FF)
                        | 0xFF000000;
            }
            color[color.length - 1] = 0xFF000000;
        }
        return color;
    }

    @Nullable
    public static Bitmap createBitmapFromRGB(byte[] data, int width, int height) {
        int[] colors = convertByteToColor(data);
        if (colors == null) {
            return null;
        }
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(colors, 0, width, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            return null;
        }
        return bmp;
    }
}
