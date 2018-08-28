package com.hzncc.kevin.robot_ir.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import com.hzncc.kevin.frareddemo.ir_sdk.CameraUtil
import com.hzncc.kevin.frareddemo.ir_sdk.Device_w324_h256
import com.hzncc.kevin.frareddemo.ir_sdk.Device_w336_h256
import com.hzncc.kevin.frareddemo.ir_sdk.LeptonStatus
import com.hzncc.kevin.robot_ir.App
import com.hzncc.kevin.robot_ir.E
import com.hzncc.kevin.robot_ir.MainActivity
import com.hzncc.kevin.robot_ir.MainActivity.Companion.irIP
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.saveBitmap
import com.hzncc.kevin.robot_ir.utils.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/8/22.
 */
class MyService : Service() {

    private var correctTemp: Float by Preference("correct_temp", 0.0f)
    private var maxWarn by Preference("max_warn", 0)
    private var minWarn by Preference("min_warn", 0)
    private var isWarn by Preference("isWarn", 0)
    //    private var is324 by Preference("is324", 336)
    var cameraUtil = if (irIP == "10.217.39.201") CameraUtil(Device_w324_h256())
    else CameraUtil(Device_w336_h256())

    companion object {
        var isRuning = false
        var hcvisionUtil = HcvisionUtil()
        var viewWidth = 0
        var viewHeight = 0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (hcvisionUtil.init()) {
            E("init success")
        } else {
            E("init failed")
        }
        isRuning = true
        getNextFrame()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cameraUtil = if (irIP == "10.217.39.201") CameraUtil(Device_w324_h256())
        else CameraUtil(Device_w336_h256())
        doAsync {
            if (isRuning) {
                stopVL()
                closeVL()
                stopIR()
                closeIR()
                Thread.sleep(1000)
            }
            openIR()
            startIR()
            openVL()
            startVL()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    var startTime: Long = 0
    var currTime: Long = 0
    var timeCounter = true

    fun countTime() {
        timeCounter = false
        doAsync {
            Thread.sleep(10000)
            timeCounter = true
        }
    }


    private fun getNextFrame() {
        doAsync {
            Thread.sleep(2000)
            while (isRuning) {
                if (cameraUtil.isOpened && cameraUtil.status == LeptonStatus.STATUS_RUNNING) {
                    App.ir_imageData = getNext()
                    if (startTime == 0L) {
                        startTime = System.currentTimeMillis()
                    } else {
                        currTime = System.currentTimeMillis()
                        val off = currTime - startTime
                        if (off >= 600000) {
                            startTime = currTime
                            cameraUtil.correct()
                        }
                    }
                    if (isWarn == 1) {
                        if (timeCounter) {
                            countTime()
                            if (App.ir_imageData.max_temp >= maxWarn || App.ir_imageData.min_temp <= minWarn) {
                                val calendarUtil = CalendarUtil()
                                val time = calendarUtil.timeInMillis
                                var isMaxWarn: Boolean
                                var warnTemp: Float
                                if (App.ir_imageData.max_temp >= maxWarn) {
                                    isMaxWarn = true
                                    warnTemp = App.ir_imageData.max_temp
                                } else {
                                    isMaxWarn = false
                                    warnTemp = App.ir_imageData.min_temp
                                }
                                if (App.instance.mData.size > 0) {
                                    val oldTime = App.instance.mData[0].time
                                    if (time - oldTime < 60) {
                                        val oldTemp = App.instance.mData[0].warnTemp
                                        if (isMaxWarn == App.instance.mData[0].isMaxWarn) {
                                            if (isMaxWarn && Math.abs(App.ir_imageData.max_temp - oldTemp) <= 0.2) {
                                                continue
                                            } else if (!isMaxWarn && Math.abs(App.ir_imageData.min_temp - oldTemp) <= 0.2) {
                                                continue
                                            }
                                        }
                                    }
                                }
                                val fileName = "${calendarUtil.format(CalendarUtil.FILENAME)}.jpeg"
                                getIRPic(viewWidth, viewHeight, fileName, App.ir_imageData, time, isMaxWarn, warnTemp)
                                getVLPic(viewWidth, viewHeight, fileName, App.ir_imageData, time, isMaxWarn, warnTemp)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getIRPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData, time: Long, isMaxWarn: Boolean, warnTemp: Float) {
        doAsync {
            val mBackEnv = GLES20BackEnv_IR(width, height)
            mBackEnv.setRenderer(GLBitmapRenderer())
            mBackEnv.setInput(ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap(), true, time, isMaxWarn, warnTemp)
        }
    }

    fun getVLPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData, time: Long, isMaxWarn: Boolean, warnTemp: Float) {
        doAsync {
            val mBackEnv = GLES20BackEnv_VL(width, height)
            mBackEnv.setRenderer(GLFrameRenderer())
            mBackEnv.setInput(App.vlData, HcvisionUtil.width, HcvisionUtil.height, ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap(), time = time, isMaxWarn = isMaxWarn, warnTemp = warnTemp)
        }
    }

    private fun getNext(): IR_ImageData {
        val ir_ImageData = IR_ImageData()
        val raw = ShortArray(cameraUtil.deviceInfo.raw_length)
        cameraUtil.nextFrame(raw)
        val bmp = ByteArray(cameraUtil.deviceInfo.bmp_length)
        cameraUtil.img_14To8(raw, bmp)
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.size, options)
        ir_ImageData.bitmap = bitmap
        ir_ImageData.width = cameraUtil.deviceInfo.width
        ir_ImageData.height = cameraUtil.deviceInfo.height
        App.irWidth = cameraUtil.deviceInfo.width
        App.irHeight = cameraUtil.deviceInfo.height
        ir_ImageData.max_x = cameraUtil.maxX
        ir_ImageData.max_y = cameraUtil.maxY
        ir_ImageData.min_x = cameraUtil.minX
        ir_ImageData.min_y = cameraUtil.minY
        ir_ImageData.max_temp = cameraUtil.maxTemp + correctTemp
        ir_ImageData.min_temp = cameraUtil.minTemp + correctTemp
        return ir_ImageData
    }

    override fun onDestroy() {
        super.onDestroy()
        stopIR()
        stopVL()
        closeIR()
        closeVL()
        isRuning = false
    }

    private fun openIR() {
        cameraUtil.open(MainActivity.irIP, 50001)
        cameraUtil.setColorName(9)
    }

    private fun startIR() {
        if (!cameraUtil.isOpened) {
            openIR()
        }
        cameraUtil.setColorName(9)
        cameraUtil.start()

    }

    private fun stopIR() {
        if (cameraUtil.isOpened) {
            cameraUtil.stop()
        }
    }


    private fun openVL() {
        if (!hcvisionUtil.isLogined()) {
            if (!hcvisionUtil.login()) {
                toast("Login failed")
                return
            }
        }
    }

    private fun startVL() {
        if (!hcvisionUtil.isLogined()) {
            openVL()
        }
        if (!hcvisionUtil.startPreview()) {
            E("startPreview failed")
            return
        }
    }

    private fun stopVL() {
        if (hcvisionUtil.isLogined() && HcvisionUtil.m_iPlayID >= 0) {
            hcvisionUtil.stopPreview()
        }
    }

    private fun closeIR() {
        cameraUtil.close()
    }

    private fun closeVL() {
        if (hcvisionUtil.isLogined()) {
            hcvisionUtil.loginOut()
        }
    }

}