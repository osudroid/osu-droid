package com.osudroid.storyboard.playback

import android.util.Log
import com.osudroid.storyboard.model.AnimationLoopType
import com.osudroid.storyboard.model.StoryboardAnimation
import com.osudroid.storyboard.model.StoryboardColor
import com.osudroid.storyboard.model.StoryboardElement
import com.osudroid.storyboard.model.commands.CommandTimeline
import com.osudroid.storyboard.model.commands.StoryboardCommandGroup
import com.osudroid.storyboard.model.commands.StoryboardTrigger
import com.osudroid.storyboard.model.commands.StoryboardTriggerType
import kotlin.math.min

/**
 * A [StoryboardElement] compiled for playback.
 *
 * Loops are unrolled into flat, per-property command timelines at construction time, so that the
 * state of the sprite at any point in time can be resolved deterministically with binary searches.
 * This makes seeking in both directions trivial. Triggers are the only stateful part - their
 * activations are tracked in [activations] and reset by [StoryboardPlayback] on backward seeks.
 */
class PlayableSprite(
    /**
     * The element this sprite plays back.
     */
    @JvmField
    val element: StoryboardElement,

    /**
     * The declaration index of this sprite within its layer, which determines the draw order.
     */
    @JvmField
    val declarationIndex: Int = 0
) {
    private val timelines = EffectiveTimelines(element)

    /**
     * The active trigger activations of this sprite.
     */
    private val activations = mutableListOf<TriggerActivation>()

    /**
     * The time at which this sprite starts affecting the scene, in milliseconds.
     *
     * If the earliest alpha command starts with an alpha of zero, the sprite only becomes visible
     * at that command's start time.
     */
    @JvmField
    val displayStartTime: Double

    /**
     * The time at which this sprite stops affecting the scene, in milliseconds.
     */
    @JvmField
    val endTime: Double

    /**
     * Whether this sprite has hit sound triggers.
     */
    val hasHitSoundTriggers
        get() = element.commands.triggers.any { it.type is StoryboardTriggerType.HitSound }

    /**
     * Whether this sprite has hit object hit triggers.
     */
    val hasHitObjectHitTriggers
        get() = element.commands.triggers.any { it.type === StoryboardTriggerType.HitObjectHit }

    // The evaluated state of this sprite at the time of the last update() call.
    @JvmField var x = element.initialX
    @JvmField var y = element.initialY
    @JvmField var scaleX = 1f
    @JvmField var scaleY = 1f
    @JvmField var rotation = 0f
    @JvmField var alpha = 1f
    @JvmField var red = 1f
    @JvmField var green = 1f
    @JvmField var blue = 1f
    @JvmField var flipHorizontal = false
    @JvmField var flipVertical = false
    @JvmField var additiveBlend = false
    @JvmField var frameIndex = 0

    /**
     * Whether this sprite is displayed at the time of the last [update] call.
     *
     * A sprite is displayed during the lifetime of its own commands and while a trigger
     * activation is running. Outside of that (e.g. a sprite that only has trigger commands and has
     * not been triggered yet), it is hidden.
     */
    @JvmField var isVisible = true

    /**
     * Whether the element has commands of its own (including loops), not accounting for triggers.
     */
    @JvmField
    val hasOwnCommands: Boolean

    /**
     * The time at which the element becomes visible by its own commands, in milliseconds. Only
     * meaningful if [hasOwnCommands] is `true`.
     */
    @JvmField
    val ownDisplayStartTime: Double

    private val ownStartTime: Double
    private val ownEndTime: Double

    init {
        ownStartTime = timelines.startTime
        ownEndTime = timelines.endTime
        hasOwnCommands = ownStartTime <= ownEndTime

        // If the sprite starts out invisible, it is displayed from the first alpha command that
        // makes it visible, as in osu!lazer.
        var displayStart = ownStartTime

        if (timelines.alpha.startValue == 0f) {
            for (command in timelines.alpha) {
                if (command.startValue > 0f || command.endValue > 0f) {
                    displayStart = command.startTime
                    break
                }
            }
        }

        ownDisplayStartTime = displayStart

        var start = ownDisplayStartTime
        var end = ownEndTime

        // The active window must cover the trigger windows so that the sprite can be displayed
        // once a trigger activates.
        for (trigger in element.commands.triggers) {
            if (trigger.type === StoryboardTriggerType.Unsupported || !trigger.hasCommands) {
                continue
            }

            start = min(start, trigger.triggerStartTime)
            end = maxOf(end, trigger.triggerEndTime + maxOf(trigger.commandsEndTime, 0.0))
        }

        displayStartTime = start
        endTime = end
    }

    /**
     * Whether this sprite affects the scene at the given time.
     *
     * @param time The time in milliseconds.
     */
    fun isActive(time: Double) = time in displayStartTime..endTime

    /**
     * Activates a trigger at the given time. A running activation of the same trigger or of the
     * same non-zero group number is canceled, matching osu!stable.
     *
     * @param trigger The trigger to activate.
     * @param time The activation time in milliseconds.
     */
    fun activate(trigger: StoryboardTrigger, time: Double) {
        if (!trigger.hasCommands) {
            return
        }

        activations.removeAll {
            it.trigger === trigger ||
                (trigger.groupNumber != 0 && it.trigger.groupNumber == trigger.groupNumber)
        }

        activations.add(TriggerActivation(trigger, time))
    }

    /**
     * Removes all trigger activations. Called on backward seeks to keep playback deterministic.
     */
    fun resetActivations() = activations.clear()

    /**
     * Evaluates the state of this sprite at the given time.
     *
     * @param time The time in milliseconds.
     */
    fun update(time: Double) {
        x = evaluate(timelines.x, time, element.initialX)
        y = evaluate(timelines.y, time, element.initialY)

        val scale = evaluate(timelines.scale, time, 1f)
        scaleX = scale * evaluate(timelines.vectorScaleX, time, 1f)
        scaleY = scale * evaluate(timelines.vectorScaleY, time, 1f)

        rotation = evaluate(timelines.rotation, time, 0f)
        alpha = evaluate(timelines.alpha, time, 1f)

        evaluateColorInto(timelines.color, time)

        flipHorizontal = evaluateBoolean(timelines.flipHorizontal, time)
        flipVertical = evaluateBoolean(timelines.flipVertical, time)
        additiveBlend = evaluateBoolean(timelines.additiveBlend, time)

        isVisible = hasOwnCommands && time >= ownDisplayStartTime && time <= ownEndTime

        applyActivations(time)

        // In stable, alpha values exceeding 1 wrap around and make the sprite disappear.
        // Storyboarders exploit this for flicker effects, so it is reproduced here (as in lazer).
        if (alpha > 1f) {
            alpha %= 1f
        }

        val animation = element as? StoryboardAnimation
        if (animation != null) {
            frameIndex = animationFrameAt(animation, time)
        }
    }

    private fun applyActivations(time: Double) {
        // Fast path for the vast majority of sprites, which have no trigger activations. This
        // also avoids allocating the removeAll lambda in the per-frame update.
        if (activations.isEmpty()) {
            return
        }

        activations.removeAll { time < it.time }

        for (activation in activations) {
            val group = activation.trigger
            val relativeTime = time - activation.time

            if (relativeTime >= activation.commandsStartTime && relativeTime <= activation.commandsEndTime) {
                isVisible = true
            }

            if (group.x.hasCommands) x = evaluate(group.x, relativeTime, x)
            if (group.y.hasCommands) y = evaluate(group.y, relativeTime, y)

            if (group.scale.hasCommands || group.vectorScaleX.hasCommands || group.vectorScaleY.hasCommands) {
                val scale = evaluate(group.scale, relativeTime, 1f)
                scaleX = scale * evaluate(group.vectorScaleX, relativeTime, 1f)
                scaleY = scale * evaluate(group.vectorScaleY, relativeTime, 1f)
            }

            if (group.rotation.hasCommands) rotation = evaluate(group.rotation, relativeTime, rotation)
            if (group.alpha.hasCommands) alpha = evaluate(group.alpha, relativeTime, alpha)

            if (group.color.hasCommands) {
                evaluateColorInto(group.color, relativeTime)
            }

            if (group.flipHorizontal.hasCommands) flipHorizontal = evaluateBoolean(group.flipHorizontal, relativeTime)
            if (group.flipVertical.hasCommands) flipVertical = evaluateBoolean(group.flipVertical, relativeTime)
            if (group.additiveBlend.hasCommands) additiveBlend = evaluateBoolean(group.additiveBlend, relativeTime)
        }
    }

    private fun animationFrameAt(animation: StoryboardAnimation, time: Double): Int {
        if (animation.frameCount <= 1 || animation.frameDelay <= 0) {
            return 0
        }

        // Playback starts at the earliest command of the animation rather than at the time it
        // becomes visible, matching osu!lazer.
        val startTime = if (hasOwnCommands) ownStartTime else displayStartTime
        val frame = ((time - startTime) / animation.frameDelay).toInt()

        return when (animation.loopType) {
            AnimationLoopType.LoopOnce -> frame.coerceIn(0, animation.frameCount - 1)
            AnimationLoopType.LoopForever -> ((frame % animation.frameCount) + animation.frameCount) % animation.frameCount
        }
    }

    private fun evaluate(timeline: CommandTimeline<Float>, time: Double, initialValue: Float): Float {
        if (!timeline.hasCommands) {
            return initialValue
        }

        val index = timeline.indexAt(time)

        if (index < 0) {
            // Before the first command, the property takes on the first command's start value.
            return timeline.startValue ?: initialValue
        }

        val command = timeline[index]
        val progress = command.progressAt(time).toFloat()

        return command.startValue + (command.endValue - command.startValue) * progress
    }

    /**
     * Evaluates a color timeline directly into [red], [green] and [blue] to avoid allocating a
     * color object per sprite per frame.
     */
    private fun evaluateColorInto(timeline: CommandTimeline<StoryboardColor>, time: Double) {
        if (!timeline.hasCommands) {
            red = 1f
            green = 1f
            blue = 1f
            return
        }

        val index = timeline.indexAt(time)

        if (index < 0) {
            val first = timeline.startValue
            red = first?.red ?: 1f
            green = first?.green ?: 1f
            blue = first?.blue ?: 1f
            return
        }

        val command = timeline[index]
        val progress = command.progressAt(time).toFloat()
        val start = command.startValue
        val end = command.endValue

        red = start.red + (end.red - start.red) * progress
        green = start.green + (end.green - start.green) * progress
        blue = start.blue + (end.blue - start.blue) * progress
    }

    private fun evaluateBoolean(timeline: CommandTimeline<Boolean>, time: Double): Boolean {
        if (!timeline.hasCommands) {
            return false
        }

        val index = timeline.indexAt(time)

        if (index < 0) {
            // Boolean properties default to false before their first command.
            return false
        }

        val command = timeline[index]

        // A P command is active during its interval. Zero-duration commands apply permanently,
        // which the parser encodes as an end value of `true`.
        return if (time < command.endTime) command.startValue else command.endValue
    }

    private class TriggerActivation(
        @JvmField val trigger: StoryboardTrigger,
        @JvmField val time: Double
    ) {
        // Relative to the activation time.
        @JvmField val commandsStartTime = trigger.commandsStartTime
        @JvmField val commandsEndTime = trigger.commandsEndTime
    }

    /**
     * The flat command timelines of an element, with all loops unrolled into absolute time.
     */
    private class EffectiveTimelines(element: StoryboardElement) {
        val x = CommandTimeline<Float>()
        val y = CommandTimeline<Float>()
        val scale = CommandTimeline<Float>()
        val vectorScaleX = CommandTimeline<Float>()
        val vectorScaleY = CommandTimeline<Float>()
        val rotation = CommandTimeline<Float>()
        val alpha = CommandTimeline<Float>()
        val color = CommandTimeline<StoryboardColor>()
        val flipHorizontal = CommandTimeline<Boolean>()
        val flipVertical = CommandTimeline<Boolean>()
        val additiveBlend = CommandTimeline<Boolean>()

        private val all = arrayOf<CommandTimeline<*>>(
            x, y, scale, vectorScaleX, vectorScaleY, rotation, alpha, color,
            flipHorizontal, flipVertical, additiveBlend
        )

        val startTime
            get() = all.minOf { it.startTime }

        val endTime
            get() = all.maxOf { it.endTime }

        init {
            copyGroup(element.commands, 0.0)

            for (loop in element.commands.loops) {
                val commandsPerIteration = loop.timelines.sumOf { it.size }

                if (commandsPerIteration == 0) {
                    continue
                }

                // Guard against pathological storyboards whose unrolled loops would exhaust
                // memory. Excess iterations are dropped with a warning.
                // A loop without a duration plays back once, as in osu!lazer.
                val totalIterations = if (loop.iterationDuration > 0) loop.totalIterations else 1
                val iterations = min(totalIterations, MAX_UNROLLED_COMMANDS / commandsPerIteration)

                if (iterations < loop.totalIterations) {
                    Log.w(
                        "PlayableSprite",
                        "Loop of ${element.filePath} exceeds $MAX_UNROLLED_COMMANDS unrolled commands, " +
                            "dropping ${loop.totalIterations - iterations} iterations"
                    )
                }

                for (i in 0 until iterations) {
                    copyGroup(loop, loop.loopStartTime + i * loop.iterationDuration)
                }
            }
        }

        private fun copyGroup(group: StoryboardCommandGroup, offset: Double) {
            copyTimeline(group.x, x, offset)
            copyTimeline(group.y, y, offset)
            copyTimeline(group.scale, scale, offset)
            copyTimeline(group.vectorScaleX, vectorScaleX, offset)
            copyTimeline(group.vectorScaleY, vectorScaleY, offset)
            copyTimeline(group.rotation, rotation, offset)
            copyTimeline(group.alpha, alpha, offset)
            copyTimeline(group.color, color, offset)
            copyTimeline(group.flipHorizontal, flipHorizontal, offset)
            copyTimeline(group.flipVertical, flipVertical, offset)
            copyTimeline(group.additiveBlend, additiveBlend, offset)
        }

        private fun <T> copyTimeline(source: CommandTimeline<T>, target: CommandTimeline<T>, offset: Double) {
            for (command in source) {
                target.add(if (offset == 0.0) command else command.shifted(offset))
            }
        }

        companion object {
            private const val MAX_UNROLLED_COMMANDS = 100_000
        }
    }
}
