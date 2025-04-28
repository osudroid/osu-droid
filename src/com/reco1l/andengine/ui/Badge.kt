@file:Suppress("LeakingThis")

package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.ResourceManager


/**
 * A badge is a small piece of information that can be used to display a value or a status.
 */
open class Badge : CompoundText() {

    override var onThemeChange: ExtendedEntity.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        background?.color = theme.accentColor * 0.15f
    }

    init {
        padding = Vec4(12f, 8f)
        foreground = BezelOutline(12f)
        background = Box().apply { cornerRadius = 12f }
    }

}

/**
 * A statistic badge is a badge that displays a value next to a label.
 */
open class LabeledBadge : LinearContainer() {

    override var onThemeChange: ExtendedEntity.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        background?.color = theme.accentColor * 0.15f
    }


    /**
     * The entity of the badge's label.
     */
    val labelEntity = text {
        font = ResourceManager.getInstance().getFont("smallFont")
        padding = Vec4(12f, 8f)
        alignment = Anchor.Center
        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.1f
            cornerRadius = 12f
        }
    }

    /**
     * The value of the badge.
     */
    val valueEntity = text {
        font = ResourceManager.getInstance().getFont("smallFont")
        padding = Vec4(12f, 8f)
        alignment = Anchor.Center
    }

    //region Shortcuts

    /**
     * The label of the badge.
     */
    var label by labelEntity::text

    /**
     * The value of the badge.
     */
    var value by valueEntity::text

    //endregion


    init {
        orientation = Orientation.Horizontal
        foreground = BezelOutline(12f)
        background = Box().apply { cornerRadius = 12f }
    }
}



