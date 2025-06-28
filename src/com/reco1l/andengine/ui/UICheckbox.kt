package com.reco1l.andengine.ui

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

class UICheckbox(initialValue: Boolean = false) : UIControl<Boolean>(initialValue) {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        if (value) {
            background?.color = theme.accentColor * 0.5f
        } else {
            background?.color = theme.accentColor * 0.25f
        }

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

        background = UIBox().apply {
            cornerRadius = 12f
        }
    }

    override fun onValueChanged() {
        super.onValueChanged()

        background!!.clearModifiers(ModifierType.Color)
        checkSprite.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)

        if (value) {
            background!!.colorTo(Theme.current.accentColor * 0.5f, 0.1f)
            checkSprite.scaleTo(1f, 0.2f, Easing.OutBounce)
        } else {
            background!!.colorTo(Theme.current.accentColor * 0.25f, 0.1f)
            checkSprite.scaleTo(0f, 0.2f, Easing.OutBounce)
        }

        checkSprite.fadeIn(0.2f)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            value = !value
        }
        return true
    }

}