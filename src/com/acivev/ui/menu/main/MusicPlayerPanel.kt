package com.acivev.ui.menu.main

import com.osudroid.utils.updateThread
import com.rian.andengine.modifier.ModifierType
import com.reco1l.andengine.*
import com.reco1l.andengine.Axes
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.shape.UITriangle
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.ui.UIIconButton
import com.reco1l.andengine.ui.UISlider
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Collapsible now-playing panel, top-right.
 *
 * Collapsed → header only: song title + thin read-only progress strip.
 * Expanded  → header + full interactive slider + time labels + prev/play/next.
 *
 */
class MusicPlayerPanel : UILinearContainer() {

    var isDraggingSeek = false
        private set
    private var isExpanded = false

    /**
     * Cached texture refs, loaded once at construction, reused every frame.
     */
    private val pauseTex = ResourceManager.getInstance().getTexture("music_pause")
    private val playTex2 = ResourceManager.getInstance().getTexture("music_play")

    /**
     * Last-seen values, updated only when something changes, preventing
     * redundant string allocations and ResourceManager lookups every frame.
     */
    private var lastTitle = ""
    private var lastStatus: Status? = null

    // Cached last-rendered seconds to avoid per-frame string allocations when the
    // displayed time hasn't actually changed (formatTime result is the same for every
    // ms within the same second).
    private var lastPosSec = -1
    private var lastLenSec = -1
    private var lastSeekSec = -1

    private val autoCloseDelayMs = 5_000L
    private var lastInteractionTime = 0L

    /** Called after PREV or NEXT is triggered so the parent scene can reset its music state. */
    var onTrackChanged: (() -> Unit)? = null

    /** Called after the user seeks to a new position so the parent scene can resync timing. */
    var onSeek: (() -> Unit)? = null

    // Mini progress fill
    private val miniProgressFill = UIBox().apply {
        relativeSizeAxes = Axes.X
        width = 0f
        height = 4f
        cornerRadius = 2f
        color = Color4.White
        inheritAncestorsColor = false
    }

    private val seekSlider = object : UISlider(0f) {
        override fun onValueChanged() {
            super.onValueChanged()

            if (isDraggingSeek) {
                val svc = GlobalManager.getInstance().songService ?: return
                svc.seekTo((value * svc.getLength()).toInt())
            }
        }
    }.apply {
        width = FillParent
        min = 0f
        max = 1f
        onStartDragging = { isDraggingSeek = true }
    }

