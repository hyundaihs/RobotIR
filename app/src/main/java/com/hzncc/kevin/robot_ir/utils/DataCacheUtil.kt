package com.hzncc.kevin.robot_ir.utils

/**
 * FT2332
 * Created by 蔡雨峰 on 2018/7/12.
 */
class DataCacheUtil(val sigleLength: Int, val count: Int = 3) {
    val data = ByteArray(sigleLength * count)
    var saveLength = 0
    var savePosition = 0
    var readPosition = -1

    fun put(newData: ByteArray) {
        System.arraycopy(newData, 0, data, savePosition * sigleLength, newData.size)
        count()
    }

    fun clean() {
        for (i in 0 until data.size) {
            data[0] = 0
        }
    }

    fun read(): ByteArray {
        val result = ByteArray(sigleLength)
        synchronized(readPosition) {
            if (readPosition >= 0) {
                System.arraycopy(data, readPosition * sigleLength, result, 0, result.size)
            }
        }
        return result
    }

    fun count() {
        savePosition++
        if (savePosition >= count) {
            savePosition = 0
        }
        saveLength = savePosition * sigleLength
        synchronized(readPosition) {
            readPosition = savePosition - 1
            if (readPosition < 0) {
                readPosition = count - 1
            }
        }
    }

    fun putByLength(newData: ByteArray, length: Int) {
        if (saveLength + length > data.size) {
            val t = data.size - saveLength
            System.arraycopy(newData, 0, data, saveLength, t)
            saveLength = 0
            System.arraycopy(newData, t, data, saveLength, length)
            countByLength(length - t)
        } else {
            System.arraycopy(newData, 0, data, saveLength, newData.size)
            countByLength(length)
        }
    }

    fun countByLength(length: Int) {
        saveLength += length
        if (saveLength >= data.size) {
            saveLength = 0
        }
        savePosition = saveLength / sigleLength
        readPosition = savePosition - 1
        if (readPosition < 0) {
            readPosition = count - 1
        }
    }


}