package com.hzncc.kevin.robot_ir.renderers

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.hzncc.kevin.robot_ir.*
import com.hzncc.kevin.robot_ir.App.Companion.pointIR1_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR1_y
import com.hzncc.kevin.robot_ir.App.Companion.pointIR2_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR2_y
import com.hzncc.kevin.robot_ir.App.Companion.pointIR3_x
import com.hzncc.kevin.robot_ir.App.Companion.pointIR3_y
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.textures.TextBitmap
import com.hzncc.kevin.robot_ir.textures.TextureBitmap
import com.hzncc.kevin.robot_ir.textures.Triangle
import com.hzncc.kevin.robot_ir.utils.Preference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/16.
 */
class GLBitmapRenderer(private val mTargetSurface: GLSurfaceView? = null) : MyGlRenderer() {

    override fun update(`object`: Any?) {
        if (`object` is IR_ImageData) {
            val ir_ImageData = `object`
            synchronized(this) {
                bitmap = ir_ImageData.bitmap
                if (isPeizhun) {
                    mTriangle.updateVertex(ir_ImageData, pointIR1_x, pointIR1_y, pointIR2_x, pointIR2_y, pointIR3_x, pointIR3_y)
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
            mTargetSurface?.requestRender()
        }
    }

    fun setPeizhun(isPz: Boolean) {
        isPeizhun = isPz
        mTriangle.isPeizhun = isPz
    }


    private var isPeizhun = false
    private val prog = TextureBitmap()
    private var maxTexBitmap = TextBitmap()
    private var minTexBitmap = TextBitmap()
    private var bitmap: Bitmap? = null
    private var maxBitmap: Bitmap? = null
    private var minBitmap: Bitmap? = null
    private var mTriangle: Triangle = Triangle()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
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
            mTriangle.draw()
            maxTexBitmap.drawFrame()
            minTexBitmap.drawFrame()
        }
    }
}