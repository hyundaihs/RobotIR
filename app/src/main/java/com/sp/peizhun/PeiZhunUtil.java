package com.sp.peizhun;

/**
 * Created by kevin on 2018/6/5.
 */

public class PeiZhunUtil {
    static {
        System.loadLibrary("peizhun-lib");
    }

    public static native void GetAffinePara1(double[] pIr, double[] pVis, double[] pPara);
}
