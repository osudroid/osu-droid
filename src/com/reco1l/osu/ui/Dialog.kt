package com.reco1l.osu.ui

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity.*
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.isVisible
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.fontColor
import com.reco1l.toolkt.animation.cancelAnimators
import com.reco1l.toolkt.animation.toAlpha
import com.reco1l.toolkt.animation.toScaleX
import com.reco1l.toolkt.animation.toScaleY
import ru.nsu.ccfit.zuev.osuplus.R


/**
 * A button to be displayed in a dialog.
 */
data class DialogButton(

    /**
     * The text to be displayed in the button.
     */
    val text: String,

    /**
     * The color of the button text and icon.
     */
    val tint: Int = Color.WHITE,

    /**
     * The function to be called when the button is clicked.
     */
    val clickListener: (MessageDialog) -> Unit
)

/**
 * A dialog that displays a message to the user.
 */
open class MessageDialog : BaseFragment() {


    override val layoutID = R.layout.dialog_message_fragment


    /**
     * The title to be displayed in the dialog.
     */
    var title: CharSequence = "Alert"
        set(value) {
            field = value
            if (isCreated) {
                findViewById<TextView>(R.id.title)?.text = value
            }
        }

    /**
     * The message to be displayed in the dialog.
     */
    open var message: CharSequence = ""
        set(value) {
            field = value
            if (isCreated) {
                findViewById<TextView>(R.id.message)?.apply {
                    if (this::class == MessageDialog::class) {
                        (parent as View).isVisible = value.isNotBlank()
                    }

                    text = if (isHTMLMessage) HtmlCompat.fromHtml(value.toString(), FROM_HTML_MODE_LEGACY) else value
                }
            }
        }

    /**
     * Whether the message is HTML formatted or not.
     */
    var isHTMLMessage = false
        set(value) {
            field = value
            if (isCreated) {
                message = message
            }
        }

    /**
     * The buttons to be displayed in the dialog.
     */
    var buttons = mutableListOf<DialogButton>()
        set(value) {
            field = value
            if (isCreated) {

                val layout = findViewById<LinearLayout>(R.id.button_layout)
                if (layout == null) {
                    Log.e("MessageDialog", "Buttons layout not found.")
                    return
                }

                layout.removeAllViews()

                for (button in value) {

                    val buttonView = Button(ContextThemeWrapper(context, R.style.button_borderless))
                    buttonView.minWidth = 300.dp
                    buttonView.minHeight = 56.dp
                    buttonView.gravity = CENTER
                    buttonView.background = requireContext().getDrawable(R.drawable.ripple)
                    buttonView.text = button.text
                    buttonView.fontColor = button.tint
                    buttonView.compoundDrawablePadding = 0
                    buttonView.setOnClickListener { button.clickListener(this@MessageDialog) }

                    layout.addView(buttonView)
                }
            }
        }

    /**
     * Whether the dialog is cancelable or not.
     */
    var allowDismiss = true

    /**
     * The function to be called when the dialog is dismissed.
     */
    var onDismiss: (() -> Unit)? = null


    override fun onLoadView() {

        title = title
        message = message
        buttons = buttons

        findViewById<TextView>(R.id.message)?.apply {
            isClickable = true
            movementMethod = LinkMovementMethod.getInstance()
        }


        val background = findViewById<View>(R.id.frg_background)!!

        background.setOnClickListener { callDismissOnBackPress() }
        background.cancelAnimators()
            .toAlpha(0f)
            .toAlpha(1f, 200, ease = EasingHelper.asInterpolator(Easing.Out))

        val body = findViewById<View>(R.id.frg_body)!!
        body.cornerRadius = 14f.dp

        body.cancelAnimators()
            .toScaleX(0.9f)
            .toScaleY(0.9f)
            .toScaleX(1f, 300, ease = EasingHelper.asInterpolator(Easing.OutBounce))
            .toScaleY(1f, 300, ease = EasingHelper.asInterpolator(Easing.OutBounce))
    }


    /**
     * The text to be show displayed in the dialog title.
     */
    fun setTitle(text: String): MessageDialog {
        title = text
        return this
    }

    /**
     * The text to be show displayed in the dialog message.
     */
    @JvmOverloads
    fun setMessage(text: String, isHTML: Boolean = false): MessageDialog {
        message = text
        isHTMLMessage = isHTML
        return this
    }

    /**
     * Whether the dialog is cancelable or not.
     */
    fun setAllowDismiss(value: Boolean): MessageDialog {
        allowDismiss = value
        return this
    }

    /**
     * The function to be called when the dialog is dismissed.
     */
    fun setOnDismiss(action: () -> Unit): MessageDialog {
        onDismiss = action
        return this
    }

    /**
     * Adds a new button.
     */
    @JvmOverloads
    open fun addButton(text: String, tint: Int = Color.WHITE, clickListener: (MessageDialog) -> Unit): MessageDialog {
        buttons.add(DialogButton(text, tint, clickListener))
        buttons = buttons
        return this
    }


    override fun callDismissOnBackPress() {
        if (allowDismiss) {
            super.callDismissOnBackPress()
        }
    }

    override fun dismiss() {
        onDismiss?.invoke()
        super.dismiss()
    }
}



