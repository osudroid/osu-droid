package com.reco1l.andengine.ui

import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

class UIDropdown(var trigger: UIComponent) : UIScrollableContainer() {

    /**
     * Whether the dropdown menu is currently expanded or not.
     */
    val isExpanded: Boolean
        get() = wrapper.hasParent()


    private val wrapper = object : UIContainer() {
        init {
            width = FillParent
            height = FillParent
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (!super.onAreaTouched(event, localX, localY) && !this@UIDropdown.contains(localX, localY)) {
                hide()
            }
            return true
        }
    }

    private val optionsContainer: UILinearContainer


    init {
        width = MatchContent
        height = MatchContent
        scrollAxes = Axes.Y
        clipToBounds = true
        background = UIBox().apply {
            cornerRadius = 14f
            applyTheme = { color = it.accentColor * 0.175f }
        }
        scaleCenter = Anchor.Center
        alpha = 0f
        scale = Vec2(0.9f)

        optionsContainer = linearContainer {
            orientation = Orientation.Vertical
            spacing = 4f
            padding = Vec4(4f)
        }

        wrapper.attachChild(this)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (isExpanded) {
            var minWidth = trigger.width

            optionsContainer.forEach { it as UITextButton
                minWidth = max(minWidth, it.contentWidth + it.padding.horizontal)
            }

            optionsContainer.minWidth = minWidth
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if (isExpanded) {
            val (sceneSpaceX, sceneSpaceY) = trigger.convertLocalToSceneCoordinates(0f, trigger.height)

            x = sceneSpaceX
            y = sceneSpaceY
            maxHeight = parent.getHeight() - sceneSpaceY
        }

        super.onManagedDraw(gl, camera)
    }


    //region Buttons

    fun addButton(block: UITextButton.() -> Unit): UITextButton {
        val button = object : UITextButton() {

            override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
                color = theme.accentColor
            }

            init {
                width = FillParent
                alignment = Anchor.CenterLeft
                background = null
                foreground = UIBox().apply {
                    cornerRadius = 12f
                    applyTheme = {
                        color = it.accentColor
                        alpha = 0f
                    }
                }
                block()
            }

            override fun onSelectionChange() {
                foreground!!.clearModifiers(ModifierType.Alpha)
                foreground!!.fadeTo(if (isSelected) 0.25f else 0f, 0.2f)
            }
        }

        optionsContainer += button
        return button
    }

    fun clearButtons() {
        optionsContainer.detachChildren()
    }

    fun forEachButton(action: (UITextButton) -> Unit) {
        optionsContainer.forEach { action(it as UITextButton) }
    }

    //endregion

    //region Visibility

    fun show() {
        if (!isExpanded) {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            fadeTo(1f, 0.2f)
            scaleTo(1f, 0.2f)

            wrapper.detachSelf()

            val scene = UIEngine.current.scene
            if (scene.hasChildScene()) {
                scene.childScene.attachChild(wrapper)
            } else {
                scene.attachChild(wrapper)
            }
        }
    }

    fun hide() {
        if (isExpanded) {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            scaleTo(0.9f, 0.2f)
            fadeTo(0f, 0.2f).after {
                updateThread {
                    wrapper.detachSelf()
                }
            }
        }
    }

    //endregion

}