package com.hzncc.kevin.robot_ir.jni;

import android.util.Log;

import com.hzncc.kevin.frareddemo.CameraSDK;

import java.util.ArrayList;
import java.util.List;

import static com.hzncc.kevin.robot_ir.jni.LeptonStatus.STATUS_CLOSED;


/**
 * FraredDemo
 * Created by 蔡雨峰 on 2017/12/7.
 */

public class CameraUtil {

    private static List<String> ips = new ArrayList<>();
    private long handle = -1;
    private DeviceInfo deviceInfo;

    public CameraUtil() {
        CameraSDK.init();
    }

    public CameraUtil(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        CameraSDK.init();
    }

    public int search() {
        return CameraSDK.search(CameraUtil.class.getName(), "searchCallBack", "(Ljava/lang/String;)V");
    }

    public void searchCallBack(String str) {
        Log.d("cameraUtil", "searchCallBack");
        ips.add(str);
    }

    public boolean isOpened() {
        return handle > -1;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public long open(String ip, int port) {
        if (null == deviceInfo) {
            return -1;
        }
        if (handle < 0) {
            handle = CameraSDK.open(ip, port, deviceInfo.typeId);
        }
        return 0;
    }

    public int start() {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.start(handle);
    }

    public int stop() {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.stop(handle);
    }

    public int close() {
        if (handle == -1) {
            return -1;
        }
        int rel = CameraSDK.close(handle);
        if (rel == 0) {
            handle = -1;
        }
        return rel;
    }

    public int correct() {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.correct(handle);
    }

    public float getFps() {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.getFps(handle);
    }

    public void nextFrame(short[] raw) {
        if (handle == -1) {
            return;
        }
        CameraSDK.nextFrame(handle, raw);
    }

    public int img_14To8(short[] raw, byte[] bmp) {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.img_14To8(handle, raw, bmp);
    }

    public int img_14To565(short[] rawData, int[] rgbData) {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.img_14To565(handle, rawData, rgbData);
    }

    public int img_14To565(short[] rawData, short[] rgbData) {
        if (handle == -1) {
            return -1;
        }
        return CameraSDK.img_14To565ToShort(handle, rawData, rgbData);
    }

    /**
     * 获取最高温坐标
     *
     * @return 0, X;1,Y
     */
    public int[] getMaxPoint() {
        int[] p = new int[2];
        if (handle == -1) {
            return p;
        }
        p[0] = CameraSDK.getMaxX(handle);
        p[1] = CameraSDK.getMaxY(handle);
        return p;
    }

    /**
     * 获取最低温坐标
     *
     * @return 0, X;1,Y
     */
    public int[] getMinPoint() {
        int[] p = new int[2];
        if (handle == -1) {
            return p;
        }
        p[0] = CameraSDK.getMinX(handle);
        p[1] = CameraSDK.getMinY(handle);
        return p;
    }

    public float getMaxTemp() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMaxTemp(handle);
    }

    public float getMinTemp() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMinTemp(handle);
    }

    public float getPointTemp(int x, int y) {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getPointTemp(handle, x, y);
    }

    public int getColorName() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getColorName(handle);
    }

    public void setColorName(int colorName) {
        if (handle == -1) {
            return;
        }
        CameraSDK.setColorName(handle, colorName);
    }

    public int getStatus() {
        if (handle == -1) {
            return STATUS_CLOSED;
        }
        return CameraSDK.getStatus(handle);
    }

    public void setStatus(int status) {
        if (handle == -1) {
            return;
        }
        CameraSDK.setStatus(handle, status);
    }

    public int getMaxX() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMaxX(handle);
    }

    public int getMaxY() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMaxY(handle);
    }

    public int getMinX() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMinX(handle);
    }

    public int getMinY() {
        if (handle == -1) {
            return 0;
        }
        return CameraSDK.getMinY(handle);
    }

//    public static void GetAffinePara1(double[] pIr, double[] pVis, double[] pPara) {
//        CameraSDK.GetAffinePara1(pIr, pVis, pPara);
//    }

}
