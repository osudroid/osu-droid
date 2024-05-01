package com.deltaflyer.osu

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import org.json.JSONObject


class BlockAreaPreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    fun getCurrentConfig(): String {
        val currentConfig = getPersistedString(
            "null"
        )
        var jsonObj = JSONObject()
        if (currentConfig == "null") {
            jsonObj.put("activated", false)
        } else {
            jsonObj = JSONObject(currentConfig)
        }
        val screen = JSONObject()
        screen.put("height", BlockAreaManager.screenHeight)
        screen.put("width", BlockAreaManager.screenWidth)
        jsonObj.put("screen", screen)
        return jsonObj.toString()
    }

    override fun onClick() {
        super.onClick()

        BlockAreaWebViewFragment().also {
            it.setCurrentConfig(getPersistedString("null"))

            it.setCallback(object : BlockAreaWebViewFragment.Callback {
                override fun onMessageReceived(message: String?) {
                    Log.d("WebViewCallback", "Message Received: $message")
                    if ("{\"message\":'ok'}" == message) {
                        // Execute JavaScript on the UI thread
                        it.activity?.runOnUiThread {
                            it.execute(
                                "loadConfig('${getCurrentConfig()}')"
                            )
                        }
                    } else {
                        persistString(message)
                        if (message != null) {
                            BlockAreaManager.reload(getCurrentConfig())
                        }
                    }
                }
            })

            it.setURL("file:///android_asset/html/blockAreaSetting.html")
            it.show()
        }
    }
}
