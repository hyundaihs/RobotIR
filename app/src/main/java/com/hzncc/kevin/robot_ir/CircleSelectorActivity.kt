package com.hzncc.kevin.robot_ir

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import kotlinx.android.synthetic.main.acitivyt_circle_selector.*

/**
 * RobotIR
 * Created by 蔡雨峰 on 2018/8/22.
 */
class CircleSelectorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivyt_circle_selector)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        seekbar.setOnSeekBarChangeListener { seekbar, curValue ->
            value.text = "v = $curValue"
        }
    }
}