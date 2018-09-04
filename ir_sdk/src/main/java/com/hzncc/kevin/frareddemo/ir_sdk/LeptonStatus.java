package com.hzncc.kevin.frareddemo.ir_sdk;

/**
 * Created by kevin on 16/12/26.
 */
public class LeptonStatus {
   public static final int STATUS_ERROR = -1;
   public static final int STATUS_CLOSED = 0;
   public static final int STATUS_OPENED =1;
   public static final int STATUS_STOP = 2;
   public static final int STATUS_STARTING = 3;
   public static final int STATUS_RUNNING =4;

    public static String getStatusByCode(int code) {
        switch (code) {
            case STATUS_CLOSED:
                return "断开链接";
            case STATUS_STOP:
                return "设备关闭";
            case STATUS_STARTING:
                return "正在打开";
            case STATUS_OPENED:
                return "设备打开";
            case STATUS_RUNNING:
                return "正在运行";
        }
        return "未知状态";
    }
}
