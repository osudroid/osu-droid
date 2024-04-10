package com.reco1l.legacy.ui

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.framework.net.Downloader
import ru.nsu.ccfit.zuev.osuplus.R

class DownloadingFragment : LoadingFragment() {
    private lateinit var mDownloader: Downloader
    private lateinit var mAwaitCall: Runnable
    private lateinit var mButton: Button
    private lateinit var mText: TextView
    private lateinit var mProgressBar: CircularProgressIndicator

    fun setDownloader(downloader: Downloader, awaitCall: Runnable) {
        mAwaitCall = awaitCall
        mDownloader = downloader
    }

    override val layoutID: Int
        get() = R.layout.fragment_downloading

    override fun onLoadView() {
        super.onLoadView()

        mText = findViewById(R.id.text)!!
        mButton = findViewById(R.id.button)!!
        mProgressBar = findViewById(R.id.progress)!!

        mAwaitCall.run()
    }

    override fun callDismissOnBackPress() {
        if (mDownloader.isDownloading) {
            mDownloader.cancel()
            return
        }

        super.callDismissOnBackPress()
    }

    val progressBar: CircularProgressIndicator
        get() = mProgressBar

    val button: Button
        get() = mButton

    val text: TextView
        get() = mText

    fun setText(text: String) {
        mText.visibility = if (TextUtils.isEmpty(text)) View.GONE else View.VISIBLE
        mText.text = text
    }
}