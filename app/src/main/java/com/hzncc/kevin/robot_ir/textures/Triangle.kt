package com.hzncc.kevin.robot_ir.textures

import android.opengl.GLES20
import com.hzncc.kevin.robot_ir.App
import com.hzncc.kevin.robot_ir.R
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.getFloatBuffer
import com.hzncc.kevin.robot_ir.loadShader
import com.hzncc.kevin.robot_ir.utils.TextResourceReader
import java.nio.FloatBuffer

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/16.
 */

class Triangle(val isFanz: Boolean = true) {

    private val vertexCount = maxCoords.size / COORDS_PER_VERTEX
    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    // 设置三角形颜色和透明度（r,g,b,a）
    internal var blue = floatArrayOf(0.0f, 0.0f, 1f, 1.0f)//蓝色不透明
    internal var red = floatArrayOf(1.0f, 0.0f, 0f, 1.0f)//红色不透明
    internal var green1 = floatArrayOf(1.0f, 1.0f, 0.2f, 1.0f)//绿色不透明
    internal var green2 = floatArrayOf(1.0f, 1.0f, 0.5f, 1.0f)//绿色不透明
    internal var green3 = floatArrayOf(1.0f, 1.0f, 0.9f, 1.0f)//绿色不透明
    private var maxBuffer: FloatBuffer? = null
    private var mixBuffer: FloatBuffer? = null
    private var point1: FloatBuffer? = null
    private var point2: FloatBuffer? = null
    private var point3: FloatBuffer? = null
    private var mProgram: Int = -1
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    var isPeizhun = false

