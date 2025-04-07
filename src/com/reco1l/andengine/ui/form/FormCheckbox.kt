package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

class FormCheckbox(defaultValue: Boolean = false) : FormControl<Boolean, Checkbox>(defaultValue) {

    override val control = Checkbox().apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
    }

    override val labelText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }

    override val valueText: ExtendedText? = null


    init {
        padding = Vec4(24f, 12f)

        attachChild(labelText)
        attachChild(control)
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!super.onAreaTouched(event, localX, localY) && event.isActionUp) {
            control.value = !control.value
        }
        return true
    }

}