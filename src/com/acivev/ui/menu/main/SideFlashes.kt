package com.acivev.ui.menu.main

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.ui.UIGradientBox
import com.reco1l.framework.Color4
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * Behavior matches lazer Flashes:
 *  - Outside kiai:  both sides flash together on the measure downbeat
 *  - During kiai:   left on even beats, right on odd beats (alternating)
 *  - Color:         skin's first combo color (skips white/black), fallback = blue
 *  - Alpha:         amplitude-driven from FFT energy
 *  - Animation:     65 ms fade-in → beat-length Easing.In fade-out
 *
 * Usage:
 * 1. Create:   `val sideFlashes = SideFlashes(w, h)`
 * 2. Attach:   `sideFlashes.attachTo(scene)`
 * 3. Per beat: `sideFlashes.onNewBeat(beat, kiaiActive, bpmMs, fft, timeSignature)`
 */
class SideFlashes(screenWidth: Float) {

    private val flashLeft = UIGradientBox(fromAlpha = 1f, toAlpha = 0f)
    private val flashRight = UIGradientBox(fromAlpha = 0f, toAlpha = 1f)

    init {
        val flashW = screenWidth * 0.15f
        flashLeft.apply {
            width = flashW
            height = UIComponent.FillParent
            x = 0f
            alpha = 0f
            color = DEFAULT_COLOR
            blendInfo = BlendInfo.Additive
        }
        flashRight.apply {
            width = flashW
            height = UIComponent.FillParent
            x = screenWidth - flashW
            alpha = 0f
            color = DEFAULT_COLOR
            blendInfo = BlendInfo.Additive
        }
    }

    /** Attach both flash boxes to [scene]. Call once during scene init. */
    fun attachTo(scene: UIScene) {
        scene.attachChild(flashLeft)
        scene.attachChild(flashRight)
    }

    /**
     * Drive the flash on each new beat.
     *
     * @param beat          current beat index (songPos / msPerBeat)
     * @param kiaiActive    whether kiai is currently active
     * @param bpmMs         current beat length in milliseconds
     * @param fft           raw FFT spectrum from the audio service (maybe null)
     * @param timeSignature beats per measure (used for downbeat detection outside kiai)
     */
    fun onNewBeat(
        beat: Int,
        kiaiActive: Boolean,
        bpmMs: Float,
        fft: FloatArray?,
        timeSignature: Int
    ) {
        // Amplitude-based alpha
        val energy = if (fft != null && fft.size > 8) {
            var sum = 0f
            for (i in 1..8) sum += fft[i]
            (sum / 8f * FFT_SCALE).coerceIn(0f, 1f)

        } else 0.5f

        val deadZone = 0.25f
        val multiplier =
            if (kiaiActive) (1f - deadZone * 0.95f) / 0.8f
            else (1f - deadZone) / 0.55f

        val targetAlpha = (0.1f + (energy - deadZone) / multiplier).coerceIn(0.1f, 1f)
        val beatLenSec  = bpmMs / 1000f

        // Outside kiai: both flash on measure downbeat
        // During kiai:  alternate L/R every beat
        val flashL =
            if (kiaiActive) beat % 2 == 0
            else beat % timeSignature == 0
        val flashR =
            if (kiaiActive) beat % 2 == 1
            else beat % timeSignature == 0

        if (flashL) trigger(flashLeft,  targetAlpha, beatLenSec)
        if (flashR) trigger(flashRight, targetAlpha, beatLenSec)
    }


    private fun trigger(box: UIGradientBox, targetAlpha: Float, beatLenSec: Float) {
        box.color = resolveColor()
        box.clearEntityModifiers()
        box.fadeTo(targetAlpha, 0.065f).after {
            box.fadeTo(0f, beatLenSec, Easing.In)
        }
    }

    /** Skin's first combo color, skipping near-white and near-black. Falls back to [DEFAULT_COLOR]. */
    private fun resolveColor(): Color4 {
        return OsuSkin.get().getComboColor().firstOrNull { c ->
            val isNearWhite = c.red > 0.9f && c.green > 0.9f && c.blue > 0.9f
            val isNearBlack = c.red < 0.1f && c.green < 0.1f && c.blue < 0.1f
            !isNearWhite && !isNearBlack
        } ?: DEFAULT_COLOR
    }

    /** Immediately stop any in-flight animations and hide both flash boxes. */
    fun fadeOut(duration: Float = 0.064f, easing: Easing = Easing.OutQuint) {
        flashLeft.clearEntityModifiers()
        flashRight.clearEntityModifiers()
        flashLeft.fadeTo(0f, duration, easing)
        flashRight.fadeTo(0f, duration, easing)
    }

    companion object {
        /** Default blue*/
        val DEFAULT_COLOR = Color4(0.4f, 0.8f, 1f, 1f)

        /** Scale applied to raw FFT values when computing flash amplitude. */
        private const val FFT_SCALE = 8f
    }
}

