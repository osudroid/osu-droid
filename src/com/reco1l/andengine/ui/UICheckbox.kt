package com.reco1l.andengine.ui

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.rem
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

class UICheckbox(initialValue: Boolean = false) : UIControl<Boolean>(initialValue) {

    private val checkIcon = FontAwesomeIcon(Icon.Check).apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        style = {
            width = 1.075f.rem
            height = 1.075f.rem
            color = it.accentColor
        }

        if (!initialValue) {
            scaleX = 0f
            scaleY = 0f
            alpha = 0f
        }
    }


    init {
        style = {
            width = 2.15f.rem
            height = 2.15f.rem
            backgroundColor = if (value) it.accentColor * 0.5f else it.accentColor * 0.25f
            radius = Radius.LG
        }
        +checkIcon
    }

    override fun onValueChanged() {
        super.onValueChanged()

        checkIcon.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)

        if (value) {
            backgroundColor = Theme.current.accentColor * 0.5f
            checkIcon.scaleTo(1f, 0.2f, Easing.OutBounce)
        } else {
            backgroundColor = Theme.current.accentColor * 0.25f
            checkIcon.scaleTo(0f, 0.2f, Easing.OutBounce)
        }

        checkIcon.fadeIn(0.2f)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            value = !value
        }
        return true
    }

}