package com.hzncc.kevin.robot_ir

import android.content.Context
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hzncc.kevin.robot_ir.data.Log_Data
import com.hzncc.kevin.robot_ir.utils.SDCardUtil
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.per_gallery_list_item.view.*
import java.io.File
import java.util.*

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/19.
 */
class GalleryActivity : AppCompatActivity() {

    private val views = ArrayList<View>(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        val index = intent.getIntExtra("index", 0)
        viewPager.adapter = MyAdapter(this, App.instance.mData)
        viewPager.setCurrentItem(index)
    }

    private class MyAdapter(val context: Context, val mData: ArrayList<Log_Data>) : PagerAdapter() {

        private val recycledViews = LinkedList<View>()

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            (container as ViewGroup).removeView(`object` as View)
            recycledViews.addLast(`object`)
        }

        override fun instantiateItem(container: ViewGroup?, position: Int): Any {
            val logData = mData[position]
            val view: View?
            if (recycledViews.size > 0) {
                view = recycledViews.getFirst()
                recycledViews.removeFirst()

            } else {
                view = LayoutInflater.from(context).inflate(R.layout.gallery_list_item, container, false)
            }
            Picasso.with(context).load(File(SDCardUtil.IMAGE_IR + logData.irImage))
                    .into(view.irImage)
            Picasso.with(context).load(File(SDCardUtil.IMAGE_VL + logData.vlImage))
                    .into(view.vlImage)
            container?.addView(view)
            return view
        }

        override fun isViewFromObject(view: View?, `object`: Any?): Boolean = view == `object`

        override fun getCount(): Int = mData.size

    }
}