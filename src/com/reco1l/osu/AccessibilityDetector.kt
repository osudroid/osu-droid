package com.reco1l.osu

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.osudroid.resources.R
import com.reco1l.osu.ui.MessageDialog
import ru.nsu.ccfit.zuev.osu.MainActivity

object AccessibilityDetector {


    /**
     * `true` if an accessibility services with the ability of perform gestures is detected.
     */
    @JvmStatic
    var isIllegalServiceDetected = true
        private set

    /**
     * The alert that notifies the user which services are not allowed
     */
    private var alert: MessageDialog? = null


    @JvmStatic
    fun check(context: MainActivity) {

        // Getting the accessibility manager.
        val manager = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager

        // Filtering services that can perform gestures.
        val illegalServices = manager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK).filter {

            CAPABILITY_CAN_PERFORM_GESTURES and it.capabilities == CAPABILITY_CAN_PERFORM_GESTURES
        }

        isIllegalServiceDetected = illegalServices.isNotEmpty()

        context.runOnUiThread {

            if (isIllegalServiceDetected) {
                if (alert == null)
                    alert = showAlert(context, illegalServices)

                return@runOnUiThread
            }

            alert?.dismiss()
            alert = null
        }
    }


    private fun showAlert(context: MainActivity, services: List<AccessibilityServiceInfo>) = MessageDialog().apply {

        val message = buildString {

            append(context.getString(R.string.accessibility_detector_message))
            appendLine()

            for (service in services) service.resolveInfo.serviceInfo.let {
                appendLine()
                append(it.packageName)
            }
        }

        setTitle(context.getString(R.string.accessibility_detector_title))
        setMessage(message)
        setAllowDismiss(false)

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        if (intent.resolveActivity(context.packageManager) != null) {
            addButton(context.getString(R.string.accessibility_detector_settings)) {
                context.startActivity(intent)
                alert = null
            }
        }

        addButton(context.getString(R.string.accessibility_detector_exit)) {
            context.forcedExit()
            it.dismiss()
        }

        show()
    }
}