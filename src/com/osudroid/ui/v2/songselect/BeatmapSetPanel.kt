package com.osudroid.ui.v2.songselect

import android.graphics.*
import android.util.Log
import com.reco1l.andengine.*
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.buffered.CompoundBuffer
import com.reco1l.andengine.buffered.MutableReference
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.Fonts
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.graphics.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import java.io.File
import kotlin.random.*


private val textBufferRef = MutableReference<CompoundBuffer?>(null)
private val coverBufferRef = MutableReference<UISprite.SpriteVBO?>(null)
private val buttonBufferRef = MutableReference<UIBox.BoxVBO?>(null)

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
        set(value) {
            if (field != value) {
                field = value
                Log.d("BeatmapSetPanel", "Transitioning: $value")
            }
        }

    private lateinit var cover: UISprite
    private lateinit var titleText: UIText
    private lateinit var artistText: UIText


    init {
        width = Size.Full
        anchor = Anchor.TopRight
        origin = Anchor.TopRight

        linearContainer {
            orientation = Orientation.Vertical
            width = Size.Full
            style = {
                spacing = 1f.srem
            }

            button = clickableContainer {
                width = Size.Full
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                onActionUp = {
                    // This will prevent race condition, during transition panelsContainer might
                    // get cleared and since the select() method is based on beatmap indices
                    // it might fail to find the corresponding BeatmapPanel causing a IOOBE.
                    if (!isTransitioning) {
                        select()
                    }
                }
                style = {
                    borderColor = it.accentColor.copy(alpha = 0.3f)
                    borderWidth = 0.25f.srem
                    minHeight = 4.5f.rem
                    radius = Radius.XL
                }

                cover = sprite {
                    width = Size.Full
                    height = Size.Full
                    scaleType = ScaleType.Crop
                    bufferSharingMode = BufferSharingMode.Dynamic
                    bufferReference = coverBufferRef
                    style = {
                        backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.5f)
                        radius = Radius.XL
                    }
                }

                box {
                    width = Size.Full
                    height = Size.Full
                    color = Colors.Black.copy(alpha = 0.6f)
                    bufferSharingMode = BufferSharingMode.Dynamic
                    bufferReference = buttonBufferRef
                    style = {
                        radius = Radius.XL
                    }
                }

                linearContainer {
                    orientation = Orientation.Vertical
                    width = Size.Full
                    style = {
                        padding = Vec4(2.5f.srem)
                    }

                    titleText = text {
                        text = "Unknown"
                        style = {
                            fontFamily = Fonts.TorusBold
                            color = it.accentColor
                        }
                        bufferSharingMode = BufferSharingMode.Dynamic
                        bufferReference = textBufferRef
                    }

                    artistText = text {
                        text = "Unknown"
                        style = { color = it.accentColor * 0.8f }
                        bufferSharingMode = BufferSharingMode.Dynamic
                        bufferReference = textBufferRef
                    }

                }

            }

            panelsContainer = linearContainer {
                orientation = Orientation.Vertical
                width = Size.Full
                height = 0f
                alpha = 0f
                style = {
                    spacing = 1f.srem
                }
            }
        }
    }


    override fun onBind(data: BeatmapSetModel) {
        titleText.text = data.beatmapSetInfo.beatmaps.getOrNull(0)?.titleText ?: "Unknown"
        artistText.text = data.beatmapSetInfo.beatmaps.getOrNull(0)?.artistText ?: "Unknown"

        if (!Config.isSafeBeatmapBg()) {
            if (data.coverTexture == null && !data.isLoadingCover) {
                loadCover(data)
            } else if (data.coverTexture != null) {
                cover.textureRegion = data.coverTexture
            }
        }
    }

    private fun loadCover(model: BeatmapSetModel) = coverLoadScope.launch {
        model.isLoadingCover = true

        if (!ResourceManager.getInstance().isTextureLoaded("cover@${model.beatmapSetInfo[0].md5}")) {

            if (!coverCacheDirectory.exists()) {
                coverCacheDirectory.mkdirs()
            }

            val cacheFile = File(coverCacheDirectory, "${model.beatmapSetInfo[0].md5}.webp")

            fun loadTextureFromFile() {
                val texture = ResourceManager.getInstance().loadHighQualityFile("cover@${model.beatmapSetInfo[0].md5}", cacheFile)
                if (texture != null) {
                    model.coverTexture = texture
                } else {
                    cacheFile.delete()
                }
            }

            if (!cacheFile.exists()) {
                cacheFile.createNewFile()

                val bitmap = BitmapFactory.decodeFile(model.beatmapSetInfo[0].backgroundPath).let { it.cropInCenter(it.width, 100) }

                cacheFile.outputStream().use { out ->
                    if (bitmap.compress(Bitmap.CompressFormat.WEBP, 10, out)) {
                        loadTextureFromFile()
                    } else {
                        cacheFile.delete()
                    }
                }
            } else {
                loadTextureFromFile()
            }
        } else {
            model.coverTexture = ResourceManager.getInstance().getTexture("cover@${model.beatmapSetInfo[0].md5}")
        }

        model.isLoadingCover = false

        if (isBound) {
            cover.textureRegion = model.coverTexture
        }
    }


    fun select(beatmapIndex: Int = 0) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[beatmapIndex]
    }

    fun selectRandom() {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[Random.nextInt(panelsContainer.childCount - 1)]
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val buttonContainerTargetWidth = if (isExpanded) width + 2f.rem else width
        val buttonContainerTargetBorderColor = if (isExpanded) Theme.current.accentColor else Theme.current.accentColor.copy(alpha = 0.3f)
        val buttonContainerTargetBorderWidth = if (isExpanded) 0.5f.srem else 0.25f.srem

        button.apply {
            width = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, width, buttonContainerTargetWidth, 0.5f)
            borderColor = Interpolation.colorLerp(deltaTimeSec, 0.1f, borderColor, buttonContainerTargetBorderColor)
            borderWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, borderWidth, buttonContainerTargetBorderWidth, 0.5f)
        }

        val panelContainerTargetHeight = if (isExpanded) panelsContainer.intrinsicHeight else 0f
        val panelContainerTargetAlpha = if (isExpanded) 1f else 0f

        panelsContainer.apply {
            height = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, height, panelContainerTargetHeight, 0.5f)
            alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, alpha, panelContainerTargetAlpha, 0.1f)
        }

        isTransitioning = button.width != buttonContainerTargetWidth
            || button.borderColor != buttonContainerTargetBorderColor
            || button.borderWidth != buttonContainerTargetBorderWidth
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

            data.beatmapSetInfo.beatmaps.forEach {
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


    override fun onRecycle() {
        isExpanded = false
    }


    companion object {
        private val coverLoadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val coverCacheDirectory = File(GlobalManager.getInstance().mainActivity.cacheDir, "covers")
    }

}