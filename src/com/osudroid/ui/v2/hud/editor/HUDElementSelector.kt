package com.osudroid.ui.v2.hud.editor

import com.reco1l.andengine.*
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.hitobject.HitObject
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.reflect.full.primaryConstructor

class HUDElementSelector(private val hud: GameplayHUD) : UIContainer(), IGameplayEvents {


    /**
     * Whether the element selector is expanded.
     */
    val isExpanded
        get() = x >= 0f


    private val elements = HUDElements.entries.map { it.type.primaryConstructor!!.call() }

    private val elementList = UIScrollableContainer().apply {

        scrollAxes = Axes.Y
        height = FillParent
        width = SELECTOR_WIDTH

        background = UIBox().apply {
            color = Color4(0xFF1E1E2E)
        }

        attachChild(UILinearContainer().apply {
            width = FillParent
            padding = Vec4(16f)
            spacing = 12f
            orientation = Orientation.Vertical

            elements.forEach { element ->
                attachChild(HUDElementPreview(element, hud))
            }
        })

    }


    init {
        height = FillParent

        x = -SELECTOR_WIDTH

        // The button to show/hide the element selector
        attachChild(object : UIContainer() {

            init {
                background = UIBox().apply {
                    cornerRadius = BUTTON_RADIUS
                    color = Color4(0xFF181825)
                }

                setSize(BUTTON_WIDTH, 150f)
                x = SELECTOR_WIDTH - BUTTON_RADIUS

                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                attachChild(UIText().apply {
                    rotation = -90f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Elements"

                    x = BUTTON_RADIUS / 2
                })
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionDown) {
                    return true
                }

                if (event.isActionUp) {
                    if (isExpanded) {
                        collapse()
                    } else {
                        expand()
                    }
                    return false
                }
                return false
            }

        })

        attachChild(elementList)
    }


    fun expand() {
        if (isExpanded) {
            return
        }

        hud.selected = null

        clearEntityModifiers()
        elementList.isVisible = true
        moveToX(0f, 0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }
        clearEntityModifiers()
        moveToX(-SELECTOR_WIDTH, 0.2f).after { elementList.isVisible = false }
    }


    //region Gameplay Events

    override fun onNoteHit(statistics: StatisticV2) {
        elements.fastForEach { it.onNoteHit(statistics) }
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        elements.fastForEach { it.onBreakStateChange(isBreak) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        elements.fastForEach { it.onGameplayUpdate(gameScene, secondsElapsed) }
    }

    override fun onGameplayTouchDown(time: Float) {
        elements.fastForEach { it.onGameplayTouchDown(time) }
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        elements.fastForEach { it.onHitObjectLifetimeStart(obj) }
    }

    override fun onAccuracyRegister(accuracy: Float) {
        elements.fastForEach { it.onAccuracyRegister(accuracy) }
    }

    //endregion


    companion object {

        const val SELECTOR_WIDTH = 340f
        const val BUTTON_WIDTH = 48f
        const val BUTTON_RADIUS = 12f

    }

}

