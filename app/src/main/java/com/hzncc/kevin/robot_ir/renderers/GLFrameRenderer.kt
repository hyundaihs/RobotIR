package com.hzncc.kevin.robot_ir.renderers

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.initFontBitmap
import com.hzncc.kevin.robot_ir.saveBitmap
import com.hzncc.kevin.robot_ir.textures.TextBitmap
import com.hzncc.kevin.robot_ir.textures.TextureYuv
import com.hzncc.kevin.robot_ir.textures.Triangle
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/15.
 */

class GLFrameRenderer(private val mTargetSurface: GLSurfaceView? = null) : GLSurfaceView.Renderer {
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
    private var mTriangle: Triangle? = null

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d("tag", "GLFrameRenderer :: onSurfaceCreated")
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        if (!prog.isProgramBuilt) {
            prog.buildProgram()
            maxTexBitmap.buildProgram()
            minTexBitmap.buildProgram()
            //初始化三角形
            mTriangle = Triangle()
            Log.d("tag", "GLFrameRenderer :: buildProgram done")
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d("tag", "GLFrameRenderer :: onSurfaceChanged")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        synchronized(this) {
//                        if (isTakePicture) {
//                val bmp = createBitmapFromGLSurface(0, 0, mTargetSurface.width, mTargetSurface.height, gl)
//                saveBitmap(mTargetSurface.context, fileName, bmp!!)
//                isTakePicture = false
//            }
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
                mTriangle?.draw()
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
    fun update(ydata: ByteArray, udata: ByteArray, vdata: ByteArray,ir_ImageData: IR_ImageData) {
        synchronized(this) {
            y!!.clear()
            u!!.clear()
            v!!.clear()
            y!!.put(ydata, 0, ydata.size)
            u!!.put(udata, 0, udata.size)
            v!!.put(vdata, 0, vdata.size)
            update(ir_ImageData)
        }

        mTargetSurface?.requestRender()
    }

    private fun update(ir_ImageData: IR_ImageData) {
        mTriangle?.updateVertex(ir_ImageData)
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
