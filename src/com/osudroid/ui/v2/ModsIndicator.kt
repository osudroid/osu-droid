package com.osudroid.ui.v2

import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.rian.osu.mods.*

class ModsIndicator : UILinearContainer() {

    /**
     * Whether to show mods that are not playable. Defined by [Mod.isUserPlayable].
     */
    var showNonPlayableMods: Boolean = false
        set(value) {
            if (field != value) {
                field = value

                // Reapply mods to update visibility
                val previousMods = mods
                mods = null
                mods = previousMods
            }
        }

    /**
     * The list of mods to display.
     */
    var mods: Iterable<Mod>? = null
        set(value) {
            if (field != value) {
                field = value

                detachChildren()

                value?.forEach { mod ->
                    if (!showNonPlayableMods && !mod.isUserPlayable) {
                        return@forEach
                    }

                    +ModIcon(mod).apply {
                        width = iconSize
                        height = iconSize
                    }
                }
            }
        }

    /**
     * The size of the mod icons in this indicator.
     */
    var iconSize = 42f
        set(value) {
            if (field != value) {
                field = value

                forEach { icon -> icon as UIComponent
                    icon.width = value
                    icon.height = value
                }
            }
        }


    init {
        orientation = Orientation.Horizontal
        spacing = -5f
    }

}