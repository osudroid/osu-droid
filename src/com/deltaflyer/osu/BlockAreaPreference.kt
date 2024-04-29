package com.deltaflyer.osu

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config


public class BlockAreaPreference : Preference {
    private val TAG = "BlockAreaPreference"

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
        screen.put("height", Config.getRES_HEIGHT())
        screen.put("width", Config.getRES_WIDTH())
        jsonObj.put("screen", screen)
        return jsonObj.toString()
    }

    override fun onClick() {
        super.onClick()
        val webViewFragment: BlockAreaWebViewFragment = BlockAreaWebViewFragment()
            .setCurrentConfig(this.getPersistedString("null"))

        webViewFragment.setCallback(object : BlockAreaWebViewFragment.Callback {
            override fun onMessageReceived(message: String?) {
                Log.d("WebViewCallback", "Message Received: $message")
                if ("{\"message\":'ok'}" == message) {
                    // Execute JavaScript on the UI thread
                    val activity: Activity? = webViewFragment.activity
                    activity?.runOnUiThread {
                        webViewFragment.execute(
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

        webViewFragment.setURL("file:///android_asset/html/blockAreaSetting.html")
        webViewFragment.show()
    }


}
