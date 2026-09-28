package com.osudroid.storyboard.model

import com.osudroid.storyboard.model.commands.StoryboardCommandGroup

/**
 * Represents a visual element of a storyboard.
 */
sealed class StoryboardElement(
    /**
     * The layer this element is rendered on.
     */
    @JvmField
    val layer: StoryboardLayerType,

    /**
     * The origin of this element.
     */
    @JvmField
    val origin: StoryboardOrigin,

    /**
     * The path of this element's texture, relative to the beatmap folder and normalized to use
     * `/` as separator.
     */
    @JvmField
    val filePath: String,

    /**
     * The initial X coordinate of this element in storyboard space.
     */
    @JvmField
    val initialX: Float,

    /**
     * The initial Y coordinate of this element in storyboard space.
     */
    @JvmField
    val initialY: Float
) {
    /**
     * The commands of this element.
     */
    @JvmField
    val commands = StoryboardCommandGroup()

    /**
     * All texture file paths this element can display.
     */
    open val texturePaths: List<String>
        get() = listOf(filePath)
}

/**
 * Represents a storyboard `Sprite` element.
 */
class StoryboardSprite(
    layer: StoryboardLayerType,
    origin: StoryboardOrigin,
    filePath: String,
    initialX: Float,
    initialY: Float
) : StoryboardElement(layer, origin, filePath, initialX, initialY)

/**
 * Represents a storyboard `Animation` element.
 */
class StoryboardAnimation(
    layer: StoryboardLayerType,
    origin: StoryboardOrigin,
    filePath: String,
    initialX: Float,
    initialY: Float,

    /**
     * The number of frames of this animation.
     */
    @JvmField
    val frameCount: Int,

    /**
     * The delay between frames in milliseconds.
     */
    @JvmField
    val frameDelay: Double,

    /**
     * The loop behavior of this animation.
     */
    @JvmField
    val loopType: AnimationLoopType
) : StoryboardElement(layer, origin, filePath, initialX, initialY) {
    override val texturePaths = List(frameCount) { framePath(it) }

    /**
     * Builds the texture path of a frame by inserting the frame index before the file extension
     * (`sprite.png` -> `sprite0.png`).
     *
     * @param frame The frame index.
     */
    fun framePath(frame: Int): String {
        val extensionIndex = filePath.lastIndexOf('.')

        return if (extensionIndex < 0) filePath + frame
        else filePath.substring(0, extensionIndex) + frame + filePath.substring(extensionIndex)
    }
}
