package com.osudroid.ui.v1

import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.toolkt.android.dp
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.roundToInt

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
        progressView = findViewById(R.id.progress)!!
        messageView = findViewById(R.id.message)!!
        textView = findViewById(R.id.text)!!

        progressView.isIndeterminate = isIndeterminate
        progressView.progress = progress
        textView.text = header

        if (Config.isShowFPS()) {
            val paint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 21f
            }

            val fontMetrics = paint.fontMetrics
            val lineHeight = -fontMetrics.ascent + fontMetrics.descent
            val scale = resources.displayMetrics.widthPixels.toFloat() / Config.getRES_WIDTH()
            val fpsHeight = ((lineHeight + 4f) * scale).roundToInt()

            findViewById<LinearLayout>(R.id.container)!!.updateLayoutParams<RelativeLayout.LayoutParams> {
                bottomMargin = 16f.dp.roundToInt() + fpsHeight
            }
        }
    }

}