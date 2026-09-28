package com.osudroid.storyboard.model.commands

import com.osudroid.storyboard.model.StoryboardColor

/**
 * Groups the [CommandTimeline]s of all animatable properties of a storyboard element.
 *
 * [StoryboardLoop]s and [StoryboardTrigger]s are themselves command groups whose command times are
 * relative to their own start time.
 */
open class StoryboardCommandGroup {
    /**
     * The timeline of the X coordinate, fed by `M` and `MX` commands.
     */
    val x = CommandTimeline<Float>()

    /**
     * The timeline of the Y coordinate, fed by `M` and `MY` commands.
     */
    val y = CommandTimeline<Float>()

    /**
     * The timeline of the uniform scale, fed by `S` commands.
     */
    val scale = CommandTimeline<Float>()

    /**
     * The timeline of the horizontal vector scale, fed by `V` commands.
     */
    val vectorScaleX = CommandTimeline<Float>()

    /**
     * The timeline of the vertical vector scale, fed by `V` commands.
     */
    val vectorScaleY = CommandTimeline<Float>()

    /**
     * The timeline of the rotation in radians, fed by `R` commands.
     */
    val rotation = CommandTimeline<Float>()

    /**
     * The timeline of the opacity, fed by `F` commands.
     */
    val alpha = CommandTimeline<Float>()

    /**
     * The timeline of the color tint, fed by `C` commands.
     */
    val color = CommandTimeline<StoryboardColor>()

    /**
     * The timeline of the horizontal flip, fed by `P` commands with the `H` parameter.
     */
    val flipHorizontal = CommandTimeline<Boolean>()

    /**
     * The timeline of the vertical flip, fed by `P` commands with the `V` parameter.
     */
    val flipVertical = CommandTimeline<Boolean>()

    /**
     * The timeline of the additive blending mode, fed by `P` commands with the `A` parameter.
     */
    val additiveBlend = CommandTimeline<Boolean>()

    /**
     * All timelines of this group.
     */
    val timelines = arrayOf<CommandTimeline<*>>(
        x, y, scale, vectorScaleX, vectorScaleY, rotation, alpha, color,
        flipHorizontal, flipVertical, additiveBlend
    )

    /**
     * The loops of this group. Only present on the top-level group of an element.
     */
    val loops = mutableListOf<StoryboardLoop>()

    /**
     * The triggers of this group. Only present on the top-level group of an element.
     */
    val triggers = mutableListOf<StoryboardTrigger>()

    /**
     * Whether any timeline of this group contains commands.
     */
    val hasCommands
        get() = timelines.any { it.hasCommands }

    /**
     * The earliest command start time across all timelines of this group, not accounting for
     * loops and triggers. [Double.MAX_VALUE] if there are no commands.
     */
    val commandsStartTime
        get() = timelines.minOf { it.startTime }

    /**
     * The latest command end time across all timelines of this group, not accounting for loops
     * and triggers. [-Double.MAX_VALUE][Double.MAX_VALUE] if there are no commands.
     */
    val commandsEndTime
        get() = timelines.maxOf { it.endTime }
}
