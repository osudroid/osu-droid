package com.osudroid.storyboard.renderer

import org.andengine.graphics.debug.RenderStats

/**
 * The storyboard's own [RenderStats] slots, so the engine does not need to know about
 * storyboards.
 */
internal object StoryboardRenderStats {

    /** Total CPU time of drawing the storyboard (evaluation, vertex building and flushes). */
    @JvmField
    val TIME_TOTAL = RenderStats.registerTimer("sb")

    /** CPU time of evaluating the sprite states (`PlayableSprite.update`). */
    @JvmField
    val TIME_EVAL = RenderStats.registerTimer("sb.eval")

    /** CPU time of issuing the batched quads to GL. */
    @JvmField
    val TIME_FLUSH = RenderStats.registerTimer("sb.flush")

    /** Sprites in the active windows of the visible layers. */
    @JvmField
    val COUNT_ACTIVE = RenderStats.registerCounter("sb.active")

    /** Sprites actually submitted to the batch. */
    @JvmField
    val COUNT_DRAWN = RenderStats.registerCounter("sb.drawn")
}
