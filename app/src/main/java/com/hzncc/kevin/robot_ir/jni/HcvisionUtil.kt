package com.hzncc.kevin.robot_ir.jni

import com.hikvision.netsdk.HCNetSDK
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30
import com.hikvision.netsdk.RealPlayCallBack
import com.hzncc.kevin.robot_ir.*
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import org.MediaPlayer.PlayM4.Player
import org.MediaPlayer.PlayM4.PlayerCallBack
import org.jetbrains.anko.doAsync


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

        val m_oIPAddr = "192.168.3.10"
//        val m_oIPAddr = "10.10.30.10"
        val m_oPort = 8000
        val m_oUser = "admin"
        val m_oPsd = "admin123456"
//        val m_oIPAddr = "192.168.0.169"
//        val m_oUser = "admin"
//        val m_oPsd = "a@123456"

        var width = 0
        var height = 0

    }

    fun init(): Boolean {
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            D("HCNetSDK init is failed!")
            return false
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, SDCardUtil.LOG_HCVISION,
                true)

        return true
    }

    fun isLogined(): Boolean {
        return m_iLogID >= 0
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
        val rel = HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)
        m_iLogID = -1
        return rel
    }

    fun startPreview(hcRender: GLFrameRenderer): Boolean {
        val previewInfo = com.hikvision.netsdk.NET_DVR_PREVIEWINFO()
        previewInfo.lChannel = 1
        previewInfo.dwStreamType = 0 // 码流类型:0-主码流，1-子码流，2-码流 3，3-虚拟码流，以此类推
        previewInfo.dwLinkMode = 1 //连接方式:0- TCP 方式，1- UDP 方式，2- 多播方式，3- RTP 方式，4-RTP/RTSP，5-RSTP/HTTP
        previewInfo.bBlocked = 0 //0- 非阻塞取流，1- 阻塞取流
        previewInfo.bPassbackRecord = 0 //0-不启用录像回传，1-启用录像回传。ANR 断网补录功能，客户端和设备之间网络异常恢复之后自动将前端数据同步过来，需要设备支持。
        previewInfo.byPreviewMode = 0 //预览模式:0- 正常预览，1- 延迟预览
        previewInfo.byProtoType = 0 //应用层取流协议:0- 私有协议，1- RTSP 协议

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
                if (!Player.getInstance().setDecodeFrameType(m_iPort, 0)) {
                    E("setDecodeFrameType failed")
                    return
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer,
                        iDataSize, 6 * 1024 * 1024)) // open stream
                {
                    E("openStream failed")
                    return
                }
                if (!Player.getInstance().setDecodeCB(m_iPort, PlayerCallBack.PlayerDecodeCB { //
                    nPort, data, nDataLen, nWidth, nHeight, nFrameTime, nDataType, Reserved
                    ->
                    width = nWidth
                    height = nHeight
//                    App.vlWidth = nWidth
//                    App.vlHeight = nHeight
                    hcRender.update(nWidth, nHeight)
                    App.yData = ByteArray(nWidth * nHeight)
                    App.vData = ByteArray(App.yData.size / 4)
                    App.uData = ByteArray(App.yData.size / 4)
                    System.arraycopy(data, 0, App.yData, 0, App.yData.size)
                    System.arraycopy(data, 0 + App.yData.size, App.vData, 0, App.vData.size)
                    System.arraycopy(data, 0 + App.yData.size + App.vData.size, App.uData, 0, App.uData.size)
                    hcRender.update(App.yData, App.uData, App.vData, App.ir_imageData)
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
            // Log.i(TAG, "处理流数据");
            if (iDataSize > 0) {
                var i = 0
                while (i < 4000) {
                    if (Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
                        break
                    }

                    if (i % 100 == 0) {
                        E("inputData failed with: " +
                                Player.getInstance().getLastError(m_iPort) + ", i:" + i);
                    }

                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()

                    }
                    i++
                }
            }
        }
    }

}