package com.osudroid.ui.v1

import android.view.View
import android.widget.TextView
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
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
        progressView = findViewById(R.id.progress)!!
        messageView = findViewById(R.id.message)!!
        textView = findViewById(R.id.text)!!

        progressView.isIndeterminate = isIndeterminate
        progressView.progress = progress
        textView.text = header
    }

}