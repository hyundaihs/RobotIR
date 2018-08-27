package com.hzncc.kevin.robot_ir.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.IBinder
import com.hzncc.kevin.robot_ir.*
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.qi_han.QiHanConnectUtil
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.utils.*
import com.hzsk.camera.CameraUtil
import com.hzsk.camera.Device_w324_h256
import kotlinx.android.synthetic.main.activity_main.*
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

    companion object {
        var isRuning = false
        var cameraUtil = CameraUtil(Device_w324_h256())
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
        doAsync {
            if (isRuning) {
                stopIR()
                closeIR()
                stopVL()
                closeVL()
                Thread.sleep(1000)
            }
            openIR()
            openVL()
            startIR()
            startVL()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNextFrame() {
        doAsync {
            while (isRuning) {
                E("service is run")
                App.ir_imageData = getNext()
                if (MainActivity.isWarn) {
                    MainActivity.isWarn = false
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
        cameraUtil.colorName = 9
    }

    private fun startIR() {
        if (!cameraUtil.isOpened) {
            openIR()
        }
        cameraUtil.colorName = 9
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