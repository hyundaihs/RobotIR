package com.hzncc.kevin.robot_ir.utils;

import android.os.Environment;
import android.util.Log;

/**
 * ZhuDao
 * Created by 蔡雨峰 on 2017/3/22.
 */
public class SDCardUtil {
    private static final String SDCARD = Environment.getExternalStorageDirectory().getPath();
    private static String ROOT = SDCARD + "/com.hzncc.robot/";
    private static final String IMAGE = ROOT + "image/";
    public static final String IMAGE_IR = ROOT + "image/ir/";
    public static final String IMAGE_VL = ROOT + "image/vl/";
    public static final String VIDEO = ROOT + "video/";
    public static final String LOG = ROOT + "log/";


    public static boolean initAll() {
        if (!isExistSDCard()) {
            Log.d("AppPath", "sdcard is not exist");
//            ROOT = Environment.getRootDirectory()+ "/com.hzncc.zhudao/";
            return false;
        }
        if (FileUtil.initPath(SDCardUtil.ROOT)) {
            Log.d("AppPath", "ROOT init success");
        } else {
            Log.d("AppPath", "ROOT init failed");
        }
        FileUtil.initPath(SDCardUtil.IMAGE);
        FileUtil.initPath(SDCardUtil.IMAGE_IR);
        FileUtil.initPath(SDCardUtil.IMAGE_VL);
        FileUtil.initPath(SDCardUtil.VIDEO);
        FileUtil.initPath(SDCardUtil.LOG);
        return true;
    }

    public static boolean isExistSDCard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }
}
