package com.hzncc.kevin.robot_ir.data

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * 红外图像数据
 *
 * @author Administrator
 */
data class IR_ImageData(var width: Int = 0, var height: Int = 0, var max_x: Int = 0, var max_y: Int = 0,
                        var min_x: Int = 0, var min_y: Int = 0, var bitmap: Bitmap? = null, var buffer: ShortBuffer? = null,
                        var max_temp: Float = -100f, var min_temp: Float = 1000f,
                        var max_gary: Short = 0, var min_gray: Short = 0)

data class Log_Data(var irImage: String = "", var vlImage: String = "")
