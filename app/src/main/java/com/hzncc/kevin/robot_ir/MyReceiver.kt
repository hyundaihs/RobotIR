package com.hzncc.kevin.robot_ir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hzncc.kevin.robot_ir.service.WarningService

/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/8/23.
 */
class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == actionSaveBitmap) {
            val warnTemp: Float = intent.getFloatExtra("temp", 0.0f)
            val mIntent = Intent(context, WarningService::class.java)
            mIntent.putExtra("temp", warnTemp)
            context.startService(mIntent)
        }
    }

}