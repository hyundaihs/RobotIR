package com.hzncc.kevin.robot_ir

import android.Manifest
import android.app.Application

import com.hzncc.kevin.robot_ir.data.IR_ImageData
import com.hzncc.kevin.robot_ir.data.Log_Data
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.PermissionListener
import org.jetbrains.anko.toast

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/17.
 */

class App : Application() {
    var ir_imageData: IR_ImageData? = null
    val mData = ArrayList<Log_Data>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        SDCardUtil.initAll()
        checkPermission()
    }

    private fun checkPermission() {
        AndPermission.with(applicationContext).requestCode(200)
                .permission(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .callback(object : PermissionListener {
                    override fun onSucceed(requestCode: Int, grantPermissions: MutableList<String>) {
                        toast("权限设置成功")
                    }

                    override fun onFailed(requestCode: Int, deniedPermissions: MutableList<String>) {
                        toast("权限设置失败")
                    }
                }).start()
    }

    companion object {
        // 伴生对象
        lateinit var instance: App
            private set
    }
}
