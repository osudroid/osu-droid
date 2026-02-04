package com.osudroid.ui.v2.settings

import android.util.Log
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.fillContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.scrollableContainer
import com.reco1l.andengine.text
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.ColorVariant
import com.reco1l.andengine.ui.SizeVariant
import com.reco1l.andengine.ui.UISelect
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormControl
import com.reco1l.andengine.ui.form.FormInput
import com.reco1l.andengine.ui.form.FormSelect
import com.reco1l.andengine.box
import com.reco1l.andengine.clickableContainer
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.component.paddingBottom
import com.reco1l.andengine.component.paddingTop
import com.reco1l.andengine.compoundText
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.vw
import com.reco1l.andengine.ui.UIIconButton
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.andengine.ui.form.FloatPreferenceSlider
import com.reco1l.andengine.ui.form.IntPreferenceSlider
import com.reco1l.andengine.ui.form.PreferenceCheckbox
import com.reco1l.andengine.ui.form.PreferenceInput
import com.reco1l.andengine.ui.form.PreferenceSelect
import com.reco1l.andengine.ui.plus
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.log10


enum class Section {
    General,
    Gameplay,
    Graphics,
    Audio,
    Library,
    Input,
    Advanced
}

data class CategoryInfo(

    /**
     * The title of the category.
     */
    val title: String,

    /**
     * The options that will be displayed in the category.
     */
    val options: List<OptionInfo>,
)

enum class OptionType {
    Checkbox,
    Input,
    Select,
    Button,
    Slider,
}

data class OptionInfo(
    /**
     * The type of the option.
     */
    val type: OptionType,

    // General props

    /**
     * The key that will be used to store the value of this option.
     */
    val key: String,

    /**
     * The title of the option.
     */
    val title: Any,

    /**
     * The summary of the option.
     */
    val summary: Any? = null,

    /**
     * The default value that will be used if no value is stored for this option.
     */
    val defaultValue: Any? = null,

    /**
     * The action that will be performed when the value of this option changes.
     */
    val onChange: ((value: Any) -> Unit)? = null,

    // Button props

    /**
     * The action that will be performed when the button is clicked.
     *
     * This property is only used in [OptionType.Button] preferences.
     *
     */
    val onClick: (() -> Unit)? = null,


    // FormSelect props

    /**
     * The entries of the select.
     *
     * This property is only used in [OptionType.Select] preferences.
     */
    @param:ArrayRes val entries: Int? = null,

    /**
     * The entry values of the select.
     *
     * This property is only used in [OptionType.Select] preferences.
     */
    @param:ArrayRes val entryValues: Int? = null,

    // FormSlider props

    /**
     * The minimum value of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val min: Float? = null,

    /**
     * The maximum value of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val max: Float? = null,

    /**
     * The step of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val step: Float? = 0.01f,

    /**
     * The value formatter that will be used to format the value.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val valueFormatter: ((value: Float) -> String)? = null,


    /**
     * Called when the component is attached to the scene.
     */
    val onAttach: (component: UIComponent) -> Unit = { },
)

class SettingsMenu : UIScene() {

    private var selectedSection = Section.General

    private lateinit var sectionButtonsContainer: UILinearContainer
    private lateinit var sectionContainer: UILinearContainer


    init {
        isBackgroundEnabled = false

        clickableContainer {
            width = Size.Full
            height = Size.Full
            backgroundColor = Colors.Black.copy(alpha = 0.25f)

            onActionUp = {
                back()
            }
        }

        clickableContainer {
            height = Size.Full

            fillContainer {
                height = Size.Full
                orientation = Orientation.Horizontal
                style = {
                    width = 0.6f.vw
                    backgroundColor = it.accentColor * 0.105f
                    spacing = 2f.srem
                }

                // Sections
                scrollableContainer {
                    scrollAxes = Axes.Y
                    height = Size.Full
                    style = {
                        padding = Vec4(
                            UIEngine.current.safeArea.x + 2f.srem,
                            2f.srem,
                            2f.srem,
                            2f.srem
                        )
                        backgroundColor = it.accentColor * 0.120f
                    }

                    sectionButtonsContainer = linearContainer {
                        orientation = Orientation.Vertical

                        +SectionButton(Section.General, Icon.Gears, generalSection)
                        +SectionButton(Section.Gameplay, Icon.Gamepad, gameplaySection)
                        +SectionButton(Section.Graphics, Icon.Display, graphicsSection)
                        +SectionButton(Section.Audio, Icon.Headphones, audioSection)
                        +SectionButton(Section.Library, Icon.Book, librarySection)
                        +SectionButton(Section.Input, Icon.ComputerMouse, inputSection)
                        +SectionButton(Section.Advanced, Icon.ScrewdriverWrench, advancedSection)
                    }
                }

                scrollableContainer {
                    scrollAxes = Axes.Y
                    width = Size.Full
                    height = Size.Full

                    sectionContainer = linearContainer {
                        orientation = Orientation.Vertical
                        width = Size.Full
                        style = {
                            spacing = 4f.srem
                            padding = Vec4(4f.srem)
                        }
                    }
                }
            }
        }

        generateSection(Section.General, Icon.Gears, generalSection)
    }

