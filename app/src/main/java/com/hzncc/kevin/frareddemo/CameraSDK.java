package com.hzncc.kevin.frareddemo;

import android.graphics.Canvas;
import android.view.Surface;

/**
 * FraredDemo
 * Created by 蔡雨峰 on 2017/12/7.
 */

public class CameraSDK {

    static {
        System.loadLibrary("native-lib");
    }

    public static native int search(String callbackClaxx, String callbackMethod, String callbackSign);

    /**
     * 初始化SDK,初始化调色板,图片头文件信息头等
     *
     * @return 是否初始化成功
     */
    public static native int init();

    /**
     * 初始化接收端口,准备接收数据流
     *
     * @param ip   设备IP
     * @param port 设备端口
     * @return 底层句柄handle
     */
    public static native long open(String ip, int port, int deviceType);

    /**
     * 发送打开命令发送数据流
     *
     * @param handle 句柄
     * @return 是否打开成功, 成功接收数据流即成功
     */
    public static native int start(long handle);

    /**
     * 发送关闭命令关闭数据流,此时接收端口仍在使用
     *
     * @param handle 句柄
     * @return 返回是否关闭成功
     */
    public static native int stop(long handle);

    /**
     * 关闭接收端口,与open成套使用
     *
     * @param handle 句柄
     * @return 返回是否关闭成功
     */
    public static native int close(long handle);

    /**
     * 校正打快门
     *
     * @param handle 句柄
     * @return 返回是否成功
     */
    public static native int correct(long handle);

    /**
     * native层绘制
     *
     * @param handle 句柄
     * @param canvas 绘制画布
     */
    public static native void nativeDraw(long handle, Canvas canvas);

    /**
     * 获取下一帧图像数据
     *
     * @param handle 句柄
     */
    public static native void nextFrame(long handle, short[] raw);

    /**
     * 获取设备帧数
     * @param handle 句柄
     * @return 返回帧数
     */
    public static native float getFps(long handle);


    /**
     * 获取环境温度
     *
     * @param handle 句柄
     * @return 温度值
     */
    public static native float getEnvTemp(long handle);

    /**
     * 获取快门温度
     *
     * @param handle 句柄
     * @return 温度值
     */
    public static native float getSutTemp(long handle);

    /**
     * 获取最高温度X坐标
     *
     * @param handle 句柄
     * @return X坐标
     */
    public static native int getMaxX(long handle);

    /**
     * 获取最高温度Y坐标
     *
     * @param handle 句柄
     * @return Y坐标
     */
    public static native int getMaxY(long handle);

    /**
     * 获取最低温度X坐标
     *
     * @param handle 句柄
     * @return X坐标
     */
    public static native int getMinX(long handle);

    /**
     * 获取最低温度Y坐标
     *
     * @param handle 句柄
     * @return Y坐标
     */
    public static native int getMinY(long handle);

    /**
     * 获取最高温度值
     *
     * @param handle 句柄
     * @return 最高温度值
     */
    public static native float getMaxTemp(long handle);

    /**
     * 获取最低温度值
     *
     * @param handle 句柄
     * @return 最低温度值
     */
    public static native float getMinTemp(long handle);

    /**
     * 获取指定点温度值
     *
     * @param handle 句柄
     * @param x      指定点X坐标
     * @param y      指定点Y坐标
     * @return 指定点温度
     */
    public static native float getPointTemp(long handle, int x, int y);

    /**
     * 14位图像转8位图像
     *
     * @param raw 待转图像
     * @return 返回新的图像数组
     */
    public static native int img_14To8(long handle, short[] raw, byte[] bmp);

    /**
     * 14位图转565图像
     *
     * @param handle  句柄
     * @param rawData raw 数据
     * @param rgbData 转换后的数据
     * @return 返回成功失败
     */
    public static native int img_14To565(long handle, short[] rawData, int[] rgbData);

    /**
     * 14位图转565图像
     *
     * @param handle  句柄
     * @param rawData raw 数据
     * @param rgbData 转换后的数据
     * @return 返回成功失败
     */
    public static native int img_14To565ToShort(long handle, short[] rawData, short[] rgbData);


    /**
     * 获取当前调色板
     *
     * @return 数组中的坐标
     */
    public static native int getColorName(long handle);

    /**
     * 设置调色板
     *
     * @param index 数组中的下标
     */
    public static native void setColorName(long handle, int index);

    /**
     * 设置当前状态
     *
     * @param handle 句柄
     * @param status 状态数值
     */
    public static native void setStatus(long handle, int status);

    /**
     * 获取当前状态
     *
     * @param handle 句柄
     * @return 状态码
     */
    public static native int getStatus(long handle);

    /**
     * 获取当前帧号
     *
     * @param handle 句柄
     * @return 帧号
     */
    public static native int getCurrentFrameNum(long handle);

    /**
     * 设置显示的Surface
     * @return 是否成功
     */
    public static native boolean setVideoSurface(Surface surface);
    public static native int pushImage(byte[] raw,int length);

}
