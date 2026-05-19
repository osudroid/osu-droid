package com.acivev.ui.menu.main

import android.util.Log
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.framework.Color4
import org.andengine.input.touch.TouchEvent
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegionFactory
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource

class Logo(
    /** Called when the logo is tapped (short press). */
    val onTap: () -> Unit,
    /** Called when a kiai expand-circle burst should be spawned at the logo center. */
    val onKiaiBurst: ((centreX: Float, centreY: Float, radius: Float) -> Unit)? = null,
) : UIContainer() {


    /** Current BPM length in ms. Updated by the host scene every frame. */
    var bpmLength: Float = 1000f

    /** Whether kiai is currently active. Updated by the host scene. */
    var kiaiActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) onKiaiStart() else onKiaiEnd()
        }


//    private val bgCircle: UICircle
    private val spectrum = Spectrum()
    private val halo: UISprite
    private val logoSprite: UISprite
    private val catJamSprite: KiaiCatJamSprite?


    /** Smoothed beat energy (0–1). Updated every frame. */
    private var currentBeatVal = 0f

    /** Slow-moving average of raw FFT energy — used as the normalization baseline. */
    private var energyAvg = 0f

    /** Press-feedback scale (1.0 normally, dips on touch-down, springs on touch-up). */
    private var pressScale = 1f

    /** Extra scale injected on kiai start — decays back to 0. */
    private var kiaiPulse = 0f

    /**
     * Extra scale injected on each beat when FFT energy is not available
     * (e.g. music is paused or stopped).  Decays in [update] the same way
     * [kiaiPulse] does.  Set via [pulseBeat].
     */
    private var beatPulse = 0f
    private var catJamAlpha = 0f
    private var holdStartMs = -1L
    private var holdTriggered = false
    private var burstSent = false


    init {
        width  = LOGO_SIZE + Spectrum.MAX_EXTRA * 2f
        height = LOGO_SIZE + Spectrum.MAX_EXTRA * 2f
        anchor = Anchor.Center
        origin = Anchor.Center

        val logoCX = width  / 2f
        val logoCY = height / 2f

        // 1. Pink background circle
//        bgCircle = UICircle().also { c ->
//            c.width  = LOGO_SIZE * 0.944f
//            c.height = LOGO_SIZE * 0.944f
//            c.anchor = Anchor.Center
//            c.origin = Anchor.Center
//            c.color  = BG_COLOR
//            c.alpha  = 0.2f
//            attachChild(c)
//        }

        // 2. Spectrum
        spectrum.configure(logoCX, logoCY, LOGO_SIZE / 2f)
        attachChild(spectrum)

        // 3. Main logo sprite
        logoSprite = UISprite().also { s ->
            s.textureRegion = ResourceManager.getInstance().getTexture("logo")
            s.width = LOGO_SIZE
            s.height = LOGO_SIZE
            s.anchor = Anchor.Center
            s.origin = Anchor.Center
            attachChild(s)
        }

        //  4. Halo
        halo = UISprite().also { s ->
            val overlayTex = ResourceManager.getInstance().getTexture("logo")
            s.textureRegion = overlayTex
            s.width = LOGO_SIZE * 1.07f
            s.height = LOGO_SIZE * 1.07f
            s.anchor = Anchor.Center
            s.origin = Anchor.Center
            s.alpha = 0.2f
            attachChild(s)
        }

        // 5. CatJam
        catJamSprite = loadCatJam()?.also { cj ->
            cj.width = LOGO_SIZE
            cj.height = LOGO_SIZE
            cj.anchor = Anchor.Center
            cj.origin = Anchor.Center
            attachChild(cj)
        }
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        val cx = width  / 2f
        val cy = height / 2f
        val dx = localX - cx
        val dy = localY - cy
        val r = LOGO_SIZE / 2f
        if (dx * dx + dy * dy > r * r) return false

        when {
            event.isActionDown -> {
                holdStartMs = System.currentTimeMillis()
                holdTriggered = false
                pressScale = 0.93f
                return true
            }
            event.isActionUp -> {
                val triggered = holdTriggered
                holdStartMs = -1L
                holdTriggered = false
                pressScale = 1.05f

                if (!triggered) onTap()
                return true
            }
            event.isActionCancel -> {
                holdStartMs = -1L
                holdTriggered = false
                pressScale = 1f
            }
        }
        return false
    }


    fun update(dt: Float, songPos: Int, bpmMs: Float, timingOffset: Int, fft: FloatArray?, isPlaying: Boolean) {
        this.bpmLength = bpmMs

        if (holdStartMs > 0 && !holdTriggered &&
            System.currentTimeMillis() - holdStartMs >= HOLD_TOGGLE_MS) {
            holdTriggered = true
            val enabled = !Config.isKiaiCatJam()
            Config.setKiaiCatJam(enabled)
            ToastLogger.showText(
                if (enabled) "🐱 CatJam: ON" else "🐱 CatJam: OFF",
                false
            )
        }

        // Beat detection via energy deviation from rolling average
        val rawEnergy = if (fft != null && fft.size >= 16) {
            var lowSum = 0f
            for (i in 1..3)  lowSum  += fft[i]
            val lows = lowSum / 3f

            var midSum = 0f
            for (i in 4..7)  midSum  += fft[i]
            val mids = midSum / 4f

            var vocalSum = 0f
            for (i in 8..12) vocalSum += fft[i]
            val vocals = vocalSum / 5f

            mids + lows * 0.5f + vocals * 0.25f
        } else 0f

        // Slow average adapts to the song's overall level (~650 ms window).
        energyAvg = lerp(energyAvg, rawEnergy, dt * 1.5f)

        // Beat = ratio of current energy above the average (0 between beats, 0.5–1 on hits).
        val rawBeat = ((rawEnergy / (energyAvg + 0.0001f) - 1f) * 3f).coerceIn(0f, 1f)
        currentBeatVal = lerp(currentBeatVal, rawBeat, dt * 20f)

        // Scale animation
        kiaiPulse = lerp(kiaiPulse,  0f, dt * 8f)
        beatPulse = lerp(beatPulse,  0f, dt * 8f)
        pressScale = lerp(pressScale, 1f, dt * 20f)

        val logoS = (1f + 0.25f * currentBeatVal + kiaiPulse + beatPulse) * pressScale
        val haloS = logoS
        val bgS =  1f + 0.12f * currentBeatVal + kiaiPulse

        logoSprite.scaleX = logoS
        logoSprite.scaleY = logoS
        halo.scaleX = haloS
        halo.scaleY = haloS
//        bgCircle.scaleX = bgS
//        bgCircle.scaleY = bgS

        // Spectrum
        spectrum.update(dt, kiaiActive)

        // CatJam
        val showCat = Config.isKiaiCatJam() && kiaiActive && isPlaying
        catJamAlpha = if (showCat)
            (catJamAlpha + dt * 5f).coerceAtMost(1f)
        else
            (catJamAlpha - dt * 10f).coerceAtLeast(0f)

        catJamSprite?.alpha = catJamAlpha * 0.5f

        if (catJamAlpha > 0f && catJamSprite != null) {
            updateCatJamFrame(songPos, bpmMs, timingOffset)
        }
    }


    private fun onKiaiStart() {
        kiaiPulse = 0.15f

        if (!burstSent) {
            burstSent = true
            val cx = absoluteX + width  / 2f
            val cy = absoluteY + height / 2f
            onKiaiBurst?.invoke(cx, cy, LOGO_SIZE / 2f)
        }
    }

    private fun onKiaiEnd() {
        burstSent = false
    }

    /** Called by the host scene on each detected BPM beat to boost the spectrum ring spin. */
    fun onBeat() = spectrum.onBeat(kiaiActive)

    /**
     * Fire an explicit scale pulse — used when music is paused or stopped so the
     * logo keeps visually bouncing at the BPM even with no live FFT data.
     */
    fun pulseBeat() {
        beatPulse = 0.07f
        spectrum.onBeat(kiaiActive)
    }


    private fun updateCatJamFrame(songPos: Int, bpmMs: Float, timingOffset: Int) {
        val cj = catJamSprite ?: return
        val frames = cj.frames
        val total  = frames.size
        if (total <= 0 || bpmMs <= 0f) return

        val totalBeats = (songPos - timingOffset).toFloat() / bpmMs
        val imagesPerBeat = total / 13f
        val input = totalBeats * imagesPerBeat
        val period = total * 2f
        val wrapped = ((input % period) + period) % period
        val frameF = if (wrapped <= total) wrapped else (period - wrapped)
        val frameIdx = frameF.toInt().coerceIn(0, total - 1)

        cj.textureRegion = frames[frameIdx]
    }


    private fun loadCatJam(): KiaiCatJamSprite? {
        return try {
            val ctx = GlobalManager.getInstance().mainActivity
            val src = QualityAssetBitmapSource(ctx, "catjam-spritesheet.png")
            if (src.width == 0 || src.height == 0) return null

            val atlas = BitmapTextureAtlas(
                GlobalManager.getInstance().engine.textureManager,
                src.width, src.height,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA
            )
            TextureRegionFactory.createFromSource(atlas, src, 0, 0, false)
            GlobalManager.getInstance().engine.textureManager.loadTexture(atlas)

            val frameSize = KiaiCatJamSprite.FRAME_SIZE
            val cols = KiaiCatJamSprite.GRID_COLS
            val total = KiaiCatJamSprite.TOTAL_FRAMES

            val frames = Array(total) { idx ->
                val col = idx % cols
                val row = idx / cols
                TextureRegion(
                    atlas,
                    (col * frameSize).toFloat(), (row * frameSize).toFloat(),
                    frameSize.toFloat(), frameSize.toFloat()
                )
            }
            KiaiCatJamSprite(frames)
        } catch (e: Exception) {
            Log.w("Logo", "Failed to load catjam", e)
            null
        }
    }


    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)


    companion object {
        /** Base size of the logo disc in game pixels. */
        val LOGO_SIZE get() = Config.getRES_HEIGHT() * 0.7f

        private const val HOLD_TOGGLE_MS = 3_000L

        // pink sampled from logo.png
        private val BG_COLOR = Color4(233 / 255f, 103 / 255f, 161 / 255f, 1.0f)
    }
}
