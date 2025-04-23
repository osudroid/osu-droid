@file:Suppress("LeakingThis")

package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.ResourceManager


data class BadgeTheme(
    val backgroundColor: Long = 0xFF1E1E2E,
    val textColor: Long = 0xFFFFFFFF,
    val withBezelEffect: Boolean = true,
    val textFont: Font = ResourceManager.getInstance().getFont("smallFont")
) : ITheme

/**
 * A badge is a small piece of information that can be used to display a value or a status.
 */
open class Badge(content: String = "") : ExtendedText(), IWithTheme<BadgeTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    init {
        text = content
        height = 38f
        padding = Vec4(12f, 0f)
        alignment = Anchor.Center
        onThemeChanged()
    }


    override fun onThemeChanged() {
        font = theme.textFont

        background = Box().apply {
            color = ColorARGB(0xFF1E1E2E)
            cornerRadius = 12f
        }

        if (theme.withBezelEffect) {
            foreground = BezelOutline(12f)
        }
    }


    companion object {
        val DefaultTheme = BadgeTheme()
    }
}

/**
 * A statistic badge is a badge that displays a value next to a label.
 */
open class LabeledBadge(label: String = "", value: String = "") : LinearContainer() {

    private val labelText = Badge(label).apply {
        height = FillParent
        foreground = null
        background!!.color = ColorARGB.Black
        background!!.alpha = 0.1f
    }

    /**
     * The value of the badge.
     */
    val valueText = ExtendedText().apply {
        height = FillParent
        font = ResourceManager.getInstance().getFont("smallFont")
        text = value
        alignment = Anchor.Center
        padding = Vec4(12f, 0f)
    }


    /**
     * The label of the badge.
     */
    var label by labelText::text


    init {
        height = 38f
        orientation = Orientation.Horizontal

        foreground = BezelOutline(12f)
        background = Box().apply {
            color = ColorARGB(0xFF222234)
            cornerRadius = 12f
        }

        attachChild(labelText)
        attachChild(valueText)
    }

}



