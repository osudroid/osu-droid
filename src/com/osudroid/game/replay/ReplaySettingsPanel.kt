package com.osudroid.game.replay

import com.osudroid.math.Precision
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A panel that contains settings for replay playback. Can be expanded and collapsed by tapping the button on the left.
 */
class ReplaySettingsPanel : UIContainer() {
    lateinit var playbackControl: ReplayPlaybackControl
        private set

    lateinit var visualSettingsControl: ReplayVisualSettingsControl
        private set

    private val isExpanded
        get() = Precision.almostEquals(x, 0f)

    private val elementContainer = UIScrollableContainer().apply {
        x = BUTTON_WIDTH

        scrollAxes = Axes.Y
        width = PANEL_WIDTH
        height = FillParent
        showVerticalIndicator = false

        linearContainer {
            width = FillParent
            spacing = 20f
            padding = Vec4(0f, 20f)
            orientation = Orientation.Vertical

            addControls()
        }
    }

    init {
        x = PANEL_WIDTH
        height = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        alpha = 0f
        elementContainer.isVisible = false

        clock = UIEngine.current.clock
        processCustomClock = false

        +ReplaySettingsPanelButton()
        +elementContainer
    }

    override fun onLoadComplete() {
        fadeTo(0.5f, 0.2f)
    }

    fun expand() {
        if (isExpanded) {
            return
        }

        elementContainer.isVisible = true

        clearEntityModifiers()
        moveToX(0f, 0.2f)
        fadeIn(0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }

        clearEntityModifiers()
        moveToX(PANEL_WIDTH, 0.2f)
        fadeTo(0.5f, 0.2f).after { elementContainer.isVisible = false }
    }

    private fun UILinearContainer.addControls() {
        playbackControl = ReplayPlaybackControl()
        +playbackControl

        visualSettingsControl = ReplayVisualSettingsControl()
        +visualSettingsControl
    }

    private inner class ReplaySettingsPanelButton : UIContainer() {
        init {
            background = UIBox().apply {
                cornerRadius = BUTTON_RADIUS
                color = Color4(0xFF181825)
            }

            setSize(BUTTON_WIDTH, 150f)
            y = -125f

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            text {
                rotation = -90f
                anchor = Anchor.Center
                origin = Anchor.Center
                font = ResourceManager.getInstance().getFont("smallFont")
                text = "Settings"
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float) = when {
            event.isActionDown || event.isActionMove -> true

            event.isActionUp -> {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }

                false
            }

            else -> false
        }
    }

    companion object {
        private const val PANEL_WIDTH = 440f
        private const val BUTTON_WIDTH = 48f
        private const val BUTTON_RADIUS = 12f
    }
}
