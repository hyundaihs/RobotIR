package com.hzncc.kevin.robot_ir.jni

import com.hikvision.netsdk.HCNetSDK
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30
import com.hikvision.netsdk.RealPlayCallBack
import com.hzncc.kevin.robot_ir.*
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import org.MediaPlayer.PlayM4.Player
import org.MediaPlayer.PlayM4.PlayerCallBack


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/8.
 */
class HcvisionUtil {
    companion object {
        var m_iLogID = -1
        var m_iPlayID = -1
        var m_iPort = -1
        var m_oNetDvrDeviceInfoV30: NET_DVR_DEVICEINFO_V30? = null

        val m_oIPAddr = "192.168.1.64"
        val m_oPort = 8000
        val m_oUser = "admin"
        val m_oPsd = "admin123456"

    }

    fun init(): Boolean {
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            D("HCNetSDK init is failed!")
            return false
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/",
                true)

        return true
    }

    fun login(): Boolean {
        if (m_iLogID < 0) {
            m_iLogID = loginDevice()
        }
        m_iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(m_oIPAddr, m_oPort,
                m_oUser, m_oPsd, m_oNetDvrDeviceInfoV30)
        return m_iLogID >= 0
    }

    fun loginDevice(): Int {
        var iLogID = -1
        iLogID = loginNormalDevice()
        return iLogID
    }

    fun loginNormalDevice(): Int {
        // get instance
        m_oNetDvrDeviceInfoV30 = NET_DVR_DEVICEINFO_V30()
        // call NET_DVR_Login_v30 to login on, port 8000 as default
        val iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(m_oIPAddr, m_oPort,
                m_oUser, m_oPsd, m_oNetDvrDeviceInfoV30)
        if (iLogID < 0) {
            E("NET_DVR_Login is failed!Err:${HCNetSDK.getInstance().NET_DVR_GetLastError()}")
            return -1
        }
//        if (m_oNetDvrDeviceInfoV30.byChanNum > 0) {
//            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan.toInt()
//            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum.toInt()
//        } else if (m_oNetDvrDeviceInfoV30.byIPChanNum > 0) {
//            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan.toInt()
//            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256
//        }
//
//        if (m_iChanNum > 1) {
//            ChangeSingleSurFace(false)
//        } else {
//            ChangeSingleSurFace(true)
//        }
        I("NET_DVR_Login is Successful!")
        return iLogID
    }

    fun loginOut(): Boolean {
        return HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)
    }

    fun startPreview(hcRender: GLFrameRenderer): Boolean {
        val previewInfo = com.hikvision.netsdk.NET_DVR_PREVIEWINFO()
        previewInfo.lChannel = 1
        previewInfo.dwStreamType = 1 // substream
        previewInfo.bBlocked = 1
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID, previewInfo,
                RealPlayCallBack { iRealHandle, iDataType, pDataBuffer, iDataSize ->
                    processRealData(1, iDataType, pDataBuffer, iDataSize, Player.STREAM_REALTIME, hcRender)
                })
        return m_iPlayID >= 0
    }

    fun stopPreview() {
        if (Player.getInstance().stop(m_iPort)) {
            D("stop success")
        }
        if (Player.getInstance().closeStream(m_iPort)) {
            D("close Stream success")
        }
        if (Player.getInstance().freePort(m_iPort)) {
            D("free port success")
        }
        if (HCNetSDK.getInstance().NET_DVR_StopRealPlay(m_iPlayID)) {
            D("stop preview success")
        }
        m_iLogID = -1
        m_iPlayID = -1
        m_iPort = -1
    }

    fun processRealData(iPlayViewNo: Int, iDataType: Int,
                        pDataBuffer: ByteArray, iDataSize: Int, iStreamMode: Int, hcRender: GLFrameRenderer) {
//
        if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
//            D("dataType = $iDataType")
            if (m_iPort >= 0) {
                return
            }
            m_iPort = Player.getInstance().getPort()
            if (m_iPort == -1) {
                D("getPort is failed with: "
                        + Player.getInstance().getLastError(m_iPort))
                return
            }
            I("getPort succ with: " + m_iPort)
            if (iDataSize > 0) {
                if (!Player.getInstance().setStreamOpenMode(m_iPort,
                        iStreamMode)) // set stream mode
                {
                    D("setStreamOpenMode failed")
                    return
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer,
                        iDataSize, 6 * 1024 * 1024)) // open stream
                {
                    E("openStream failed")
                    return
                }

                if (!Player.getInstance().setDecodeFrameType(m_iPort, 0)) {
                    E("setDecodeFrameType failed")
                    return
                }
                if (!Player.getInstance().setDecodeCB(m_iPort, PlayerCallBack.PlayerDecodeCB { //
                    nPort, data, nDataLen, nWidth, nHeight, nFrameTime, nDataType, Reserved
                    ->
                    //                    D("解码类型:$nDataType")
//                    val datas = java.nio.ByteBuffer.wrap
                    hcRender.update(nWidth, nHeight)
                    val y = ByteArray(nWidth * nHeight)
                    val v = ByteArray(y.size / 4)
                    val u = ByteArray(y.size / 4)
                    System.arraycopy(data, 0, y, 0, y.size)
                    System.arraycopy(data, 0 + y.size, v, 0, v.size)
                    System.arraycopy(data, 0 + y.size + v.size, u, 0, u.size)
                    if (App.instance.ir_imageData != null) {
                        hcRender.update(y, u, v, App.instance.ir_imageData!!)
                    }
                    MainActivity.hcRenderSet = true
                })) {
                    E("setDecodeCB failed")
                } else {
                    E("setDecodeCB success")
                }
                if (!Player.getInstance().play(m_iPort, null)) {
                    E("play failed")
                    return
                }
                return
            }
        } else {
//            D("dataType = $iDataType")
            if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
                E("input data failed")
            }
//            if (!) {
//                var i = 0
//                while (i < 4000) {
//                    if (Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
//                        break
//                    }
//
//                    if (i % 100 == 0) {
//                        E("inputData failed with: " +
//                                Player.getInstance().getLastError(m_iPort) + ", i:" + i);
//                    }
//
//                    try {
//                        Thread.sleep(10)
//                    } catch (e: InterruptedException) {
//                        e.printStackTrace()
//
//                    }
//
//                    i++
//                }
//            }
        }
    }

}