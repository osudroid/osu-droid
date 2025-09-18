package com.osudroid.ui.v2.songselect

import android.graphics.*
import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.graphics.*
import com.reco1l.toolkt.kotlin.*
import kotlinx.coroutines.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import java.io.File
import kotlin.random.*


class BeatmapSetPanel(val beatmapCarrousel: BeatmapCarrousel) : RecyclableComponent<BeatmapSetModel>(), IPanelContainer<BeatmapPanel> {

    override val isRecyclable: Boolean
        get() = !isPressed && !isExpanded && !button.run { isAnimating || background!!.isAnimating || foreground!!.isAnimating }

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

                panelContainer.forEach { panel -> panel as BeatmapPanel
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
     * The local position for the currently selected panel.
     */
    val selectedPanelY: Float
        get() = panelContainer.absoluteY + (selectedPanel?.let { it.absoluteY + it.height / 2f } ?: 0f)

    /**
     * The container for the beatmap panels.
     */
    val panelContainer = UILinearContainer()

    /**
     * The button that represents the beatmap set
     */
    val button = object : UILinearContainer() {

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

            if (event.isActionDown) {
                background?.apply {
                    clearModifiers(ModifierType.Color)
                    colorTo(Color4.White, 0.1f).eased(Easing.Out)
                }
            }

            if (event.isActionUp) {
                select()
            }

            if (event.isActionUp || event.isActionCancel || event.isActionOutside) {
                background?.apply {
                    clearModifiers(ModifierType.Color)
                    colorTo(Theme.current.accentColor * 0.4f, 0.2f)
                }
            }
            return true
        }

    }


    private val titleText: UIText
    private val artistText: UIText
    private lateinit var statusBadge: UIBadge


    init {
        orientation = Orientation.Vertical
        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        spacing = 4f

        +button.apply {
            orientation = Orientation.Vertical
            width = FillParent
            anchor = Anchor.TopRight
            origin = Anchor.TopRight
            padding = Vec4(14f)
            minHeight = 100f

            background = if (Config.isSafeBeatmapBg())
                UIBox().apply {
                    cornerRadius = 14f
                    applyTheme = { color = it.accentColor * 0.175f }
                }
            else
                UIShapedSprite().apply {
                    buffer = sharedSpriteVBO
                    shape = UIBox().apply {
                        cornerRadius = 14f
                        buffer = sharedBackgroundVBO
                    }

                    applyTheme = {
                        color = it.accentColor * 0.4f
                    }
                }


            foreground = UIBox().apply {
                paintStyle = PaintStyle.Outline
                cornerRadius = 14f
                lineWidth = 2f
                buffer = sharedForegroundVBO
                applyTheme = {
                    color = it.accentColor * 0.2f
                }
            }

            titleText = text {
                font = ResourceManager.getInstance().getFont("smallFont")
                text = "Unknown"
                applyTheme = { color = it.accentColor }
                buffer = sharedTextCB
            }

            artistText = text {
                font = ResourceManager.getInstance().getFont("xs")
                text = "Unknown"
                applyTheme = { color = it.accentColor * 0.8f }
                buffer = sharedTextCB
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 10f
                padding = Vec4(0f, 6f, 0f, 0f)

                statusBadge = badge {
                    text = "Unknown"
                    sizeVariant = SizeVariant.Small
                    textEntity.buffer = sharedTextCB
                    (background as UIBox).buffer = sharedBadgeBackgroundVBO
                }
            }
        }

        +panelContainer.apply {
            width = FillParent
            orientation = Orientation.Vertical
            spacing = 4f

            alpha = 0f
            height = 0f
            isVisible = false
        }
    }


    override fun onBind(data: BeatmapSetModel) {
        titleText.text = data.beatmapSetInfo.beatmaps.getOrNull(0)?.titleText ?: "Unknown"
        artistText.text = data.beatmapSetInfo.beatmaps.getOrNull(0)?.artistText ?: "Unknown"

        if (!Config.isSafeBeatmapBg()) {
            if (data.coverTexture == null && !data.isLoadingCover) {
                loadCover(data)
            } else if (data.coverTexture != null) {
                (button.background as UIShapedSprite).textureRegion = data.coverTexture
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
            (button.background as UIShapedSprite).textureRegion = model.coverTexture
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        panelContainer.maxHeight = panelContainer.contentHeight
        super.onManagedUpdate(deltaTimeSec)
    }


    fun select(beatmapIndex: Int = 0) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelContainer[beatmapIndex]
    }

    fun selectRandom() {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelContainer[Random.nextInt(panelContainer.childCount - 1)]
    }


    fun expand() {
        if (isExpanded) {
            return
        }
        isExpanded = true

        padding = Vec4(0f, 12f)

        button.apply {
            clearModifiers(ModifierType.SizeX)
            sizeToX(parent.innerWidth + 100f, 0.1f)
        }

        panelContainer.apply {
            boundData?.beatmapSetInfo?.beatmaps?.forEach { +BeatmapPanel(this@BeatmapSetPanel, it) }
            onHandleInvalidations(false)

            isVisible = true
            clearModifiers(ModifierType.SizeY, ModifierType.Alpha)
            fadeIn(0.2f)
            sizeToY(contentHeight + padding.vertical, 0.2f).then {
                beatmapCarrousel.autoScrollToSelectedPanel = true
            }
        }

        (button.foreground as UIBox).apply {
            lineWidth = 4f
            clearModifiers(ModifierType.Color)
            colorTo(Theme.current.accentColor, 0.1f)
        }
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }
        isExpanded = false

        padding = Vec4.Zero
        selectedPanel = null

        button.apply {
            clearModifiers(ModifierType.SizeX)
            sizeToX(parent.innerWidth, 0.1f)
        }

        panelContainer.apply {
            clearModifiers(ModifierType.SizeY, ModifierType.Alpha)
            fadeOut(0.1f)
            sizeToY(0f, 0.1f).then {
                isVisible = false
                detachChildren()
            }
        }

        (button.foreground as UIBox).apply {
            lineWidth = 2f
            clearModifiers(ModifierType.Color)
            colorTo(Theme.current.accentColor * 0.3f, 0.1f)
        }
    }


    override fun onRecycle() {
        isExpanded = false
    }


    companion object {

        private val coverLoadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val coverCacheDirectory = File(GlobalManager.getInstance().mainActivity.cacheDir, "covers")

        private val sharedTextCB = CompoundBuffer(UIText.TextTextureBuffer(256), UIText.TextVertexBuffer(256)).asSharedDynamically()
        private val sharedBackgroundVBO = UIBox.BoxVBO(14f, UICircle.approximateSegments(14f, 14f, 90f), PaintStyle.Fill).asSharedStatically()
        private val sharedForegroundVBO = UIBox.BoxVBO(14f, UICircle.approximateSegments(14f, 14f, 90f), PaintStyle.Outline).asSharedStatically()
        private val sharedBadgeBackgroundVBO = UIBox.BoxVBO(6f, UICircle.approximateSegments(6f, 6f, 90f), PaintStyle.Fill).asSharedStatically()
        private val sharedSpriteVBO = UISprite.SpriteVBO().asSharedDynamically()
    }

}