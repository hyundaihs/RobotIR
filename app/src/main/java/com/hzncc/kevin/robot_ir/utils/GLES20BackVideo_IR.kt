package com.hzncc.kevin.robot_ir.utils

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.GLES20
import android.util.Log
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.renderers.GLRGBRenderer
import com.hzncc.kevin.robot_ir.utils.EGLHelper.SURFACE_WINDOW
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer


/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/3/9.
 */

class GLES20BackVideo_IR(private val mWidth: Int, private val mHeight: Int) {

    private val mEGLHelper: EGLHelper
    private var renderer: GLRGBRenderer? = null
    private lateinit var mInputBuffers: Array<ByteBuffer>
    private lateinit var mOutputBuffers: Array<ByteBuffer>
    private var mMuxer: MediaMuxer? = null

    val bitmap: Bitmap?
        get() {
            if (renderer == null) {
                Log.e(TAG, "getBitmap: Renderer was not set.")
                return null
            }
            renderer!!.onDrawFrame(mEGLHelper.mGL)
            return convertToBitmap()
        }

    init {
        mEGLHelper = EGLHelper()
        try {
            mMuxer = MediaMuxer(SDCardUtil.VIDEO + "surface.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            Log.i(TAG, "init mmuxer error " + e)
        }
//        val videoCodecInfo = selectCodec(VideoEncoder.MIME_TYPE)
//        if (videoCodecInfo == null) {
//            Log.e(TAG, "Unable to find an appropriate codec for " + VideoEncoder.MIME_TYPE)
//            return
//        }
        val format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)  // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)//设置码率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)//设置帧率
        if (!mIsAllKeyFrame) {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)//设置全关键帧
        }
        try {
            val mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // get Surface for encoder input
            // this method only can call between #configure and #start
            val mSurface = mMediaCodec.createInputSurface()    // API >= 18
            mMediaCodec.start()

            mEGLHelper.setSurfaceType(SURFACE_WINDOW, mSurface)
            Log.i(TAG, "prepare finishing")
        } catch (e: IOException) {
            Log.e(TAG, "" + e)
            e.printStackTrace()
        }
        mEGLHelper.eglInit(mWidth, mHeight)
    }

    fun setRenderer(renderer: GLRGBRenderer) {
        this.renderer = renderer
        renderer.onSurfaceCreated(mEGLHelper.mGL, mEGLHelper.mEglConfig)
        renderer.onSurfaceChanged(mEGLHelper.mGL, mWidth, mHeight)
    }

    fun destroy() {
        mEGLHelper.destroy()
    }


    private fun convertToBitmap(): Bitmap {
        val iat = IntArray(mWidth * mHeight)
        val ib = IntBuffer.allocate(mWidth * mHeight)
        mEGLHelper.mGL.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                ib)
        val ia = ib.array()

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (i in 0 until mHeight) {
            System.arraycopy(ia, i * mWidth, iat, (mHeight - i - 1) * mWidth, mWidth)
        }
        val bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat))
        return bitmap
    }

    fun setInput(iR_ImageData: IR_ImageData) {
        renderer!!.update(iR_ImageData)
    }

    companion object {
        internal val TAG = "GLES20BackEnv_IR"
        protected val MIME_TYPE = "video/avc"
        protected val FRAME_RATE = 30
        protected val BIT_RATE = 4 * 1024 * 1024
        protected val IFRAME_INTERVAL = 1//1秒
        protected var mIsAllKeyFrame = false
        protected val TIMEOUT_USEC: Long = 10000
    }
}
