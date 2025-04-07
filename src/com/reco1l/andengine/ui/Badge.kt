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
    val backgroundColor: Long = 0xFF222234,
    val textColor: Long = 0xFFFFFFFF,
    val selectedBackgroundColor: Long = 0xFFF27272,
    val selectedTextColor: Long = 0xFF222234,
    val withBezelEffect: Boolean = true,
    val cornerRadius: Float = 14f,
    val iconSize: Float = 28f,
    val textFont: Font = ResourceManager.getInstance().getFont("smallFont")
) : ITheme


@Suppress("LeakingThis")
open class Badge : LinearContainer(), IWithTheme<BadgeTheme> {


    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    /**
     * The text of the button.
     */
    var text
        get() = labelEntity.text
        set(value) { labelEntity.text = value }


    //region Icons

    /**
     * The leading icon.
     */
    var leadingIcon: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value, 0)
                }
            }
        }

    /**
     * The trailing icon.
     */
    var trailingIcon: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value)
                }
            }
        }

    //endregion


    private val labelEntity = ExtendedText().apply {
        alignment = Anchor.Center
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    init {
        orientation = Orientation.Horizontal
        height = 60f
        padding = Vec4(16f, 0f, 18f, 0f)
        spacing = 8f
        scaleCenter = Anchor.Center

        attachChild(labelEntity)
        onThemeChanged()
    }


    override fun onThemeChanged() {
        foreground = if (theme.withBezelEffect) BezelOutline(theme.cornerRadius) else null
        trailingIcon?.let(::onIconChange)
        leadingIcon?.let(::onIconChange)
        labelEntity.font = theme.textFont
        labelEntity.color = ColorARGB(theme.textColor)
    }


    //region Callbacks

    /**
     * Called when an icon is added or changed.
     */
    open fun onIconChange(icon: ExtendedEntity) {
        icon.height = theme.iconSize
        icon.width = theme.iconSize
        icon.anchor = Anchor.CenterLeft
        icon.origin = Anchor.CenterLeft
    }

    //endregion

    companion object {
        val DefaultTheme = BadgeTheme()
    }
}