package com.reco1l.andengine.ui

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

class Checkbox(initialValue: Boolean = false) : Control<Boolean>(initialValue) {

    override var onThemeChange: ExtendedEntity.(Theme) -> Unit = { theme ->
        background?.color = if (value) theme.accentColor * 0.5f else theme.accentColor * 0.15f
        foreground?.color = if (value) theme.accentColor else theme.accentColor * 0.2f
        checkSprite.color = theme.accentColor
    }


    private val checkSprite = sprite {
        textureRegion = ResourceManager.getInstance().getTexture("check")
        width = 32f
        height = 32f
        anchor = Anchor.Center
        origin = Anchor.Center

        if (!initialValue) {
            scaleX = 0f
            scaleY = 0f
            alpha = 0f
        }
    }


    init {
        width = 48f
        height = 48f

        foreground = Box().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 12f
        }
        background = Box().apply {
            cornerRadius = 12f
        }
    }

    override fun onValueChanged() {
        super.onValueChanged()

        background!!.clearModifiers(ModifierType.Color)
        background!!.colorTo(if (value) Theme.current.accentColor * 0.5f else Theme.current.accentColor * 0.15f, 0.1f)

        foreground!!.clearModifiers(ModifierType.Color)
        foreground!!.colorTo(if (value) Theme.current.accentColor else Theme.current.accentColor * 0.2f, 0.1f)

        checkSprite.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
        checkSprite.fadeIn(0.2f)
        checkSprite.scaleTo(if (value) 1f else 0f, 0.2f, Easing.OutBounce)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            value = !value
        }
        return true
    }

}