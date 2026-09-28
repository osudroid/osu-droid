package com.osudroid.storyboard.model.commands

import com.osudroid.storyboard.model.StoryboardEasing

/**
 * An ordered list of [StoryboardCommand]s that target the same property of a storyboard element.
 *
 * Commands are kept sorted by their start time so that the value of the property at any point in
 * time can be resolved with a binary search.
 */
class CommandTimeline<T> : Iterable<StoryboardCommand<T>> {
    private val commands = mutableListOf<StoryboardCommand<T>>()

    /**
     * Whether this timeline contains any commands.
     */
    val hasCommands
        get() = commands.isNotEmpty()

    /**
     * The number of commands in this timeline.
     */
    val size
        get() = commands.size

    /**
     * The earliest start time of the commands in this timeline.
     */
    val startTime
        get() = if (commands.isEmpty()) Double.MAX_VALUE else commands.first().startTime

    /**
     * The latest end time of the commands in this timeline.
     */
    var endTime = -Double.MAX_VALUE
        private set

    /**
     * The start value of the first command in this timeline.
     */
    val startValue: T?
        get() = commands.firstOrNull()?.startValue

    operator fun get(index: Int) = commands[index]

    override fun iterator() = commands.iterator()

    /**
     * Adds a command to this timeline, keeping the command list sorted by start time.
     *
     * @param easing The easing of the command.
     * @param startTime The start time of the command in milliseconds.
     * @param endTime The end time of the command in milliseconds.
     * @param startValue The value at the start of the command.
     * @param endValue The value at the end of the command.
     */
    fun add(easing: StoryboardEasing, startTime: Double, endTime: Double, startValue: T, endValue: T) =
        add(StoryboardCommand(easing, startTime, endTime, startValue, endValue))

    /**
     * Adds a command to this timeline, keeping the command list sorted by start time.
     *
     * @param command The command to add.
     */
    fun add(command: StoryboardCommand<T>) {
        // Commands mostly arrive in chronological order, so search from the back.
        var index = commands.size

        while (index > 0 && commands[index - 1].startTime > command.startTime) {
            --index
        }

        commands.add(index, command)
        endTime = maxOf(endTime, command.endTime)
    }

    /**
     * Finds the index of the last command whose start time is less than or equal to [time] via
     * binary search.
     *
     * @param time The time in milliseconds.
     * @return The index of the command, or `-1` if [time] is before the first command.
     */
    fun indexAt(time: Double): Int {
        var low = 0
        var high = commands.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) ushr 1

            if (commands[mid].startTime <= time) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return result
    }
}
