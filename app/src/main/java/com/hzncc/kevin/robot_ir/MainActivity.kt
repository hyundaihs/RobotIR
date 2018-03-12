package com.hzncc.kevin.robot_ir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.data.Log_Data
import com.hzncc.kevin.robot_ir.jni.CameraUtil
import com.hzncc.kevin.robot_ir.jni.Device_w324_h256
import com.hzncc.kevin.robot_ir.jni.HcvisionUtil
import com.hzncc.kevin.robot_ir.jni.LeptonStatus
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.renderers.GLRGBRenderer
import com.hzncc.kevin.robot_ir.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.per_gallery_list_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.nio.ShortBuffer


class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View) {
        handle = true
        when (v.id) {
            R.id.openIR -> openIR()
            R.id.openVL -> openCamera()
            R.id.openWarn -> openWarn()
            R.id.max -> showPickers(true)
            R.id.min -> showPickers()
        }
    }

    private fun openIR() {
        if (openIR.text == "开启红外") {
            cameraUtil.open("192.168.3.231", 50001)
            cameraUtil.start()
            openIR.text = "关闭红外"
        } else {
            cameraUtil.stop()
            openIR.text = "开启红外"
        }
    }

    private fun openCamera() {
        if (openVL.text == "开启可见光") {
            doAsync {
                if (hcvisionUtil.init()) {
                    E("init success")
                } else {
                    E("init failed")
                }
                if (hcvisionUtil.login()) {
                    E("login success")
                } else {
                    E("login failed")
                }

                if (hcvisionUtil.startPreview(hcRender)) {
                    E("startPreview success")
                } else {
                    E("startPreview failed")
                }
            }
            openVL.text = "关闭可见光"
        } else {
            openVL.text = "开启可见光"
            if (HcvisionUtil.m_iLogID > 0) {
                hcvisionUtil.stopPreview()
            }
        }
    }

    private fun openWarn() {
        isWarn = !isWarn
        if (isWarn) {
            openWarn.text = "关闭报警"
        } else {
            openWarn.text = "开启报警"
        }
    }

    private var maxWarn by Preference("max_warn", 0)
    private var minWarn by Preference("min_warn", 0)
    private var correctTemp: Float by Preference("correct_temp", 0.0f)

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(actionSaveBitmap)
        registerReceiver(broadcastReceiver, intentFilter)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        closeThread()
        cameraUtil.close()
        hcvisionUtil.stopPreview()
        surfaceViewL.onPause()
        surfaceViewR.onPause()
        super.onDestroy()
    }


    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == actionSaveBitmap) {
                adapter.notifyItemChanged(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceViewL.setEGLContextClientVersion(2)
        surfaceViewR.setEGLContextClientVersion(2)
        irRender = GLRGBRenderer(surfaceViewL)
        surfaceViewL.setRenderer(irRender)
        surfaceViewL.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcRender = GLFrameRenderer(surfaceViewR)
        surfaceViewR.setRenderer(hcRender)
        surfaceViewR.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcvisionUtil = HcvisionUtil()
        cameraUtil = CameraUtil(Device_w324_h256())
        openThread()
        adapter = MyAdapter(this, App.instance.mData, { v, position ->
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("index", position)
            startActivity(intent)
        })
        gallery.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        gallery.adapter = adapter
        surfaceViewR.setOnClickListener {
            if (right_buttons.visibility == View.VISIBLE) {
                right_buttons.startAnimation(toR)
            } else {
                right_buttons.startAnimation(fromR)
            }
        }
        fromR.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                countTimeGone()
            }

            override fun onAnimationStart(animation: Animation?) {
                right_buttons.visibility = View.VISIBLE
            }

        })
        toR.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                right_buttons.visibility = View.GONE
            }

            override fun onAnimationStart(animation: Animation?) {
            }

        })
        initDatas()
    }

    private fun initDatas() {
        val file = File(SDCardUtil.IMAGE_IR)
        val files = file.listFiles()
        App.instance.mData.clear()
        App.instance.mData.addAll(FileUtil.getFileName(files, "jpeg"))
        adapter.notifyDataSetChanged()
        max.text = "高温报警:$maxWarn℃"
        min.text = "低温报警:$minWarn℃"
        correct.text = "温度补偿:$correctTemp℃"
        correct.setOnClickListener {
            showCorrectTmpPickers()
        }
    }

    private fun countTimeGone() {
        doAsync {
            Thread.sleep(5000)
            uiThread {
                if (!handle) {
                    right_buttons.startAnimation(toR)
                }
            }
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
    private lateinit var adapter: MyAdapter
    val broadcastReceiver: MyBroadcastReceiver = MyBroadcastReceiver()
    lateinit var hcvisionUtil: HcvisionUtil
    lateinit var cameraUtil: CameraUtil
    val fromR by lazy {
        AnimationUtils.loadAnimation(this, R.anim.slide_from_right)
    }
    val toR by lazy {
        AnimationUtils.loadAnimation(this, R.anim.slide_to_right)
    }

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
            synchronized(surfaceViewL) {
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
                        App.ir_imageData = getNext()
                        irRender.update(App.ir_imageData)
                        if (isWarn) {
                            if (App.ir_imageData.max_temp >= maxWarn || App.ir_imageData.min_temp <= minWarn) {
                                if (!warning) {
                                    warning = true
                                    val calendarUtil = CalendarUtil()
                                    val fileName = "${calendarUtil.format(CalendarUtil.FILENAME)}.jpeg"
                                    getIRPic(surfaceViewL.width, surfaceViewL.height, fileName, App.ir_imageData)
                                    getVLPic(surfaceViewR.width, surfaceViewR.height, fileName, App.ir_imageData)
                                }
                            } else {
                                warning = false
                            }
                        }
                    }
                }
            }
        }
    }

    fun getIRPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData) {
        doAsync {
            val mBackEnv = GLES20BackEnv_IR(width, height)
            mBackEnv.setRenderer(GLRGBRenderer())
            mBackEnv.setInput(ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap(), true)
        }
    }

    fun getVLPic(width: Int, height: Int, fileName: String, ir_imageData: IR_ImageData) {
        doAsync {
            val mBackEnv = GLES20BackEnv_VL(width, height)
            mBackEnv.setRenderer(GLFrameRenderer())
            mBackEnv.setInput(App.yData, App.uData, App.vData, HcvisionUtil.width, HcvisionUtil.height, ir_imageData)
            saveBitmap(fileName, mBackEnv.getBitmap())
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

    private class MyAdapter(val context: Context, val mData: List<Log_Data>,
                            val onItemClick: (view: View, position: Int) -> Unit)
        : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val logData = mData[position]
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            holder.itemView.irImage.setImageBitmap(
                    BitmapFactory.decodeFile(SDCardUtil.IMAGE_IR + logData.irImage, options))
            holder.itemView.vlImage.setImageBitmap(
                    BitmapFactory.decodeFile(SDCardUtil.IMAGE_VL + logData.vlImage, options))
//            Picasso.with(context).load(File(SDCardUtil.IMAGE_IR + logData.irImage))
//                    .into(holder.itemView.irImage)
//            Picasso.with(context).load(File(SDCardUtil.IMAGE_VL + logData.vlImage))
//                    .into(holder.itemView.vlImage)
            holder.itemView.setOnClickListener {
                onItemClick.invoke(holder.itemView, position)
            }
        }

        override fun getItemCount(): Int = mData.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent?.context).inflate(R.layout.per_gallery_list_item, null, false)
            return ViewHolder(view)
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    }

}
