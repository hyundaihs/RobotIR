package com.hzncc.kevin.robot_ir

import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
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
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.qi_han.QiHanConnectUtil
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.renderers.GLRGBRenderer
import com.hzncc.kevin.robot_ir.service.MonitorService
import com.hzncc.kevin.robot_ir.service.MyService
import com.hzncc.kevin.robot_ir.service.MyService.Companion.cameraUtil
import com.hzncc.kevin.robot_ir.service.MyService.Companion.hcvisionUtil
import com.hzncc.kevin.robot_ir.utils.FileUtil
import com.hzncc.kevin.robot_ir.utils.HcvisionUtil
import com.hzncc.kevin.robot_ir.utils.Preference
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import com.hzsk.camera.LeptonStatus
import com.sp.peizhun.PeiZhunUtil
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.io.File


class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View) {
        handle = true
        when (v.id) {
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

    private fun startService() {
        startService(Intent(this, MyService::class.java))
        startService(Intent(this, MonitorService::class.java))
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

    }

    override fun onDestroy() {
        closeThread()
        cameraUtil.close()
        hcvisionUtil.stopPreview()
        hcvisionUtil.loginOut()
        surfaceViewL.onPause()
        surfaceViewR.onPause()
        super.onDestroy()
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == actionSaveBitmap) {
                irImage.setImageBitmap(
                        BitmapFactory.decodeFile(SDCardUtil.IMAGE_IR + App.instance.mData[0].irImage))
                vlImage.setImageBitmap(
                        BitmapFactory.decodeFile(SDCardUtil.IMAGE_VL + App.instance.mData[0].vlImage))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        surfaceViewL.setEGLContextClientVersion(2)
        surfaceViewR.setEGLContextClientVersion(2)
        bitIrRender = GLBitmapRenderer(surfaceViewL)
        surfaceViewL.setRenderer(bitIrRender)
        surfaceViewL.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcRender = GLFrameRenderer(surfaceViewR)
        surfaceViewR.setRenderer(hcRender)
        surfaceViewR.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)

        irImage.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        vlImage.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        ipAlert.setOnClickListener {
            showDialog()
        }

        initDatas()
        openThread()

        startService()
    }

    override fun onBackPressed() {
        home()
    }

    fun home() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }

    fun showDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("修改IP地址")
        val view = LayoutInflater.from(this).inflate(R.layout.alert_ip_layout,
                null, false)
        val irEdit = view.findViewById<EditText>(R.id.irEdit)
        val vlEdit = view.findViewById<EditText>(R.id.vlEdit)

        dialog.setView(view)
        irEdit.setText(irIP)
        vlEdit.setText(vlIP)
        dialog.setPositiveButton("修改", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (irEdit.text.trim().isEmpty() || vlEdit.text.trim().isEmpty()) {
                    toast("IP配置失败")
                    return
                }
                irIP = irEdit.text.toString()
                vlIP = vlEdit.text.toString()
                startService()
            }
        })
        dialog.setNegativeButton("取消", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                toast("取消配置")
            }
        })

        dialog.create()
        dialog.show()
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

        if (App.instance.mData.size > 0) {
            irImage.setImageBitmap(
                    BitmapFactory.decodeFile(SDCardUtil.IMAGE_IR + App.instance.mData[0].irImage))
            vlImage.setImageBitmap(
                    BitmapFactory.decodeFile(SDCardUtil.IMAGE_VL + App.instance.mData[0].vlImage))
        }
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
        bitIrRender.setPeizhun(isPeizhun)
        hcRender.setPeizhun(isPeizhun)
    }

    private fun savePeizhun() {
        peizhun.text = "配准"
        isPeizhun = false
        bitIrRender.setPeizhun(isPeizhun)
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


    companion object {
        var irIP: String by Preference("ir_ip", "10.217.39.201")
        var vlIP: String by Preference("vl_ip", "10.217.39.200")
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
                MyService.viewWidth = surfaceViewL.width
                MyService.viewHeight = surfaceViewL.height
                if (cameraUtil.getStatus() == LeptonStatus.STATUS_RUNNING) {
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
                    bitIrRender.update(App.ir_imageData)
                    hcRender.update(HcvisionUtil.width, HcvisionUtil.height)
                    if (MyService.hcvisionUtil.dataCacheUtil != null) {
                        hcRender.update(MyService.hcvisionUtil.dataCacheUtil!!.read(), App.ir_imageData)
                    }
                }
            }
        }
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
