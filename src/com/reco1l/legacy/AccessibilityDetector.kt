package com.reco1l.legacy

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.app.AlertDialog
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AccessibilityDetector
{


    /**
     * `true` if an accessibility services with the ability of perform gestures is detected.
     */
    @JvmStatic
    var isIllegalServiceDetected = true
        private set

    /**
     * The alert that notifies the user which services are not allowed
     */
    private var alert: AlertDialog? = null


    @JvmStatic
    fun start(context: MainActivity) = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({

        if (!context.hasWindowFocus())
            return@scheduleAtFixedRate

        // Getting the accessibility manager.
        val manager = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager

        // Filtering services that can perform gestures.
        val illegalServices = manager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK).filter {

            CAPABILITY_CAN_PERFORM_GESTURES and it.capabilities == CAPABILITY_CAN_PERFORM_GESTURES
        }

        isIllegalServiceDetected = illegalServices.isNotEmpty()

        context.runOnUiThread {

            if (isIllegalServiceDetected)
            {
                if (alert == null)
                    alert = showAlert(context, illegalServices)

                return@runOnUiThread
            }

            alert?.dismiss()
            alert = null
        }

    }, 0, 1000, TimeUnit.MILLISECONDS)!!


    private fun showAlert(context: MainActivity, services: List<AccessibilityServiceInfo>) = AlertDialog.Builder(context).apply {

        val message = buildString {

            append(context.getString(R.string.accessibility_detector_message))
            appendLine()

            for (service in services) service.resolveInfo.serviceInfo.let {
                appendLine()
                append(it.packageName)
            }
        }

        setTitle(R.string.accessibility_detector_title)
        setMessage(message)
        setCancelable(false)
        setNeutralButton(R.string.accessibility_detector_settings) { _, _ ->

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
            alert = null
        }
        setNegativeButton(R.string.accessibility_detector_exit) { alert, _ ->

            context.forcedExit()
            alert.dismiss()
        }

    }.show()
}