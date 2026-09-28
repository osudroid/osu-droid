package com.osudroid.storyboard.model.commands

/**
 * Represents a storyboard `L` (loop) command.
 *
 * The command times of this group are relative to [loopStartTime]. Every command repeats with a
 * period of [iterationDuration], which spans from the start of the earliest command to the end of
 * the latest command of the group, as in osu!lazer.
 */
class StoryboardLoop(
    /**
     * The absolute start time of the loop in milliseconds.
     */
    @JvmField
    val loopStartTime: Double,

    /**
     * The total number of times this loop plays. At least 1.
     */
    @JvmField
    val totalIterations: Int
) : StoryboardCommandGroup() {
    /**
     * The duration of a single iteration in milliseconds.
     */
    val iterationDuration
        get() = if (hasCommands) maxOf(commandsEndTime - commandsStartTime, 0.0) else 0.0

    /**
     * The absolute start time of the earliest command of the first iteration.
     */
    val startTime
        get() = loopStartTime + (if (hasCommands) commandsStartTime else 0.0)

    /**
     * The absolute end time of the last iteration.
     */
    val endTime
        get() = startTime + iterationDuration * totalIterations
}
