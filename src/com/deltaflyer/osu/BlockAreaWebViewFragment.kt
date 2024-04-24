package com.deltaflyer.osu

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.edlplan.ui.fragment.WebViewFragment
import ru.nsu.ccfit.zuev.osuplus.R


class BlockAreaWebViewFragment : WebViewFragment() {

    private lateinit var myWebView: WebView
    private lateinit var currentConfig: String
    private lateinit var callback: Callback

    override fun onLoadView() {
        super.onLoadView()
        myWebView = findViewById<WebView>(R.id.web)!!;
        myWebView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun postMessage(json: String?) {
                callback.onJsonReceived(json)
            }
        }, "Android")
    }


    fun setCurrentConfig(persistedString: String): BlockAreaWebViewFragment {
        this.currentConfig = persistedString
        return this
    }

    fun setCallback(callback: Callback): BlockAreaWebViewFragment {
        this.callback = callback
        return this
    }


    interface Callback {
        fun onJsonReceived(json: String?)
    }
}



