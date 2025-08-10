package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
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
abstract class FormControl<V : Any, C: UIControl<V>>(initialValue: V): UILinearContainer() {

    /**
     * The control that is used to change the value.
     */
    abstract val control: C

    /**
     * The key that is used to identify the control.
     */
    var key
        get() = control.key
        set(value) { control.key = value }


    /**
     * The text that is displayed as the value of the control.
     */
    open val valueText: UIText? = null

    /**
     * The text that is displayed as the label of the control.
     */
    open val labelText = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        applyTheme = { color = it.accentColor }
    }

    /**
     * The button that is used to reset the value of the control to its default value.
     */
    open val resetButton = UITextButton().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        scaleCenter = Anchor.Center
        font = ResourceManager.getInstance().getFont("xs")
        text = "Reset"
        padding = Vec4(4f, 0f, 8f, 0f)
        content.spacing = -2f
        leadingIcon = UISprite().apply {
            textureRegion = ResourceManager.getInstance().getTexture("reset")
            width = 16f
            height = 16f
        }

        isVisible = false
        alpha = 0f
        translationX = -10f

        // We want to have a highlitghted color.
        isSelected = true

        onActionUp = {
            if (this@FormControl.isVisible) {
                value = defaultValue
            }
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

    /**
     * Whether to show the reset button or not. If true, the reset button will be shown when the
     * value of the control is not equal to the default value.
     */
    var showResetButton = true


    init {
        width = FillParent
        padding = Vec4(24f, 8f)
        background = UIBox().apply {
            color = Color4.White
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

        if (showResetButton) {
            resetButton.apply {
                if (!isVisible && value != defaultValue) {
                    clearEntityModifiers()
                    isVisible = true
                    translateToX(0f, 0.1f)
                    fadeTo(1f, 0.1f)
                } else if (isVisible && value == defaultValue) {
                    clearEntityModifiers()
                    translateToX(-10f, 0.1f)
                    fadeTo(0f, 0.1f).after {
                        isVisible = false
                    }
                }
            }
        } else {
            resetButton.isVisible = false
        }

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