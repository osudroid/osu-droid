package com.reco1l.andengine.ui

import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*

data class CheckboxTheme(
    val uncheckedColor: Long = 0xFF222234,
    val checkedColor: Long = 0xFFF27272,
) : ITheme

class Checkbox(initialValue: Boolean = false) : Control<Boolean>(initialValue), IWithTheme<CheckboxTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    init {
        width = 48f
        height = 24f
        onThemeChanged()
    }


    override fun onThemeChanged() {
        background = Box().apply {
            color = ColorARGB(if (value) theme.checkedColor else theme.uncheckedColor)
            cornerRadius = 12f
        }
    }

    override fun onValueChanged() {
        super.onValueChanged()

        background!!.clearModifiers(ModifierType.Color)
        background!!.colorTo(if (value) theme.checkedColor else theme.uncheckedColor, 0.1f)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            value = !value
        }
        return true
    }


    companion object {
        val DefaultTheme = CheckboxTheme()
    }
}