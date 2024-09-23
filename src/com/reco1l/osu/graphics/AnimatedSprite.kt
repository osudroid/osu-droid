package com.reco1l.osu.graphics

import com.reco1l.osu.graphics.AnimationState.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*
import kotlin.math.*


open class AnimatedSprite(frames: Array<TextureRegion?>) : ExtendedSprite() {


    @JvmOverloads
    constructor(texturePrefix: StringSkinData? = null, textureName: String?, frameCount: Int) : this(Array(frameCount.coerceAtLeast(1)) {

        if (texturePrefix == null) {
            ResourceManager.getInstance().getTexture(textureName + it)
        } else {
            ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, (textureName ?: "") + it)
        }
    })

    constructor(textureName: String, withHyphen: Boolean, fps: Float) : this(mutableListOf<TextureRegion?>().also { frames ->

        // FIXME: Using 120 is a workaround, the amount of available frames for a given texture should be computed previously to this.
        //  This can be an issue for skins with more than 120 frames animations.

        for (i in 0 until (if (fps < 0) 120 else fps.toInt())) {
            val frameName = textureName + (if (withHyphen) "-" else "") + i

            if (!ResourceManager.getInstance().isTextureLoaded(frameName)) {
                break
            }

            frames.add(ResourceManager.getInstance().getTexture(frameName))
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