package com.hzncc.kevin.robot_ir.renderers

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.hzncc.kevin.robot_ir.createBitmapFromGLSurface
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.initFontBitmap
import com.hzncc.kevin.robot_ir.saveBitmap
import com.hzncc.kevin.robot_ir.textures.TextBitmap
import com.hzncc.kevin.robot_ir.textures.TextureBitmap
import com.hzncc.kevin.robot_ir.textures.Triangle
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/16.
 */
class GLBitmapRenderer(private val mTargetSurface: GLSurfaceView) : GLSurfaceView.Renderer {
    private val prog = TextureBitmap()
    private var maxTexBitmap = TextBitmap()
    private var minTexBitmap = TextBitmap()
    private var bitmap: Bitmap? = null
    private var maxBitmap: Bitmap? = null
    private var minBitmap: Bitmap? = null
    private var mTriangle: Triangle? = null
    private var isTakePicture = false
    private var fileName: String = ""

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d("tag", "GLFrameRenderer :: onSurfaceCreated")
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

    fun takePicture(fileName: String) {
        if (!isTakePicture) {
            this.fileName = fileName
            isTakePicture = true
        }
    }

    override fun onDrawFrame(gl: GL10) {
        synchronized(this) {
            if (isTakePicture) {
                val bmp = createBitmapFromGLSurface(0, 0, mTargetSurface.width, mTargetSurface.height, gl)
                saveBitmap(mTargetSurface.context, fileName, bmp!!, true)
                isTakePicture = false
            }
            if (null != bitmap && !bitmap!!.isRecycled) {
                prog.buildTextures(bitmap!!)
            }
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


    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
//    fun update(bitmap: Bitmap) {
//        synchronized(this) {
//
//        }
//        // request to render
//        mTargetSurface.requestRender()
//    }

    fun update(ir_ImageData: IR_ImageData) {
        synchronized(this) {
            bitmap = ir_ImageData.bitmap
            mTriangle?.updateVertex(ir_ImageData)
            maxBitmap = initFontBitmap(ir_ImageData.max_temp.toString(), true)
            minBitmap = initFontBitmap(ir_ImageData.min_temp.toString())
            if (null != maxBitmap && !maxBitmap!!.isRecycled) {
                maxTexBitmap.updateVertex(ir_ImageData, ir_ImageData.max_x, ir_ImageData.max_y)
            }
            if (null != minBitmap && !minBitmap!!.isRecycled) {
                minTexBitmap.updateVertex(ir_ImageData, ir_ImageData.min_x, ir_ImageData.min_y)
            }
        }
        mTargetSurface.requestRender()
    }

}