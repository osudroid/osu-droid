package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.modmenu.ModMenu
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A single difficulty entry shown below an expanded [BeatmapSetPanel].
 */
@Suppress("LeakingThis")
open class BeatmapPanel(private val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : UIContainer() {

    private var isExpanded = false

    private val selectionIndicator: UIContainer

    private val backgroundBox = UIBox().apply {
        cornerRadius = Radius.XL
    }

    private val borderBox = UIBox().apply {
        cornerRadius = Radius.XL
        paintStyle = PaintStyle.Outline
        lineWidth = 0.25f.srem
    }


    override var applyTheme: ThemeApplier = { theme ->
        backgroundBox.color = (theme.accentColor * 0.1f).copy(alpha = 0.6f)
        borderBox.color = theme.accentColor.copy(alpha = 0.3f)
    }


    init {
        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        cullingMode = CullingMode.CameraBounds
        background = backgroundBox
        foreground = borderBox

        // Strip on the left edge marking the currently selected difficulty.
        selectionIndicator = container {
            width = SELECTION_INDICATOR_WIDTH
            height = FillParent
            alpha = 0f
            background = UIBox().apply {
                cornerRadius = Radius.XL
                applyTheme = { color = it.accentColor }
            }

            +FontAwesomeIcon(Icon.CaretRight).apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                iconSize = 10f
                color = Colors.Black
            }
        }

        linearContainer {
            orientation = Orientation.Vertical
            padding = Vec4(SELECTION_INDICATOR_WIDTH + 4f.srem, 3f.srem, 4f.srem, 3f.srem)
            spacing = 1f.srem

            text {
                text = beatmapInfo.version
                font = ResourceManager.getInstance().getFont("torusBold")
                applyTheme = { color = it.accentColor }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 2f.srem

                +StarRatingBadge().apply {
                    rating = beatmapInfo.getStarRating().toDouble()
                    sizeVariant = SizeVariant.Small
                    applyColorsInstantly()
                }

                // Skinnable star row like the legacy song menu.
                +DifficultyStars().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    rating = beatmapInfo.getStarRating()
                }
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        val accentColor = Theme.current.accentColor
        val targetBorderColor = if (isExpanded) accentColor else accentColor.copy(alpha = 0.3f)
        val targetBorderWidth = if (isExpanded) 0.5f.srem else 0.25f.srem
        val targetWidth = if (isExpanded) parent.innerWidth + 3f.rem else parent.innerWidth

        selectionIndicator.alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, selectionIndicator.alpha, if (isExpanded) 1f else 0f, 0.05f)

        borderBox.color = Interpolation.colorLerp(deltaTimeSec, 0.1f, borderBox.color, targetBorderColor)
        borderBox.lineWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, borderBox.lineWidth, targetBorderWidth, 0.5f)
        width = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, width, targetWidth, 0.5f)

        super.onManagedUpdate(deltaTimeSec)
    }


    fun expand() {
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (event.isActionUp) {

            if (SongSelect.selectedBeatmap == beatmapInfo && beatmapSetPanel.selectedPanel == this) {
                ResourceManager.getInstance().getSound("menuhit")?.play()

                GlobalManager.getInstance().gameScene.setOldScene(SongSelect)
                GlobalManager.getInstance().gameScene.startGame(beatmapInfo, null, ModMenu.enabledMods)
            } else {
                ResourceManager.getInstance().getSound("menuclick")?.play()
            }

            beatmapSetPanel.selectedPanel = this
        }

        return true
    }


    companion object {
        private val SELECTION_INDICATOR_WIDTH
            get() = .75f.rem
    }

}
