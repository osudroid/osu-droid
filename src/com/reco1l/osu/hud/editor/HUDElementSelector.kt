package com.reco1l.osu.hud.editor

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.container.LinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.ScrollableContainer
import com.reco1l.andengine.shape.Box
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec4
import com.reco1l.osu.hud.GameplayHUD
import com.reco1l.osu.hud.HUDElements
import com.reco1l.osu.hud.IGameplayEvents
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.reflect.full.primaryConstructor

class HUDElementSelector(private val hud: GameplayHUD) : Container(), IGameplayEvents {


    private val elements = HUDElements.map { it.primaryConstructor!!.call() }


    init {
        relativeSizeAxes = Axes.Y
        height = 1f

        x = -SELECTOR_WIDTH

        // The button to show/hide the element selector
        attachChild(object : Container() {

            init {
                background = RoundedBox().apply {
                    cornerRadius = BUTTON_RADIUS
                    color = ColorARGB(0xFF181825)
                }

                setSize(BUTTON_WIDTH, 150f)
                x = SELECTOR_WIDTH - BUTTON_RADIUS

                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                attachChild(ExtendedText().apply {
                    rotation = -90f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Elements"

                    x = BUTTON_RADIUS / 2
                })
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionUp) {

                    this@HUDElementSelector.clearEntityModifiers()

                    if (this@HUDElementSelector.x < 0f) {
                        this@HUDElementSelector.moveToX(0f, 0.2f)

                        hud.moveToX(SELECTOR_WIDTH, 0.2f)
                        hud.sizeToX(Config.getRES_WIDTH() - SELECTOR_WIDTH, 0.2f)
                    } else {
                        this@HUDElementSelector.moveToX(-SELECTOR_WIDTH, 0.2f)

                        hud.moveToX(0f, 0.2f)
                        hud.sizeToX(Config.getRES_WIDTH().toFloat(), 0.2f)
                    }
                }

                return false
            }

        })

        attachChild(ScrollableContainer().apply {

            scrollAxes = Axes.Y
            relativeSizeAxes = Axes.Y
            height = 1f
            width = SELECTOR_WIDTH
            indicatorY!!.width = 4f

            background = Box().apply {
                color = ColorARGB(0xFF1E1E2E)
            }

            attachChild(LinearContainer().apply {
                relativeSizeAxes = Axes.X
                width = 1f
                padding = Vec4(16f)
                spacing = 12f
                orientation = Orientation.Vertical

                elements.forEach { element ->
                    attachChild(HUDElementPreview(element, hud))
                }
            })

        })
    }


    //region Gameplay Events

    override fun onNoteHit(statistics: StatisticV2) {
        elements.fastForEach { it.onNoteHit(statistics) }
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        elements.fastForEach { it.onBreakStateChange(isBreak) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        elements.fastForEach { it.onGameplayUpdate(gameScene, statistics, secondsElapsed) }
    }

    override fun onAccuracyRegister(accuracy: Float) {
        elements.fastForEach { it.onAccuracyRegister(accuracy) }
    }

    //endregion


    companion object {

        const val SELECTOR_WIDTH = 300f
        const val BUTTON_WIDTH = 48f
        const val BUTTON_RADIUS = 12f

    }

}

