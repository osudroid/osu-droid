package com.osudroid.ui.v2

import com.edlplan.framework.easing.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.container.*
import com.reco1l.framework.*
import com.rian.osu.utils.*
import org.anddev.andengine.input.touch.*

class ModsIndicator(val mods: ModHashMap, val iconSize: Float = 42f) : LinearContainer() {

    /**
     * Indicator for the mods.
     */
    var isExpanded = true


    init {
        orientation = Orientation.Horizontal
        spacing = 10f

        for (mod in mods.values) {
            +ModIcon(mod).apply {
                width = iconSize
                height = iconSize
            }
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        spacing = if (isExpanded) {
            Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.06f), 10f, spacing, 0f, 0.06f, Easing.OutBounce)
        } else {
            Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.06f), -5f, spacing, 0f, 0.06f, Easing.OutBounce)
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            isExpanded = !isExpanded
        }
        return true
    }
}