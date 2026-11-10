package com.rian.andengine.modifier

import com.reco1l.andengine.component.UIComponent

/**
 * Tracks the lifetime of [UniversalModifier]s for one target grouping of a [UIComponent].
 *
 * Within a target grouping, a [UniversalModifier] overrides other [UniversalModifier]s of the same [ModifierType]:
 *
 * - When a [UniversalModifier] starts, earlier [UniversalModifier]s of the same [ModifierType] that are still running
 * are aborted.
 * - When a [UniversalModifier] is added, [UniversalModifier]s of the same [ModifierType] that are scheduled to start
 * after it are aborted.
 *
 * Any [UniversalModifier] that is removed before it completes (overridden, removed, or cleared) is aborted. It invokes
 * its [UniversalModifier.onAborted] callback instead of [UniversalModifier.onFinished]. A [UniversalModifier] only
 * counts as completed once its [UniversalModifier.onFinished] callback has been invoked.
 */
class UniversalModifierTargetTracker(
    /**
     * The target grouping.
     */
    val targetGrouping: String?,

    /**
     * The [UIComponent] whose [UniversalModifier]s are being tracked.
     */
    private val component: UIComponent
) {
    private var modifierId = 0L
    private val _modifiers = mutableListOf<UniversalModifier>()

    /**
     * [UniversalModifier]s that have been aborted but not yet released back to their pool.
     *
     * When a [UniversalModifier] is aborted by an override, releasing is deferred until the next [update] because pools
     * are LIFO. Releasing immediately would hand the aborted instance to the next [UniversalModifier] acquisition, while
     * a [UniversalModifierSequence] (or a caller holding the returned [UniversalModifier]) may still reference it (e.g.
     * to attach an [UniversalModifier.after] callback). Explicit removals release right away (see [releaseAborted]),
     * including any override-aborted [UniversalModifier]s that are still pending.
     */
    private val abortedModifiers = mutableListOf<UniversalModifier>()

    /**
     * The [UniversalModifier]s that are currently active on this [targetGrouping]. This list is ordered by the time at
     * which the [UniversalModifier]s are applied.
     */
    val modifiers: List<UniversalModifier>
        get() = _modifiers

    /**
     * Adds a [UniversalModifier] to this [UniversalModifierTargetTracker].
     *
     * [UniversalModifier]s of the same [ModifierType] that are scheduled to start after [modifier] are aborted.
     *
     * @param modifier The [UniversalModifier] to add.
     * @param customModifierId When not `null`, the [UniversalModifier.id] to assign for ordering.
     */
    @JvmOverloads
    fun add(modifier: UniversalModifier, customModifierId: Long? = null) {
        if (customModifierId == 0L) {
            throw IllegalArgumentException("customModifierId cannot be 0.")
        }

        if (modifier.type.targetGrouping != targetGrouping) {
            throw IllegalArgumentException(
                "Target grouping ${modifier.type.targetGrouping} does not match tracker target grouping $targetGrouping."
            )
        }

        modifier.id = customModifierId ?: ++modifierId

        var index = _modifiers.binarySearch(modifier)

        if (index < 0) {
            index = -index - 1
        }

        _modifiers.add(index, modifier)

        // Abort all following modifiers of the same type, as this modifier overrides them.
        var i = index + 1

        while (i < _modifiers.size) {
            if (_modifiers[i].type == modifier.type) {
                abortAt(i)
            } else {
                i++
            }
        }

        // If the new modifier could have an immediate effect, make the effect happen immediately.
        val time = component.time

        if (time != null && modifier.startTime <= time.current) {
            abortBefore(index)
            modifier.apply(time.current)
        }

        dispatchAborted()
    }

    /**
     * Removes a [UniversalModifier] from this [UniversalModifierTargetTracker].
     *
     * If the [UniversalModifier] is successfully removed, its [UniversalModifier.onAborted]
     * callback is invoked, and it is then released back to its pool.
     *
     * @param modifier The [UniversalModifier] to remove.
     * @return Whether [modifier] was removed successfully.
     */
    fun remove(modifier: UniversalModifier): Boolean {
        val index = _modifiers.indexOf(modifier)

        if (index < 0) {
            return false
        }

        abortAt(index)
        releaseAborted()

        return true
    }

    /**
     * Removes all [UniversalModifier]s that start at or after the specified time from this
     * [UniversalModifierTargetTracker].
     *
     * Removed [UniversalModifier]s are aborted (see [remove]).
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     */
    fun clearAfter(time: Float) = abortWhere(firstIndexStartingAtOrAfter(time)) { true }

    /**
     * Removes all [UniversalModifier]s with the given [ModifierType] that start at or after the specified time from
     * this [UniversalModifierTargetTracker].
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     * @param type The [ModifierType] to remove.
     */
    fun clearAfter(time: Float, type: ModifierType) =
        abortWhere(firstIndexStartingAtOrAfter(time)) { it.type == type }

    /**
     * Removes all [UniversalModifier]s with the given [ModifierType]s that start at or after the specified time from
     * this [UniversalModifierTargetTracker].
     *
     * @param time The time after which to remove [UniversalModifier]s, in seconds relative to [component]'s
     * [UIComponent.time].
     * @param types The types of [ModifierType] to remove.
     */
    fun clearAfter(time: Float, vararg types: ModifierType) =
        abortWhere(firstIndexStartingAtOrAfter(time)) { it.type in types }

    /**
     * Removes all [UniversalModifier]s from this [UniversalModifierTargetTracker].
     *
     * Removed [UniversalModifier]s are aborted (see [remove]).
     */
    fun clear() {
        // Move everything at once rather than removing one by one from the front of the list.
        for (i in _modifiers.indices) {
            abortedModifiers.add(_modifiers[i])
        }

        _modifiers.clear()
        releaseAborted()
    }

    /**
     * Removes all [UniversalModifier]s with the specified [ModifierType] from this [UniversalModifierTargetTracker].
     *
     * Unlike the vararg variant, this method avoids array allocation in cases where there is only one [ModifierType] to
     * remove.
     *
     * Removed [UniversalModifier]s are aborted (see [remove]).
     *
     * @param type The [ModifierType] to remove.
     */
    fun clear(type: ModifierType) = abortWhere { it.type == type }

    /**
     * Removes all [UniversalModifier]s with the specified [ModifierType]s from this [UniversalModifierTargetTracker].
     *
     * Removed [UniversalModifier]s are aborted (see [remove]).
     *
     * @param types The types of [ModifierType] to remove.
     */
    fun clear(vararg types: ModifierType) = abortWhere { it.type in types }

    /**
     * Finishes [UniversalModifier]s in this [UniversalModifierTargetTracker], applying them
     * until the end and then removing them.
     *
     * Only [UniversalModifier]s that are present when this method is called are finished. If a
     * [UniversalModifier.onFinished] callback adds one or more [UniversalModifier]s, they are
     * kept and played normally.
     *
     * @param type The [ModifierType] to finish, or `null` to finish all [UniversalModifier]s.
     */
    @JvmOverloads
    fun finish(type: ModifierType? = null) {
        if (modifiers.isEmpty()) {
            return
        }

        val toFinish = mutableListOf<UniversalModifier>()

        // Iterate in reverse so that removals do not shift the indices that are yet to be visited.
        for (i in _modifiers.size - 1 downTo 0) {
            if (type == null || _modifiers[i].type == type) {
                toFinish.add(_modifiers.removeAt(i))
            }
        }

        // Process in chronological order. If an onFinished callback adds a new modifier, it will be
        // placed according to its startTime and played normally.
        for (i in toFinish.size - 1 downTo 0) {
            val modifier = toFinish[i]

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
        releaseAborted()

        val modifiers = _modifiers

        // First pass: Apply modifiers and abort the ones they override.
        var i = 0

        while (i < modifiers.size) {
            var modifier = modifiers[i]

            if (time < modifier.startTime) {
                break
            }

            if (!modifier.applied) {
                // This is the first time we are applying the current modifier, which aborts earlier modifiers of
                // the same type.
                i = abortBefore(i)
                modifier = modifiers[i]
            }

            if (!modifier.appliedToEnd) {
                modifier.apply(time)
            }

            i++
        }

        // Invoke abort callbacks only after the first pass, as they may mutate the modifier list.
        dispatchAborted()

        // Second pass: Remove finished modifiers and invoke callbacks.
        // We iterate forward using a while loop to safely handle removals and ensure chronological callbacks.
        i = 0

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

    /**
     * Aborts still-running [UniversalModifier]s of the same [ModifierType] that precede the
     * [UniversalModifier] at [index], which is about to be applied for the first time.
     *
     * @param index The index of the [UniversalModifier] that is about to be applied for the first time.
     * @return The new index of that [UniversalModifier].
     */
    private fun abortBefore(index: Int): Int {
        val modifier = modifiers[index]
        var newIndex = index
        var i = 0

        while (i < newIndex) {
            val prevModifier = modifiers[i]

            if (prevModifier.type != modifier.type || prevModifier.appliedToEnd) {
                i++
                continue
            }

            // We may have applied the previous modifier too far into the future. To avoid the current modifier from
            // reading into potentially incorrect initial values, we reapply the previous modifier using the current
            // modifier's start time as a basis.
            prevModifier.apply(modifier.startTime)

            if (prevModifier.appliedToEnd) {
                // The previous modifier ends before the current one starts, so it completed rather than being
                // aborted. Leave it for its callback to be invoked.
                i++
                continue
            }

            abortAt(i)
            newIndex--
        }

        return newIndex
    }

    /**
     * Aborts [UniversalModifier]s from [fromIndex] onwards that match [predicate], invokes their
     * [UniversalModifier.onAborted] callbacks in chronological order, and then releases them back to their pool.
     */
    private inline fun abortWhere(fromIndex: Int = 0, predicate: (UniversalModifier) -> Boolean) {
        var i = fromIndex

        while (i < _modifiers.size) {
            if (predicate(_modifiers[i])) {
                abortAt(i)
            } else {
                i++
            }
        }

        releaseAborted()
    }

    /**
     * Finds the index of the first [UniversalModifier] that starts at or after [time].
     */
    private fun firstIndexStartingAtOrAfter(time: Float): Int {
        var i = _modifiers.size

        while (i > 0 && _modifiers[i - 1].startTime >= time) {
            i--
        }

        return i
    }

    /**
     * Moves the [UniversalModifier] at [index] to [abortedModifiers]. Its [UniversalModifier.onAborted] callback is
     * invoked by the next [dispatchAborted] call, which every aborting operation makes once it is done mutating
     * [_modifiers].
     */
    private fun abortAt(index: Int) {
        abortedModifiers.add(_modifiers.removeAt(index))
    }

    /**
     * Invokes the [UniversalModifier.onAborted] callbacks of [abortedModifiers] that have not been invoked yet.
     *
     * Callbacks may re-enter this [UniversalModifierTargetTracker], including aborting more [UniversalModifier]s and
     * calling this method again. Each callback is cleared before it is invoked so that it is invoked at most once.
     */
    private fun dispatchAborted() {
        var i = 0

        // abortedModifiers may grow while iterating if a callback aborts more modifiers.
        while (i < abortedModifiers.size) {
            val modifier = abortedModifiers[i++]
            val onAborted = modifier.onAborted ?: continue

            modifier.onAborted = null
            onAborted(component)
        }
    }

    /**
     * Invokes any pending [UniversalModifier.onAborted] callbacks, then releases all [abortedModifiers] back to their
     * pool.
     *
     * This is called at the start of [update] for [UniversalModifier]s aborted by an override, and right away by
     * explicit removals ([remove], [clear], [clearAfter]).
     *
     * Explicit removals do not need to defer and must not do so. A [UIComponent] is commonly cleared and then detached,
     * after which it is no longer updated, so deferred releases would keep its [UniversalModifier]s out of the pool until
     * it is updated again.
     */
    private fun releaseAborted() {
        dispatchAborted()

        for (i in abortedModifiers.indices) {
            abortedModifiers[i].release()
        }

        abortedModifiers.clear()
    }
}
