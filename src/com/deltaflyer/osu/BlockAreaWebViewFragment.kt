package com.deltaflyer.osu

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.edlplan.ui.fragment.WebViewFragment
import ru.nsu.ccfit.zuev.osuplus.R


class BlockAreaWebViewFragment : WebViewFragment() {

    private lateinit var webview: WebView
    private lateinit var callback: Callback
    lateinit var url: String

    override fun onLoadView() {
        webview = findViewById<WebView>(R.id.web)!!.also {
            val webSettings = it.settings
            webSettings.javaScriptEnabled = true
            webSettings.userAgentString = "osudroid"
            it.loadUrl(url)
            it.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun postMessage(message: String?) {
                    callback.onMessageReceived(message)
                }
            }, "Android")
        }

        findViewById<View>(R.id.close_button)!!.setOnClickListener { v: View? -> dismiss() }
    }



    fun setCallback(callback: Callback): BlockAreaWebViewFragment {
        this.callback = callback
        return this
    }

    fun execute(js: String) {
        Log.d("WebViewFruit", "Exec: $js")

        this.webview.evaluateJavascript(js) { value ->
            Log.d("WebViewFruit", value)
        }
    }


    interface Callback {
        fun onMessageReceived(message: String?)
    }
}



