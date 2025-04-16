package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*

data class ModalTheme(
    val backgroundColor: Long = 0xFF161622,
    val cornerRadius: Float = 16f,
) : ITheme

@Suppress("LeakingThis")
open class Modal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val content: Container = Container().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipChildren = true
        scaleCenter = Anchor.Center
    }

) : ExtendedEntity(), IWithTheme<ModalTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    /**
     * Whether the modal shouldn't hide when the user clicks outside of the content.
     */
    var staticBackdrop = false


    init {
        width = FitParent
        height = FitParent

        isVisible = false
        alpha = 0f

        content.scaleX = 0.9f
        content.scaleY = 0.9f

        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.2f
        }

        attachChild(content)
        onThemeChanged()
    }


    override fun onThemeChanged() {
        content.background = Box().apply {
            color = ColorARGB(theme.backgroundColor)
            cornerRadius = theme.cornerRadius
        }
        content.foreground = BezelOutline(theme.cornerRadius)
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


    companion object {
        val DefaultTheme = ModalTheme()
    }
}