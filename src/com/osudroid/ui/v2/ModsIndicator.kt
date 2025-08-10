package com.osudroid.ui.v2

import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.toolkt.data.*
import com.rian.osu.utils.*
import org.json.JSONArray

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
    var mods: JSONArray? = null
        set(value) {
            if (field != value) {
                field = value

                detachChildren()
                value?.forEach { json ->

                    val modAcronym = json.optString("acronym", json.optString("a"))
                    val mod = ModUtils.allModsInstances.find { it.acronym.equals(modAcronym, ignoreCase = true) }!!

                    if (!showNonPlayableMods && !mod.isUserPlayable) {
                        // Skip mods that are not selectable by the user
                        return@forEach
                    }

                    // Mods that come from the server have a abbreviated structure.
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