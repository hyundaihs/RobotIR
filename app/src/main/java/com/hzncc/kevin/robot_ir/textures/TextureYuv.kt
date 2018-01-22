package com.hzncc.kevin.robot_ir.textures

import android.opengl.GLES20
import android.util.Log
import com.hzncc.kevin.robot_ir.getByteBuffer
import com.hzncc.kevin.robot_ir.loadShader
import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/16.
 */
class TextureYuv {
    private var _program: Int = -1
    private var _positionHandle: Int = -1
    private var _coordHandle: Int = -1
    private var _yhandle: Int = -1
    private var _uhandle: Int = -1
    private var _vhandle: Int = -1
    var isProgramBuilt: Boolean = false
    private var _video_width: Int = 0
    private var _video_height: Int = 0
    private var _ytid: Int = -1
    private var _utid: Int = -1
    private var _vtid: Int = -1
    private val _textureI: Int = 0
    private val _textureII: Int = 1
    private val _textureIII: Int = 2
    private var _vertice_buffer: ByteBuffer? = null
    private var _coord_buffer: ByteBuffer? = null
    private var _tIindex: Int = 0
    private var _tIIindex: Int = 1
    private var _tIIIindex: Int = 2

    fun buildProgram() {
        _vertice_buffer = getByteBuffer(squareVertices)
        _coord_buffer = getByteBuffer(coordVertices)
        if (_program <= 0) {
            _program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        }
        getHandles()
        isProgramBuilt = true
    }


    fun getHandles() {
        /*
        * get handle for "vPosition" and "a_texCoord"
        */
        _positionHandle = GLES20.glGetAttribLocation(_program, "vPosition")
        Log.d("tag", "_positionHandle = " + _positionHandle)
        checkGlError("glGetAttribLocation vPosition")
        if (_positionHandle == -1) {
            throw RuntimeException("Could not get attribute location for vPosition")
        }
        _coordHandle = GLES20.glGetAttribLocation(_program, "a_texCoord")
        Log.d("tag", "_coordHandle = " + _coordHandle)
        checkGlError("glGetAttribLocation a_texCoord")
        if (_coordHandle == -1) {
            throw RuntimeException("Could not get attribute location for a_texCoord")
        }

        /*
        * get uniform location for y/u/v, we pass data through these uniforms
        */
        _yhandle = GLES20.glGetUniformLocation(_program, "tex_y")
        Log.d("tag", "_yhandle = " + _yhandle)
        checkGlError("glGetUniformLocation tex_y")
        if (_yhandle == -1) {
            throw RuntimeException("Could not get uniform location for tex_y")
        }
        _uhandle = GLES20.glGetUniformLocation(_program, "tex_u")
        Log.d("tag", "_uhandle = " + _uhandle)
        checkGlError("glGetUniformLocation tex_u")
        if (_uhandle == -1) {
            throw RuntimeException("Could not get uniform location for tex_u")
        }
        _vhandle = GLES20.glGetUniformLocation(_program, "tex_v")
        Log.d("tag", "_vhandle = " + _vhandle)
        checkGlError("glGetUniformLocation tex_v")
        if (_vhandle == -1) {
            throw RuntimeException("Could not get uniform location for tex_v")
        }
    }

