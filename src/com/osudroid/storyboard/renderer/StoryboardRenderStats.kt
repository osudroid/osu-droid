package com.osudroid.storyboard.renderer

import org.andengine.graphics.debug.RenderStats

/**
 * The storyboard's own [RenderStats] slots, so the engine does not need to know about
 * storyboards.
 */
object StoryboardRenderStats {

    /**
     * Total CPU time of drawing the storyboard: evaluation and handing the quads to the quad batch;
     * the batch's draws are counted by the engine.
     */
    val TIME_TOTAL = RenderStats.registerTimer("sb")

    /** CPU time of evaluating the sprite states (`PlayableSprite.update`). */
    val TIME_EVAL = RenderStats.registerTimer("sb.eval")

    /** Sprites in the active windows of the visible layers. */
    val COUNT_ACTIVE = RenderStats.registerCounter("sb.active")

    /** Sprites actually submitted to the batch. */
    val COUNT_DRAWN = RenderStats.registerCounter("sb.drawn")
}
