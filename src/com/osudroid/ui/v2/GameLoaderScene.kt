package com.osudroid.ui.v2

import com.edlplan.framework.easing.*
import com.osudroid.data.*
import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.theme.vw
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

    private val beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)
        ?: BeatmapOptions(beatmapInfo.setDirectory)

    private var fadeTimeout = if (isRestart) 500L else 2000L
    private var minimumTimeout = if (isRestart) 500L else 2000L

    init {
        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")

        sprite {
            width = Size.Full
            height = Size.Full
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture(if (Config.isSafeBeatmapBg()) "menu-background" else "::background")
        }

        dimBox = box {
            width = Size.Full
            height = Size.Full
            color = Color4.Black
            alpha = 0.7f
        }

        mainContainer = fillContainer {
            orientation = Orientation.Horizontal
            width = Size.Full
            height = Size.Full
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleCenter = Anchor.Center
            style = {
                padding = UIEngine.current.safeArea.copy(
                    y = 4f.srem,
                    w = 4f.srem + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)
                )
                spacing = 4f.srem
            }

            fadeIn(0.2f, Easing.OutCubic)
            scaleTo(1f, 0.2f, Easing.OutCubic)

            container {
                width = Size.Full
                height = Size.Full

                if (beatmapInfo.epilepsyWarning) {
                    compoundText {
                        leadingIcon = FontAwesomeIcon(Icon.TriangleExclamation)
                        text = StringTable.get(com.osudroid.resources.R.string.epilepsy_warning)
                    }
                }

                linearContainer {
                    width = Size.Full
                    orientation = Orientation.Vertical
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = {
                        spacing = 2f.srem
                    }

                    text {
                        width = Size.Full
                        fontSize = FontSize.XL
                        wrapText = true
                        text = beatmapInfo.titleText
                        style = { color = it.accentColor }
                    }

                    text {
                        width = Size.Full
                        fontSize = FontSize.XL
                        wrapText = true
                        text = beatmapInfo.version
                        style = { color = it.accentColor }
                    }

                    text {
                        width = Size.Full
                        fontSize = FontSize.LG
                        wrapText = true
                        text = "by ${beatmapInfo.artistText}"
                        style = { color = it.accentColor * 0.9f }
                    }

                    if (mods.isNotEmpty()) {
                        +ModsIndicator().also { it.mods = mods.values }
                    }
                }

                linearContainer {
                    orientation = Orientation.Vertical
                    anchor = Anchor.BottomLeft
                    origin = Anchor.BottomLeft
                    style = {
                        spacing = 4f.srem
                    }

                    +CircularProgressBar().apply {
                        style = {
                            width = 2.15f.rem
                            height = 2.15f.rem
                        }
                    }

                    if (!Multiplayer.isMultiplayer) {
                        textButton {
                            text = "Back"
                            leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)
                            onActionUp = {
                                ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                                cancel()
                            }
                            onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                        }
                    }
                }
            }

            +QuickSettingsLayout()
        }
    }

    /**
     * Cancels loading and goes back to the song menu.
     */
    fun cancel() {
        if (Multiplayer.isMultiplayer) {
            return
        }

        gameScene.cancelLoading()

        val global = GlobalManager.getInstance()
        val songMenu = global.songMenu
        val selectedBeatmap = songMenu.selectedBeatmap

        global.engine.scene = songMenu.scene

        if (selectedBeatmap != null) {
            songMenu.playMusic(selectedBeatmap.audioPath, selectedBeatmap.previewTime)
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isStarting) {

            if (gameScene.isReadyToStart) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (System.currentTimeMillis() - lastTimeTouched > minimumTimeout || Multiplayer.isMultiplayer) {
                    isStarting = true

                    // This is used instead of getBackgroundBrightness to directly obtain the
                    // updated value from the brightness slider.
                    val backgroundBrightness = Config.getFloat("bgbrightness", 0.25f)

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
            height = Size.Full
            scrollAxes = Axes.Y
            alpha = 0.5f

            style = {
                width = 0.3f.vw
            }

            linearContainer {
                width = Size.Full
                orientation = Orientation.Vertical
                style = {
                    spacing = 2f.srem
                }

                collapsibleCard {
                    width = Size.Full
                    title = "Beatmap"

                    content.apply {

                        val offsetSlider = FormSlider().apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_category_offset)
                            control.min = -250f
                            control.max = 250f
                            value = beatmapOptions.offset.toFloat()
                            defaultValue = beatmapOptions.offset.toFloat()
                            valueFormatter = { "${it.roundToInt()}ms" }

                            onValueChanged = {
                                beatmapOptions.offset = it.roundToInt()
                                DatabaseManager.beatmapOptionsTable.upsert(beatmapOptions)
                            }
                        }
                        +offsetSlider

                        linearContainer {
                            anchor = Anchor.TopCenter
                            origin = Anchor.TopCenter
                            style = {
                                spacing = 2f.srem
                                padding = Vec4(2f.srem)
                            }

                            fun StepButton(step: Int) = textButton {
                                text = abs(step).toString()
                                leadingIcon = FontAwesomeIcon(if (step >= 0) Icon.Plus else Icon.Minus)
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

                    onExpandStatusChange = {
                        beatmapCardCollapsed = !it
                    }

                    if (beatmapCardCollapsed) {
                        collapse(true)
                    }
                }

                collapsibleCard {
                    width = Size.Full
                    title = "Settings"

                    content.apply {

                        +FloatPreferenceSlider("bgbrightness", 0.25f).apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_bgbrightness_title)
                            control.min = 0f
                            control.max = 1f
                            control.onStopDragging = {
                                if (!isStarting) {
                                    dimBox.fadeTo(0.7f, 0.1f)
                                }
                            }
                            valueFormatter = { "${(it * 100f).roundToInt()}%" }
                            onValueChanged = {
                                Config.setBackgroundBrightness(it)

                                // Storyboard and video should not be enabled if the background brightness is too low,
                                // so we trigger a reload when changing brightness.
                                gameScene.loadStoryboard(beatmapInfo)
                                gameScene.loadVideo(beatmapInfo)

                                if (!isStarting) {
                                    dimBox.alpha = 1f - it
                                }
                            }
                        }

                        +PreferenceCheckbox("enableStoryboard").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_enableStoryboard_title)
                            onValueChanged = {
                                gameScene.loadStoryboard(beatmapInfo)
                            }
                        }

                        +PreferenceCheckbox("enableVideo").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_video_title)
                            onValueChanged = {
                                gameScene.loadVideo(beatmapInfo)
                            }
                        }

                        +PreferenceCheckbox("showscoreboard").apply {
                            label = StringTable.get(com.osudroid.resources.R.string.opt_show_scoreboard_title)
                        }
                    }

                    onExpandStatusChange = {
                        settingsCardCollapsed = !it
                    }

                    if (settingsCardCollapsed) {
                        collapse(true)
                    }
                }
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            alpha = 1f
            lastTimeTouched = System.currentTimeMillis()

            // When the player is restarting, and they touch the layout, assume they want to change settings.
            // In that case, show this loading scene longer.
            if (isRestart) {
                fadeTimeout = 1500L
                minimumTimeout = 1500L
            }

            return super.onAreaTouched(event, localX, localY)
        }

        override fun onManagedUpdate(deltaTimeSec: Float) {

            val elapsed = System.currentTimeMillis() - lastTimeTouched

            if (alpha > 0.5f && elapsed > fadeTimeout) {
                alpha -= deltaTimeSec * 1.5f
            }

            super.onManagedUpdate(deltaTimeSec)
        }

    }

    companion object {
        private var beatmapCardCollapsed = false
        private var settingsCardCollapsed = false
    }
}