package com.osudroid.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.osudroid.resources.R
import com.reco1l.osu.ui.MessageDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var contentObserver: ContentObserver? = null
    private var scope: CoroutineScope? = null

    private val excludedServices = setOf(
        // Android system application
        "com.android.systemui",
        // MIUI system application
        "com.miui.voiceassist",
        // Commonly used quick gesture application in Motorola devices
        "com.motorola.actions",
        // System application in Transsion devices (TECNO, Infinix, Itel)
        "com.transsion.aivoiceassistant",
    )


    @JvmStatic
    fun register(context: MainActivity) {
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope

        newScope.launch { check(context) }

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                newScope.launch { check(context) }
            }
        }.also {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                it
            )
        }
    }

    @JvmStatic
    fun unregister(context: Context) {
        contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        contentObserver = null
        scope?.cancel()
        scope = null
    }

    private suspend fun check(context: MainActivity) {
        currentCoroutineContext().ensureActive()

        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        val illegalServices = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).filter {
            (AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES and it.capabilities == AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) &&
            !excludedServices.contains(it.resolveInfo.serviceInfo.packageName)
        }

        isIllegalServiceDetected = illegalServices.isNotEmpty()

        withContext(Dispatchers.Main) {
            if (isIllegalServiceDetected) {
                if (alert == null)
                    alert = showAlert(context, illegalServices)

                return@withContext
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