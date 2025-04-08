package com.reco1l.andengine.ui.form

import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*

@Suppress("LeakingThis")
abstract class FormControl<V : Any?, C: Control<V>>(initialValue: V): Container() {

    /**
     * The control that is used to change the value.
     */
    abstract val control: C


    /**
     * The callback that is called when the value of the control changes.
     */
    var onValueChanged: ((V) -> Unit)? = null

    /**
     * The value of the control.
     */
    var value: V
        get() = control.value
        set(value) { control.value = value }

    /**
     * The default value of the control.
     */
    var defaultValue = initialValue


    /**
     * The text that is displayed as the label of the control.
     */
    protected abstract val labelText: ExtendedText

    /**
     * The text that is displayed as the value of the control.
     */
    protected abstract val valueText: ExtendedText?


    /**
     * The label.
     */
    var label = ""
        set(value) {
            if (field != value) {
                field = value
                labelText.text = value
            }
        }

    /**
     * The formatter that is used to format the value of the control.
     */
    var valueFormatter: (V) -> String = { it.toString() }
        set(value) {
            if (field != value) {
                field = value
                onControlValueChanged()
            }
        }


    init {
        width = FitParent
        background = Box().apply {
            color = ColorARGB.White
            alpha = 0f
        }
    }


    /**
     * Called when the value of the control changes.
     */
    open fun onControlValueChanged() {
        valueText?.text = valueFormatter(value)
        onValueChanged?.invoke(value)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        // Copying alpha from the control to preserve the fade effect when the control is disabled.
        labelText.alpha = control.alpha
        valueText?.alpha = control.alpha

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        background?.clearModifiers(ModifierType.Alpha)

        if (event.isActionDown) {
            background?.fadeTo(0.1f, 0.2f)
        }
        if ((event.isActionUp || event.isActionCancel) && background?.alpha != 0f) {
            background?.fadeTo(0f, 0.2f)
        }

        return super.onAreaTouched(event, localX, localY)
    }

}