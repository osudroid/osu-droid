package com.reco1l.osu.graphics

import com.reco1l.osu.graphics.AnimationState.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*


open class AnimatedSprite(frames: List<TextureRegion>) : ExtendedSprite() {


    @JvmOverloads
    constructor(texturePrefix: StringSkinData? = null, textureName: String?, frameCount: Int) : this(List(frameCount.coerceAtLeast(1)) {

        if (texturePrefix == null) {
            ResourceManager.getInstance().getTexture(textureName + it)
        } else {
            ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, (textureName ?: "") + it)
        }
    })

    constructor(vararg textureNames: String) : this(textureNames.map {
        ResourceManager.getInstance().getTexture(it)
    })


    /**
     * List of frames for the animation.
     *
     * Changing this value will reset the animation.
     */
    var frames = frames
        set(value) {
            if (field != value) {
                state = STOPPED
                textureRegion = value.firstOrNull()
                field = value
            }
        }

    /**
     * The current state of the animation.
     *
     * Setting FPS to 0 is equivalent to setting the state to [STOPPED].
     */
    var fps = frames.size.toFloat()

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

        if (frames.isEmpty()) {
            return
        }

        if (state == PLAYING) {
            elapsedSec += pSecondsElapsed

            var frameIndex = (elapsedSec * fps).toInt()

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