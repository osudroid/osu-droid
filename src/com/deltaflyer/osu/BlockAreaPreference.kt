package com.deltaflyer.osu

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference


public class BlockAreaPreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onClick() {
        super.onClick()
        val webViewFragment: BlockAreaWebViewFragment = BlockAreaWebViewFragment()
            .setCurrentConfig(this.getPersistedString("null"))
            .setCallback(object : BlockAreaWebViewFragment.Callback {
                override fun onJsonReceived(json: String?) {
                    Log.d("WebViewCallback", "JSON Received: $json")
                    persistString(json)
                }
            })

        webViewFragment.setURL("file:///android_asset/html/blockAreaSetting.html")
        webViewFragment.show()
    }


}
