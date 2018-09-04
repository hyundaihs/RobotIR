package com.hzncc.kevin.robot_ir.utils

/**
 * Created by kevin on 2018/8/30.
 */
class BitmapCacheUtil<T>(val count: Int = 3) {
    val data = HashMap<Int, T>()
    var savePosition = 0
    var readPosition = -1

    fun put(newData: T) {
        data.put(savePosition, newData)
        count()
    }

    fun clean() {
        data.clear()
    }

    fun read(): T? {
        synchronized(readPosition){
            return data.get(readPosition)
        }
    }

    fun count() {
        savePosition++
        if (savePosition >= count) {
            savePosition = 0
        }
        synchronized(readPosition) {
            readPosition = savePosition - 1
            if (readPosition < 0) {
                readPosition = count - 1
            }
        }
    }

}