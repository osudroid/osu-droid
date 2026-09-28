package com.osudroid.storyboard.model.commands

/**
 * Represents a storyboard `T` (trigger) command.
 *
 * The command times of this group are relative to the activation time of the trigger. Unlike
 * loops, trigger commands are not unrolled ahead of time - they are evaluated relative to the
 * activation time whenever the trigger fires within its active window.
 */
class StoryboardTrigger(
    /**
     * The condition that activates this trigger.
     */
    @JvmField
    val type: StoryboardTriggerType,

    /**
     * The start of the time window in which this trigger can activate, in milliseconds.
     */
    @JvmField
    val triggerStartTime: Double,

    /**
     * The end of the time window in which this trigger can activate, in milliseconds.
     */
    @JvmField
    val triggerEndTime: Double,

    /**
     * The trigger group. A new activation cancels a running activation of the same group, unless
     * the group is `0`.
     */
    @JvmField
    val groupNumber: Int
) : StoryboardCommandGroup()
