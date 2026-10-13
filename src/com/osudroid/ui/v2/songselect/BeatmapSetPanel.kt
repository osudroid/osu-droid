package com.osudroid.ui.v2.songselect

import android.util.Log
import com.osudroid.data.BeatmapInfo
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Color4
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.random.Random

/**
 * A beatmap set entry of the [BeatmapCarrousel]. When selected it expands and lists its difficulties as [BeatmapPanel]s.
 */
class BeatmapSetPanel(val beatmapCarrousel: BeatmapCarrousel) : RecyclableComponent<BeatmapSetModel>(), IPanelContainer<BeatmapPanel> {

    override val isRecyclable: Boolean
        get() = !button.isPressed && !isExpanded && !isTransitioning


    /**
     * The local position for the currently selected panel.
     */
    val selectedPanelY: Float
        get() = panelsContainer.absoluteY + (selectedPanel?.let { it.absoluteY + it.height / 2f } ?: 0f)


    /**
     * The currently selected beatmap panel.
     */
    override var selectedPanel: BeatmapPanel? = null
        set(value) {
            if (field != value) {
                field = value

                if (beatmapCarrousel.selectedPanel == this) {
                    SongSelect.onBeatmapSelected(value?.beatmapInfo)
                }

                panelsContainer.forEach { panel ->
                    panel as BeatmapPanel
                    if (panel != value) {
                        panel.collapse()
                    } else {
                        panel.expand()
                    }
                }

                if (value != null) {
                    beatmapCarrousel.autoScrollToSelectedPanel = true
                }
            }
        }

    /**
     * Whether the panel is expanded or not.
     */
    var isExpanded = false
        private set


    /**
     * The container for the beatmap panels.
     */
    lateinit var panelsContainer: UILinearContainer

    /**
     * The button itself.
     */
    lateinit var button: UIClickableContainer


    private var isTransitioning = false

    /**
     * The width of the button before the carousel arc inset is applied, animated between collapsed and expanded.
     */
    private var buttonBaseWidth = -1f

    private var arcInset = 0f

    /**
     * Animated from 0 to 1 while the set gets selected, drives the selection strip, cover inset and brightness.
     */
    private var selectionProgress = 0f

    private var coverBrightness = COVER_BRIGHTNESS

    private lateinit var cover: UIShapedSprite
    private lateinit var selectionIndicator: UIContainer
    private lateinit var titleText: UIText
    private lateinit var artistText: UIText

    private val buttonBorder = UIBox().apply {
        cornerRadius = Radius.XL
        paintStyle = PaintStyle.Outline
        lineWidth = BORDER_WIDTH
    }

    private val coverBackground = UIBox().apply {
        cornerRadius = Radius.XL
    }


    override var applyTheme: ThemeApplier = { theme ->
        buttonBorder.color = theme.accentColor.copy(alpha = 0.3f)
        // Matches the former 60 % black overlay drawn over a 50 % background box.
        coverBackground.color = (theme.accentColor * 0.1f * 0.25f).copy(alpha = 0.8f)
    }