    private val posText = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        color = Color4.White
        inheritAncestorsColor = false
        text = "0:00"
    }

    private val totalText = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        color = Color4.White
        inheritAncestorsColor = false
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        text = "0:00"
    }

    private val playPauseBtn = UIIconButton().apply {
        icon = ResourceManager.getInstance().getTexture("music_play")
        onActionUp = {
            lastInteractionTime = System.currentTimeMillis()
            val svc = GlobalManager.getInstance().songService
            if (svc != null) {
                if (svc.status == Status.PLAYING)
                    GlobalManager.getInstance().mainMenuV2?.musicControl(MusicOption.PAUSE)
                else
                    GlobalManager.getInstance().mainMenuV2?.musicControl(MusicOption.PLAY)
            }
        }
    }

    private val songTitleText = UIText().apply {
        font = ResourceManager.getInstance().getFont("font")
        color = Color4.White
        inheritAncestorsColor = false
        width = FillParent
        clipToBounds = true
        autoScrollSpeed = 25f
        text = ""
    }

    private val arrowTriangle = UITriangle().apply {
        width = 14f
        height = 10f
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        rotationCenter = Anchor.Center
        color = Color4.White
        inheritAncestorsColor = false
        rotation = 180f  // starts collapsed → pointing up
    }

    private val expandedContent: UILinearContainer

    init {
        seekSlider.onStopDragging = {
            val seekTarget = seekSlider.value

            isDraggingSeek = false
            lastInteractionTime = System.currentTimeMillis()

            val svc = GlobalManager.getInstance().songService
            if (svc != null) {
                svc.seekTo((seekTarget * svc.getLength()).toInt())
                onSeek?.invoke()
            }
        }

        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        orientation = Orientation.Vertical
        width = 550f
        height = MatchContent
        x = -10f
        y = 10f

        background = UIBox().apply {
            color = Color4(0.1f, 0.12f, 0.16f, 0.88f)
            cornerRadius = 20f
        }

        // Header
        val header = object : UIContainer() {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (event.isActionUp) { lastInteractionTime = System.currentTimeMillis(); toggle() }
                return true
            }
            // Keep title from overlapping the arrow as the panel width animates
            override fun onSizeChanged() {
                super.onSizeChanged()
                songTitleText.maxWidth = (innerWidth - arrowTriangle.width - 16f).coerceAtLeast(0f)
            }
        }.apply {
            width = FillParent
            height = MatchContent
            padding = Vec4(16f, 12f, 16f, 8f)

            +songTitleText
            +arrowTriangle

            // White fill bar anchored to the bottom
            +UIContainer().apply {
                anchor = Anchor.BottomLeft
                origin = Anchor.BottomLeft
                relativeSizeAxes = Axes.X
                width = 1f
                height = 4f
                padding  = Vec4(0f, 4f, 12f, 0f)
                +miniProgressFill
            }
        }
        +header

        // Expanded section
        expandedContent = UILinearContainer().apply {
            orientation = Orientation.Vertical
            width = FillParent
            height = 0f    // animated open/close
            spacing = 10f
            padding = Vec4(16f, 0f, 16f, 12f)
            clipToBounds = true
            isVisible = false

            +seekSlider

            // Time labels row
            container {
                width = FillParent
                height = MatchContent
                +posText
                +totalText
            }

            // Controls row
            linearContainer {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                spacing = 12f

                iconButton {
                    icon = ResourceManager.getInstance().getTexture("music_prev")
                    onActionUp = {
                        lastInteractionTime = System.currentTimeMillis()
                        GlobalManager.getInstance().mainMenuV2?.musicControl(MusicOption.PREV)
                        onTrackChanged?.invoke()
                    }
                }

                +playPauseBtn

                iconButton {
                    icon = ResourceManager.getInstance().getTexture("music_next")
                    onActionUp = {
                        lastInteractionTime = System.currentTimeMillis()
                        GlobalManager.getInstance().mainMenuV2?.musicControl(MusicOption.NEXT)
                        onTrackChanged?.invoke()
                    }
                }
            }
        }

        +expandedContent
    }

    fun collapse() {
        if (isExpanded) toggle()
    }

    private fun toggle() {
        if (isExpanded) {
            isExpanded = false
            arrowTriangle.clearModifiers(ModifierType.Rotation)
            arrowTriangle.rotateTo(180f, 0.15f)

            clearModifiers(ModifierType.Width)
            widthTo(550f, 0.15f)

            expandedContent.clearModifiers(ModifierType.Height)
            expandedContent.heightTo(0f, 0.15f).after {
                updateThread {
                    expandedContent.isVisible = false
                    onContentChanged()
                }
            }
        } else {
            isExpanded = true
            arrowTriangle.clearModifiers(ModifierType.Rotation)
            arrowTriangle.rotateTo(0f, 0.15f)

            clearModifiers(ModifierType.Width)
            widthTo(450f, 0.15f)

            expandedContent.isVisible = true
            expandedContent.clearModifiers(ModifierType.Height)
            expandedContent.heightTo(expandedContent.contentHeight, 0.15f)
        }
    }

    fun update() {
        val songService = GlobalManager.getInstance().songService ?: return
        val info = GlobalManager.getInstance().mainMenuV2?.beatmapInfo

        val title = if (info != null) "${info.artistText} - ${info.titleText}" else ""
        if (title != lastTitle) {
            lastTitle = title
            songTitleText.text = title
        }

        val pos = songService.position
        val len = songService.getLength()
        val progress = if (len > 0) (pos.toFloat() / len.toFloat()).coerceIn(0f, 1f) else 0f

        miniProgressFill.width = progress

        if (isExpanded) {
            if (lastInteractionTime > 0 &&
                System.currentTimeMillis() - lastInteractionTime >= autoCloseDelayMs &&
                !isDraggingSeek) {
                collapse()
            }

            val lenSec = len / 1000
            if (lenSec != lastLenSec) {
                lastLenSec = lenSec
                totalText.text = formatTime(len)
            }

            if (!isDraggingSeek) {
                seekSlider.value = progress
                val posSec = pos / 1000
                if (posSec != lastPosSec) {
                    lastPosSec = posSec
                    posText.text = formatTime(pos)
                }
            } else {
                val seekSec = (seekSlider.value * len).toInt() / 1000
                if (seekSec != lastSeekSec) {
                    lastSeekSec = seekSec
                    posText.text = formatTime((seekSlider.value * len).toInt())
                }
            }

            val status = songService.status
            if (status != lastStatus) {
                lastStatus = status
                playPauseBtn.icon = if (status == Status.PLAYING) pauseTex else playTex2
            }
        }
    }

    private fun formatTime(ms: Int): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
