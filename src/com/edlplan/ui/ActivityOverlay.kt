package com.edlplan.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.edlplan.ui.fragment.BaseFragment
import java.util.*

object ActivityOverlay {
    private var fragmentManager: FragmentManager? = null
    private val displayingOverlay: MutableList<BaseFragment> = ArrayList()
    private var context: Activity? = null
    private var containerId = 0
    @JvmStatic
    fun initial(activity: AppCompatActivity, id: Int) {
        context = activity
        containerId = id
        fragmentManager = activity.supportFragmentManager
        if (fragmentManager == null) {
            throw RuntimeException("FragmentManager not found!")
        }
    }

    @JvmStatic
    @Synchronized
    fun onBackPress(): Boolean {
        if (fragmentManager != null && displayingOverlay.size > 0) {
            displayingOverlay[displayingOverlay.size - 1].callDismissOnBackPress()
            return true
        }
        return false
    }

    @Synchronized
    fun dismissOverlay(fragment: BaseFragment) {
        if (fragmentManager != null) {
            if (displayingOverlay.contains(fragment)) {
                displayingOverlay.remove(fragment)
                fragmentManager!!.beginTransaction().remove(fragment).commit()
            }
        }
    }

    @Synchronized
    fun addOverlay(fragment: BaseFragment, tag: String?) {
        if (fragmentManager != null) {
            if (displayingOverlay.contains(fragment) || fragmentManager!!.findFragmentByTag(tag) != null) {
                displayingOverlay.remove(fragment)
                fragmentManager!!.beginTransaction()
                        .remove(fragment)
                        .add(containerId, fragment, tag)
                        .commit()
                return
            }
            displayingOverlay.add(fragment)
            fragmentManager!!.beginTransaction()
                    .add(containerId, fragment, tag)
                    .commit()
        }
    }

    fun runOnUiThread(runnable: Runnable?) {
        context!!.runOnUiThread(runnable)
    }
}