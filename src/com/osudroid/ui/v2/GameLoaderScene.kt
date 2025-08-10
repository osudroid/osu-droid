package com.osudroid.ui.v2

import com.edlplan.framework.easing.*
import com.osudroid.data.*
import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.rian.osu.utils.*
import kotlin.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.helper.StringTable

class GameLoaderScene(private val gameScene: GameScene, private val beatmapInfo: BeatmapInfo, mods: ModHashMap, private val isRestart: Boolean) : UIScene() {

    private var lastTimeTouched = System.currentTimeMillis()
    private var isStarting = false

    private val dimBox: UIBox
    private val mainContainer: UIContainer

    private var beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)

    init {


        // Background
        sprite {
            width = FillParent
            height = FillParent
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture(if (Config.isSafeBeatmapBg()) "menu-background" else "::background")
        }

        // Dim
        dimBox = box {
            width = FillParent
            height = FillParent
            color = Color4.Black
            alpha = 0.7f
        }

        // Beatmap info
        mainContainer = container {
            width = FillParent
            height = FillParent
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleCenter = Anchor.Center

            fadeIn(0.2f, Easing.OutCubic)
            scaleTo(1f, 0.2f, Easing.OutCubic)

            if (beatmapInfo.epilepsyWarning) {
                ResourceManager.getInstance().loadHighQualityAsset("warning", "warning.png")

                linearContainer {
                    x = 60f
                    y = 60f
                    color = Color4(0xFFFFA726)
                    spacing = 6f

                    sprite {
                        width = 24f
                        height = 24f
                        y = 2f
                        textureRegion = ResourceManager.getInstance().getTexture("warning")
                    }

                    text {
                        font = ResourceManager.getInstance().getFont("smallFont")
                        text = StringTable.get(com.osudroid.resources.R.string.epilepsy_warning)
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 10f
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                x = 60f

                // Title
                text {
                    font = ResourceManager.getInstance().getFont("bigFont")
                    text = beatmapInfo.titleText

                    if (!isRestart) {
                        width = 700f
                        clipToBounds = true
                    }

                    autoScrollSpeed = 30f
                    applyTheme = { color = it.accentColor }
                }

                // Difficulty
                text {
                    font = ResourceManager.getInstance().getFont("middleFont")
                    text = beatmapInfo.version

                    if (!isRestart) {
                        width = 700f
                        clipToBounds = true
                    }

                    applyTheme = { color = it.accentColor }
                }

                // Creator
                text {
                    font = ResourceManager.getInstance().getFont("middleFont")
                    text = "by ${beatmapInfo.artistText}"
                    applyTheme = { color = it.accentColor * 0.9f }
                }

                // Mods
                if (mods.isNotEmpty()) {
                    +ModsIndicator().also { it.mods = mods.serializeMods() }
                }
            }

            +CircularProgressBar().apply {
                width = 32f
                height = 32f
                anchor = Anchor.BottomLeft
                origin = Anchor.BottomLeft
                x = 60f
                y = -60f
            }

            if (!isRestart) {
                beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)
                    ?: BeatmapOptions(beatmapInfo.setDirectory)

                +QuickSettingsLayout()
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isStarting) {

            if (gameScene.isReadyToStart) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (System.currentTimeMillis() - lastTimeTouched > MINIMUM_TIMEOUT || Multiplayer.isMultiplayer || isRestart) {
                    isStarting = true

                    if (beatmapOptions != null) {
                        DatabaseManager.beatmapOptionsTable.update(beatmapOptions!!)
                    }

                    // This is used instead of getBackgroundBrightness to directly obtain the
                    // updated value from the brightness slider.
                    val backgroundBrightness = Config.getInt("bgbrightness", 25)

                    mainContainer.fadeOut(0.1f, Easing.OutExpo)

                    dimBox.clearModifiers(ModifierType.Alpha)
                    dimBox.fadeTo(1f - backgroundBrightness / 100f, 0.2f).after {

                        gameScene.hud.apply {
                            alpha = 0f
                            scaleX = 0.9f
                            scaleY = 0.9f
                            scaleCenter = Anchor.Center

                            scaleTo(1f, 0.2f, Easing.OutCubic)
                            fadeIn(0.1f, Easing.OutExpo)
                        }

                        gameScene.start()
                    }
                }

            } else {
                lastTimeTouched = System.currentTimeMillis()
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    private inner class QuickSettingsLayout : UIScrollableContainer() {

        init {
            anchor = Anchor.CenterRight
            origin = Anchor.CenterRight
            width = 460f
            height = FillParent
            x = -20f
            scrollAxes = Axes.Y
            alpha = 0.5f

            linearContainer {
                width = FillParent
                spacing = 20f
                padding = Vec4(0f, 20f)
                orientation = Orientation.Vertical

                collapsibleCard {
                    width = FillParent
                    title = "Beatmap"

                    content.apply {
                        val offsetSlider = FormSlider().apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_category_offset)
                            control.min = -250f
                            control.max = 250f
                            value = beatmapOptions?.offset?.toFloat() ?: 0f
                            defaultValue = beatmapOptions?.offset?.toFloat() ?: 0f
                            valueFormatter = { "${it.roundToInt()}ms" }

                            onValueChanged = {
                                if (beatmapOptions == null) {
                                    beatmapOptions = BeatmapOptions(beatmapInfo.setDirectory)
                                    DatabaseManager.beatmapOptionsTable.insert(beatmapOptions!!)
                                }

                                beatmapOptions!!.offset = it.roundToInt()
                            }
                        }
                        +offsetSlider

                        linearContainer {
                            spacing = 10f
                            padding = Vec4(0f, 16f)
                            anchor = Anchor.TopCenter
                            origin = Anchor.TopCenter

                            fun StepButton(step: Int) = textButton {
                                text = abs(step).toString()
                                height = 42f
                                spacing = 2f
                                padding = Vec4(12f, 0f, 24f, 0f)

                                leadingIcon = UISprite(ResourceManager.getInstance().getTexture(if (step < 0) "minus" else "plus"))
                                leadingIcon!!.height = 20f

                                onActionUp = {
                                    offsetSlider.value += step
                                }
                            }

                            StepButton(-5)
                            StepButton(-1)
                            StepButton(1)
                            StepButton(5)
                        }
                    }
                }

                collapsibleCard {
                    width = FillParent
                    title = "Settings"

                    content.apply {

                        +IntPreferenceSlider("bgbrightness", 25).apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_bgbrightness_title)
                            control.min = 0f
                            control.max = 100f
                            control.onStopDragging = {
                                if (!isStarting) {
                                    dimBox.fadeTo(0.7f, 0.1f)
                                }
                            }
                            valueFormatter = { "${it.roundToInt()}%" }
                            onValueChanged = {
                                Config.setBackgroundBrightness(it / 100f)

                                if (!isStarting) {
                                    dimBox.alpha = 1f - it / 100f
                                }
                            }
                        }

                        +PreferenceCheckbox("enableStoryboard").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_enableStoryboard_title)
                        }

                        +PreferenceCheckbox("enableVideo").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_video_title)
                        }

                        +PreferenceCheckbox("showscoreboard").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_show_scoreboard_title)
                        }
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
        private const val FADE_TIMEOUT = 2000L
        private const val MINIMUM_TIMEOUT = 2000L
    }

}