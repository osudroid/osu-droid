package com.osudroid.storyboard.renderer

import android.graphics.Color
import com.edlplan.andengine.TextureHelper
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardAnimation
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardSample
import com.osudroid.storyboard.parser.StoryboardParser
import com.osudroid.storyboard.playback.StoryboardPlayback
import com.reco1l.andengine.component.UIComponent
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import org.andengine.engine.camera.Camera
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.helper.FileUtils

/**
 * Renders a beatmap's storyboard.
 *
 * This component renders the storyboard background and all layers up to the `Foreground` layer
 * (and custom layers above it), followed by the background dim, and is meant to be attached below
 * all gameplay elements. The `Overlay` layer is rendered by [overlayComponent], which is meant to be
 * attached above gameplay elements.
 *
 * All sprites are streamed through a shared [StoryboardBatch], with the storyboard's 640x480
 * coordinate space mapped to a centered letterbox that fills the screen height (or width,
 * whichever is limiting). Sprites outside of that area are not cropped.
 */
class StoryboardComponent : UIComponent() {
    /**
     * The component rendering the `Overlay` layer, meant to be attached above gameplay elements.
     */
    @JvmField
    val overlayComponent = StoryboardOverlayComponent(this)

    /**
     * When `true`, the storyboard background is not drawn. Used when a background video is
     * playing so that the video shows through.
     */
    @JvmField
    var transparentBackground = false

    /**
     * The background brightness in the range `[0, 1]`. A dim overlay with `1 - brightness` alpha
     * is drawn above the storyboard layers. The sprites of the `Overlay` layer are darkened
     * instead, as they are displayed in front of gameplay elements.
     */
    @JvmField
    var brightness = 1f

    /**
     * The playback of the loaded storyboard, or `null` if no storyboard is loaded.
     */
    var playback: StoryboardPlayback? = null
        private set

    private var texturePool: StoryboardTexturePool? = null
    private var sounds: HashMap<String, BassSoundProvider>? = null
    private var loadedOsuPath: String? = null
    private var loadedSignature = 0L

    internal val batch = StoryboardBatch()

    /**
     * Whether a storyboard is loaded.
     */
    val isStoryboardAvailable
        get() = playback != null

    /**
     * The earliest point in time at which the loaded storyboard displays a sprite or plays a
     * sample, in milliseconds, or `null` if there is none.
     */
    val earliestEventTime
        get() = playback?.earliestEventTime

    init {
        width = Config.getRES_WIDTH().toFloat()
        height = Config.getRES_HEIGHT().toFloat()
    }

    /**
     * Loads the storyboard of a beatmap. Parsing, playback compilation and texture loading are
     * performed on the calling thread, so this is meant to be called from a background thread.
     *
     * @param osuPath The path of the beatmap's `.osu` file.
     * @param scope The [CoroutineScope] to use for cancellation checkpoints.
     */
    @JvmOverloads
    fun load(osuPath: String, scope: CoroutineScope? = null) {
        if (isLoadedFor(osuPath)) {
            return
        }

        release()

        val signature = computeSignature(osuPath)
        val storyboard = StoryboardParser(osuPath, scope).parse()

        if (storyboard != null) {
            val directory = File(osuPath).parentFile!!

            // The resources are assigned before they are loaded so that release() unloads them
            // if loading is interrupted.
            val pool = StoryboardTexturePool(directory, storyboard.useSkinSprites)
            texturePool = pool
            pool.load(storyboard)

            scope?.ensureActive()
            loadSounds(storyboard, directory, scope)

            playback = StoryboardPlayback(storyboard).also { it.onSamplePlayed = ::playSample }
        }

        loadedOsuPath = osuPath
        loadedSignature = signature
    }

    /**
     * Whether the storyboard of the given beatmap is loaded and its files have not changed since.
     * This is also the case if the beatmap was found to have no storyboard.
     *
     * @param osuPath The path of the `.osu` file of the beatmap.
     */
    fun isLoadedFor(osuPath: String) = osuPath == loadedOsuPath && computeSignature(osuPath) == loadedSignature

    /**
     * Resets the playback of the loaded storyboard so that it can be played back from the start.
     */
    fun resetPlayback() {
        stopSamples()
        playback?.reset()
    }

    /**
     * Stops all playing storyboard samples.
     */
    fun stopSamples() {
        sounds?.values?.forEach { it.stop() }
    }

    private fun computeSignature(osuPath: String): Long {
        val osuFile = File(osuPath)
        val osbFile = FileUtils.listFiles(osuFile.parentFile, ".osb")?.firstOrNull()

        return osuFile.lastModified() * 31 + (osbFile?.lastModified() ?: 0L)
    }

    private fun loadSounds(storyboard: Storyboard, directory: File, scope: CoroutineScope?) {
        val sounds = HashMap<String, BassSoundProvider>()
        this.sounds = sounds

        for (sample in storyboard.samples) {
            if (sample.filePath in sounds) {
                continue
            }

            scope?.ensureActive()

            val file = File(directory, sample.filePath)
            val sound = BassSoundProvider()

            if (file.isFile && sound.prepare(file.absolutePath)) {
                sounds[sample.filePath] = sound
            }
        }
    }

    private fun playSample(sample: StoryboardSample) {
        val sound = sounds?.get(sample.filePath) ?: return

        sound.setFrequency(GameHelper.getSpeedMultiplier())
        sound.play(sample.volume / 100f)
    }

