package com.osudroid.ui.v1

import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.toolkt.android.dp
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osuplus.R

class LoadingBadgeFragment : BaseFragment() {

    override val layoutID = R.layout.loading_badge_fragment


    var progress = 0
        set(value) {
            field = value
            if (::progressView.isInitialized) {
                progressView.progress = value
            }
        }

    var isIndeterminate = true
        set(value) {
            field = value
            if (::progressView.isInitialized) {
                progressView.isIndeterminate = value
            }
        }

    var header = "Loading..."
        set(value) {
            field = value
            if (::textView.isInitialized) {
                textView.text = value
            }
        }

    var message = "Please wait..."
        set(value) {
            field = value
            if (::messageView.isInitialized) {
                messageView.text = value
                messageView.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
            }
        }


    private lateinit var textView: TextView

    private lateinit var messageView: TextView

    private lateinit var progressView: CircularProgressIndicator

    init {
        isDismissOnBackPress = false
        interceptBackPress = false
    }

    override fun onLoadView() {
        findViewById<LinearLayout>(R.id.container)!!.updateLayoutParams<RelativeLayout.LayoutParams> {
            var fpsHeight = 0

            if (Config.isShowFPS()) {
                val font = ResourceManager.getInstance().getFont("smallFont")

                if (font != null) {
                    val scale = resources.displayMetrics.widthPixels.toFloat() / Config.getRES_WIDTH()

                    fpsHeight = ((font.lineHeight + 4f) * scale).roundToInt()
                }
            }

            bottomMargin = 16f.dp.roundToInt() + fpsHeight
        }

        progressView = findViewById(R.id.progress)!!
        messageView = findViewById(R.id.message)!!
        textView = findViewById(R.id.text)!!

        progressView.isIndeterminate = isIndeterminate
        progressView.progress = progress
        textView.text = header
    }

}