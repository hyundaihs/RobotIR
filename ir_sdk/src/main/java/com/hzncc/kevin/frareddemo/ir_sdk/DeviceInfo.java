package com.hzncc.kevin.frareddemo.ir_sdk;

/**
 * FraredDemo
 * Created by 蔡雨峰 on 2017/12/6.
 */

public class DeviceInfo {
    public static final int COLOR_TABLE_SIZE = 256 * 4; //调色板长度
    public static final int FILE_HEAD_SIZE = 14; //文件头产长度
    public static final int INFO_HEAD_SIZE = 40; //信息头长度
    public int typeId; // 设备类型ID
    public int width; //设备图像宽度
    public int height; //设备图像高度
    public int raw_length; //设备raw数据长度
    public int bmp_length; //设备bmp图片数据长度
    DeviceInfo(int id,int w,int h){
        typeId = id;
        this.width = w;
        this.height = h;
        raw_length = width * height;
        bmp_length = raw_length + COLOR_TABLE_SIZE + FILE_HEAD_SIZE + INFO_HEAD_SIZE;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "typeId=" + typeId +
                ", width=" + width +
                ", height=" + height +
                ", raw_length=" + raw_length +
                ", bmp_length=" + bmp_length +
                '}';
    }
}