    init {
        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight

        linearContainer {
            orientation = Orientation.Vertical
            width = FillParent
            spacing = 1f.srem

            button = clickableContainer {
                width = FillParent
                minHeight = 5.5f.rem
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                foreground = buttonBorder

                onActionUp = {
                    // This will prevent race condition, during transition panelsContainer might
                    // get cleared and since the select() method is based on beatmap indices
                    // it might fail to find the corresponding BeatmapPanel causing a IOOBE.
                    if (!isTransitioning) {
                        select()
                    }
                }

                // Accent fill, uncovered as a strip on the left edge when selected.
                selectionIndicator = container {
                    width = FillParent
                    height = FillParent
                    alpha = 0f
                    background = UIBox().apply {
                        cornerRadius = Radius.XL
                        applyTheme = { color = it.accentColor }
                    }

                    container {
                        width = SELECTION_INDICATOR_WIDTH
                        height = FillParent

                        +FontAwesomeIcon(Icon.ChevronRight).apply {
                            anchor = Anchor.Center
                            origin = Anchor.Center
                            iconSize = 12f
                            color = Colors.Black
                        }
                    }
                }

                // Shaped sprite so the cover is masked by the rounded background box.
                cover = UIShapedSprite().apply {
                    height = FillParent
                    // Crop draws the texture quad larger than the bounds, so clip the overflow.
                    clipToBounds = true
                    shape = coverBackground
                    // Darkened by tinting instead of a black overlay box, which would cost an extra draw call.
                    color = Color4(COVER_BRIGHTNESS, COVER_BRIGHTNESS, COVER_BRIGHTNESS)
                }
                +cover

                linearContainer {
                    orientation = Orientation.Vertical
                    width = FillParent
                    padding = Vec4(SELECTION_INDICATOR_WIDTH + 3f.srem, 3f.srem, 3f.srem, 3f.srem)

                    titleText = text {
                        text = "Unknown"
                        font = ResourceManager.getInstance().getFont("torusBoldLarge")
                        applyTheme = { color = it.accentColor }
                    }

                    artistText = text {
                        text = "Unknown"
                        font = ResourceManager.getInstance().getFont("torusBold")
                        applyTheme = { color = it.accentColor * 0.8f }
                    }
                }
            }

            panelsContainer = linearContainer {
                orientation = Orientation.Vertical
                width = FillParent
                height = 0f
                alpha = 0f
                spacing = 1f.srem
            }
        }
    }


    override fun onBind(data: BeatmapSetModel) {
        val firstBeatmap = data.beatmapSetInfo.beatmaps.getOrNull(0)

        titleText.text = firstBeatmap?.titleText ?: "Unknown"
        artistText.text = firstBeatmap?.artistText ?: "Unknown"

        cover.textureRegion = data.coverTexture
    }

    override fun onRecycle() {
        // The component may be released while expanded (e.g. its set got filtered out),
        // so reset the whole expansion state instead of waiting for the update loop.
        if (beatmapCarrousel.selectedPanel == this) {
            beatmapCarrousel.selectedPanel = null
        }

        isExpanded = false
        selectedPanel = null
        isTransitioning = false

        releasePanels()
        panelsContainer.height = 0f
        panelsContainer.alpha = 0f

        cover.textureRegion = null
    }


    fun select(beatmapIndex: Int = 0) {
        beatmapCarrousel.selectedPanel = this

        if (panelsContainer.childCount == 0) {
            return
        }

        val index = beatmapIndex.coerceIn(0, panelsContainer.childCount - 1)
        selectedPanel = panelsContainer.getChildByIndex(index) as BeatmapPanel
    }

    /**
     * Selects the panel of the given beatmap, falling back to the first panel if it is not part of this set.
     */
    fun select(beatmapInfo: BeatmapInfo) {
        beatmapCarrousel.selectedPanel = this

        for (i in 0 until panelsContainer.childCount) {
            val panel = panelsContainer.getChildByIndex(i) as BeatmapPanel

            if (panel.beatmapInfo.path == beatmapInfo.path) {
                selectedPanel = panel
                return
            }
        }

        select(0)
    }

