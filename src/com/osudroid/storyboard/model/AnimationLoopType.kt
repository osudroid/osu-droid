package com.osudroid.storyboard.model

/**
 * Represents the loop behavior of a [StoryboardAnimation].
 */
enum class AnimationLoopType {
    /**
     * The animation repeats indefinitely.
     */
    LoopForever,

    /**
     * The animation plays once and remains on its last frame.
     */
    LoopOnce;

    companion object {
        /**
         * Parses a loop type from its name or numeric representation.
         *
         * Invalid values fall back to [LoopForever], matching osu!stable behavior.
         *
         * @param value The value to parse.
         * @return The parsed [AnimationLoopType].
         */
        @JvmStatic
        fun parse(value: String?) = when (value) {
            "1", "LoopOnce" -> LoopOnce
            else -> LoopForever
        }
    }
}
