package com.reco1l.osu.graphics

import com.reco1l.osu.graphics.AnimationState.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*


open class AnimatedSprite(frames: Array<TextureRegion?>) : ExtendedSprite() {

    /**
     * Creates an animated sprite with the texture name and frame count.
     *
     * @param textureName The base name of the texture (do not include the frame number or the hyphen if it has).
     * @param withHyphen Whether the texture name has a hyphen before the frame number.
     * @param fps The frame rate of the animation.
     */
    @JvmOverloads
    constructor(textureName: String, withHyphen: Boolean, fps: Float = DEFAULT_FPS) : this(mutableListOf<TextureRegion?>().also { frames ->

        var frameCount = if (fps < 0) ResourceManager.getInstance().getFrameCount(textureName) else fps.toInt()

        // ResourceManager can return -1 if the textures are not present.
        if (frameCount < 0) {
            frameCount = DEFAULT_FPS.toInt()
        }

        for (i in 0 until frameCount) {
            val frameName = textureName + (if (withHyphen) "-" else "") + i

            if (ResourceManager.getInstance().isTextureLoaded(frameName)) {
                frames.add(ResourceManager.getInstance().getTexture(frameName))
            }
        }

        if (frames.isEmpty()) {
            frames.add(ResourceManager.getInstance().getTexture(textureName))
        }
    }.toTypedArray()) {
        this.fps = fps
    }


    /**
     * List of frames for the animation.
     *
     * Changing this value will reset the animation.
     */
    var frames = frames
        set(value) {
            state = STOPPED
            textureRegion = value.firstOrNull()
            field = value
        }

    /**
     * The current state of the animation.
     *
     * Negative values will set the frame count as the frame rate.
     */
    var fps = -1f

    /**
     * The current state of the animation.
     *
     * @see AnimationState
     */
    var state = PLAYING
        set(value) {

            // Reset the elapsed time when the animation is started.
            if (field == STOPPED) {
                elapsedSec = 0f
            }

            field = value
        }

    /**
     * Whether the animation should loop.
     */
    var isLoop = true

    /**
     * The elapsed time since the animation started.
     */
    var elapsedSec = 0f


    /**
     * The current frame index.
     */
    val frameIndex
        get() = frames.indexOf(textureRegion)


    init {
        textureRegion = frames.firstOrNull()
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (frames.isNotEmpty() && state == PLAYING) {

            elapsedSec += pSecondsElapsed

            val framePerSec = if (fps < 0) frames.size.toFloat() else fps

            var frameIndex = (elapsedSec * framePerSec).roundToInt()

            if (isLoop) {
                frameIndex %= frames.size
            } else {
                frameIndex = frameIndex.coerceAtMost(frames.size - 1)

                if (frameIndex == frames.size - 1) {
                    state = STOPPED
                }
            }

            textureRegion = frames[frameIndex]
        }

        super.onManagedUpdate(pSecondsElapsed)
    }


    override fun reset() {
        super.reset()
        elapsedSec = 0f
    }


    companion object {

        const val DEFAULT_FPS = 60f

    }

}

/**
 * The animation state.
 */
enum class AnimationState {

    /**
     * The animation is playing.
     */
    PLAYING,

    /**
     * The animation is paused.
     */
    PAUSED,

    /**
     * The animation is stopped.
     */
    STOPPED
}