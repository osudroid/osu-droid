package com.edlplan.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.preference.PreferenceFragmentCompat
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.ActivityOverlay
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osuplus.R

abstract class SettingsFragment : PreferenceFragmentCompat(), BackPressListener {
    var root: View? = null
        private set
    var isCreated = false
        private set

    @Suppress("UNCHECKED_CAST")
    fun <T : View?> findViewById(@IdRes id: Int): T? {
        val o: Any? = if (root != null) root!!.findViewById<View>(id) else null
        return if (o == null) {
            null
        } else {
            o as T
        }
    }

    protected abstract fun onLoadView()
    protected fun playBackgroundHideInAnim(duration: Int) {
        val background = findViewById<View>(R.id.frg_background)
        if (background != null) {
            background.alpha = 0f
            background.animate().cancel()
            background.animate()
                    .alpha(1f)
                    .setDuration(duration.toLong())
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .start()
        }
    }

    protected fun playBackgroundHideOutAnim(duration: Int) {
        val background = findViewById<View>(R.id.frg_background)
        if (background != null) {
            background.animate().cancel()
            background.animate()
                    .alpha(0f)
                    .setDuration(duration.toLong())
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .start()
        }
    }

    open fun show() {
        ActivityOverlay.addOverlay(this, this.javaClass.name + "@" + this.hashCode())
    }

    open fun dismiss() {
        ActivityOverlay.dismissOverlay(this)
    }

    override fun callDismissOnBackPress() {
        dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isCreated = true
        root = super<PreferenceFragmentCompat>.onCreateView(inflater, container, savedInstanceState)
        onLoadView()
        return root
    }
}