package com.edlplan.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.edlplan.ui.fragment.BackPressListener
import java.util.*

object ActivityOverlay {
    private var fragmentManager: FragmentManager? = null
    private val displayingOverlay = mutableListOf<Fragment>()
    private var context: Activity? = null
    private var containerId = 0
    private val stateMap = mutableMapOf<String, Fragment.SavedState?>()
    @JvmStatic
    fun initial(activity: AppCompatActivity, id: Int) {
        context = activity
        containerId = id
        fragmentManager = activity.supportFragmentManager
    }

    @JvmStatic
    @Synchronized
    fun onBackPress(): Boolean {
        if (fragmentManager != null && displayingOverlay.size > 0) {
            val overlay: Fragment = displayingOverlay[displayingOverlay.size - 1]
            if(overlay is BackPressListener) {
                overlay.callDismissOnBackPress()
            }
            return true
        }
        return false
    }

    @Synchronized
    fun dismissOverlay(fragment: Fragment) {
        if (fragmentManager != null) {
            if (displayingOverlay.contains(fragment)) {
                displayingOverlay.remove(fragment)
                fragmentManager!!.beginTransaction().remove(fragment).commitAllowingStateLoss()
            }
        }
    }

    @Synchronized
    fun addOverlay(fragment: Fragment, tag: String?) {
        if (fragmentManager != null) {
            if (fragment.isAdded) {
                return
            }
            if (displayingOverlay.contains(fragment) || fragmentManager!!.findFragmentByTag(tag) != null) {
                displayingOverlay.remove(fragment)
                fragmentManager!!.beginTransaction()
                        .remove(fragment)
                        .add(containerId, fragment, tag)
                        .commitAllowingStateLoss()
                return
            }
            displayingOverlay.add(fragment)
            fragmentManager!!.beginTransaction()
                    .add(containerId, fragment, tag)
                    .commitAllowingStateLoss()
        }
    }

    @Synchronized
    fun saveState(tag: String?) {
        val fragment = fragmentManager!!.findFragmentByTag(tag)
        if (fragmentManager != null && fragment?.isAdded == true) {
            stateMap[tag!!] = fragmentManager!!.saveFragmentInstanceState(fragment)
        }
    }

    @Synchronized
    fun loadState(tag: String?): Fragment.SavedState? {
        val fragment = fragmentManager!!.findFragmentByTag(tag)
        if (fragmentManager != null && fragment?.isAdded == true) {
            return stateMap.remove(tag!!)
        }

        return null
    }

    fun runOnUiThread(runnable: Runnable?) {
        context!!.runOnUiThread(runnable)
    }
}