package com.hzncc.kevin.robot_ir

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLException
import android.util.Log
import com.bigkoo.pickerview.OptionsPickerView
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.utils.BitmapUtil
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import org.jetbrains.anko.doAsync
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/8.
 */
fun Any.D(msg: String) {
    Log.d(this.javaClass.simpleName, msg)
}

fun Any.E(msg: String) {
    Log.e(this.javaClass.simpleName, msg)
}

fun Any.I(msg: String) {
    Log.i(this.javaClass.simpleName, msg)
}

fun Any.detectOpenGLES20(context: Context): Boolean {
    val am: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = am.deviceConfigurationInfo
    return info.reqGlEsVersion >= 0x20000
}

var ir_ImageData: IR_ImageData? = null

/**
 * @param vertexs float 数组
 * @return 获取浮点形缓冲数据
 */
fun Any.getFloatBuffer(vertexs: FloatArray): FloatBuffer {
    val buffer: FloatBuffer
    val qbb = ByteBuffer.allocateDirect(vertexs.size * 4)
    qbb.order(ByteOrder.nativeOrder())
    buffer = qbb.asFloatBuffer()
    //写入数组
    buffer.put(vertexs)
    //设置默认的读取位置
    buffer.position(0)
    return buffer
}

/**
 * @param vertexs float 数组
 * @return 获取浮点形缓冲数据
 */
fun Any.getByteBuffer(vertexs: FloatArray): ByteBuffer {
    val buffer: ByteBuffer
    buffer = ByteBuffer.allocateDirect(vertexs.size * 4)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asFloatBuffer().put(vertexs)
    buffer.position(0)
    return buffer
}

/**
 * create shader with given source.
 */
fun Any.loadShader(shaderType: Int, source: String): Int {
    var shader = GLES20.glCreateShader(shaderType)
    if (shader != 0) {
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("tag", "Could not compile shader $shaderType:", null)
            Log.e("tag", GLES20.glGetShaderInfoLog(shader), null)
            GLES20.glDeleteShader(shader)
            shader = 0
        }
    }
    return shader
}

/**
 * android中绘制字体，使用画布canvas
 */
fun Any.initFontBitmap(font: String, isMax: Boolean = false): Bitmap {
    val p = Paint()
    //字体设置
    val fontType = "宋体"
    val typeface = Typeface.create(fontType, Typeface.BOLD)
    //消除锯齿
    p.setAntiAlias(true)
    //字体为红色
    p.setColor(Color.WHITE)
    p.setTypeface(typeface)
    p.setTextSize(28f)
    val rect = Rect()
    p.getTextBounds(font, 0, font.length, rect)

    val textBitmap = Bitmap.createBitmap(rect.width() + 6, rect.height() + 6, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(textBitmap)
    if (isMax) {
        //背景颜色
        canvas.drawColor(Color.RED)
    } else {
        //背景颜色
        canvas.drawColor(Color.BLUE)
    }
    //绘制字体
    canvas.drawText(font, 3f, rect.height().toFloat() + 3, p)
    return textBitmap
}

fun Any.createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int, gl: GL10): Bitmap? {
    val bitmapBuffer = IntArray(w * h)
    val bitmapSource = IntArray(w * h)
    val intBuffer = IntBuffer.wrap(bitmapBuffer)
    intBuffer.position(0)
    try {
        gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                intBuffer)
        var offset1: Int
        var offset2: Int
        for (i in 0 until h) {
            offset1 = i * w
            offset2 = (h - i - 1) * w
            for (j in 0 until w) {
                val texturePixel = bitmapBuffer[offset1 + j]
                val blue = texturePixel shr 16 and 0xff
                val red = texturePixel shl 16 and 0x00ff0000
                val pixel = texturePixel and -0xff0100 or red or blue
                bitmapSource[offset2 + j] = pixel
            }
        }
    } catch (e: GLException) {
        return null
    }

    return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
}

val actionSaveBitmap: String = "ACTION_SAVE_BITMAP"

fun Any.saveBitmap(context: Context, fileName: String, bitmap: Bitmap, isIR: Boolean = false) {
    doAsync {
        var path = ""
        if (isIR) {
            path = SDCardUtil.IMAGE_IR
        } else {
            path = SDCardUtil.IMAGE_VL
        }
        BitmapUtil.saveBitmap(path, fileName, bitmap)
        val intent = Intent(actionSaveBitmap)
        intent.putExtra("fileName", fileName)
        context.sendBroadcast(intent)
    }
}

fun Context.showPicker(selected: Int = 1, list: List<Int>, isMax: Boolean, listener: (String, Int) -> Unit) {
    val pvOptions = OptionsPickerView.Builder(this, OptionsPickerView.OnOptionsSelectListener { options1, option2, options3, v ->
        //返回的分别是三个级别的选中位置
        val tx = list[options1].toString()
        listener.invoke(tx, options1)
    })
            .setSubmitText("确定")//确定按钮文字
            .setCancelText("取消")//取消按钮文字
            .setTitleText(if (isMax) "最高温度报警" else "最低温度报警")//标题
            .setSubCalSize(18)//确定和取消文字大小
            .setTitleSize(20)//标题文字大小
            .setTitleColor(Color.BLACK)//标题文字颜色
            .setSubmitColor(Color.BLUE)//确定按钮文字颜色
            .setCancelColor(Color.BLUE)//取消按钮文字颜色
//                .setTitleBgColor(-0xcccccd)//标题背景颜色 Night mode
//                .setBgColor(-0x1000000)//滚轮背景颜色 Night mode
            .setContentTextSize(18)//滚轮文字大小
            .setLinkage(false)//设置是否联动，默认true
            //                .setLabels("省", "市", "区")//设置选择的三级单位
            //                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
            .setCyclic(false, false, false)//循环与否
            .setSelectOptions(selected, 1, 1)  //设置默认选中项
            .setOutSideCancelable(true)//点击外部dismiss default true
            .isDialog(true)//是否显示为对话框样式
            .build()
    pvOptions.setPicker(list)//添加数据源
    pvOptions.show()
}

