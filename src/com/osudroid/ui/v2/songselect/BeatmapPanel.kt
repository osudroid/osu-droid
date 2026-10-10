package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.modmenu.ModMenu
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A single difficulty entry shown below an expanded [BeatmapSetPanel].
 *
 * Panels are pooled by the [BeatmapCarrousel] and rebound with [bind] whenever a set expands.
 */
@Suppress("LeakingThis")
open class BeatmapPanel : UIContainer() {

    /**
     * The set panel this panel is currently listed under.
     */
    lateinit var beatmapSetPanel: BeatmapSetPanel
        private set

    /**
     * The beatmap this panel currently shows.
     */
    lateinit var beatmapInfo: BeatmapInfo
        private set

    private var isExpanded = false

    /**
     * Whether the panel is inside the carousel's visible area, off-screen panels are not updated.
     */
    var isOnScreen = true

    /**
     * The width before the carousel arc inset is applied, animated between collapsed and expanded.
     */
    private var baseWidth = -1f

    private var arcInset = 0f

    private val selectionIndicator: UIContainer
    private lateinit var versionText: UIText
    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var difficultyStars: DifficultyStars

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
            padding = Vec4(SELECTION_INDICATOR_WIDTH + 4f.srem, 2f.srem, 4f.srem, 2f.srem)
            spacing = 0.5f.srem

            versionText = text {
                font = ResourceManager.getInstance().getFont("torusBold")
                applyTheme = { color = it.accentColor }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 2f.srem

                starRatingBadge = StarRatingBadge().apply {
                    sizeVariant = SizeVariant.Small
                }
                +starRatingBadge

                // Skinnable star row like the legacy song menu.
                difficultyStars = DifficultyStars().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                }
                +difficultyStars
            }
        }
    }

    /**
     * Shows [beatmapInfo] under [beatmapSetPanel] and resets the state left over from the previous use.
     */
    fun bind(beatmapSetPanel: BeatmapSetPanel, beatmapInfo: BeatmapInfo) {
        this.beatmapSetPanel = beatmapSetPanel
        this.beatmapInfo = beatmapInfo

        versionText.text = beatmapInfo.version
        starRatingBadge.rating = beatmapInfo.getStarRating().toDouble()
        starRatingBadge.applyColorsInstantly()
        difficultyStars.rating = beatmapInfo.getStarRating()

        isExpanded = false
        isOnScreen = true
        baseWidth = -1f
        arcInset = 0f
        selectionIndicator.alpha = 0f
        selectionIndicator.isVisible = false
        borderBox.color = Theme.current.accentColor.copy(alpha = 0.3f)
        borderBox.lineWidth = 0.25f.srem
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isOnScreen) {
            return
        }

        val accentColor = Theme.current.accentColor
        val targetBorderColor = if (isExpanded) accentColor else accentColor.copy(alpha = 0.3f)
        val targetBorderWidth = if (isExpanded) 0.5f.srem else 0.25f.srem
        val targetWidth = if (isExpanded) parent.innerWidth else parent.innerWidth - 2f.rem

        selectionIndicator.alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, selectionIndicator.alpha, if (isExpanded) 1f else 0f, 0.05f)
        // Transparent entities are still drawn.
        selectionIndicator.isVisible = selectionIndicator.alpha > 0f

        borderBox.color = Interpolation.colorLerpWithSnap(deltaTimeSec, 0.1f, borderBox.color, targetBorderColor)
        borderBox.lineWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, borderBox.lineWidth, targetBorderWidth, 0.5f)
        if (baseWidth < 0f) {
            baseWidth = targetWidth
        }
        baseWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, baseWidth, targetWidth, 0.5f)
        width = baseWidth - arcInset

        super.onManagedUpdate(deltaTimeSec)
    }

    /**
     * Sets the inset of the carousel arc, see [BeatmapCarrousel]. Applied to the right-anchored width.
     */
    fun setArcInset(inset: Float) {
        if (arcInset != inset) {
            arcInset = inset

            if (baseWidth >= 0f) {
                width = baseWidth - inset
            }
        }
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