    /**
     * build a set of textures, one for Y, one for U, and one for V.
     */
    fun buildTextures(y: Buffer, u: Buffer, v: Buffer, width: Int, height: Int) {
        val videoSizeChanged = width != _video_width || height != _video_height
        if (videoSizeChanged) {
            _video_width = width
            _video_height = height
            Log.d("tag", "buildTextures videoSizeChanged: w=$_video_width h=$_video_height")
        }

        // building texture for Y data
        if (_ytid < 0 || videoSizeChanged) {
            if (_ytid >= 0) {
                Log.d("tag", "glDeleteTextures Y")
                GLES20.glDeleteTextures(1, intArrayOf(_ytid), 0)
                checkGlError("glDeleteTextures")
            }
            // GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            checkGlError("glGenTextures")
            _ytid = textures[0]
            Log.d("tag", "glGenTextures Y = " + _ytid)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _ytid)
        checkGlError("glBindTexture")
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, _video_width, _video_height, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y)
        checkGlError("glTexImage2D")
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // building texture for U data
        if (_utid < 0 || videoSizeChanged) {
            if (_utid >= 0) {
                Log.d("tag", "glDeleteTextures U")
                GLES20.glDeleteTextures(1, intArrayOf(_utid), 0)
                checkGlError("glDeleteTextures")
            }
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            checkGlError("glGenTextures")
            _utid = textures[0]
            Log.d("tag", "glGenTextures U = " + _utid)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _utid)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, _video_width / 2, _video_height / 2, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // building texture for V data
        if (_vtid < 0 || videoSizeChanged) {
            if (_vtid >= 0) {
                Log.d("tag", "glDeleteTextures V")
                GLES20.glDeleteTextures(1, intArrayOf(_vtid), 0)
                checkGlError("glDeleteTextures")
            }
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            checkGlError("glGenTextures")
            _vtid = textures[0]
            Log.d("tag", "glGenTextures V = " + _vtid)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _vtid)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, _video_width / 2, _video_height / 2, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    /**
     * render the frame
     * the YUV data will be converted to RGB by shader.
     */
    fun drawFrame() {
        GLES20.glUseProgram(_program)
        checkGlError("glUseProgram")

        GLES20.glVertexAttribPointer(_positionHandle, 2, GLES20.GL_FLOAT, false, 8, _vertice_buffer)
        checkGlError("glVertexAttribPointer mPositionHandle")
        GLES20.glEnableVertexAttribArray(_positionHandle)

        GLES20.glVertexAttribPointer(_coordHandle, 2, GLES20.GL_FLOAT, false, 8, _coord_buffer)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(_coordHandle)

        // bind textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + _textureI)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _ytid)
        GLES20.glUniform1i(_yhandle, _tIindex)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + _textureII)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _utid)
        GLES20.glUniform1i(_uhandle, _tIIindex)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + _textureIII)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _vtid)
        GLES20.glUniform1i(_vhandle, _tIIIindex)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()

        GLES20.glDisableVertexAttribArray(_positionHandle)
        GLES20.glDisableVertexAttribArray(_coordHandle)


    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        // create shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        // just check
        Log.d("tag", "vertexShader = " + vertexShader)
        Log.d("tag", "pixelShader = " + pixelShader)

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, pixelShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("tag", "Could not link program: ", null)
                Log.e("tag", GLES20.glGetProgramInfoLog(program), null)
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun checkGlError(op: String) {
        val error: Int = GLES20.glGetError()
        while (error != GLES20.GL_NO_ERROR) {
            Log.e("tag", "***** $op: glError $error", null)
            throw RuntimeException(op + ": glError " + error)
        }
    }

    companion object {

        private val lBold = 0.075f
        private val rBold = 0.915f

        private val squareVertices = floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f) // fullscreen

        private val coordVertices = floatArrayOf(lBold, 1.0f, rBold, 1.0f, lBold, 0.0f, rBold, 0.0f)// whole-texture

        private val VERTEX_SHADER = ("attribute vec4 vPosition;\n" + "attribute vec2 a_texCoord;\n"
                + "varying vec2 tc;\n" + "void main() {\n" + "gl_Position = vPosition;\n" + "tc = a_texCoord;\n" + "}\n")

        private val FRAGMENT_SHADER = ("precision mediump float;\n" + "uniform sampler2D tex_y;\n"
                + "uniform sampler2D tex_u;\n" + "uniform sampler2D tex_v;\n" + "varying vec2 tc;\n" + "void main() {\n"
                + "vec4 c = vec4((texture2D(tex_y, tc).r - 16./255.) * 1.164);\n"
                + "vec4 U = vec4(texture2D(tex_u, tc).r - 128./255.);\n"
                + "vec4 V = vec4(texture2D(tex_v, tc).r - 128./255.);\n" + "c += V * vec4(1.596, -0.813, 0, 0);\n"
                + "c += U * vec4(0, -0.392, 2.017, 0);\n" + "c.a = 1.0;\n" + "gl_FragColor = c;\n" + "}\n")
    }
}