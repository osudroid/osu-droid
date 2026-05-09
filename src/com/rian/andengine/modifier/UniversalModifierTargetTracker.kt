package com.rian.andengine.modifier

import com.reco1l.andengine.component.UIComponent

/**
 * Tracks the lifetime of [UniversalModifier]s for one specified target member of a [UIComponent].
 */
class UniversalModifierTargetTracker(
    /**
     * The target member.
     */
    val targetMember: String?,

    /**
     * The [UIComponent] whose [UniversalModifier]s are being tracked.
     */
    private val component: UIComponent
) {
    private var modifierId = 0L
    private val _modifiers = mutableListOf<UniversalModifier>()

    /**
     * The [UniversalModifier]s that are currently active on this [targetMember]. This list is ordered by the time at
     * which the [UniversalModifier]s are applied.
     */
    val modifiers: List<UniversalModifier>
        get() = _modifiers

    /**
     * Adds a [UniversalModifier] to this [UniversalModifierTargetTracker].
     *
     * @param modifier The [UniversalModifier] to add.
     * @param customModifierId When not `null`, the [UniversalModifier.id] to assign for ordering.
     */
    @JvmOverloads
    fun add(modifier: UniversalModifier, customModifierId: Long? = null) {
        if (customModifierId == 0L) {
            throw IllegalArgumentException("customModifierId cannot be 0.")
        }

        if (modifier.type.targetMember != targetMember) {
            throw IllegalArgumentException(
                "Target member ${modifier.type.targetMember} does not match tracker target member $targetMember."
            )
        }

        modifier.id = customModifierId ?: ++modifierId

        var index = _modifiers.binarySearch(modifier)

        if (index < 0) {
            index = -index - 1
        }

        _modifiers.add(index, modifier)

        // If the new modifier could have an immediate effect, make the effect happen immediately.
        val time = component.time

        if (time != null && modifier.startTime <= time.current) {
            if (index > 0) {
                val prevModifier = modifiers[index - 1]

                if (!prevModifier.appliedToEnd) {
                    prevModifier.apply(modifier.startTime)
                }
            }

            modifier.apply(time.current)
        }
    }

    /**
     * Removes a [UniversalModifier] from this [UniversalModifierTargetTracker].
     *
     * @param modifier The [UniversalModifier] to remove.
     * @return Whether [modifier] was removed successfully. If so,
     */
    fun remove(modifier: UniversalModifier): Boolean {
        val removed = _modifiers.remove(modifier)

        if (removed) {
            modifier.release()
        }

        return removed
    }

    /**
     * Removes all [UniversalModifier]s that start at or after the specified time from this
     * [UniversalModifierTargetTracker].
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     */
    fun clearAfter(time: Float) {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            if (modifier.startTime < time) {
                break
            }

            modifier.release()
            _modifiers.removeAt(i)
        }
    }

    /**
     * Removes all [UniversalModifier]s with the given [ModifierType] that start at or after the specified time from
     * this [UniversalModifierTargetTracker].
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     * @param type The [ModifierType] to remove.
     */
    fun clearAfter(time: Float, type: ModifierType) {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            if (modifier.startTime < time) {
                break
            }

            if (modifier.type == type) {
                modifier.release()
                _modifiers.removeAt(i)
            }
        }
    }

    /**
     * Removes all [UniversalModifier]s with the given [ModifierType]s that start at or after the specified time from
     * this [UniversalModifierTargetTracker].
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     * @param types The types of [ModifierType] to remove.
     */
    fun clearAfter(time: Float, vararg types: ModifierType) {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            if (modifier.startTime < time) {
                break
            }

            if (modifier.type in types) {
                modifier.release()
                _modifiers.removeAt(i)
            }
        }
    }

    /**
     * Removes all [UniversalModifier]s from this [UniversalModifierTargetTracker].
     */
    fun clear() {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            modifier.release()
            _modifiers.removeAt(i)
        }
    }

    /**
     * Removes all [UniversalModifier]s with the specified [ModifierType] from this [UniversalModifierTargetTracker].
     *
     * Unlike the vararg variant, this method avoids array allocation in cases where there is only one [ModifierType] to
     * remove.
     *
     * @param type The [ModifierType] to remove.
     */
    fun clear(type: ModifierType) {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            if (modifier.type == type) {
                modifier.release()
                _modifiers.removeAt(i)
            }
        }
    }

    /**
     * Removes all [UniversalModifier]s with the specified [ModifierType]s from this [UniversalModifierTargetTracker].
     *
     * @param types The types of [ModifierType] to remove.
     */
    fun clear(vararg types: ModifierType) {
        for (i in modifiers.size - 1 downTo 0) {
            val modifier = modifiers[i]

            if (modifier.type in types) {
                modifier.release()
                _modifiers.removeAt(i)
            }
        }
    }

    /**
     * Finishes all [UniversalModifier]s in this [UniversalModifierTargetTracker], applying them until the end and then
     * removing them.
     */
    fun finish() {
        while (modifiers.isNotEmpty()) {
            // We remove at 0 to process the elements in chronological order.
            // If a callback adds a new modifier, it will be placed according to its startTime.
            // This ensures the entire chain is finished until the tracker is truly empty.
            val modifier = _modifiers.removeAt(0)

            modifier.apply(modifier.endTime)
            modifier.onFinished?.invoke(component)
            modifier.release()
        }
    }

    /**
     * Updates the [UniversalModifier]s in this [UniversalModifierTargetTracker] based on the provided time.
     *
     * @param time The time.
     */
    fun update(time: Float) {
        val modifiers = _modifiers
        val modifierCount = modifiers.size

        // First pass: Apply modifiers and handle state hand-offs.
        // We use a fixed count to avoid processing modifiers added during this frame.
        for (i in 0 until modifierCount) {
            val modifier = modifiers[i]

            if (time < modifier.startTime) {
                break
            }

            if (!modifier.applied && i > 0) {
                // This is the first time we are applying the current modifier.
                // To avoid conflicts with the previously active modifier, we mark that modifier as removable.
                val prevModifier = modifiers[i - 1]

                if (!prevModifier.appliedToEnd) {
                    // We may have applied the previous modifier too far into the future. To avoid the current modifier
                    // from reading into potentially incorrect initial values, we reapply the previous modifier using
                    // the current modifier's start time as a basis.
                    prevModifier.apply(modifier.startTime)
                }
            }

            if (!modifier.appliedToEnd) {
                modifier.apply(time)
            }
        }

        // Second pass: Remove finished modifiers and invoke callbacks.
        // We iterate forward using a while loop to safely handle removals and ensure chronological callbacks.
        var i = 0

        while (i < modifiers.size) {
            val modifier = modifiers[i]

            // We must not process modifiers that have not started yet.
            // This also prevents processing re-entrant additions if they start in the future.
            if (time < modifier.startTime) {
                break
            }

            if (modifier.appliedToEnd) {
                modifiers.removeAt(i)
                modifier.onFinished?.invoke(component)
                modifier.release()
                // Do not increment i, next modifier is now at current index.
            } else {
                i++
            }
        }
    }
}