    /**
     * Advances the storyboard playback to the given time. Seeking in both directions is
     * supported.
     *
     * This only updates the active sprite windows; the (expensive) per-sprite state evaluation
     * happens while drawing, so heavy storyboards slow down rendering at worst but can never
     * stall the update thread and with it the gameplay clock.
     *
     * @param timeMs The gameplay time in milliseconds.
     */
    fun updateTime(timeMs: Double) {
        playback?.setTime(timeMs)
    }

    /**
     * Updates the passing state, controlling the `Pass`/`Fail` layer visibility and
     * `Passing`/`Failing` triggers.
     *
     * @param passing Whether the player is in a passing state.
     */
    fun setPassingState(passing: Boolean) {
        val playback = playback ?: return

        playback.setPassing(passing, playback.currentTime)
    }

    /**
     * Notifies the storyboard that hit samples were played, activating matching hit sound
     * triggers.
     *
     * @param samples The played samples.
     */
    fun onHitSamplesPlayed(samples: List<GameplayHitSampleInfo>) {
        val playback = playback ?: return

        if (!playback.hasHitSoundTriggers) {
            return
        }

        val bankSamples = samples.mapNotNull { it.sampleInfo as? BankHitSampleInfo }

        if (bankSamples.isNotEmpty()) {
            playback.onHitSound(bankSamples, playback.currentTime)
        }
    }

    /**
     * Notifies the storyboard that a hit object was hit, activating hit object hit triggers.
     */
    fun onHitObjectHit() {
        val playback = playback ?: return

        playback.onHitObjectHit(playback.currentTime)
    }

    /**
     * Releases the loaded storyboard and unloads its textures and samples.
     */
    fun release() {
        playback = null
        loadedOsuPath = null
        loadedSignature = 0L
        texturePool?.clear()
        texturePool = null
        sounds?.values?.forEach { it.free() }
        sounds = null
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        beginDraw(pGLState)

        val playback = playback ?: return

        batch.begin(pGLState)
        drawBackground(playback)
        drawLayers(playback, MAIN_LAYERS, drawAlpha)
        drawDim()
        batch.end()
    }

    /**
     * Draws the given storyboard layers, letterboxed to the centered 640x480 storyboard space.
     * Shared with [StoryboardOverlayComponent]. The batch must have been started with
     * [StoryboardBatch.begin] by the caller.
     */
    internal fun drawLayers(
        playback: StoryboardPlayback,
        layers: Array<StoryboardLayerType>,
        alpha: Float,
        colorMultiplier: Float = 1f
    ) {
        val pool = texturePool ?: return

        val screenWidth = width
        val screenHeight = height
        val scale = min(screenWidth / STORYBOARD_WIDTH, screenHeight / STORYBOARD_HEIGHT)
        val offsetX = (screenWidth - STORYBOARD_WIDTH * scale) / 2
        val offsetY = (screenHeight - STORYBOARD_HEIGHT * scale) / 2

        // Note: unlike lazer, the storyboard is intentionally NOT masked to its 4:3/16:9 box.
        // osu!stable and the previous osu-droid renderer never cropped storyboards, and sprites
        // outside the box are expected to be visible on wide screens.
        batch.setSpace(offsetX, offsetY, scale)

        for (layer in layers) {
            if (!playback.isLayerVisible(layer)) {
                continue
            }

            val sprites = playback.activeSprites(layer)

            for (i in sprites.indices) {
                val sprite = sprites[i]

                sprite.update(playback.currentTime)

                if (!sprite.isVisible || sprite.alpha * alpha < ALPHA_EPSILON) {
                    continue
                }

                val element = sprite.element
                val path = (element as? StoryboardAnimation)?.framePath(sprite.frameIndex) ?: element.filePath

                batch.draw(sprite, pool.get(path), alpha, colorMultiplier)
            }
        }

        batch.resetSpace()
    }

    private fun drawBackground(playback: StoryboardPlayback) {
        if (transparentBackground) {
            return
        }

        val storyboard = playback.storyboard
        val backgroundFilename = storyboard.backgroundFilename

        if (storyboard.usesBackgroundImage() || backgroundFilename == null) {
            // The storyboard displays the background itself - cover the static background
            // with black.
            batch.drawQuad(blackRegion, 0f, 0f, width, height, 0f, 0f, 0f, 1f)
            return
        }

        val region = texturePool?.get(backgroundFilename) ?: return
        val scale = min(width / region.width, height / region.height)
        val backgroundWidth = region.width * scale
        val backgroundHeight = region.height * scale
        val left = (width - backgroundWidth) / 2
        val top = (height - backgroundHeight) / 2

        batch.drawQuad(
            blackRegion, 0f, 0f, width, height,
            0f, 0f, 0f, 1f
        )
        batch.drawQuad(
            region,
            left, top, left + backgroundWidth, top + backgroundHeight,
            1f, 1f, 1f, drawAlpha
        )
    }

    private fun drawDim() {
        val dimAlpha = (1f - brightness) * drawAlpha

        if (dimAlpha < ALPHA_EPSILON) {
            return
        }

        batch.drawQuad(blackRegion, 0f, 0f, width, height, 0f, 0f, 0f, dimAlpha)
    }

    companion object {
        const val STORYBOARD_WIDTH = 640f
        const val STORYBOARD_HEIGHT = 480f

        private const val ALPHA_EPSILON = 1f / 255f

        private val MAIN_LAYERS = arrayOf(
            StoryboardLayerType.Video,
            StoryboardLayerType.Background,
            StoryboardLayerType.Fail,
            StoryboardLayerType.Pass,
            StoryboardLayerType.Foreground,
            StoryboardLayerType.Custom
        )

        private val blackRegion: TextureRegion by lazy {
            TextureHelper.create1xRegion(Color.argb(255, 0, 0, 0))
        }
    }
}
