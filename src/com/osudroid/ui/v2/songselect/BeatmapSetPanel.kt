package com.osudroid.ui.v2.songselect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.io.File
import kotlin.math.min
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

    private lateinit var cover: UIShapedSprite
    private lateinit var selectionIndicator: UIContainer
    private lateinit var titleText: UIText
    private lateinit var artistText: UIText

    private val buttonBorder = UIBox().apply {
        cornerRadius = Radius.XL
        paintStyle = PaintStyle.Outline
        lineWidth = 0.25f.srem
    }

    private val coverBackground = UIBox().apply {
        cornerRadius = Radius.XL
    }


    override var applyTheme: ThemeApplier = { theme ->
        buttonBorder.color = theme.accentColor.copy(alpha = 0.3f)
        coverBackground.color = (theme.accentColor * 0.1f).copy(alpha = 0.5f)
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

                // Shaped sprite so the cover is masked by the rounded background box.
                cover = UIShapedSprite().apply {
                    width = FillParent
                    height = FillParent
                    // Crop draws the texture quad larger than the bounds, so clip the overflow.
                    clipToBounds = true
                    shape = coverBackground
                }
                +cover

                box {
                    width = FillParent
                    height = FillParent
                    color = Colors.Black.copy(alpha = 0.6f)
                    cornerRadius = Radius.XL
                }

                // Strip on the left edge marking the currently selected set.
                selectionIndicator = container {
                    width = SELECTION_INDICATOR_WIDTH
                    height = FillParent
                    alpha = 0f
                    background = UIBox().apply {
                        cornerRadius = Radius.XL
                        applyTheme = { color = it.accentColor }
                    }

                    +FontAwesomeIcon(Icon.ChevronRight).apply {
                        anchor = Anchor.Center
                        origin = Anchor.Center
                        iconSize = 12f
                        color = Colors.Black
                    }
                }

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

        val coverTexture = data.coverTexture
        if (coverTexture != null) {
            cover.textureRegion = coverTexture
        } else {
            cover.textureRegion = null

            if (!data.isLoadingCover && firstBeatmap != null) {
                loadCover(data)
            }
        }
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

        panelsContainer.detachChildren()
        panelsContainer.height = 0f
        panelsContainer.alpha = 0f

        cover.textureRegion = null
    }


    private fun loadCover(model: BeatmapSetModel) = coverLoadScope.launch {
        model.isLoadingCover = true

        try {
            val beatmap = model.beatmapSetInfo.beatmaps[0]
            val textureName = "cover@${beatmap.md5}"

            if (ResourceManager.getInstance().isTextureLoaded(textureName)) {
                model.coverTexture = ResourceManager.getInstance().getTexture(textureName)
            } else {
                if (!coverCacheDirectory.exists()) {
                    coverCacheDirectory.mkdirs()
                }

                val cacheFile = File(coverCacheDirectory, "${beatmap.md5}.webp")

                if (!cacheFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(beatmap.backgroundPath)?.cropInCenter(COVER_HEIGHT)

                    if (bitmap != null) {
                        cacheFile.outputStream().use { out ->
                            if (!bitmap.compress(Bitmap.CompressFormat.WEBP, 10, out)) {
                                cacheFile.delete()
                            }
                        }
                    }
                }

                if (cacheFile.exists()) {
                    val texture = ResourceManager.getInstance().loadHighQualityFile(textureName, cacheFile)

                    if (texture != null) {
                        model.coverTexture = texture
                    } else {
                        cacheFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BeatmapSetPanel", "Failed to load cover for ${model.beatmapSetInfo.directory}", e)
        }

        model.isLoadingCover = false

        if (boundData == model) {
            cover.textureRegion = model.coverTexture
        }
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

        val accentColor = Theme.current.accentColor
        val buttonContainerTargetWidth = if (isExpanded) width + 3f.rem else width
        val buttonContainerTargetBorderColor = if (isExpanded) accentColor else accentColor.copy(alpha = 0.3f)
        val buttonContainerTargetBorderWidth = if (isExpanded) 0.5f.srem else 0.25f.srem

        button.width = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, button.width, buttonContainerTargetWidth, 0.5f)
        buttonBorder.color = Interpolation.colorLerp(deltaTimeSec, 0.1f, buttonBorder.color, buttonContainerTargetBorderColor)
        buttonBorder.lineWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, buttonBorder.lineWidth, buttonContainerTargetBorderWidth, 0.5f)

        selectionIndicator.alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, selectionIndicator.alpha, if (isExpanded) 1f else 0f, 0.05f)

        val panelContainerTargetHeight = if (isExpanded) panelsContainer.contentHeight + panelsContainer.padding.vertical else 0f
        val panelContainerTargetAlpha = if (isExpanded) 1f else 0f

        panelsContainer.apply {
            height = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, height, panelContainerTargetHeight, 0.5f)
            alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, alpha, panelContainerTargetAlpha, 0.1f)
        }

        isTransitioning = button.width != buttonContainerTargetWidth
            || buttonBorder.color != buttonContainerTargetBorderColor
            || buttonBorder.lineWidth != buttonContainerTargetBorderWidth
            || panelsContainer.height != panelContainerTargetHeight
            || panelsContainer.alpha != panelContainerTargetAlpha

        if (!isExpanded && !isTransitioning && panelsContainer.childCount > 0) {
            panelsContainer.detachChildren()
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    fun expand() {
        val data = boundData

        if (data == null) {
            Log.w("BeatmapSetPanel", "Cannot expand panel without bound data!")
            return
        }

        if (!isExpanded) {
            isExpanded = true

            data.beatmapSetInfo.beatmaps.sortedBy { it.getStarRating() }.forEach {
                panelsContainer.attachChild(BeatmapPanel(this@BeatmapSetPanel, it))
            }
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            selectedPanel = null
        }
    }


    companion object {

        private const val COVER_HEIGHT = 100

        private val SELECTION_INDICATOR_WIDTH
            get() = 1.25f.rem

        private val coverLoadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val coverCacheDirectory: File
            get() = File(GlobalManager.getInstance().mainActivity.cacheDir, "covers")


        /**
         * Crops a horizontal band of [bandHeight] pixels from the vertical center of the bitmap, keeping the full width.
         */
        private fun Bitmap.cropInCenter(bandHeight: Int): Bitmap {
            val croppedHeight = min(bandHeight, height)
            return Bitmap.createBitmap(this, 0, (height - croppedHeight) / 2, width, croppedHeight)
        }
    }

}
