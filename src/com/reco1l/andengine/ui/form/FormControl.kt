package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Represents a form control that is used to change the value of a property.
 */
@Suppress("LeakingThis", "MemberVisibilityCanBePrivate")
abstract class FormControl<V : Any, C: Control<V>>(initialValue: V): ConstraintContainer() {

    /**
     * The control that is used to change the value.
     */
    abstract val control: C


    /**
     * The text that is displayed as the value of the control.
     */
    open val valueText: ExtendedText? = null

    /**
     * The text that is displayed as the label of the control.
     */
    open val labelText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
    }

    /**
     * The button that is used to reset the value of the control to its default value.
     */
    open val resetButton = object : Container() {

        init {
            padding = Vec4(6f, 0f)
            anchor = Anchor.CenterRight
            origin = Anchor.CenterLeft
            foreground = BezelOutline(12f)
            background = Box().apply {
                cornerRadius = 12f
                color = ColorARGB(0xFF222234)
            }
            translationX = 12f

            +ExtendedSprite().apply {
                textureRegion = ResourceManager.getInstance().getTexture("reset")
                width = 24f
                height = 24f
                anchor = Anchor.Center
                origin = Anchor.Center
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (isVisible) {
                if (event.isActionUp) {
                    value = defaultValue
                }
                return true
            }
            return false
        }

    }


    /**
     * The value of the control.
     */
    var value
        get() = control.value
        set(value) { control.value = value }

    /**
     * The default value of the control.
     */
    var defaultValue = initialValue

    /**
     * The callback that is called when the value of the control changes.
     */
    var onValueChanged: ((V) -> Unit)? = null

    /**
     * The label text.
     */
    var label
        get() = labelText.text
        set(value) { labelText.text = value }

    /**
     * Whether the control is enabled or not. If disabled, the control will not process any
     * touch events.
     */
    var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                onEnableStateChange()
            }
        }

    /**
     * The formatter that is used to format the value of the control.
     */
    var valueFormatter: (value: V) -> String = Any::toString
        set(value) {
            if (field != value) {
                field = value
                onControlValueChanged()
            }
        }


    init {
        width = FitParent
        padding = Vec4(24f)
        background = Box().apply {
            color = ColorARGB.White
            alpha = 0f
        }
    }


    //region Callbacks

    /**
     * Called when the value of the control changes.
     */
    open fun onControlValueChanged() {
        valueText?.text = valueFormatter(value)
        onValueChanged?.invoke(control.value)
    }

    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)

        if (isEnabled) {
            fadeTo(1f, 0.2f)
        } else {
            fadeTo(0.25f, 0.2f)
        }
    }

    //endregion


    override fun onManagedUpdate(deltaTimeSec: Float) {

        resetButton.isVisible = value != defaultValue

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (!isEnabled) {
            return true
        }

        val consumed = super.onAreaTouched(event, localX, localY)

        if (!consumed && event.isActionUp) {
            background!!.clearModifiers(ModifierType.Sequence)
            background!!.beginSequence {
                fadeTo(0.2f)
                fadeOut(0.2f)
            }
        }

        return consumed
    }

}