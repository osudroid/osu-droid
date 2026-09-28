package com.osudroid.storyboard.playback

import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardSample
import com.osudroid.storyboard.model.commands.StoryboardTriggerType

/**
 * Drives the playback of a [Storyboard].
 *
 * Playback is deterministic: [update] evaluates the state of every active sprite at the given
 * time, and seeking in either direction is supported. Trigger activations are the only stateful
 * part and are reset when a backward seek is detected.
 *
 * To keep per-frame costs proportional to the number of *currently visible* sprites rather than
 * the total sprite count (heavy storyboards contain tens of thousands of elements), each layer
 * maintains an active window: sprites sorted by display start time are swept in as time advances
 * and pruned once their end time passes. Backward seeks rebuild the window from scratch.
 */
class StoryboardPlayback(
    /**
     * The storyboard to play back.
     */
    @JvmField
    val storyboard: Storyboard
) {
    /**
     * The compiled sprites of this storyboard, grouped per layer in declaration order.
     */
    @JvmField
    val layers = LinkedHashMap<StoryboardLayerType, List<PlayableSprite>>().also {
        for ((layer, elements) in storyboard.layers) {
            it[layer] = elements.mapIndexed { index, element -> PlayableSprite(element, index) }
        }
    }

    private val layerStates = layers.mapValues { (_, sprites) -> LayerState(sprites) }

    /**
     * All sprites that have triggers, across all layers.
     */
    private val spritesWithTriggers = layers.values.flatten().filter { it.element.commands.triggers.isNotEmpty() }

    /**
     * Whether any sprite of this storyboard has hit sound triggers. When `false`, hit sample
     * notifications can be skipped entirely.
     */
    @JvmField
    val hasHitSoundTriggers = spritesWithTriggers.any { it.hasHitSoundTriggers }

    /**
     * Whether any sprite of this storyboard has hit object hit triggers.
     */
    @JvmField
    val hasHitObjectHitTriggers = spritesWithTriggers.any { it.hasHitObjectHitTriggers }

    /**
     * The samples of this storyboard, sorted by time.
     */
    private val samples = storyboard.samples.sortedBy { it.time }

    private var nextSampleIndex = 0

    /**
     * Invoked when a sample of this storyboard should be played.
     */
    var onSamplePlayed: ((StoryboardSample) -> Unit)? = null

    /**
     * The earliest point in time at which a sprite becomes visible or a sample is played, in
     * milliseconds, or `null` if this storyboard has neither.
     *
     * Storyboards use events in negative time to display an intro before the audio starts.
     */
    @JvmField
    val earliestEventTime: Double? = run {
        var earliest = Double.MAX_VALUE

        for (sprites in layers.values) {
            for (sprite in sprites) {
                if (sprite.hasOwnCommands) {
                    earliest = minOf(earliest, sprite.ownDisplayStartTime)
                }
            }
        }

        if (samples.isNotEmpty()) {
            earliest = minOf(earliest, samples[0].time)
        }

        if (earliest == Double.MAX_VALUE) null else earliest
    }

    /**
     * Whether the player is currently in a passing state, controlling the visibility of the
     * [Pass][StoryboardLayerType.Pass] and [Fail][StoryboardLayerType.Fail] layers.
     */
    var isPassing = true
        private set

    /**
     * The time of the last [update] call in milliseconds.
     */
    var currentTime = -Double.MAX_VALUE
        private set

    /**
     * The sprites of the given layer that are active at [currentTime], in declaration (draw)
     * order.
     *
     * @param layer The layer to get the active sprites of.
     */
    fun activeSprites(layer: StoryboardLayerType): List<PlayableSprite> =
        layerStates[layer]?.active ?: emptyList()

    /**
     * Advances the playback time and updates the active sprite windows without evaluating sprite
     * states. This is cheap and safe to call from the update thread every tick - the actual
     * (expensive) state evaluation happens per drawn sprite in the renderer, so that heavy
     * storyboards can never stall the gameplay clock.
     *
     * @param time The time in milliseconds.
     */
    fun setTime(time: Double) {
        if (time < currentTime) {
            // Backward seeks invalidate all trigger activations and rebuild the active windows
            // to keep playback deterministic.
            for (sprite in spritesWithTriggers) {
                sprite.resetActivations()
            }

            for (state in layerStates.values) {
                state.reset()
            }

            nextSampleIndex = samples.indexOfFirst { it.time >= time }.let { if (it < 0) samples.size else it }
        }

        currentTime = time

        for (state in layerStates.values) {
            state.sweep(time)
        }

        while (nextSampleIndex < samples.size && samples[nextSampleIndex].time <= time) {
            val sample = samples[nextSampleIndex++]

            // Samples that are passed by a large margin (e.g. when skipping or seeking) are not
            // played, to avoid layering all of them at once.
            if (time - sample.time < SAMPLE_ALLOWABLE_LATE_START && isLayerVisible(sample.layer)) {
                onSamplePlayed?.invoke(sample)
            }
        }
    }

    /**
     * Resets this playback to its initial state, so that it can be played back from the start.
     */
    fun reset() {
        for (sprite in spritesWithTriggers) {
            sprite.resetActivations()
        }

        for (state in layerStates.values) {
            state.reset()
        }

        nextSampleIndex = 0
        isPassing = true
        currentTime = -Double.MAX_VALUE
    }

    /**
     * Advances the playback time and evaluates the state of all active sprites on visible
     * layers. Prefer [setTime] plus per-sprite evaluation during drawing in production - this is
     * primarily a convenience for tests.
     *
     * @param time The time in milliseconds.
     */
    fun update(time: Double) {
        setTime(time)

        for ((layer, state) in layerStates) {
            if (!isLayerVisible(layer)) {
                continue
            }

            val active = state.active

            for (i in active.indices) {
                active[i].update(time)
            }
        }
    }

    /**
     * Whether the given layer is currently visible.
     *
     * @param layer The layer to check.
     */
    fun isLayerVisible(layer: StoryboardLayerType) = when (layer) {
        StoryboardLayerType.Pass -> isPassing
        StoryboardLayerType.Fail -> !isPassing
        else -> true
    }

    /**
     * Notifies this playback that the hit samples of a single hit object were played, activating
     * matching hit sound triggers whose window contains the given time.
     *
     * The full set of samples played for the hit object must be passed together, since a trigger
     * (e.g. `HitSoundNormalSoftClap`) can require a bank on the normal sample and a different bank
     * on an addition sample at the same time.
     *
     * @param samples The samples played for a single hit object.
     * @param time The time the samples were played at, in milliseconds.
     */
    fun onHitSound(samples: List<BankHitSampleInfo>, time: Double) {
        if (!hasHitSoundTriggers) {
            return
        }

        for (sprite in spritesWithTriggers) {
            for (trigger in sprite.element.commands.triggers) {
                val type = trigger.type as? StoryboardTriggerType.HitSound ?: continue

                if (time in trigger.triggerStartTime..trigger.triggerEndTime && type.matches(samples)) {
                    sprite.activate(trigger, time)
                }
            }
        }
    }

    /**
     * Notifies this playback that a hit object was hit, activating hit object hit triggers whose
     * window contains the given time.
     *
     * @param time The time the hit object was hit at, in milliseconds.
     */
    fun onHitObjectHit(time: Double) {
        if (!hasHitObjectHitTriggers) {
            return
        }

        for (sprite in spritesWithTriggers) {
            for (trigger in sprite.element.commands.triggers) {
                if (trigger.type === StoryboardTriggerType.HitObjectHit &&
                    time in trigger.triggerStartTime..trigger.triggerEndTime) {
                    sprite.activate(trigger, time)
                }
            }
        }
    }

    /**
     * Updates the passing state, activating `Passing`/`Failing` triggers whose window contains
     * the given time when the state changes.
     *
     * @param passing Whether the player is in a passing state.
     * @param time The time of the state change in milliseconds.
     */
    fun setPassing(passing: Boolean, time: Double) {
        if (isPassing == passing) {
            return
        }

        isPassing = passing

        for (sprite in spritesWithTriggers) {
            for (trigger in sprite.element.commands.triggers) {
                val matches = when (trigger.type) {
                    is StoryboardTriggerType.Passing -> passing
                    is StoryboardTriggerType.Failing -> !passing
                    else -> false
                }

                if (matches && time in trigger.triggerStartTime..trigger.triggerEndTime) {
                    sprite.activate(trigger, time)
                }
            }
        }
    }

    /**
     * The active sprite window of a layer.
     */
    private class LayerState(sprites: List<PlayableSprite>) {
        /**
         * The sprites of the layer sorted by display start time, used to sweep sprites into the
         * active window as time advances.
         */
        private val spritesByStartTime = sprites.sortedBy { it.displayStartTime }

        /**
         * The sprites that are active at the current time, in declaration (draw) order.
         */
        val active = ArrayList<PlayableSprite>()

        private var nextIndex = 0

        fun reset() {
            nextIndex = 0
            active.clear()
        }

        fun sweep(time: Double) {
            while (nextIndex < spritesByStartTime.size &&
                spritesByStartTime[nextIndex].displayStartTime <= time) {
                val sprite = spritesByStartTime[nextIndex]
                ++nextIndex

                if (time <= sprite.endTime) {
                    insert(sprite)
                }
            }

            var removed = 0

            for (i in active.indices) {
                val sprite = active[i]

                if (time > sprite.endTime) {
                    ++removed
                } else if (removed > 0) {
                    active[i - removed] = sprite
                }
            }

            if (removed > 0) {
                active.subList(active.size - removed, active.size).clear()
            }
        }

        /**
         * Inserts a sprite into [active], keeping the list sorted by declaration index so that
         * the draw order matches the file order.
         */
        private fun insert(sprite: PlayableSprite) {
            var low = 0
            var high = active.size

            while (low < high) {
                val mid = (low + high) ushr 1

                if (active[mid].declarationIndex < sprite.declarationIndex) {
                    low = mid + 1
                } else {
                    high = mid
                }
            }

            active.add(low, sprite)
        }
    }

    companion object {
        /**
         * The amount of time in milliseconds beyond the start time of a sample within which the
         * sample is still played.
         */
        private const val SAMPLE_ALLOWABLE_LATE_START = 100.0
    }
}
