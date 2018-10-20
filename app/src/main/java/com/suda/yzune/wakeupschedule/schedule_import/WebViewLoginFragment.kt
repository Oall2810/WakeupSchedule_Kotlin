package com.suda.yzune.wakeupschedule.schedule_import


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.Toast
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.apply_info.ApplyInfoActivity
import com.suda.yzune.wakeupschedule.utils.PreferenceUtils
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.fragment_web_view_login.*
import org.jetbrains.anko.startActivity


class WebViewLoginFragment : Fragment() {

    private val GET_FRAME_CONTENT_STR = "document.getElementById('iframeautoheight').contentWindow.document.body.innerHTML"

    private lateinit var type: String
    private lateinit var viewModel: ImportViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_view_login, container, false)
    }

    @JavascriptInterface
    override fun onResume() {
        super.onResume()
        ViewUtils.resizeStatusBar(context!!.applicationContext, v_status)

        viewModel = ViewModelProviders.of(activity!!).get(ImportViewModel::class.java)

        val url = PreferenceUtils.getStringFromSP(activity!!.applicationContext, "school_url", "")
        if (url != "") {
            et_url.setText(url)
        }

        if (type == "apply") {
            tv_tips.text = "1. 在上方输入教务网址，部分学校需要连接校园网\n2. 登录后点击到个人课表或者相关的页面\n3. 点击右下角的按钮抓取源码，并上传到服务器"
        }
        wv_course.settings.javaScriptEnabled = true
        wv_course.addJavascriptInterface(InJavaScriptLocalObj(), "local_obj")
        wv_course.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                ll_error.visibility = View.VISIBLE
                wv_course.visibility = View.GONE
            }
        }
        wv_course.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    pb_load.progress = newProgress
                    pb_load.visibility = View.GONE
                } else {
                    pb_load.progress = newProgress * 5
                    pb_load.visibility = View.VISIBLE
                }
            }
        }
        //设置自适应屏幕，两者合用
        wv_course.settings.useWideViewPort = true //将图片调整到适合WebView的大小
        wv_course.settings.loadWithOverviewMode = true // 缩放至屏幕的大小
        // 缩放操作
        wv_course.settings.setSupportZoom(true) //支持缩放，默认为true。是下面那个的前提。
        wv_course.settings.builtInZoomControls = true //设置内置的缩放控件。若为false，则该WebView不可缩放
        wv_course.settings.displayZoomControls = false //隐藏原生的缩放控件
        initEvent()
    }

    private fun initEvent() {
        viewModel.getPostHtmlResponse().observe(this, Observer {
            when (it) {
                "OK" -> {
                    Toasty.success(activity!!.applicationContext, "上传源码成功~请等待适配哦", Toast.LENGTH_LONG).show()
                    activity!!.startActivity<ApplyInfoActivity>()
                    activity!!.finish()
                }
                "error" -> {
                    Toasty.error(activity!!.applicationContext, "上传网页源码失败，请检查网络", Toast.LENGTH_LONG).show()
                }
            }
        })

        tv_got_it.setOnClickListener {
            tv_got_it.visibility = View.GONE
            tv_tips.visibility = View.GONE
            v_tips.visibility = View.GONE
        }

        tv_go.setOnClickListener {
            startVisit()
        }

        et_url.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startVisit()
            }
            return@setOnEditorActionListener false
        }

        fab_import.setOnClickListener {
            wv_course.loadUrl("javascript:var ifrs=document.getElementsByTagName(\"iframe\");" +
                    "var iframeContent=\"\";" +
                    "for(var i=0;i<ifrs.length;i++){" +
                    "iframeContent=iframeContent+ifrs[i].contentDocument.body.parentElement.outerHTML;" +
                    "}" +
                    "window.local_obj.showSource(document.getElementsByTagName('html')[0].innerHTML + iframeContent);")
        }
    }

    private fun startVisit() {
        wv_course.visibility = View.VISIBLE
        ll_error.visibility = View.GONE
        val url = if (et_url.text.toString().startsWith("http://") || et_url.text.toString().startsWith("https://"))
            et_url.text.toString() else "http://" + et_url.text.toString()
        if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {
            wv_course.loadUrl(url)
            PreferenceUtils.saveStringToSP(activity!!.applicationContext, "school_url", url)
        } else {
            Toasty.error(context!!.applicationContext, "请输入正确的网址╭(╯^╰)╮").show()
        }
    }

    internal inner class InJavaScriptLocalObj {
        @JavascriptInterface
        fun showSource(html: String) {
            if (type != "apply") {
                if (html.contains("星期一") && html.contains("星期二")) {
                    Log.d("方正", html.substring(html.indexOf("星期一")))
                    if (type == "FZ") {
                        viewModel.importBean2CourseBean(viewModel.html2ImportBean(html), html)
                    } else if (type == "newFZ") {
                        viewModel.parseNewFZ(html)
                    }
                } else {
                    Toasty.info(context!!.applicationContext, "你貌似还没有点到个人课表哦", Toast.LENGTH_LONG).show()
                }
            } else {
                viewModel.postHtml(
                        school = viewModel.getSchoolInfo()[0],
                        type = viewModel.getSchoolInfo()[1],
                        qq = viewModel.getSchoolInfo()[2],
                        html = html)
            }
        }
    }

    override fun onDestroyView() {
        wv_course.clearCache(true)
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(param0: String) =
                WebViewLoginFragment().apply {
                    type = param0
                }
    }
}
