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
        height = 48f
        padding = Vec4(10f, 0f)
        alignment = Anchor.Center
        onThemeChanged()
    }


    override fun onThemeChanged() {
        font = theme.textFont

        background = RoundedBox().apply {
            color = ColorARGB(0xFF1E1E2E)
            cornerRadius = 14f
        }

        if (theme.withBezelEffect) {
            foreground = BezelOutline(14f)
        }
    }


    companion object {
        val DefaultTheme = BadgeTheme()
    }
}

/**
 * A statistic badge is a badge that displays a value next to a label.
 */
open class StatisticBadge(label: String = "", value: String = "") : LinearContainer() {

    private val labelText = Badge(label).apply {
        height = FitParent
        foreground = null
    }

    private val valueText = ExtendedText().apply {
        height = FitParent
        font = ResourceManager.getInstance().getFont("smallFont")
        text = value
        alignment = Anchor.Center
        padding = Vec4(16f, 0f)
    }


    /**
     * The label of the badge.
     */
    var label by labelText::text

    /**
     * The value of the badge.
     */
    var value by valueText::text


    init {
        height = 48f
        orientation = Orientation.Horizontal

        foreground = BezelOutline(14f)
        background = RoundedBox().apply {
            color = ColorARGB(0xFF222234)
            cornerRadius = 14f
        }

        attachChild(labelText)
        attachChild(valueText)
    }

}



