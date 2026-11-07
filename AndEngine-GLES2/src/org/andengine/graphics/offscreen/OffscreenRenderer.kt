package org.andengine.graphics.offscreen

import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import org.andengine.graphics.RenderTarget
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesRestorable
import org.andengine.graphics.gles.RestorePhase

/**
 * Draws into offscreen textures at the start of a frame, before the scene.
 *
 * Switching to another framebuffer while the screen is half drawn makes tile-based GPUs store the
 * screen and load it back afterwards. Entities that keep their look in a texture therefore
 * [request] a [Job], and the engine runs all requested jobs once at the start of the next drawn
 * frame ([runPending]), before anything is drawn to the screen. While the scene is drawn the
 * entities only draw their textures.
 *
 * Offscreen content is lost with the GL context: [contentGeneration] changes then, and content
 * drawn under an older generation must be drawn again.
 */
object OffscreenRenderer {

    /** Something to draw offscreen, on the GL thread, before the scene of the frame is drawn. */
    fun interface Job {
        fun render(drawContext: DrawContext, camera: Camera)
    }

    private val pending = ArrayList<Job>()
    private val running = ArrayList<Job>()

    /** Changes with every loss of the GL context; offscreen content of another generation is gone. */
    @JvmStatic
    var contentGeneration = 0
        private set

    private var lossListenerRegistered = false

    private val lossListener = object : GlesRestorable {
        override val restorePhase = RestorePhase.Framebuffers
        override fun onContextLost() {
            contentGeneration++
        }
        override fun restore() {}
    }

    private val clearColor = FloatArray(4)

    /** Runs [job] once at the start of the next drawn frame; a job requested twice runs once. Any thread. */
    @JvmStatic
    fun request(job: Job) {
        synchronized(pending) {
            if (!pending.contains(job)) pending.add(job)
        }
    }

    /** Takes back a [request] that has not run yet. Any thread. */
    @JvmStatic
    fun cancel(job: Job) {
        synchronized(pending) {
            pending.remove(job)
        }
    }

    /**
     * Runs the requested jobs; called by the engine on the GL thread before it draws the scene.
     * The framebuffer and viewport found before are restored afterwards.
     */
    @JvmStatic
    fun runPending(drawContext: DrawContext, camera: Camera) {
        RenderTargetPool.onFrame()
        synchronized(pending) {
            if (pending.isEmpty()) return
            running.addAll(pending)
            pending.clear()
        }
        if (!lossListenerRegistered) {
            GlesBackend.registry.register(lossListener)
            lossListenerRegistered = true
        }
        val state = GlesBackend.state
        val framebuffer = state.currentFramebuffer.coerceAtLeast(0)
        val viewportX = state.viewportX
        val viewportY = state.viewportY
        val viewportWidth = state.viewportWidth
        val viewportHeight = state.viewportHeight
        var next = 0
        try {
            while (next < running.size) {
                running[next++].render(drawContext, camera)
            }
        } finally {
            // Jobs after one that threw run at the next frame.
            if (next < running.size) {
                synchronized(pending) {
                    for (i in next until running.size) {
                        if (!pending.contains(running[i])) pending.add(running[i])
                    }
                }
            }
            running.clear()
            state.bindFramebuffer(framebuffer)
            if (viewportWidth > 0 && viewportHeight > 0) {
                state.viewport(viewportX, viewportY, viewportWidth, viewportHeight)
            }
        }
    }

    /**
     * Draws [content] into the pixels [0, width) x [0, height) of [target], from a [Job]. That area
     * is first cleared to transparent black, and the depth buffer of the target is cleared.
     * The local rectangle [left]..[right] x [top]..[bottom] is mapped onto it with an identity
     * model-view, [top] onto the texture row at v = height / target height and [bottom] onto v = 0.
     */
    @JvmStatic
    fun draw(
        target: RenderTarget,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        drawContext: DrawContext,
        content: Runnable
    ) {
        require(width in 1..target.width && height in 1..target.height) {
            "${width}x$height does not fit into the ${target.width}x${target.height} target '${target.label}'"
        }
        val context = GlesRenderContext.instance
        QuadBatcher.flush()
        context.beginPass(target, clearColor, true)
        GlesBackend.state.viewport(0, 0, width, height)

        drawContext.pushProjectionGLMatrix()
        drawContext.loadProjectionGLMatrixIdentity()
        drawContext.orthoProjectionGLMatrixf(left, right, bottom, top, -1f, 1f)
        drawContext.pushModelViewGLMatrix()
        drawContext.loadModelViewGLMatrixIdentity()
        try {
            content.run()
            QuadBatcher.flush()
        } finally {
            drawContext.popModelViewGLMatrix()
            drawContext.popProjectionGLMatrix()
            context.endPass()
        }
    }
}
