package com.reco1l.osu.ui

import android.content.DialogInterface
import android.content.DialogInterface.*
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.reco1l.osu.mainThread
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.ensureLayoutParams
import com.reco1l.toolkt.android.horizontalMargin
import com.reco1l.toolkt.android.horizontalPadding
import ru.nsu.ccfit.zuev.osu.GlobalManager


object Dialog {

    @JvmStatic
    @JvmOverloads
    fun showAlert(

        title: String,
        message: String? = null,
        cancelable: Boolean = true,
        view: View? = null,

        // We can use pairs of button text and click listener for this but the Java syntax to do
        // pairs is genuinely a pain in the ass.

        positiveButtonText: String? = null,
        onPositiveButtonClick: ((DialogInterface) -> Unit)? = null,
        negativeButtonText: String? = null,
        onNegativeButtonClick: ((DialogInterface) -> Unit)? = null,

        onCancel: OnCancelListener? = null,

    ) = mainThread {

        AlertDialog.Builder(GlobalManager.getInstance().mainActivity).apply {

            setView(view)
            setTitle(title)
            setMessage(message)
            setCancelable(cancelable)
            setOnCancelListener(onCancel)

            if (positiveButtonText != null && onPositiveButtonClick != null) {
                setPositiveButton(positiveButtonText) { dialog, _ -> onPositiveButtonClick(dialog) }
            }

            if (negativeButtonText != null && onNegativeButtonClick != null) {
                setNegativeButton(negativeButtonText) { dialog, _ -> onNegativeButtonClick(dialog) }
            }

        }.show()

    }

    @JvmStatic
    @JvmOverloads
    fun showPrompt(

        title: String,
        message: String? = null,
        cancelable: Boolean = true,
        onInsert: (String) -> Unit,
        onCancel: OnCancelListener? = null,

    ) = mainThread {

        val context = GlobalManager.getInstance().mainActivity

        val frame = FrameLayout(context)
        frame.horizontalPadding = 20.dp

        val input = EditText(context)
        frame.addView(input)

        showAlert(
            title = title,
            message = message,
            cancelable = cancelable,
            view = frame,
            positiveButtonText = "Accept",
            onPositiveButtonClick = {
                it.dismiss()
                onInsert(input.text.toString())
            },
            negativeButtonText = "Cancel",
            onNegativeButtonClick = DialogInterface::dismiss,
            onCancel = onCancel
        )
    }

}

