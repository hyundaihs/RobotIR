package com.hzncc.kevin.robot_ir

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.data.Log_Data
import com.hzncc.kevin.robot_ir.utils.Preference
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import org.jetbrains.anko.toast


/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/17.
 */

class App : Application() {
    val mData = ArrayList<Log_Data>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        ir_imageData = IR_ImageData()
        SDCardUtil.initAll()
//        checkPermission()
        initBackgroundCallBack()
    }

    private fun initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(MyCallBack())
    }

    private inner class MyCallBack : ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
        }

        override fun onActivityResumed(activity: Activity?) {
        }

        override fun onActivityStarted(activity: Activity?) {
            appCount++
            if (isRunInBackground) {
                //应用从后台回到前台 需要做的操作
                if (null != activity)
                    back2App(activity)
            }

        }

        override fun onActivityDestroyed(activity: Activity?) {
        }

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        }

        override fun onActivityStopped(activity: Activity?) {
            appCount--
            if (appCount == 0) {
                //应用进入后台 需要做的操作
                if (null != activity)
                    leaveApp(activity)
            }
        }

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        }

    }

    /**
     * 从后台回到前台需要执行的逻辑
     *
     * @param activity
     */
    private fun back2App(activity: Activity) {
        isRunInBackground = false
    }

    /**
     * 离开应用 压入后台或者退出应用
     *
     * @param activity
     */
    private fun leaveApp(activity: Activity) {
        isLogined = false
        isRunInBackground = true
    }


    /**
     * 程序是否在前台运行
     *
     */
    fun isAppOnForeground(): Boolean {
        val activityManager = applicationContext
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = applicationContext.packageName
        /**
         * 获取Android设备中所有正在运行的App
         */
        /**
         * 获取Android设备中所有正在运行的App
         */
        val appProcesses = activityManager
                .runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName == packageName && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true
            }
        }
        return false
    }

//    private fun checkPermission() {
//        AndPermission.with(applicationContext).requestCode(200)
//                .permission(
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE
//                )
//                .callback(object : PermissionListener {
//                    override fun onSucceed(requestCode: Int, grantPermissions: MutableList<String>) {
////                        toast("权限设置成功")
//                    }
//
//                    override fun onFailed(requestCode: Int, deniedPermissions: MutableList<String>) {
//                        toast("权限获取失败")
//                    }
//                }).start()
//    }

    companion object {
        // 伴生对象
        lateinit var instance: App
        var ir_imageData = IR_ImageData()
        var vlData: ByteArray? = null

        var isPeizhund: Boolean by Preference("isPeizhund", false)

        var pointIR1_x: Float by Preference("pointIR1_x", 50.0f)
        var pointIR1_y: Float by Preference("pointIR1_y", 50.0f)
        var pointIR2_x: Float by Preference("pointIR2_x", 100.0f)
        var pointIR2_y: Float by Preference("pointIR2_y", 100.0f)
        var pointIR3_x: Float by Preference("pointIR3_x", 150.0f)
        var pointIR3_y: Float by Preference("pointIR3_y", 150.0f)

        var pointVL1_x: Float by Preference("pointVL1_x", 50.0f)
        var pointVL1_y: Float by Preference("pointVL1_y", 50.0f)
        var pointVL2_x: Float by Preference("pointVL2_x", 100.0f)
        var pointVL2_y: Float by Preference("pointVL2_y", 100.0f)
        var pointVL3_x: Float by Preference("pointVL3_x", 150.0f)
        var pointVL3_y: Float by Preference("pointVL3_y", 150.0f)

        var param1: Float by Preference("param1", 0.0f)
        var param2: Float by Preference("param2", 0.0f)
        var param3: Float by Preference("param3", 0.0f)
        var param4: Float by Preference("param4", 0.0f)
        var param5: Float by Preference("param5", 0.0f)
        var param6: Float by Preference("param6", 0.0f)

        var irWidth = 0
        var irHeight = 0
        //        var vlWidth = 0
//        var vlHeight = 0
        var appCount = 0
        var isLogined = false
        var isRunInBackground = false

    }
}
