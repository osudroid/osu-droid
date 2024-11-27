package com.edlplan.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.ActivityOverlay
import com.edlplan.ui.EasingHelper
import com.reco1l.osu.*
import ru.nsu.ccfit.zuev.osuplus.R

abstract class BaseFragment : Fragment(), BackPressListener {
    var root: View? = null
        private set
    private var background: View? = null
    var onDismissListener: OnDismissListener? = null
    var isDismissOnBackgroundClick = false
    var isCreated = false
        private set
    var isDismissOnBackPress = true


    /**
     * If true, the fragment will intercept back press event when it's received.
     */
    var interceptBackPress = true


    private var isLoaded = false

    private var isDismissCalled = false


    @get:IdRes
    val backgroundId: Int
        get() = R.id.frg_background

    @Suppress("UNCHECKED_CAST")
    fun <T : View?> findViewById(@IdRes id: Int): T? {
        val o: Any? = if (root != null) root!!.findViewById<View>(id) else null
        return if (o == null) {
            null
        } else {
            o as T
        }
    }

    @get:LayoutRes
    protected abstract val layoutID: Int
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
        isDismissCalled = true

        if (isLoaded) {
            isDismissCalled = false
            ActivityOverlay.dismissOverlay(this)
            onDismissListener?.OnDismiss()
        }
    }

    fun save() {
        ActivityOverlay.saveState(this.javaClass.name + "@" + this.hashCode())
    }

    fun load(): SavedState? {
        return ActivityOverlay.loadState(this.javaClass.name + "@" + this.hashCode())
    }

    override fun callDismissOnBackPress() {
        if (isDismissOnBackPress) {
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isLoaded = false
        isCreated = true
        root = inflater.inflate(layoutID, container, false)
        findViewById<View>(backgroundId)?.setOnClickListener {
            if (isDismissOnBackgroundClick) {
                dismiss()
            }
        }

        onLoadView()
        isLoaded = true

        if (isDismissCalled) {
            dismiss()
            isDismissCalled = false
        }

        return root
    }

    fun interface OnDismissListener {
        fun OnDismiss()
    }
}