package com.hzncc.kevin.robot_ir.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.hzncc.kevin.robot_ir.App
import com.hzncc.kevin.robot_ir.E
import com.hzncc.kevin.robot_ir.R
import com.hzncc.kevin.robot_ir.qi_han.QiHanConnectUtil
import com.hzncc.kevin.robot_ir.utils.SDCardUtil

/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/8/23.
 */
class WarningService : Service() {

    private val mMediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(this, R.raw.tts)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mMediaPlayer.start()
        if (App.instance.mData.size > 0)
            sendMessage(this, SDCardUtil.IMAGE_IR + App.instance.mData[0].irImage,
                    SDCardUtil.IMAGE_VL + App.instance.mData[0].vlImage,
                    intent.getFloatExtra("temp", 0f))
        return super.onStartCommand(intent, flags, startId)
    }

    fun sendMessage(context: Context, ir: String, vl: String, temp: Float) {
        val contacts = QiHanConnectUtil.getContactInfo(context)
        if (null != contacts && contacts.size > 0) {
            if (!QiHanConnectUtil.sendStringMessage2AllContact(context,
                            contacts, "有新的报警信息,异常温度:$temp℃", "")) {
                E("sendMessage failed")
            }
            val path = ArrayList<String>()
            path.add(ir)
            path.add(vl)
            if (QiHanConnectUtil.sendPicture2AllContact(context, contacts, path) <= 0) {
                E("send Pic failed")
            }
        } else {
            E("没有联系人")
        }

    }

    override fun onDestroy() {
        mMediaPlayer.release()
        super.onDestroy()
    }
}