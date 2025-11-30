package com.reco1l.andengine.utils

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.badge
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.fillContainer
import com.reco1l.andengine.iconButton
import com.reco1l.andengine.labeledBadge
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.textButton
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.ColorVariant
import com.reco1l.andengine.ui.SizeVariant
import com.reco1l.andengine.ui.UICheckbox
import com.reco1l.andengine.ui.UISelect
import com.reco1l.andengine.ui.UISelect.Option
import com.reco1l.andengine.ui.UISlider
import com.reco1l.andengine.ui.UITextInput
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormInput
import com.reco1l.andengine.ui.form.FormSelect
import com.reco1l.andengine.ui.form.FormSlider
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec2
import com.reco1l.framework.math.Vec4

object ComponentTestScene : UIScrollableContainer() {
    init {
        width = Size.Full
        height = Size.Full
        anchor = Anchor.Center
        origin = Anchor.Center
        scrollAxes = Axes.Y
        backgroundColor = Color4.White / 0.3f
        style = {
            scrollPadding = Vec2(0f, 8.rem)
        }

        linearContainer {
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter
            orientation = Orientation.Vertical
            style = {
                spacing = 4f.srem
                padding = Vec4(3f.srem)
            }

            fillContainer {
                width = Size.Full
                style = {
                    spacing = 3f.srem
                }

                textButton {
                    width = Size.Full
                    leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)
                    text = "Button left"
                }

                textButton {
                    width = Size.Full
                    trailingIcon = FontAwesomeIcon(Icon.ArrowRight)
                    text = "Button right"
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                badge {
                    sizeVariant = SizeVariant.Small
                    text = "Small"
                }
                badge {
                    sizeVariant = SizeVariant.Medium
                    text = "Medium"
                }
                badge {
                    sizeVariant = SizeVariant.Large
                    text = "Large"
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                labeledBadge {
                    sizeVariant = SizeVariant.Small
                    label = "Label"
                    value = "Small"
                }
                labeledBadge {
                    sizeVariant = SizeVariant.Medium
                    label = "Label"
                    value = "Medium"
                }
                labeledBadge {
                    sizeVariant = SizeVariant.Large
                    label = "Label"
                    value = "Large"
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                textButton {
                    text = "Button"
                }

                textButton {
                    leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)
                    text = "Button"
                }

                textButton {
                    text = "Button"
                    colorVariant = ColorVariant.Secondary
                }

                textButton {
                    leadingIcon = FontAwesomeIcon(Icon.Gear)
                    text = "Button"
                    colorVariant = ColorVariant.Secondary
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                textButton {
                    text = "Button"
                    sizeVariant = SizeVariant.Small
                }

                textButton {
                    text = "Button"
                    sizeVariant = SizeVariant.Medium
                }

                textButton {
                    text = "Button"
                    sizeVariant = SizeVariant.Large
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                iconButton {
                    icon = FontAwesomeIcon(Icon.Gear)
                }

                iconButton {
                    icon = FontAwesomeIcon(Icon.Gear)
                    isSelected = true
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                +UITextInput("").apply {
                    placeholder = "Input"
                    style = {
                        width = 8.rem
                    }
                }

                +UISelect<String>().apply {
                    options = listOf(
                        Option<String>("1", "Option 1"),
                        Option<String>("2", "Option 2"),
                        Option<String>("3", "Option 3"),
                    )
                    style = {
                        width = 9.rem
                    }
                }

            }

            linearContainer {
                orientation = Orientation.Horizontal
                style = {
                    spacing = 3f.srem
                }

                +UISlider().apply {
                    style = {
                        width = 10.rem
                    }
                    value = 1f
                    min = 0.5f
                    max = 1.5f
                    onValueChange = {
                        UIEngine.current.fontScale = it
                    }
                }

                +UICheckbox()

            }

            +FormSlider().apply {
                label = "Slider"
                width = Size.Full
            }

            +FormInput().apply {
                label = "Input"
                width = Size.Full
            }

            +FormSelect<String>().apply {
                width = Size.Full
                label = "Select"
                options = listOf(
                    Option<String>("1", "Option 1"),
                    Option<String>("2", "Option 2"),
                    Option<String>("3", "Option 3"),
                )
            }

            +FormCheckbox().apply {
                label = "Checkbox"
                width = Size.Full
            }

        }

    }
}