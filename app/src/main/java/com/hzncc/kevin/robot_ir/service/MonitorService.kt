package com.hzncc.kevin.robot_ir.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/8/22.
 */
class MonitorService : Service() {
    companion object {
        var isRuning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRuning) {
            monitor()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun monitor() {
        isRuning = true
        doAsync {
            while (true) {
                if (!MyService.isRuning) {
                    uiThread {
                        startService(Intent(this@MonitorService, MyService::class.java))
                    }
                }
                Thread.sleep(10000)
            }
        }
    }

}