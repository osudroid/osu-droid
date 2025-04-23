package com.osudroid.ui.v2

import com.edlplan.framework.easing.*
import com.osudroid.data.*
import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.ExtendedEntity.Companion.FitParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.rian.osu.utils.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import kotlin.math.*

class GameLoaderScene(private val gameScene: GameScene, beatmap: BeatmapInfo, mods: ModHashMap, private val isRestart: Boolean) : ExtendedScene() {

    private var timeoutStartTime = -1L
    private var isStarting = false

    private val dimBox: Box
    private val mainContainer: Container


    init {

        // Background
        sprite {
            width = FitParent
            height = FitParent
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture(if (Config.isSafeBeatmapBg()) "menu-background" else "::background")
        }

        // Dim
        dimBox = box {
            width = FitParent
            height = FitParent
            color = ColorARGB.Black
            alpha = 0.7f
        }

        // Beatmap info
        mainContainer = container {
            width = FitParent
            height = FitParent
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleCenter = Anchor.Center

            fadeIn(0.2f, Easing.OutCubic)
            scaleTo(1f, 0.2f, Easing.OutCubic)

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 10f
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                x = 60f

                // Title
                text {
                    font = ResourceManager.getInstance().getFont("font")
                    text = beatmap.titleText
                }

                // Difficulty
                text {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = beatmap.version
                }

                // Creator
                text {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "by ${beatmap.artistText}"
                    color = ColorARGB(0xFF8282A8)
                }

                // Mods
                if (mods.isNotEmpty()) {
                    +ModsIndicator(mods).apply {
                        isExpanded = false
                    }
                }
            }

            +LoadingCircle()

            if (!isRestart) {
                scrollableContainer {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    width = 400f
                    height = FitParent
                    x = -20f
                    scrollAxes = Axes.Y

                    +QuickSettingsLayout()
                }
            }
        }
    }


    override fun onSceneTouchEvent(event: TouchEvent): Boolean {
        timeoutStartTime = System.currentTimeMillis()
        return super.onSceneTouchEvent(event)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isStarting) {

            if (gameScene.isReadyToStart) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (System.currentTimeMillis() - timeoutStartTime > MINIMUM_TIMEOUT || Multiplayer.isMultiplayer || isRestart) {
                    isStarting = true

                    val backgroundBrigthness = Config.getInt("bgbrightness", 25)

                    mainContainer.fadeOut(0.1f, Easing.OutExpo)
                    dimBox.fadeTo(1f - backgroundBrigthness / 100f, 0.2f).then {
                        gameScene.start()
                    }
                }

            } else {
                timeoutStartTime = System.currentTimeMillis()
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    private class LoadingCircle : Container() {

        private val rotatingCircle: Circle

        init {
            width = 32f
            height = 32f
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            x = 60f
            y = -60f

            circle {
                width = FitParent
                height = FitParent
                alpha = 0.3f
            }

            rotatingCircle = circle {
                width = FitParent
                height = FitParent
                rotationCenter = Anchor.Center
                setPortion(0.2f)
            }

        }

        override fun onManagedUpdate(deltaTimeSec: Float) {
            rotatingCircle.rotation += 20f * deltaTimeSec
            super.onManagedUpdate(deltaTimeSec)
        }
    }


    private class QuickSettingsLayout : LinearContainer() {

        private var lastTimeTouched = System.currentTimeMillis()


        init {
            width = FitParent
            spacing = 20f
            padding = Vec4(0f, 20f)
            orientation = Orientation.Vertical

            collapsibleCard {
                width = FitParent
                title = "Beatmap"

                content.apply {

                    val offsetSlider = FormSlider(0f).apply {
                        label = "Offset"
                        control.min = -250f
                        control.max = 250f
                        valueFormatter = { "${it.roundToInt()}ms" }
                    }
                    +offsetSlider

                    linearContainer {
                        spacing = 10f
                        padding = Vec4(12f, 0f, 12f, 12f)
                        anchor = Anchor.TopCenter
                        origin = Anchor.TopCenter

                        fun StepButton(step: Int) = button {
                            width = 50f
                            text = (if (step > 0) "+" else "") + step.toString()
                            onActionUp = {
                                offsetSlider.value += step.toFloat()
                            }
                        }

                        StepButton(-10)
                        StepButton(-1)
                        StepButton(1)
                        StepButton(10)
                    }
                }
            }

            collapsibleCard {
                width = FitParent
                title = "Settings"

                content.apply {

                    +IntPreferenceSlider("bgbrightness").apply {
                        label = "Background brightness"
                        control.min = 0f
                        control.max = 100f
                        valueFormatter = { "${it.roundToInt()}%" }
                        onValueChanged = {
                            Config.setBackgroundBrightness(it / 100f)
                        }
                    }

                    +PreferenceCheckbox("enableStoryboard").apply {
                        label = "Enable storyboard"
                    }

                    +PreferenceCheckbox("enableVideo").apply {
                        label = "Enable background video"
                    }

                    +PreferenceCheckbox("showscoreboard").apply {
                        label = "Show scoreboard"
                    }
                }
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            alpha = 1f
            lastTimeTouched = System.currentTimeMillis()
            return super.onAreaTouched(event, localX, localY)
        }

        override fun onManagedUpdate(deltaTimeSec: Float) {

            val elapsed = System.currentTimeMillis() - lastTimeTouched

            if (alpha > 0.5f && elapsed > FADE_TIMEOUT) {
                alpha -= deltaTimeSec * 1.5f
            }

            super.onManagedUpdate(deltaTimeSec)
        }

    }


    companion object {
        const val FADE_TIMEOUT = 2000L
        const val MINIMUM_TIMEOUT = 2000L
    }

}