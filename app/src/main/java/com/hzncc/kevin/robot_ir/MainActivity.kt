package com.hzncc.kevin.robot_ir

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.jni.CameraUtil
import com.hzncc.kevin.robot_ir.jni.Device_w324_h256
import com.hzncc.kevin.robot_ir.jni.HcvisionUtil
import com.hzncc.kevin.robot_ir.jni.LeptonStatus
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.renderers.GLRGBRenderer
import com.hzncc.kevin.robot_ir.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.nio.ShortBuffer
import android.app.NotificationManager
import android.app.ProgressDialog
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.view.MotionEvent
import com.sp.peizhun.PeiZhunUtil
import com.hzncc.kevin.robot_ir.App.Companion.isPeizhund
import com.hzncc.kevin.robot_ir.App.Companion.param1
import com.hzncc.kevin.robot_ir.App.Companion.param2
import com.hzncc.kevin.robot_ir.App.Companion.param3
import com.hzncc.kevin.robot_ir.App.Companion.param4
import com.hzncc.kevin.robot_ir.App.Companion.param5
import com.hzncc.kevin.robot_ir.App.Companion.param6
import com.hzncc.kevin.robot_ir.App.Companion.pointIR1_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR1_y
import com.hzncc.kevin.robot_ir.App.Companion.pointIR2_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR2_y
import com.hzncc.kevin.robot_ir.App.Companion.pointIR3_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR3_y
import com.hzncc.kevin.robot_ir.App.Companion.pointVL1_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL1_y
import com.hzncc.kevin.robot_ir.App.Companion.pointVL2_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL2_y
import com.hzncc.kevin.robot_ir.App.Companion.pointVL3_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL3_y
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View) {
        handle = true
        when (v.id) {
            R.id.openIR -> {
//                if (openIR.text.toString() == "开启监控") {
//                    openIR.text = "开启中"
//                    openCameras()
//                } else if (openIR.text.toString() == "关闭监控") {
//                    closeCameras()
//                    openIR.text = "开启监控"
//                }
                openIR()
            }
            R.id.openVL -> openCamera()
            R.id.openWarn -> openWarn()
            R.id.max -> showPickers(true)
            R.id.min -> showPickers()
            R.id.peizhun -> {
                if (peizhun.text.toString().equals("配准")) {
                    startPeizhun()
                } else {
                    savePeizhun()
                }
            }
            R.id.clean -> clear()
            R.id.correct -> showCorrectTmpPickers()
            R.id.irImage or R.id.vlImage -> {
                val intent = Intent(this, GalleryActivity::class.java)
                intent.putExtra("index", 0)
                startActivity(intent)
            }
        }
    }

    private fun openCameras() {
//        val progressDialog = ProgressDialog(this)
//        progressDialog.show()
        doAsync {
            if (!hcvisionUtil.isLogined()) {
                if (hcvisionUtil.login()) {
                    E("login success")

                } else {
                    E("login failed")
                    uiThread {
                        toast("Login failed")
                    }
                }
            }

            if (hcvisionUtil.startPreview(hcRender)) {
                E("startPreview success")
            } else {
                E("startPreview failed")
                uiThread {
                    toast("startPreview failed")
                }
            }

            cameraUtil.open("192.168.3.9", 50001)
//            cameraUtil.open("10.10.30.9", 50001)
            cameraUtil.colorName = 9
            cameraUtil.start()
            uiThread {
                openIR.text = "关闭监控"
//                progressDialog.cancel()
            }
        }
    }

    private fun closeCameras() {
        doAsync {
            cameraUtil.stop()
            if (HcvisionUtil.m_iLogID > 0) {
                hcvisionUtil.stopPreview()
            }
        }
    }


    private fun openIR() {
        if (openIR.text == "开启红外") {
            openIR.text = "开启中..."
            cameraUtil.open("192.168.3.9", 50001)
//            cameraUtil.open("10.10.30.9", 50001)
            cameraUtil.colorName = 9
            cameraUtil.start()
            openIR.text = "关闭红外"
        } else if (openIR.text == "关闭红外") {
            cameraUtil.stop()
            openIR.text = "开启红外"
        }
    }

    private fun openCamera() {
        doAsync {
            if (openVL.text == "开启可见光") {
                uiThread {
                    openVL.text = "开启中..."
                }
                if (!hcvisionUtil.isLogined()) {
                    if (hcvisionUtil.login()) {
                        E("login success")
//                        uiThread {
//                            toast("Login failed")
//                        }
                    } else {
                        E("login failed")
//                        uiThread {
//                            toast("Login failed")
//                        }
                    }
                }
                if (hcvisionUtil.startPreview(hcRender)) {
                    E("startPreview success")
                } else {
                    E("startPreview failed")
//                    uiThread {
//                        toast("startPreview failed")
//                    }
                }
                uiThread {
                    openVL.text = "关闭可见光"
                }
            } else if (openVL.text == "关闭可见光") {
                if (HcvisionUtil.m_iLogID > 0) {
                    hcvisionUtil.stopPreview()
                }
                uiThread {
                    openVL.text = "开启可见光"
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        System.exit(0)
    }

    private fun openWarn() {
        if (!isOpenWarn) {
            openWarn.text = "关闭报警"
            isOpenWarn = true
            openWarnCount()
        } else {
            openWarn.text = "开启报警"
            isOpenWarn = false
        }
    }

    private var maxWarn by Preference("max_warn", 0)
    private var minWarn by Preference("min_warn", 0)
    private var correctTemp: Float by Preference("correct_temp", 0.0f)
    private var isPeizhun = false
    private var irDownX = 0f
    private var irDownY = 0f
    private var vlDownX = 0f
    private var vlDownY = 0f
    private var touchIr = 0
    private var touchVl = 0
    private var touchSpace = 20

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(actionSaveBitmap)
        registerReceiver(broadcastReceiver, intentFilter)
        if (!hcvisionUtil.isLogined()) {
            doAsync {
                if (hcvisionUtil.login()) {
                    E("login success")
                    uiThread {
                        toast("LoginSuccess")
                    }
                } else {
                    E("login failed")
                }
            }
        }
    }

    override fun onDestroy() {
        closeThread()
        cameraUtil.close()
        hcvisionUtil.stopPreview()
        hcvisionUtil.loginOut()
        surfaceViewL.onPause()
        surfaceViewR.onPause()
        mMediaPlayer?.release()
        super.onDestroy()
    }


    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == actionSaveBitmap) {
//                val options = BitmapFactory.Options().apply {
//                    inSampleSize = 4
//                }
                irImage.setImageBitmap(
                        BitmapFactory.decodeFile(SDCardUtil.IMAGE_IR + App.instance.mData[0].irImage))
                vlImage.setImageBitmap(
                        BitmapFactory.decodeFile(SDCardUtil.IMAGE_VL + App.instance.mData[0].vlImage))
                warn()
            }
        }
    }

    var mMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceViewL.setEGLContextClientVersion(2)
        surfaceViewR.setEGLContextClientVersion(2)
        if (isBitmapDraw) {
            bitIrRender = GLBitmapRenderer(surfaceViewL)
            surfaceViewL.setRenderer(bitIrRender)
        } else {
            irRender = GLRGBRenderer(surfaceViewL)
            surfaceViewL.setRenderer(irRender)
        }
        surfaceViewL.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcRender = GLFrameRenderer(surfaceViewR)
        surfaceViewR.setRenderer(hcRender)
        surfaceViewR.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcvisionUtil = HcvisionUtil()
        if (hcvisionUtil.init()) {
            E("init success")
        } else {
            E("init failed")
        }
        cameraUtil = CameraUtil(Device_w324_h256())

        mMediaPlayer = MediaPlayer.create(this, R.raw.tts)

        openThread()
        initDatas()
    }

    fun clear() {
        val preferences = getSharedPreferences("isPeizhund", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.commit()
    }

    private fun initDatas() {
        val file = File(SDCardUtil.IMAGE_IR)
        val files = file.listFiles()
        App.instance.mData.clear()
        App.instance.mData.addAll(FileUtil.getFileName(files, "jpeg"))
        max.text = "高温报警:$maxWarn℃"
        min.text = "低温报警:$minWarn℃"
        correct.text = "温度补偿:$correctTemp℃"

        irImage.setImageBitmap(
                BitmapFactory.decodeFile(SDCardUtil.IMAGE_IR + App.instance.mData[0].irImage))
        vlImage.setImageBitmap(
                BitmapFactory.decodeFile(SDCardUtil.IMAGE_VL + App.instance.mData[0].vlImage))

        surfaceViewL.setOnTouchListener { v, event ->
            if (isPeizhun) {
                val w = surfaceViewL.width.toFloat()
                val h = surfaceViewL.height.toFloat()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        irDownX = App.irWidth.toFloat() - event.rawX * (App.irWidth.toFloat() / w)
                        irDownY = App.irHeight.toFloat() - event.rawY * (App.irHeight.toFloat() / h)
                        when (true) {
                            (Math.abs(irDownX - pointIR1_x) <= touchSpace && (Math.abs(irDownY - pointIR1_y + touchSpace)) <= touchSpace) -> {
                                touchIr = 1
                            }
                            (Math.abs(irDownX - pointIR2_x) <= touchSpace && (Math.abs(irDownY - pointIR2_y + touchSpace)) <= touchSpace) -> {
                                touchIr = 2
                            }
                            (Math.abs(irDownX - pointIR3_x) <= touchSpace && (Math.abs(irDownY - pointIR3_y + touchSpace)) <= touchSpace) -> {
                                touchIr = 3
                            }
                            else -> {
                                touchIr = 0
                            }
                        }
                        D("irDownX = $irDownX irDownY = $irDownY pointIR1_x = $pointIR1_x pointIR1_y = $pointIR1_y touchIr = $touchIr")
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val x = App.irWidth.toFloat() - event.rawX * (App.ir_imageData.width.toFloat() / w)
                        val y = App.irHeight.toFloat() - event.rawY * (App.ir_imageData.height.toFloat() / h)
                        when (touchIr) {
                            1 -> {
                                pointIR1_x += (x - irDownX)
                                pointIR1_y += (y - irDownY)
                            }
                            2 -> {
                                pointIR2_x += (x - irDownX)
                                pointIR2_y += (y - irDownY)
                            }
                            3 -> {
                                pointIR3_x += (x - irDownX)
                                pointIR3_y += (y - irDownY)
                            }
                            else -> {
                            }
                        }
                        irDownX = x
                        irDownY = y
                    }
                    MotionEvent.ACTION_UP -> {
                        touchIr = 0
                        irDownX = 0f
                        irDownY = 0f
                    }
                }
            }
            isPeizhun
        }
        surfaceViewR.setOnTouchListener { v, event ->
            if (isPeizhun) {
                val w = surfaceViewR.width.toFloat()
                val h = surfaceViewR.height.toFloat()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        vlDownX = App.irWidth.toFloat() - (event.rawX - surfaceViewL.width) * (App.irWidth.toFloat() / w)
                        vlDownY = App.irHeight.toFloat() - event.rawY * (App.irHeight.toFloat() / h)
                        when (true) {
                            (Math.abs(vlDownX - pointVL1_x) <= touchSpace && (Math.abs(vlDownY - pointVL1_y + touchSpace)) <= touchSpace) -> {
                                touchVl = 1
                            }
                            (Math.abs(vlDownX - pointVL2_x) <= touchSpace && (Math.abs(vlDownY - pointVL2_y + touchSpace)) <= touchSpace) -> {
                                touchVl = 2
                            }
                            (Math.abs(vlDownX - pointVL3_x) <= touchSpace && (Math.abs(vlDownY - pointVL3_y + touchSpace)) <= touchSpace) -> {
                                touchVl = 3
                            }
                            else -> {
                                touchVl = 0
                            }
                        }
                        D("vlDownX = $vlDownX vlDownY = $vlDownY pointIR1_x = $pointVL1_x pointIR1_y = $pointVL1_y touchIr = $touchVl")
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val x = App.irWidth.toFloat() - (event.rawX - surfaceViewL.width) * (App.irWidth.toFloat() / w)
                        val y = App.irHeight.toFloat() - event.rawY * (App.irHeight.toFloat() / h)
                        when (touchVl) {
                            1 -> {
                                pointVL1_x += (x - vlDownX)
                                pointVL1_y += (y - vlDownY)
                            }
                            2 -> {
                                pointVL2_x += (x - vlDownX)
                                pointVL2_y += (y - vlDownY)
                            }
                            3 -> {
                                pointVL3_x += (x - vlDownX)
                                pointVL3_y += (y - vlDownY)
                            }
                            else -> {
                            }
                        }
                        vlDownX = x
                        vlDownY = y
                    }
                    MotionEvent.ACTION_UP -> {
                        touchVl = 0
                        vlDownX = 0f
                        vlDownY = 0f
                    }
                }
            }
            isPeizhun
        }
    }

    fun openWarnCount() {
        doAsync {
            while (isOpenWarn) {
                isWarn = true
                Thread.sleep(10000)
            }
        }
    }

    private fun startPeizhun() {
        peizhun.text = "确定"
        isPeizhun = true
        if (isBitmapDraw) {
            bitIrRender.setPeizhun(isPeizhun)
        } else {
            irRender.setPeizhun(isPeizhun)
        }
        hcRender.setPeizhun(isPeizhun)
    }

    private fun savePeizhun() {
        peizhun.text = "配准"
        isPeizhun = false
        if (isBitmapDraw) {
            bitIrRender.setPeizhun(isPeizhun)
        } else {
            irRender.setPeizhun(isPeizhun)
        }
        hcRender.setPeizhun(isPeizhun)

        val pPointIR: DoubleArray = kotlin.DoubleArray(6)
        pPointIR[0] = pointIR1_x.toDouble()
        pPointIR[1] = pointIR1_y.toDouble()
        pPointIR[2] = pointIR2_x.toDouble()
        pPointIR[3] = pointIR2_y.toDouble()
        pPointIR[4] = pointIR3_x.toDouble()
        pPointIR[5] = pointIR3_y.toDouble()
        val pPointVis: DoubleArray = kotlin.DoubleArray(6)
        pPointVis[0] = pointVL1_x.toDouble()
        pPointVis[1] = pointVL1_y.toDouble()
        pPointVis[2] = pointVL2_x.toDouble()
        pPointVis[3] = pointVL2_y.toDouble()
        pPointVis[4] = pointVL3_x.toDouble()
        pPointVis[5] = pointVL3_y.toDouble()

        val pDbAffPara = DoubleArray(6)
        PeiZhunUtil.GetAffinePara1(pPointIR, pPointVis, pDbAffPara)
        param1 = pDbAffPara[0].toFloat()
        param2 = pDbAffPara[1].toFloat()
        param3 = pDbAffPara[2].toFloat()
        param4 = pDbAffPara[3].toFloat()
        param5 = pDbAffPara[4].toFloat()
        param6 = pDbAffPara[5].toFloat()
        isPeizhund = true
        for (i in 0 until 6) {
            D("pDbAffPara[$i] = ${pDbAffPara[i]}")
        }
    }


    private fun showCorrectTmpPickers() {
        val rel = IntArray(3)
        if (correctTemp > 0) {
            getCorrentIndex(correctTemp, rel)
        }
        showTempPicker(rel[0], rel[1], rel[2], { option1, option2, option3 ->
            correctTemp = getCorrectTemp(option1, option2, option3)
            correct.text = "温度补偿:$correctTemp℃"
        })
    }

    private fun showPickers(isMax: Boolean = false) = if (isMax) {
        var select = 0
        if (maxWarn > 0) {
            select = options.indexOf(maxWarn)
        } else {
            select = 1
        }
        showPicker(select, options, isMax, { //
            content: String, position: Int ->
            max.text = "高温报警:$content℃"
            maxWarn = content.toInt()
        })
    } else {
        var select = 0
        if (minWarn > 0) {
            select = options.indexOf(minWarn)
        } else {
            select = 1
        }
        showPicker(select, options, isMax, { //
            content, position ->
            min.text = "低温报警:$content℃"
            minWarn = content.toInt()
        })
    }

    fun closeThread() {
        isRun = false
    }

    private lateinit var hcRender: GLFrameRenderer
    private lateinit var irRender: GLRGBRenderer
    private lateinit var bitIrRender: GLBitmapRenderer
    val broadcastReceiver: MyBroadcastReceiver = MyBroadcastReceiver()
    lateinit var hcvisionUtil: HcvisionUtil
    lateinit var cameraUtil: CameraUtil
    private val isBitmapDraw = true

    companion object {
        var viewWidth = 0
        var viewHeight = 0
        var isRun = true
        val bitPaint = Paint()
        val blackPaint = Paint()
        val whitePaint = Paint()
        var crossSize = 0
        var offSize = 0
        var rateW = 0f
        var rateH = 0f

        var hcRenderSet = false
        var isOpenWarn = false
        var isWarn = false
        var handle = false // 右边按钮弹出后是否被操作
        var warning = false //是否正在报警

        val options: List<Int> by lazy {
            ArrayList<Int>().apply {
                for (i in 0..60) {
                    add(i)
                }
            }
        }
    }

    var startTime: Long = 0
    var currTime: Long = 0

    fun openThread() {
        doAsync {
            while (isRun) {
                if (cameraUtil.getStatus() == LeptonStatus.STATUS_RUNNING) {
                    if (startTime == 0L) {
                        startTime = System.currentTimeMillis()
                    } else {
                        currTime = System.currentTimeMillis()
                        val off = currTime - startTime
                        if (off >= 600000) {
                            startTime = currTime
                            cameraUtil.correct()
                            D("校正")
                        }
                    }
                    App.ir_imageData = getNext(isBitmapDraw)
                    if (isBitmapDraw) {
                        bitIrRender.update(App.ir_imageData)
                    } else {
                        irRender.update(App.ir_imageData)
                    }
                    if (isWarn) {
                        isWarn = false
                        if (App.ir_imageData.max_temp >= maxWarn || App.ir_imageData.min_temp <= minWarn) {
                            val calendarUtil = CalendarUtil()
                            val time = calendarUtil.timeInMillis
                            var isMaxWarn = false
                            var warnTemp = 0.0f
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
                            getIRPic(surfaceViewL.width, surfaceViewL.height, fileName, App.ir_imageData, time, isMaxWarn, warnTemp)
                            getVLPic(surfaceViewR.width, surfaceViewR.height, fileName, App.ir_imageData, time, isMaxWarn, warnTemp)
                        }
                    }
                }
            }
        }
    }

    fun warn() {
        mMediaPlayer?.start()
//        val path = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tts)
//        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val builder = Notification.Builder(this)
//        builder.setContentText("有新的报警信息")
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//        builder.setTicker("新消息")
//        builder.setAutoCancel(true)
//        builder.setSound(path)
//
//        builder.setWhen(System.currentTimeMillis())//设置时间，设置为系统当前的时间
//        val notification = builder.build()
////        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//        notification.sound = path
//        notification.defaults = Notification.DEFAULT_ALL
//        manager.notify(1, notification)
    }

    fun getIRPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData, time: Long, isMaxWarn: Boolean, warnTemp: Float) {
        doAsync {
            val mBackEnv = GLES20BackEnv_IR(width, height)
            if (isBitmapDraw) {
                mBackEnv.setRenderer(GLBitmapRenderer())
            } else {
                mBackEnv.setRenderer(GLRGBRenderer())
            }
            mBackEnv.setInput(ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap(), true, time, isMaxWarn, warnTemp)
        }
    }

    fun getVLPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData, time: Long, isMaxWarn: Boolean, warnTemp: Float) {
        doAsync {
            val mBackEnv = GLES20BackEnv_VL(width, height)
            mBackEnv.setRenderer(GLFrameRenderer())
            mBackEnv.setInput(App.yData, App.uData, App.vData, HcvisionUtil.width, HcvisionUtil.height, ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap(), time = time, isMaxWarn = isMaxWarn, warnTemp = warnTemp)
        }
    }

    private fun getNext(is8byte: Boolean = false): IR_ImageData {
        val ir_ImageData = IR_ImageData()
        val raw = ShortArray(cameraUtil.deviceInfo.raw_length)
        cameraUtil.nextFrame(raw)
        if (is8byte) {
            val bmp = ByteArray(cameraUtil.deviceInfo.bmp_length)
            cameraUtil.img_14To8(raw, bmp)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.size, options)
            ir_ImageData.bitmap = bitmap
        } else {
            val bmp = ShortArray(cameraUtil.deviceInfo.raw_length)
            cameraUtil.img_14To565(raw, bmp)
            ir_ImageData.buffer = ShortBuffer.wrap(bmp)
        }
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

    private fun drawCross(canvas: Canvas, ir_imageData: IR_ImageData) {
        val maxStr = "Max:${ir_imageData.max_temp}"
        var x = ir_imageData.max_x * rateW
        var y = ir_imageData.max_y * rateH
        canvas.drawLine(x - crossSize, y, x + crossSize, y, blackPaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, blackPaint)
        canvas.drawLine(x - crossSize, y, x + crossSize, y, whitePaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, whitePaint)
        var rect = Rect()
        blackPaint.getTextBounds(maxStr, 0, maxStr.length, rect)
        var text_x: Float
        var text_y: Float
        text_x = if (viewWidth - x < rect.width())
            x - rect.width().toFloat() - offSize.toFloat()
        else
            x + offSize
        text_y = if (y < rect.height()) y + rect.height().toFloat() + offSize.toFloat() else y - offSize
        drawText(canvas, maxStr, text_x, text_y)

        val minStr = "Min:${ir_imageData.min_temp}"
        x = ir_imageData.min_x * rateW
        y = ir_imageData.min_y * rateH
        canvas.drawLine(x - crossSize, y, x + crossSize, y, blackPaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, blackPaint)
        canvas.drawLine(x - crossSize, y, x + crossSize, y, whitePaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, whitePaint)
        rect = Rect()
        blackPaint.getTextBounds(minStr, 0, minStr.length, rect)
        text_x = if (viewWidth - x < rect.width())
            x - rect.width().toFloat() - offSize.toFloat()
        else
            x + offSize
        text_y = if (y < rect.height()) y + rect.height().toFloat() + offSize.toFloat() else y - offSize
        drawText(canvas, minStr, text_x, text_y)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float) {
        canvas.drawText(text, x, y, blackPaint)
        canvas.drawText(text, x, y, whitePaint)
    }

}
