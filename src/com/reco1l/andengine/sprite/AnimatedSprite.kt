package com.reco1l.andengine.sprite

import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*


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
            stop()
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
     * Whether the animation is playing.
     *
     * Setting this value to false will pause the animation and vice-versa.
     * To stop the animation, use the [stop] method.
     */
    var isPlaying = true

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
    var frameIndex = 0
        private set


    init {
        textureRegion = frames.firstOrNull()
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        val frameCount = frames.size

        if (frameCount > 0 && isPlaying) {

            elapsedSec += pSecondsElapsed

            val framePerSec = if (fps < 0) frameCount.toFloat() else fps
            val frameTime = (elapsedSec * framePerSec).toInt()

            if (isLoop) {
                frameIndex = frameTime % frameCount
            } else if (frameTime >= frameCount) {
                frameIndex = frameCount - 1
                stop()
            }

            textureRegion = frames[frameTime]
        } else if (isPlaying) {
            isPlaying = false
        }

        super.onManagedUpdate(pSecondsElapsed)
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
        super.reset()
        elapsedSec = 0f
    }


    companion object {

        const val DEFAULT_FPS = 60f

    }

}