    fun buildProgram() {
        // 初始化顶点字节缓冲区，用于存放形状的坐标
        maxBuffer = getFloatBuffer(maxCoords)
        mixBuffer = getFloatBuffer(mixCoords)
        point1 = getFloatBuffer(point1Coords)
        point2 = getFloatBuffer(point2Coords)
        point3 = getFloatBuffer(point3Coords)
        val vertex_shader = TextResourceReader.readTextFileFromResource(App.instance.applicationContext, R.raw.vertex_shader_shape)
        val fragment_shader = TextResourceReader.readTextFileFromResource(App.instance.applicationContext, R.raw.fragment_shader_shape)
        // 编译shader代码
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                vertex_shader)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragment_shader)

        // 创建空的OpenGL ES Program
        mProgram = GLES20.glCreateProgram()

        // 将vertex shader添加到program
        GLES20.glAttachShader(mProgram, vertexShader)

        // 将fragment shader添加到program
        GLES20.glAttachShader(mProgram, fragmentShader)

        // 创建可执行的 OpenGL ES program
        GLES20.glLinkProgram(mProgram)
    }

    fun updateVertex(ir_ImageData: IR_ImageData) {
        val ww: Float = ir_ImageData.width.toFloat() / 2
        val hh: Float = ir_ImageData.height.toFloat() / 2
        var maxx: Float = (ir_ImageData.max_x.toFloat() - ww) / ww
        var maxy: Float = (hh - ir_ImageData.max_y.toFloat()) / hh
        var minx: Float = (ir_ImageData.min_x.toFloat() - ww) / ww
        var miny: Float = (hh - ir_ImageData.min_y.toFloat()) / hh
        if (isFanz) {
            maxx = -maxx
            maxy = -maxy
            minx = -minx
            miny = -miny
        }
        maxCoords = createTriangleCoords(maxx, maxy, caleModel(maxx, maxy))
        mixCoords = createTriangleCoords(minx, miny, caleModel(minx, miny))
        // 初始化顶点字节缓冲区，用于存放形状的坐标
        // 初始化顶点字节缓冲区，用于存放形状的坐标
        maxBuffer = getFloatBuffer(maxCoords)
        mixBuffer = getFloatBuffer(mixCoords)
    }

    fun updateVertex(ir_ImageData: IR_ImageData, point1_x: Float, point1_y: Float, point2_x: Float, point2_y: Float, point3_x: Float, point3_y: Float) {
        updateVertex(ir_ImageData)
        val ww: Float = ir_ImageData.width.toFloat() / 2
        val hh: Float = ir_ImageData.height.toFloat() / 2
        var p1_x: Float = (point1_x - ww) / ww
        var p1_y: Float = (hh - point1_y) / hh
        var p2_x: Float = (point2_x - ww) / ww
        var p2_y: Float = (hh - point2_y) / hh
        var p3_x: Float = (point3_x - ww) / ww
        var p3_y: Float = (hh - point3_y) / hh
        if (isFanz) {
            p1_x = -p1_x
            p1_y = -p1_y
            p2_x = -p2_x
            p2_y = -p2_y
            p3_x = -p3_x
            p3_y = -p3_y
        }
        point1Coords = createTriangleCoords(p1_x, p1_y, caleModel(p1_x, p1_y))
        point2Coords = createTriangleCoords(p2_x, p2_y, caleModel(p2_x, p2_y))
        point3Coords = createTriangleCoords(p3_x, p3_y, caleModel(p3_x, p3_y))
        // 初始化顶点字节缓冲区，用于存放形状的坐标
        // 初始化顶点字节缓冲区，用于存放形状的坐标
        point1 = getFloatBuffer(point1Coords)
        point2 = getFloatBuffer(point2Coords)
        point3 = getFloatBuffer(point3Coords)
    }

    fun caleModel(sX: Float, sY: Float): Int {
        if (sY < -0.89) {
            return 3
        } else if (sX < -0.93) {
            return 1
        } else if (sX > 0.93) {
            return 2
        } else {
            return 0
        }
    }

    /**
     * sX,sY,基点
     * model,朝向 0,正常,1,左,2,右,3,下
     */
    fun createTriangleCoords(sX: Float, sY: Float, model: Int): FloatArray {
        if (model == 1) {
            val coords = floatArrayOf(//默认按逆时针方向绘制
                    sX, sY, // 顶点
                    sX + 0.05f, sY - 0.025f,//左下角
                    sX + 0.05f, sY + 0.025f//右下角
            )
            return coords
        } else if (model == 2) {
            val coords = floatArrayOf(//默认按逆时针方向绘制
                    sX, sY, // 顶点
                    sX - 0.05f, sY + 0.025f,//左下角
                    sX - 0.05f, sY - 0.025f//右下角
            )
            return coords
        } else if (model == 3) {
            val coords = floatArrayOf(//默认按逆时针方向绘制
                    sX, sY, // 顶点
                    sX + 0.025f, sY + 0.05f,//左下角
                    sX - 0.025f, sY + 0.05f//右下角
            )
            return coords
        } else {
            val coords = floatArrayOf(//默认按逆时针方向绘制
                    sX, sY, // 顶点
                    sX - 0.025f, sY - 0.05f,//左下角
                    sX + 0.025f, sY - 0.05f//右下角
            )
            return coords
        }
    }

    fun draw() {
        // 添加program到OpenGL ES环境中
        GLES20.glUseProgram(mProgram)

        // 获取指向vertex shader的成员vPosition的handle
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")

        // 启用一个指向三角形的顶点数组的handle
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // 获取指向fragment shader的成员vColor的handle
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")

        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, maxBuffer)

        //  设置颜色
        GLES20.glUniform4fv(mColorHandle, 1, red, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, mixBuffer)

        //  设置颜色
        GLES20.glUniform4fv(mColorHandle, 1, blue, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        if (isPeizhun) {
            //准备三角形的坐标数据
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, point1)

            //  设置颜色
            GLES20.glUniform4fv(mColorHandle, 1, green1, 0)

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            //准备三角形的坐标数据
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, point2)

            //  设置颜色
            GLES20.glUniform4fv(mColorHandle, 1, green2, 0)

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            //准备三角形的坐标数据
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, point3)

            //  设置颜色
            GLES20.glUniform4fv(mColorHandle, 1, green3, 0)

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        }

        // 禁用指向三角形的顶点数组
        GLES20.glDisableVertexAttribArray(mPositionHandle)


    }

    companion object {
        //设置每个顶点的坐标数
        internal val COORDS_PER_VERTEX = 2
        //设置三角形顶点数组
        internal var maxCoords = floatArrayOf(0.0f, 0.0f, //
                0.0f, 0.0f, //
                0.0f, 0.0f
        )
        internal var mixCoords = floatArrayOf(0.0f, 0.0f,  //
                0.0f, 0.0f, //
                0.0f, 0.0f
        )
        internal var point1Coords = floatArrayOf(0.0f, 0.0f,  //
                0.0f, 0.0f, //
                0.0f, 0.0f
        )
        internal var point2Coords = floatArrayOf(0.0f, 0.0f,  //
                0.0f, 0.0f, //
                0.0f, 0.0f
        )
        internal var point3Coords = floatArrayOf(0.0f, 0.0f,  //
                0.0f, 0.0f, //
                0.0f, 0.0f
        )
    }
}
