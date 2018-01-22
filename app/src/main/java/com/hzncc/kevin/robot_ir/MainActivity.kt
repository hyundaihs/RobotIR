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
import android.view.*
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.data.Log_Data
import com.hzncc.kevin.robot_ir.jni.CameraUtil
import com.hzncc.kevin.robot_ir.jni.Device_w324_h256
import com.hzncc.kevin.robot_ir.jni.HcvisionUtil
import com.hzncc.kevin.robot_ir.jni.LeptonStatus
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer
import com.hzncc.kevin.robot_ir.renderers.GLFrameRenderer
import com.hzncc.kevin.robot_ir.utils.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.per_gallery_list_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity(){

    private var maxWarn by Preference("max_warn", 0)
    private var minWarn by Preference("min_warn", 0)

    override fun onPause() {
        super.onPause()
//        if (null != cameraUtil && cameraUtil!!.isOpened) {
//            surfaceViewL.onPause()
//        }
//        if (hcRenderSet) {
//            surfaceViewR.onPause()
//        }
        unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
//        if (null != cameraUtil && cameraUtil!!.isOpened) {
//            surfaceViewL.onResume()
//        }
//        if (hcRenderSet) {
//            surfaceViewR.onResume()
//        }
        val intentFilter = IntentFilter(actionSaveBitmap)
        registerReceiver(broadcastReceiver, intentFilter)
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
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action === actionSaveBitmap) {
                val fileName = intent.getStringExtra("fileName")
                App.instance.mData.add(0, Log_Data(fileName, fileName))
                adapter?.notifyItemChanged(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceViewL.setEGLContextClientVersion(2)
        surfaceViewR.setEGLContextClientVersion(2)
        irRender = GLBitmapRenderer(surfaceViewL)
        surfaceViewL.setRenderer(irRender)
        hcRender = GLFrameRenderer(surfaceViewR)
        surfaceViewR.setRenderer(hcRender)
        surfaceViewR.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        hcvisionUtil = HcvisionUtil()
        cameraUtil = CameraUtil(Device_w324_h256())
        openThread()
        open.setOnClickListener {
            if (!cameraUtil.isOpened || HcvisionUtil.m_iLogID < 0) {
                doAsync {
                    cameraUtil.open("192.168.1.231", 50001)
                    cameraUtil.start()

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

                    if (hcvisionUtil.startPreview(hcRender!!)) {
                        E("startPreview success")
                    } else {
                        E("startPreview failed")
                    }
                    uiThread {
                        open.text = "关闭监控"
                    }
                }
            } else {
                doAsync {
                    cameraUtil.close()
                    hcvisionUtil.stopPreview()
                    uiThread {
                        open.text = "开启监控"
                    }
                }
            }
        }
        openWarn.setOnClickListener {
            isWarn = !isWarn
            if (isWarn) {
                openWarn.text = "关闭报警"
            } else {
                openWarn.text = "开启报警"
            }
        }
        adapter = MyAdapter(this, App.instance.mData, { v, position ->
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("index", position)
            startActivity(intent)
        })
        gallery.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        gallery.adapter = adapter
        val file = File(SDCardUtil.IMAGE_IR)
        val files = file.listFiles()
        App.instance.mData.addAll(FileUtil.getFileName(files, "png"))
        adapter?.notifyDataSetChanged()
        max.setOnClickListener {
            showPickers(true)
        }
        min.setOnClickListener {
            showPickers()
        }
        maxData.addAll(optionsMax)
        minData.addAll(optionsMin)
        max.text = "高温报警:$maxWarn"
        min.text = "低温报警:$minWarn"
    }

    private fun showPickers(isMax: Boolean = false) = if (isMax) {
        var select = 0
        if (maxWarn > 0) {
            select = maxData.indexOf(maxWarn)
        } else {
            select = 1
        }
        showPicker(select, maxData, isMax, { //
            content: String, position: Int ->
            max.text = "高温报警:$content"
            maxWarn = content.toInt()
        })
    } else {
        var select = 0
        if (minWarn > 0) {
            select = minData.indexOf(minWarn)
        } else {
            select = 1
        }
        showPicker(select, minData, isMax, { //
            content, position ->
            min.text = "低温报警:$content"
            minWarn = content.toInt()
        })
    }

    fun closeThread() {
        isRun = false
    }

    private var hcRender: GLFrameRenderer? = null
    private var irRender: GLBitmapRenderer? = null
    private val maxData = ArrayList<Int>()
    private val minData = ArrayList<Int>()
    private var adapter: MyAdapter? = null
    val broadcastReceiver: MyBroadcastReceiver = MyBroadcastReceiver()
    lateinit var hcvisionUtil: HcvisionUtil
    lateinit var cameraUtil: CameraUtil

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

        val optionsMax = arrayOf(//"
                59, 58, 57, 56, 55, 54, 53, 52, 51, 50,
                49, 48, 47, 46, 45, 44, 43, 42, 41, 40,
                39, 38, 37, 36, 35, 34, 33, 32, 31, 30,
                29, 28, 27, 26, 25, 24, 23, 22, 21, 20,
                19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
                9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
        val optionsMin = arrayOf(//
                59, 58, 57, 56, 55, 54, 53, 52, 51, 50,
                49, 48, 47, 46, 45, 44, 43, 42, 41, 40,
                39, 38, 37, 36, 35, 34, 33, 32, 31, 30,
                29, 28, 27, 26, 25, 24, 23, 22, 21, 20,
                19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
                9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

    }

    fun initDraw(surfaceView: SurfaceView) {
        viewWidth = surfaceView.getWidth()
        viewHeight = surfaceView.getHeight()
        isRun = true
        bitPaint.setAntiAlias(true)
        bitPaint.setFilterBitmap(true)

        blackPaint.setTextAlign(Paint.Align.CENTER)
        whitePaint.setTextAlign(Paint.Align.CENTER)

        blackPaint.setStrokeWidth(4f)
        blackPaint.setStyle(Paint.Style.STROKE)
        blackPaint.setFakeBoldText(true)
        blackPaint.setColor(Color.BLACK)

        whitePaint.setStyle(Paint.Style.FILL)
        whitePaint.setStrokeWidth(2f)
        whitePaint.setColor(Color.WHITE)

        val size = DisplayUtil.sp2px(this, 16)
        blackPaint.setTextSize(size.toFloat())
        whitePaint.setTextSize(size.toFloat())

        crossSize = DisplayUtil.sp2px(this, 5)
        offSize = DisplayUtil.sp2px(this, 5)
        measureRate()
    }

    fun measureRate() {
        rateW = viewWidth.toFloat() / cameraUtil.getDeviceInfo()?.width!!
        rateH = viewHeight.toFloat() / cameraUtil.getDeviceInfo()?.height!!
    }


    fun openThread() {
        doAsync {
            synchronized(surfaceViewL) {
                initDraw(surfaceViewL)
                while (isRun) {
                    if (cameraUtil.getStatus() == LeptonStatus.STATUS_RUNNING) {
                        App.instance.ir_imageData = getNext()
                        if (isWarn) {
                            if (App.instance.ir_imageData!!.max_temp >= maxWarn || App.instance.ir_imageData!!.min_temp <= minWarn) {
                                val calendarUtil = CalendarUtil()
                                val fileName = "${calendarUtil.format(CalendarUtil.FILENAME)}.jpeg"
                                irRender?.takePicture(fileName)
                                hcRender?.takePicture(fileName)
                            }
                        }
                        if (!App.instance.ir_imageData?.bitmap!!.isRecycled) {
                            irRender?.update(App.instance.ir_imageData!!)
                        }
//                        val ir_imageData2: IR_ImageData = ir_imageData.copy(bitmap = null)
//                        hcRender?.update(ir_imageData2)
                    }
                }
            }
        }
    }

    private fun getNext(): IR_ImageData {
        val ir_ImageData = IR_ImageData()
        val raw = ShortArray(cameraUtil.deviceInfo?.raw_length!!)
        cameraUtil.nextFrame(raw)

        val bmp = ByteArray(cameraUtil.deviceInfo?.bmp_length!!)
        cameraUtil.img_14To8(raw, bmp)
        //        createFileWithByte(bmp)
        //        Bitmap bitmap = Bitmap.createBitmap(bmp, cameraUtil?.getDeviceInfo().width,
        //                cameraUtil?.getDeviceInfo().height, Bitmap.Config.RGB_565)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.size, options)
        ir_ImageData.buffer = ByteBuffer.wrap(bmp)
        ir_ImageData.bitmap = bitmap
        ir_ImageData.width = bitmap.width
        ir_ImageData.height = bitmap.height
        ir_ImageData.max_x = cameraUtil.maxX
        ir_ImageData.max_y = cameraUtil.maxY
        ir_ImageData.min_x = cameraUtil.minX
        ir_ImageData.min_y = cameraUtil.minY
        ir_ImageData.max_temp = cameraUtil.maxTemp
        ir_ImageData.min_temp = cameraUtil.minTemp
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
//            holder.itemView.irImage.setImageURI(SDCardUtil.IMAGE_IR + logData.irImage)
//            holder.itemView.vlImage.setImageURI(SDCardUtil.IMAGE_VL + logData.vlImage)
            Picasso.with(context).load(File(SDCardUtil.IMAGE_IR + logData.irImage))
                    .into(holder.itemView.irImage)
            Picasso.with(context).load(File(SDCardUtil.IMAGE_VL + logData.vlImage))
                    .into(holder.itemView.vlImage)
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
