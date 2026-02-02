package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*

@Suppress("LeakingThis")
open class UIModal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val card: UIContainer = UIContainer().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipToBounds = true
        scaleCenter = Anchor.Center
        style = {
            backgroundColor = it.accentColor * 0.15f
            radius = Radius.LG
        }
    }

) : UIComponent() {


    /**
     * Whether the modal shouldn't hide when the user clicks outside of the content.
     */
    var staticBackdrop = false

    /**
     * Whether the modal should be detached from the scene when hidden.
     */
    var detachOnHide = false


    init {
        style = {
            backgroundColor = Color4.Black.copy(alpha = 0.3f)
        }

        width = Size.Full
        height = Size.Full

        isVisible = false
        alpha = 0f

        card.scaleX = 0.9f
        card.scaleY = 0.9f

        attachChild(card)
    }


    //region Input

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (!isVisible) {
            return false
        }

        if (!super.onAreaTouched(event, localX, localY) && !card.contains(localX, localY)) {
            if (!staticBackdrop) {
                hide()
            }
        }
        return true
    }

    //endregion

    //region Visibilty state

    /**
     * Creates the show animation for the modal.
     */
    open fun createShowAnimation(): () -> UniversalModifier = {
        card.scaleTo(1f, 0.2f)
        fadeIn(0.2f)
    }

    /**
     * Creates the hide animation for the modal.
     */
    open fun createHideAnimation(): () -> UniversalModifier = {
        card.scaleTo(0.9f, 0.2f)
        fadeOut(0.2f)
    }


    /**
     * Called when [show] is called. This is where you should set up the modal's animations.
     */
    protected open fun onShow() {
        // If there's not parent previously set, attach to the current scene.
        if (parent == null) {
            var currentScene = UIEngine.current.scene

            // Find the top-most scene in the hierarchy.
            while (currentScene.hasChildScene()) {
                currentScene = currentScene.childScene
            }

            currentScene.attachChild(this)
        }
    }

    /**
     * Called after all show animations are finished.
     */
    protected open fun onShown() = Unit

    /**
     * Called when [hide] is called.
     */
    protected open fun onHide() = Unit

    /**
     * Called after all hide animations are finished.
     */
    protected open fun onHidden() {
        if (detachOnHide) {
            detachSelf()
        }
    }


    /**
     * Shows the modal.
     */
    fun show() {
        if (!isVisible) {
            onShow()
            isVisible = true

            clearModifiers(ModifierType.Parallel)
            createShowAnimation()().after {
                onShown()
            }
        }
    }

    /**
     * Hides the modal.
     */
    fun hide() {
        if (isVisible) {
            onHide()

            clearModifiers(ModifierType.Parallel)
            createHideAnimation()().after {
                isVisible = false
                onHidden()
            }
        }
    }

    //endregion

}


abstract class UIDialog<T : UIComponent>(val innerContent: T) : UIModal(card = UILinearContainer().apply {
    orientation = Orientation.Vertical
    style = {
        backgroundColor = it.accentColor * 0.15f
        radius = Radius.XL
    }
}) {

    val titleEntity = UIText().apply {
        width = Size.Full
        alignment = Anchor.Center

        style = {
            color = it.accentColor * 0.7f
            fontSize = FontSize.SM
            padding = Vec4(3f.srem)
        }
    }

    val buttonLayout: UIFillContainer


    /**
     * The title of the dialog.
     */
    var title by titleEntity::text


    init {
        detachOnHide = true

        card.apply {
            width = 0.5f.pct
            anchor = Anchor.Center
            origin = Anchor.Center

            +titleEntity

            box {
                width = Size.Full
                height = 2f
                style = {
                    color = it.accentColor.copy(alpha = 0.1f)
                }
            }

            +innerContent

            box {
                width = Size.Full
                height = 2f
                style = {
                    color = it.accentColor.copy(alpha = 0.1f)
                }
            }

            buttonLayout = fillContainer {
                width = Size.Full
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = {
                    padding = Vec4(3f.srem)
                    spacing = 3f.srem
                }
            }
        }
    }


    fun addButton(block: UITextButton.() -> Unit) {
        addButton(UITextButton().apply(block))
    }

    fun addButton(button: UIButton) {
        buttonLayout.apply {
            attachChild(button.apply {
                width = Size.Full
            })
        }

    }
}

open class UIMessageDialog : UIDialog<UIText>(
    innerContent = UIText().apply {
        width = Size.Full
        fontSize = FontSize.SM
        alignment = Anchor.Center
        padding = Vec4(24f)

        style = {
            color = it.accentColor
        }
    }
) {

    /**
     * The text of the dialog.
     */
    var text by innerContent::text

}

open class UIConfirmDialog : UIMessageDialog() {

    /**
     * Callback invoked when the user confirms the dialog.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * Callback invoked when the user cancels the dialog.
     */
    var onCancel: (() -> Unit)? = null


    init {
        addButton(UITextButton().apply {
            text = "Yes"
            isSelected = true
            onActionUp = {
                onConfirm?.invoke()
                hide()
            }
        })

        addButton(UITextButton().apply {
            text = "No"
            onActionUp = {
                onCancel?.invoke()
                hide()
            }
        })
    }

}