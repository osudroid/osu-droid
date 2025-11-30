package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
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

    /**
     * A callback that is invoked when the dropdown menu is expanded.
     */
    var onExpand: (() -> Unit)? = null

    /**
     * A callback that is invoked when the dropdown menu is collapsed.
     */
    var onCollapse: (() -> Unit)? = null


    private val wrapper = object : UIContainer() {
        init {
            width = Size.Full
            height = Size.Full
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
        width = Size.Auto
        height = Size.Auto
        scrollAxes = Axes.Y
        clipToBounds = true

        style = {
            radius = Radius.LG
            backgroundColor = it.accentColor * 0.175f
        }

        scaleCenter = Anchor.TopCenter
        alpha = 0f
        scaleY = 0f

        optionsContainer = linearContainer {
            orientation = Orientation.Vertical
            style = {
                spacing = 0.5f.srem
                padding = Vec4(1f.srem)
            }
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
            maxHeight = min(optionsContainer.height, parent.height - sceneSpaceY)
        }

        super.onManagedDraw(gl, camera)
    }


    //region Buttons

    fun addButton(block: UITextButton.() -> Unit): UITextButton {
        val button = UITextButton().apply {
            width = Size.Full
            alignment = Anchor.CenterLeft
            colorVariant = ColorVariant.Tertiary
            style += {
                color = it.accentColor
                backgroundColor = (it.accentColor * 0.9f) / 0f
                radius = Radius.LG
            }
            block()
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
            scaleToY(1f, 0.3f, Easing.OutBounce)

            wrapper.detachSelf()

            val scene = UIEngine.current.scene
            if (scene.hasChildScene()) {
                scene.childScene.attachChild(wrapper)
            } else {
                scene.attachChild(wrapper)
            }

            onExpand?.invoke()
        }
    }

    fun hide() {
        if (isExpanded) {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            scaleToY(0f, 0.2f, Easing.OutExpo)
            fadeTo(0f, 0.2f).after {
                updateThread {
                    wrapper.detachSelf()
                }
            }

            onCollapse?.invoke()
        }
    }

    //endregion

}