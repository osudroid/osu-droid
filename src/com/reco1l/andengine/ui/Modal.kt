package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class Modal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val card: Container = Container().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipToBounds = true
        scaleCenter = Anchor.Center
    }

) : ExtendedEntity() {

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->
        card.background?.color = theme.accentColor * 0.15f
    }


    /**
     * Whether the modal shouldn't hide when the user clicks outside of the content.
     */
    var staticBackdrop = false

    /**
     * Whether the modal should be detached from the scene when hidden.
     */
    var detachOnHide = false


    init {
        width = FillParent
        height = FillParent

        isVisible = false
        alpha = 0f

        card.scaleX = 0.9f
        card.scaleY = 0.9f
        card.background = Box().apply { cornerRadius = 16f }
        card.foreground = BezelOutline(16f)

        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.2f
        }

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
    protected open fun onShow() = Unit

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
            createShowAnimation()().then {
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
            createHideAnimation()().then {
                isVisible = false
                onHidden()
            }
        }
    }

    //endregion

}


abstract class Dialog<T : ExtendedEntity>(val innerContent: T) : Modal(card = LinearContainer().apply {
    orientation = Orientation.Vertical
}) {

    val titleEntity = ExtendedText().apply {
        width = FillParent
        font = ResourceManager.getInstance().getFont("smallFont")
        alignment = Anchor.Center
        padding = Vec4(0f, 12f)
    }

    val buttonLayout: LinearContainer


    /**
     * The title of the dialog.
     */
    var title by titleEntity::text


    init {
        detachOnHide = true

        card.apply {
            relativeSizeAxes = Axes.X
            width = 0.5f
            anchor = Anchor.Center
            origin = Anchor.Center

            +titleEntity

            box {
                width = FillParent
                height = 1f
                applyTheme = {
                    color = it.accentColor
                    alpha = 0.1f
                }
            }

            +innerContent

            buttonLayout = linearContainer {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                padding = Vec4(24f)
                spacing = 12f
            }
        }
    }


    fun addButton(button: Button) {
        buttonLayout += button
    }
}

open class MessageDialog : Dialog<ExtendedText>(
    innerContent = ExtendedText().apply {
        width = FillParent
        font = ResourceManager.getInstance().getFont("smallFont")
        alignment = Anchor.Center
        padding = Vec4(24f)
    }
) {

    /**
     * The text of the dialog.
     */
    var text by innerContent::text

}