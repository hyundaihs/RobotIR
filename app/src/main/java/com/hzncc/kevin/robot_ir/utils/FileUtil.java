package com.hzncc.kevin.robot_ir.utils;

import android.content.Context;
import android.text.format.DateFormat;

import com.hzncc.kevin.robot_ir.data.Log_Data;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FileUtil {
    public static final String JPG_SUFFIX = ".jpg";// JPG图片的后缀
    /**
     * 删除成功
     */
    public static final int DEL_SUCCESS = 1;
    /**
     * 删除失败
     */
    public static final int DEL_FAIL = -1;
    /**
     * 文件不存在
     */
    public static final int FILE_IS_NULL = 0;

    /**
     * 从Assets中读取txt文件内容
     *
     * @param context  上下文
     * @param fileName 要读取的文件名称
     * @return 返回一个字符串
     */
    public static String getTxtFromAssets(Context context, String fileName) {
        String Result = null;
        try {
            InputStreamReader inputReader = new InputStreamReader(context
                    .getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";

            while ((line = bufReader.readLine()) != null)
                Result += line;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result;
    }

    /**
     * 将一个byte数组保存位一个本地文件
     *
     * @param path 文件保存使用的名称
     * @param b    要保存的byte数组
     * @return 返回保存好的文件对象 如果保存失败file为null
     */
    public static File saveBytesToFile(String path, byte[] b) {
        BufferedOutputStream stream = null;
        File file = null;
        try {
            file = new File(path);
            FileOutputStream fstream = new FileOutputStream(file);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return file;
    }

    /**
     * 根据路径获取文件，并自动检查文件是否存在
     *
     * @param path 要得到文件对象的路径
     * @return 返回文件对象file
     */
    public static File getNewFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            initPath(path);
        }
        return file;
    }

    /**
     * 创建一个新的文件夹
     *
     * @param path 要创建的文件路径
     * @return 判断是否创建成功
     */
    public static boolean initPath(String path) {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.exists();
    }

    /**
     * 获取当前系统时间并拼接之后作为文件名使用
     *
     * @param suffix 需要拼接到时间字符串后面的字符串
     * @return 返回拼接好的字符串
     */
    public static String getDataToFileName(String suffix) {
        String name = DateFormat.format("yyyyMMdd_hhmmss",
                Calendar.getInstance(Locale.CHINA))
                + suffix;
        return name;
    }

    /**
     * 删除某个文件
     *
     * @param path 要删除的文件路径
     * @return 0 代表文件不存在 1 代表删除成功 -1 代表删除失败
     */
    public static int delFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.delete() ? DEL_SUCCESS : DEL_FAIL;
        } else {
            return FILE_IS_NULL;
        }
    }

    //读取指定目录下的所有TXT文件的文件名
    public static List<Log_Data> getFileName(File[] files, String dix) {
        List<Log_Data> strs = new ArrayList<>();
        if (files != null) {    // 先判断目录是否为空，否则会报空指针
            for (int i = files.length - 1; i >= 0; i--) {
                File file = files[i];
                if (file.isDirectory()) {//检查此路径名的文件是否是一个目录(文件夹)
                    getFileName(file.listFiles(), dix);
                } else {
                    String fileName = file.getName();
                    if (fileName.endsWith("." + dix)) {
                        if (new File(SDCardUtil.IMAGE_VL + fileName).exists()) {
                            strs.add(new Log_Data(fileName, fileName, 0L, false, 0f));
                        }
                    }
                }
            }
        }
        return strs;
    }
}
