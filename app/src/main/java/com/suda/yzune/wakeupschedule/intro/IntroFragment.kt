package com.suda.yzune.wakeupschedule.intro


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.suda.yzune.wakeupschedule.GlideApp

import com.suda.yzune.wakeupschedule.R

class IntroFragment : Fragment() {

    private var imageUrl = ""
    private var description = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_intro, container, false)
        GlideApp.with(this)
                .load(imageUrl)
                .error(R.drawable.net_work_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view.findViewById(R.id.iv_intro))
        view.findViewById<TextView>(R.id.tv_description).text = description
        return view
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                IntroFragment().apply {
                    imageUrl = param1
                    description = param2
                }
    }
}
