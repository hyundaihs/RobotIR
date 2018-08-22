package com.hzncc.kevin.robot_ir.renderers

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.hzncc.kevin.robot_ir.App
import com.hzncc.kevin.robot_ir.App.Companion.isPeizhund
import com.hzncc.kevin.robot_ir.App.Companion.pointVL1_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL1_y
import com.hzncc.kevin.robot_ir.App.Companion.pointVL2_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL2_y
import com.hzncc.kevin.robot_ir.App.Companion.pointVL3_x
import com.hzncc.kevin.robot_ir.App.Companion.pointVL3_y
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.initFontBitmap
import com.hzncc.kevin.robot_ir.textures.TextBitmap
import com.hzncc.kevin.robot_ir.textures.TextureYuv
import com.hzncc.kevin.robot_ir.textures.Triangle
import com.hzncc.kevin.robot_ir.utils.HcvisionUtil
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/15.
 */

class GLFrameRenderer(private val mTargetSurface: GLSurfaceView? = null) : MyGlRenderer() {

    override fun update(`object`: Any?) {
        if (`object` is IR_ImageData) {
            val ir_ImageData = `object`
            if (isPeizhund) {
                val posMax = countPosition(ir_ImageData.max_x, ir_ImageData.max_y)
                val posMin = countPosition(ir_ImageData.min_x, ir_ImageData.min_y)
                ir_ImageData.max_x = posMax[0]
                ir_ImageData.max_y = posMax[1]
                ir_ImageData.min_x = posMin[0]
                ir_ImageData.min_y = posMin[1]
            }

            if (isPeizhun) {
                mTriangle.updateVertex(ir_ImageData, pointVL1_x, pointVL1_y, pointVL2_x, pointVL2_y, pointVL3_x, pointVL3_y)
            } else {
                mTriangle.updateVertex(ir_ImageData)
            }
            maxBitmap = initFontBitmap(ir_ImageData.max_temp, true)
            minBitmap = initFontBitmap(ir_ImageData.min_temp)


            if (null != maxBitmap && !maxBitmap!!.isRecycled) {
                maxTexBitmap.updateVertex(ir_ImageData, ir_ImageData.max_x, ir_ImageData.max_y)
            }
            if (null != minBitmap && !minBitmap!!.isRecycled) {
                minTexBitmap.updateVertex(ir_ImageData, ir_ImageData.min_x, ir_ImageData.min_y)
            }
        }
    }

    fun countPosition(xi: Int, yi: Int): IntArray {
        val pos = IntArray(2)
        val x2 = App.param1
        val x1 = App.param2
        val cx = App.param3
        val y2 = App.param4
        val y1 = App.param5
        val cy = App.param6

        pos[0] = (xi * x2 + yi * x1 + cx).toInt()
        pos[1] = (xi * y2 + yi * y1 + cy).toInt()
        return pos
    }

    fun setPeizhun(isPz: Boolean) {
        isPeizhun = isPz
        mTriangle.isPeizhun = isPz
    }

    private var isPeizhun = false
    private val prog = TextureYuv()
    private var maxTexBitmap = TextBitmap()
    private var minTexBitmap = TextBitmap()
    private var mVideoWidth = -1
    private var mVideoHeight = -1
    private var y: ByteBuffer? = null
    private var u: ByteBuffer? = null
    private var v: ByteBuffer? = null
    private var maxBitmap: Bitmap? = null
    private var minBitmap: Bitmap? = null
    private var mTriangle: Triangle = Triangle()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        if (!prog.isProgramBuilt) {
            prog.buildProgram()
            maxTexBitmap.buildProgram()
            minTexBitmap.buildProgram()
            //初始化三角形
            mTriangle.buildProgram()
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        synchronized(this) {
            if (y != null) {
                // reset position, have to be done
                y!!.position(0)
                u!!.position(0)
                v!!.position(0)
                prog.buildTextures(y!!, u!!, v!!, mVideoWidth, mVideoHeight)
                if (null != maxBitmap && !maxBitmap!!.isRecycled) {
                    maxTexBitmap.buildTextures(maxBitmap!!)
                }
                if (null != minBitmap && !minBitmap!!.isRecycled) {
                    minTexBitmap.buildTextures(minBitmap!!)
                }
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                prog.drawFrame()
                mTriangle.draw()
                maxTexBitmap.drawFrame()
                minTexBitmap.drawFrame()
            }
        }
    }

    /**
     * this method will be called from native code, it happens when the video is about to play or
     * the video size changes.
     */
    fun update(w: Int, h: Int) {
        synchronized(this) {
            if (w > 0 && h > 0) {
                if (w != mVideoWidth && h != mVideoHeight) {
                    this.mVideoWidth = w
                    this.mVideoHeight = h
                    val yarraySize = w * h
                    val uvarraySize = yarraySize / 4
                    synchronized(this) {
                        y = ByteBuffer.allocate(yarraySize)
                        u = ByteBuffer.allocate(uvarraySize)
                        v = ByteBuffer.allocate(uvarraySize)
                    }
                }
            }
        }
    }

    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
    fun update(data: ByteArray, ir_ImageData: IR_ImageData) {
        synchronized(this) {
            y!!.clear()
            u!!.clear()
            v!!.clear()
            y!!.put(data, 0, HcvisionUtil.width * HcvisionUtil.height)
            v!!.put(data, HcvisionUtil.width * HcvisionUtil.height,
                    HcvisionUtil.width * HcvisionUtil.height / 4)
            u!!.put(data, HcvisionUtil.width * HcvisionUtil.height +
                    HcvisionUtil.width * HcvisionUtil.height / 4, HcvisionUtil.width * HcvisionUtil.height / 4)
            update(ir_ImageData)
        }

        mTargetSurface?.requestRender()
    }
}
