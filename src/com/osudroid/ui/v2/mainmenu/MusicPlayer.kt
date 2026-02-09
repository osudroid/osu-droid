package com.osudroid.ui.v2.mainmenu

import com.osudroid.MusicManager
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.container
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.iconButton
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.text
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.ColorVariant
import com.reco1l.andengine.ui.SizeVariant
import com.reco1l.andengine.ui.UIButton
import com.reco1l.andengine.ui.UIIconButton
import com.reco1l.andengine.ui.UIModal
import com.reco1l.andengine.ui.UISlider
import com.reco1l.andengine.ui.plus
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.LibraryManager
import java.text.SimpleDateFormat

class MusicPlayer(private val trigger: UIButton) : UIModal() {

    private lateinit var titleText: UIText
    private lateinit var artistText: UIText
    private lateinit var progressSlider: UISlider
    private lateinit var playPauseButton: UIIconButton
    private lateinit var currentTime: UIText
    private lateinit var totalTime: UIText

    private var draggingProgressSlider = false

    private val timeFormat = SimpleDateFormat("mm:ss")
    private val timeFormatWithHours = SimpleDateFormat("HH:mm:ss")


    init {
        // Removes the background dim
        style = {}

        card.apply {
            anchor = Anchor.TopLeft
            origin = Anchor.TopRight
            scaleCenter = Anchor.Center
            style += {
                width = 20f.rem
                padding = Vec4(4f.srem)
            }

            linearContainer {
                width = Size.Full
                orientation = Orientation.Vertical
                style = {
                    spacing = 2f.srem
                }

                linearContainer {
                    width = Size.Full
                    orientation = Orientation.Vertical

                    titleText = text {
                        width = Size.Full
                        style = {
                            color = it.accentColor
                        }
                    }

                    artistText = text {
                        width = Size.Full
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.9f
                        }
                    }
                }

                container {
                    width = Size.Full

                    currentTime = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.8f
                        }
                    }

                    totalTime = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.8f
                        }
                    }
                }

                +UISlider().apply {
                    width = Size.Full
                    step = 0.01f
                    onStartDragging = { draggingProgressSlider = true }
                    onStopDragging = {
                        MusicManager.position = (value * 1000).toInt()
                        draggingProgressSlider = false
                    }
                    progressSlider = this
                }

                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    style = {
                        spacing = 4f.srem
                    }

                    iconButton {
                        icon = FontAwesomeIcon(Icon.BackwardFast)
                        colorVariant = ColorVariant.Tertiary
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scaleCenter = Anchor.Center

                        onActionUp = {
                            MusicManager.currentBeatmap = LibraryManager.selectPreviousBeatmapSet().beatmaps.random()
                            MusicManager.load()
                            MusicManager.play()
                        }
                    }

                    playPauseButton = iconButton {
                        icon = FontAwesomeIcon(Icon.Play)
                        sizeVariant = SizeVariant.Large
                        colorVariant = ColorVariant.Primary
                        style += {
                            radius = Radius.Full
                        }

                        onActionUp = {
                            if (MusicManager.isPlaying) {
                                MusicManager.pause()
                            } else {
                                MusicManager.play()
                            }
                        }
                    }

                    iconButton {
                        icon = FontAwesomeIcon(Icon.ForwardFast)
                        colorVariant = ColorVariant.Tertiary
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scaleCenter = Anchor.Center

                        onActionUp = {
                            MusicManager.currentBeatmap = LibraryManager.selectNextBeatmapSet().beatmaps.random()
                            MusicManager.load()
                            MusicManager.play()
                        }
                    }
                }

            }
        }
    }

    override fun onAttached() {
        super.onAttached()

        val (triggerX, triggerY) = trigger.convertLocalToSceneCoordinates(trigger.width, trigger.height)
        card.x = triggerX
        card.y = triggerY + 3f.srem
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        titleText.text = MusicManager.currentBeatmap?.titleText ?: "Unknown"
        artistText.text = MusicManager.currentBeatmap?.artistText ?: "Unknown"

        val format = if (MusicManager.length >= 3600000) timeFormatWithHours else timeFormat
        currentTime.text = format.format(MusicManager.position)
        totalTime.text = format.format(MusicManager.length)

        (playPauseButton.icon as FontAwesomeIcon).icon = if (MusicManager.isPlaying) Icon.Pause else Icon.Play

        progressSlider.max = MusicManager.length / 1000f
        if (!draggingProgressSlider) {
            progressSlider.value = MusicManager.position / 1000f
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}