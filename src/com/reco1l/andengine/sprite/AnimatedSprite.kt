package com.reco1l.andengine.sprite

import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*




open class AnimatedSprite(frames: Array<TextureRegion?>) : ExtendedSprite() {

    /**
     * Creates an animated sprite with the texture name and frame count.
     *
     * @param textureName The base name of the texture (do not include the frame number or the hyphen if it has).
     * @param withHyphen Whether the texture name has a hyphen before the frame number.
     * @param framePerSecond The frame rate of the animation, or 0 to use the amount of frames as the frame rate.
     */
    constructor(textureName: String, withHyphen: Boolean, framePerSecond: Float): this(mutableListOf<TextureRegion?>().also { frames ->

        val frameCount = ResourceManager.getInstance().getFrameCount(textureName)

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

        // When the frame rate is 0, use the amount of frames as the frame rate.
        // If there are no frames, use the default frame rate which is 60 FPS.
        val fps = if (framePerSecond > 0) framePerSecond else frames.size.toFloat()

        if (fps > 0) {
            frameTime = 1f / fps
        }
    }


    /**
     * List of frames for the animation.
     *
     * Changing this value will reset the animation.
     */
    var frames = frames
        set(value) {
            if (!field.contentEquals(value)) {
                stop()
                field = value
                textureRegion = value.firstOrNull()
            }
        }

    /**
     * The duration of each frame in seconds.
     */
    var frameTime = DEFAULT_FRAME_TIME

    /**
     * Whether the animation should loop.
     */
    var isLoop = true

    /**
     * The elapsed time since the animation started.
     */
    var elapsedSec = 0f

    /**
     * Whether the animation is playing.
     */
    var isPlaying = true
        private set

    /**
     * The current frame index.
     */
    var frameIndex = 0
        private set(value) {
            if (field != value) {
                field = value
                textureRegion = frames[value]
            }
        }


    /**
     * The duration of the animation in seconds.
     */
    val duration
        get() = frames.size * frameTime


    init {
        if (frames.isNotEmpty()) {
            @Suppress("LeakingThis")
            textureRegion = frames[0]
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (frames.isNotEmpty()) {

            if (isPlaying) {
                elapsedSec += deltaTimeSec

                if (elapsedSec >= duration) {

                    if (isLoop) {
                        elapsedSec %= duration
                    } else {
                        elapsedSec = duration
                        isPlaying = false
                    }
                }
            }

            frameIndex = min(frames.size - 1, (elapsedSec / frameTime).toInt())
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    fun play() {
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }

    fun stop() {
        isPlaying = false
        elapsedSec = 0f
        frameIndex = 0
    }


    override fun reset() {
        stop()
        isPlaying = true
        super.reset()
    }


    companion object {

        const val DEFAULT_FRAME_TIME = 1f / 60f

    }

}