    fun generateSection(section: Section, icon: Int, content: List<CategoryInfo>) {
        sectionContainer.alpha = 0f
        sectionContainer.detachChildren()
        (sectionContainer.parent as UIScrollableContainer).scrollY = 0f

        sectionContainer.apply {

            compoundText {
                fontSize = FontSize.LG
                text = section.name
                leadingIcon = FontAwesomeIcon(icon)
                style = {
                    color = it.accentColor
                    spacing = 2f.srem
                    padding = Vec4(2f.srem)
                }
            }

            content.forEach { category ->

                linearContainer {
                    orientation = Orientation.Vertical
                    width = Size.Full
                    style = {
                        spacing = 4f.srem
                    }

                    text {
                        width = Size.Full
                        alignment = Anchor.Center
                        text = category.title
                        style = {
                            radius = Radius.XL
                            color = it.accentColor * 0.9f
                            backgroundColor = it.accentColor * 0.115f
                            padding = Vec4(0f, 2f.srem)
                        }
                    }

                    category.options.forEach { option ->

                        @Suppress("UNCHECKED_CAST") val control = when (option.type) {
                            OptionType.Checkbox -> {
                                PreferenceCheckbox(option.key, option.defaultValue as? Boolean ?: false)
                            }

                            OptionType.Input -> {
                                PreferenceInput(option.key, option.defaultValue as? String ?: "")
                            }

                            OptionType.Select -> {
                                PreferenceSelect(option.key, if (option.defaultValue is String) listOf(option.defaultValue) else option.defaultValue as? List<String> ?: emptyList()).apply {
                                    // Si hay entries y entryValues definidos, cargar las opciones desde los recursos
                                    if (option.entries != null && option.entryValues != null) {
                                        val context = GlobalManager.getInstance().mainActivity
                                        val entriesArray = context.resources.getStringArray(option.entries)
                                        val valuesArray = context.resources.getStringArray(option.entryValues)

                                        Log.i("SettingsMenu", "entriesArray: ${entriesArray.contentToString()}")
                                        Log.i("SettingsMenu", "valuesArray: ${valuesArray.contentToString()}")
                                        Log.i("SettingsMenu", "value ${this.value}")
                                        Log.i("SettingsMenu", "initialValue: ${this.initialValue}")


                                        if (entriesArray.size == valuesArray.size) {
                                            options = entriesArray.mapIndexed { index, entry ->
                                                UISelect.Option(
                                                    value = valuesArray[index],
                                                    text = entry
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OptionType.Button -> {
                                UITextButton()
                            }

                            OptionType.Slider -> {
                                FloatPreferenceSlider(option.key, option.defaultValue as? Float ?: 0f).apply {
                                    control.min = option.min ?: 0f
                                    control.max = option.max ?: 1f
                                    control.step = option.step ?: 1f

                                    val step = option.step ?: 1f
                                    if (step > 0f && step < 1f) {
                                        control.precision = kotlin.math.ceil(-log10(step.toDouble())).toInt()
                                    }

                                    valueFormatter = option.valueFormatter ?: {
                                        if (option.step == 1f) {
                                            it.toInt().toString()
                                        } else {
                                            it.toString()
                                        }
                                    }
                                }
                            }

                        }

                        val title = when (option.title) {
                            is Int -> GlobalManager.getInstance().mainActivity.getString(option.title)
                            is String -> option.title
                            else -> option.title.toString()
                        }

                        if (control is FormControl<*, *>) {
                            control.key = option.key
                            control.label = title
                            control.style += {
                                padding = Vec4.Zero
                            }
                            control.onValueChanged = option.onChange
                        } else if (control is UITextButton) {
                            control.text = title
                            control.colorVariant = ColorVariant.Primary
                            control.onActionUp = option.onClick
                        }

                        control.width = Size.Full
                        option.onAttach(control)
                        +control
                    }
                }

            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (sectionContainer.alpha < 1f) {
            sectionContainer.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), sectionContainer.alpha, 1f, 0f, 0.2f)
        }
        super.onManagedUpdate(deltaTimeSec)
    }


    inner class SectionButton(private val section: Section, iconVal: Int, content: List<CategoryInfo> = listOf()) : UIIconButton() {
        init {
            width = Size.Full
            icon = FontAwesomeIcon(iconVal)
            sizeVariant = SizeVariant.Large
            colorVariant = ColorVariant.Tertiary

            onActionUp = {
                if (selectedSection != section) {
                    selectedSection = section
                    generateSection(section, iconVal, content)
                }
            }
        }

        override fun onManagedUpdate(deltaTimeSec: Float) {
            colorVariant = if (selectedSection == section) ColorVariant.Secondary else ColorVariant.Tertiary
            super.onManagedUpdate(deltaTimeSec)
        }
    }

}