package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*

@Suppress("LeakingThis")
open class Modal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val content: Container = Container().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipToBounds = true
        scaleCenter = Anchor.Center
    }

) : ExtendedEntity() {

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->
        content.background?.color = theme.accentColor * 0.15f
    }


    /**
     * Whether the modal shouldn't hide when the user clicks outside of the content.
     */
    var staticBackdrop = false


    init {
        width = FillParent
        height = FillParent

        isVisible = false
        alpha = 0f

        content.scaleX = 0.9f
        content.scaleY = 0.9f
        content.background = Box().apply { cornerRadius = 16f }
        content.foreground = BezelOutline(16f)

        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.2f
        }

        attachChild(content)
    }


    //region Input

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (!isVisible) {
            return false
        }

        if (!super.onAreaTouched(event, localX, localY)) {
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
        content.scaleTo(1f, 0.2f)
        fadeIn(0.2f)
    }

    /**
     * Creates the hide animation for the modal.
     */
    open fun createHideAnimation(): () -> UniversalModifier = {
        content.scaleTo(0.9f, 0.2f)
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
    protected open fun onHidden() = Unit


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