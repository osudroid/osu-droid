package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*

data class ModalTheme(
    val backgroundColor: Long = 0xFF161622,
    val cornerRadius: Float = 16f,
) : ITheme

@Suppress("LeakingThis")
open class Modal : ExtendedEntity(), IWithTheme<ModalTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    init {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipChildren = true
        scaleCenter = Anchor.Center

        isVisible = false
        scaleX = 0.9f
        scaleY = 0.9f
        alpha = 0f

        onThemeChanged()
    }


    override fun onThemeChanged() {
        background = RoundedBox().apply {
            color = ColorARGB(theme.backgroundColor)
            cornerRadius = theme.cornerRadius
        }
        foreground = BezelOutline(theme.cornerRadius)
    }


    //region Input

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!isVisible) {
            return false
        }

        super.onAreaTouched(event, localX, localY)

        // Prevent touch events from propagating to the entites below.
        return true
    }

    //endregion

    //region Visibilty state

    /**
     * Creates the show animation for the modal.
     */
    open fun createShowAnimation(): () -> UniversalModifier = {
        beginParallel {
            fadeIn(0.2f)
            scaleTo(1f, 0.2f)
        }
    }

    /**
     * Creates the hide animation for the modal.
     */
    open fun createHideAnimation(): () -> UniversalModifier = {
        beginParallel {
            scaleTo(0.9f, 0.2f)
            fadeOut(0.2f)
        }
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