    fun selectRandom() {
        beatmapCarrousel.selectedPanel = this

        if (panelsContainer.childCount == 0) {
            return
        }

        selectedPanel = panelsContainer.getChildByIndex(Random.nextInt(panelsContainer.childCount)) as BeatmapPanel
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        // Covers are loaded and unloaded asynchronously by the carousel.
        val coverTexture = boundData?.coverTexture
        if (cover.textureRegion !== coverTexture) {
            cover.textureRegion = coverTexture
        }

        val accentColor = Theme.current.accentColor
        val buttonContainerTargetWidth = if (isExpanded) width + 3f.rem else width
        val buttonContainerTargetBorderColor = if (isExpanded) accentColor else accentColor.copy(alpha = 0.3f)
        val buttonContainerTargetBorderWidth = if (isExpanded) SELECTED_BORDER_WIDTH else BORDER_WIDTH

        if (buttonBaseWidth < 0f) {
            buttonBaseWidth = buttonContainerTargetWidth
        }
        buttonBaseWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, buttonBaseWidth, buttonContainerTargetWidth, 0.5f)
        button.width = buttonBaseWidth - arcInset
        buttonBorder.color = Interpolation.colorLerpWithSnap(deltaTimeSec, 0.1f, buttonBorder.color, buttonContainerTargetBorderColor)
        buttonBorder.lineWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, buttonBorder.lineWidth, buttonContainerTargetBorderWidth, 0.5f)

        selectionProgress = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, selectionProgress, if (isExpanded) 1f else 0f, 0.05f)
        selectionIndicator.alpha = selectionProgress
        // Transparent entities are still drawn.
        selectionIndicator.isVisible = selectionProgress > 0f

        val coverInset = SELECTION_INDICATOR_WIDTH * selectionProgress
        cover.x = coverInset
        cover.width = button.width - coverInset

        // Brighter when selected.
        val brightness = COVER_BRIGHTNESS + (SELECTED_COVER_BRIGHTNESS - COVER_BRIGHTNESS) * selectionProgress
        if (coverBrightness != brightness) {
            coverBrightness = brightness
            cover.color = Color4(brightness, brightness, brightness)
        }

        val panelContainerTargetHeight = if (isExpanded) panelsContainer.contentHeight else 0f
        val panelContainerTargetAlpha = if (isExpanded) 1f else 0f

        panelsContainer.apply {
            height = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, height, panelContainerTargetHeight, 0.5f)
            alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, alpha, panelContainerTargetAlpha, 0.1f)

            // The panels slide out from under the set button as the height animates, instead of being uncovered in place.
            val slideOffset = height - contentHeight
            if (padding.top != slideOffset) {
                padding = Vec4(0f, slideOffset, 0f, 0f)
            }
        }

        isTransitioning = buttonBaseWidth != buttonContainerTargetWidth
            || buttonBorder.color != buttonContainerTargetBorderColor
            || buttonBorder.lineWidth != buttonContainerTargetBorderWidth
            || panelsContainer.height != panelContainerTargetHeight
            || panelsContainer.alpha != panelContainerTargetAlpha

        // While the height animates, the panels would otherwise overflow onto the button and the neighbouring sets.
        panelsContainer.clipToBounds = isTransitioning

        if (!isExpanded && !isTransitioning && panelsContainer.childCount > 0) {
            releasePanels()
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    /**
     * Sets the inset of the carousel arc, see [BeatmapCarrousel]. Applied to the button's right-anchored width.
     */
    fun setArcInset(inset: Float) {
        if (arcInset != inset) {
            arcInset = inset

            if (buttonBaseWidth >= 0f) {
                button.width = buttonBaseWidth - inset
            }
        }
    }

    fun expand() {
        val data = boundData

        if (data == null) {
            Log.w("BeatmapSetPanel", "Cannot expand panel without bound data!")
            return
        }

        if (!isExpanded) {
            isExpanded = true

            // Releases panels left over from a collapse that is still animating.
            releasePanels()

            data.beatmapSetInfo.beatmaps.sortedBy { it.getStarRating() }.forEach {
                panelsContainer.attachChild(beatmapCarrousel.obtainBeatmapPanel(this, it))
            }
        }
    }

    /**
     * Detaches all difficulty panels and returns them to the carousel's pool.
     */
    private fun releasePanels() {
        panelsContainer.forEach { beatmapCarrousel.releaseBeatmapPanel(it as BeatmapPanel) }
        panelsContainer.detachChildren()
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            selectedPanel = null

            // Only the selected set's panels are tracked by the carousel.
            panelsContainer.forEach { (it as BeatmapPanel).isOnScreen = true }
        }
    }


    companion object {

        private const val COVER_BRIGHTNESS = 0.4f
        private const val SELECTED_COVER_BRIGHTNESS = 0.5f

        val BORDER_WIDTH
            get() = 0.5f.srem

        val SELECTED_BORDER_WIDTH
            get() = 0.75f.srem

        private val SELECTION_INDICATOR_WIDTH
            get() = 1.25f.rem
    }

}
