package com.reco1l.osu.ui

import com.google.android.material.progressindicator.CircularProgressIndicator
import ru.nsu.ccfit.zuev.osuplus.R


class ProgressDialog : MessageDialog() {


    override val layoutID = R.layout.dialog_progress


    /**
     * The progress of the dialog.
     */
    var progress: Int = 0
        set(value) {
            field = value
            if (isCreated) {
                findViewById<CircularProgressIndicator>(R.id.progress)!!.progress = value
            }
        }

    /**
     * Whether the progress is indeterminate or not.
     */
    var indeterminate: Boolean = false
        set(value) {
            field = value
            if (isCreated) {
                findViewById<CircularProgressIndicator>(R.id.progress)!!.isIndeterminate = value
            }
        }

    /**
     * The maximum value of the progress.
     */
    var max: Int = 0
        set(value) {
            field = value
            if (isCreated) {
                findViewById<CircularProgressIndicator>(R.id.progress)!!.max = value
            }
        }


    override fun onLoadView() {
        super.onLoadView()

        max = max
        progress = progress
        indeterminate = indeterminate
    }


    /**
     * Sets the progress of the dialog.
     */
    fun setProgress(value: Int): ProgressDialog {
        progress = value
        return this
    }

    /**
     * Sets whether the progress is indeterminate or not.
     */
    fun setIndeterminate(value: Boolean): ProgressDialog {
        indeterminate = value
        return this
    }

    /**
     * Sets the maximum value of the progress.
     */
    fun setMax(value: Int): ProgressDialog {
        max = value
        return this
    }

}