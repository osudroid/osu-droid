package org.andengine.graphics.debug

import android.os.SystemClock
import android.util.Log
import org.andengine.graphics.gles.GlesGraphicsDevice

/**
 * Lightweight per-frame render counters, used to measure the GL layer before and after
 * each step of the GL core rewrite.
 *
 * All counting methods are no-ops while [enabled] is `false`, so the call sites can stay
 * in release builds. Counters are written by the GL thread only, except [onUpdate], which
 * is written by the update thread. The engine runs update and draw in lockstep under
 * `EngineLock`, so the GL thread always sees the value of the update that ran right before.
 *
 * Every [WINDOW_MS] the values of the frames in that window are averaged into [summary]
 * and handed to [listener] (on the GL thread). Every second window is also logged to
 * logcat with the tag [TAG].
 */
object RenderStats {

    const val TAG = "RenderStats"

    // Callers register their own named timers and counters, see [registerTimer]/[registerCounter].
    private const val TIMER_COUNT = 8
    private const val COUNTER_COUNT = 8
    private const val WINDOW_MS = 500L
    private const val NANOS_PER_MS = 1_000_000.0

    @JvmField
    @Volatile
    var enabled = false

    /** Receives the summary text of every finished window, called on the GL thread. */
    @JvmField
    @Volatile
    var listener: Listener? = null

    /** The summary text of the last finished window. */
    @Volatile
    var summary = ""
        private set

    // Current frame.
    private var drawCalls = 0
    private var vertices = 0
    private var textureBinds = 0
    private var programSwitches = 0
    private var blendChanges = 0
    private var bufferBinds = 0
    private var framebufferBinds = 0
    private var uploads = 0
    private var uploadBytes = 0L
    private var drawStartNanos = 0L
    private var updateNanos = 0L
    private val timerNanos = LongArray(TIMER_COUNT)
    private val counters = IntArray(COUNTER_COUNT)

    private val gpuTimer = GpuFrameTimer(GlesGraphicsDevice.instance)

    // Current window.
    private var windowStartMs = 0L
    private var windowFrames = 0
    private var windowIndex = 0L
    private var lastFrameNanos = 0L
    private var sumDrawCalls = 0L
    private var maxDrawCalls = 0
    private var sumVertices = 0L
    private var sumTextureBinds = 0L
    private var sumProgramSwitches = 0L
    private var sumBlendChanges = 0L
    private var sumBufferBinds = 0L
    private var sumFramebufferBinds = 0L
    private var sumUploads = 0L
    private var sumUploadBytes = 0L
    private var sumFrameNanos = 0L
    private var maxFrameNanos = 0L
    private var sumDrawNanos = 0L
    private var maxDrawNanos = 0L
    private var sumUpdateNanos = 0L
    private var maxUpdateNanos = 0L
    private val sumTimerNanos = LongArray(TIMER_COUNT)
    private val sumCounters = LongArray(COUNTER_COUNT)
    private var sumGpuNanos = 0L
    private var maxGpuNanos = 0L
    private var gpuFrames = 0

    // region Counters

    @JvmStatic
    fun draw(vertexCount: Int) {
        if (enabled) {
            drawCalls++
            vertices += vertexCount
        }
    }

    @JvmStatic
    fun textureBind() {
        if (enabled) textureBinds++
    }

    @JvmStatic
    fun programSwitch() {
        if (enabled) programSwitches++
    }

    @JvmStatic
    fun blendChange() {
        if (enabled) blendChanges++
    }

    @JvmStatic
    fun bufferBind() {
        if (enabled) bufferBinds++
    }

    @JvmStatic
    fun framebufferBind() {
        if (enabled) framebufferBinds++
    }

    @JvmStatic
    fun upload(byteCount: Int) {
        if (enabled) {
            uploads++
            uploadBytes += byteCount
        }
    }

    private val timerNames = arrayOfNulls<String>(TIMER_COUNT)
    private val counterNames = arrayOfNulls<String>(COUNTER_COUNT)

    /**
     * Registers a named timer shown in the summary and returns its slot for [addTime].
     * Registering the same name again returns the existing slot.
     *
     * @throws IllegalStateException if all slots are taken.
     */
    @JvmStatic
    fun registerTimer(name: String): Int = register(timerNames, name)

    /**
     * Registers a named counter shown in the summary and returns its slot for [count].
     * Registering the same name again returns the existing slot.
     *
     * @throws IllegalStateException if all slots are taken.
     */
    @JvmStatic
    fun registerCounter(name: String): Int = register(counterNames, name)

    @Synchronized
    private fun register(names: Array<String?>, name: String): Int {
        for (i in names.indices) {
            if (names[i] == name) return i
        }
        for (i in names.indices) {
            if (names[i] == null) {
                names[i] = name
                return i
            }
        }
        throw IllegalStateException("No free RenderStats slot for '$name'")
    }

    /** Returns the current time for [addTime], or 0 while disabled. */
    @JvmStatic
    fun now(): Long = if (enabled) System.nanoTime() else 0L

    /** Adds the time since [start] (from [now]) to the given timer slot of the current frame. */
    @JvmStatic
    fun addTime(timer: Int, start: Long) {
        if (enabled && start != 0L) timerNanos[timer] += System.nanoTime() - start
    }

    @JvmStatic
    fun count(counter: Int, amount: Int) {
        if (enabled) counters[counter] += amount
    }

    /** The value of a counter in the current, unfinished frame. */
    @JvmStatic
    fun currentCount(counter: Int): Int = counters[counter]

    // endregion

    // region Frame timing

    /** Called by the update thread with the duration of one engine update. */
    @JvmStatic
    fun onUpdate(nanos: Long) {
        if (enabled) updateNanos = nanos
    }

    /** Called on the GL thread once the update finished and the scene is about to be drawn. */
    @JvmStatic
    fun beginDraw() {
        if (!enabled) return
        drawStartNanos = System.nanoTime()
        gpuTimer.begin()
    }

    /** Called on the GL thread after the scene was drawn. Closes the current frame. */
    @JvmStatic
    fun endDraw() {
        if (!enabled) return

        gpuTimer.end()
        val gpuSum = gpuTimer.collect()
        if (gpuTimer.collectedFrames > 0) {
            sumGpuNanos += gpuSum
            gpuFrames += gpuTimer.collectedFrames
            if (gpuTimer.collectedMaxNanos > maxGpuNanos) maxGpuNanos = gpuTimer.collectedMaxNanos
        }

        val now = System.nanoTime()
        val drawNanos = now - drawStartNanos
        val frameNanos = if (lastFrameNanos == 0L) 0L else now - lastFrameNanos
        lastFrameNanos = now

        windowFrames++
        sumDrawCalls += drawCalls
        if (drawCalls > maxDrawCalls) maxDrawCalls = drawCalls
        sumVertices += vertices
        sumTextureBinds += textureBinds
        sumProgramSwitches += programSwitches
        sumBlendChanges += blendChanges
        sumBufferBinds += bufferBinds
        sumFramebufferBinds += framebufferBinds
        sumUploads += uploads
        sumUploadBytes += uploadBytes
        sumFrameNanos += frameNanos
        if (frameNanos > maxFrameNanos) maxFrameNanos = frameNanos
        sumDrawNanos += drawNanos
        if (drawNanos > maxDrawNanos) maxDrawNanos = drawNanos
        sumUpdateNanos += updateNanos
        if (updateNanos > maxUpdateNanos) maxUpdateNanos = updateNanos
        for (i in 0 until TIMER_COUNT) sumTimerNanos[i] += timerNanos[i]
        for (i in 0 until COUNTER_COUNT) sumCounters[i] += counters[i].toLong()

        resetFrame()

        val nowMs = SystemClock.uptimeMillis()
        if (windowStartMs == 0L) {
            windowStartMs = nowMs
        } else if (nowMs - windowStartMs >= WINDOW_MS) {
            finishWindow(nowMs)
        }
    }

    // endregion

    /** Drops all collected data, e.g. after the GL context was recreated. */
    @JvmStatic
    fun reset() {
        resetFrame()
        resetWindow(0L)
        lastFrameNanos = 0L
        gpuTimer.invalidate()
    }

    private fun finishWindow(nowMs: Long) {
        val frames = windowFrames.coerceAtLeast(1)
        val elapsedMs = nowMs - windowStartMs

        val text = buildString(256) {
            append("FPS ").append(fmt(windowFrames * 1000.0 / elapsedMs))
            append("  frame ").append(ms(sumFrameNanos / frames)).append('/').append(ms(maxFrameNanos)).append(" ms\n")
            append("draw ").append(ms(sumDrawNanos / frames)).append('/').append(ms(maxDrawNanos))
            append("  update ").append(ms(sumUpdateNanos / frames)).append('/').append(ms(maxUpdateNanos)).append(" ms\n")
            append("calls ").append(sumDrawCalls / frames).append('/').append(maxDrawCalls)
            append("  verts ").append(sumVertices / frames).append('\n')
            append("tex ").append(sumTextureBinds / frames)
            append("  prog ").append(sumProgramSwitches / frames)
            append("  blend ").append(sumBlendChanges / frames)
            append("  buf ").append(sumBufferBinds / frames)
            append("  fbo ").append(sumFramebufferBinds / frames).append('\n')
            append("upload ").append(sumUploads / frames)
            append(" (").append(fmt(sumUploadBytes / frames / 1024.0)).append(" KiB)")
            append('\n').append("gpu ")
            if (!gpuTimer.isSupported) {
                append("n/a")
            } else if (gpuFrames > 0) {
                append(ms(sumGpuNanos / gpuFrames)).append('/').append(ms(maxGpuNanos)).append(" ms")
            } else {
                append("-")
            }
            appendNamed(this, "time", timerNames, sumTimerNanos, frames, asMillis = true)
            appendNamed(this, "count", counterNames, sumCounters, frames, asMillis = false)
        }

        summary = text
        listener?.onRenderStats(text)

        if (windowIndex++ % 2 == 0L) {
            Log.i(TAG, text.replace('\n', '|'))
        }

        resetWindow(nowMs)
    }

    private fun resetFrame() {
        drawCalls = 0
        vertices = 0
        textureBinds = 0
        programSwitches = 0
        blendChanges = 0
        bufferBinds = 0
        framebufferBinds = 0
        uploads = 0
        uploadBytes = 0L
        timerNanos.fill(0L)
        counters.fill(0)
    }

    private fun resetWindow(nowMs: Long) {
        windowStartMs = nowMs
        windowFrames = 0
        sumDrawCalls = 0L
        maxDrawCalls = 0
        sumVertices = 0L
        sumTextureBinds = 0L
        sumProgramSwitches = 0L
        sumBlendChanges = 0L
        sumBufferBinds = 0L
        sumFramebufferBinds = 0L
        sumUploads = 0L
        sumUploadBytes = 0L
        sumFrameNanos = 0L
        maxFrameNanos = 0L
        sumDrawNanos = 0L
        maxDrawNanos = 0L
        sumUpdateNanos = 0L
        maxUpdateNanos = 0L
        sumTimerNanos.fill(0L)
        sumCounters.fill(0L)
        sumGpuNanos = 0L
        maxGpuNanos = 0L
        gpuFrames = 0
    }

    /** Appends one line with the per-frame average of every registered slot that was used. */
    private fun appendNamed(
        out: StringBuilder,
        label: String,
        names: Array<String?>,
        sums: LongArray,
        frames: Int,
        asMillis: Boolean
    ) {
        var first = true
        for (i in names.indices) {
            val name = names[i] ?: continue
            if (sums[i] == 0L) continue

            out.append(if (first) "\n$label " else "  ")
            first = false
            out.append(name).append(' ')
            if (asMillis) out.append(ms(sums[i] / frames)) else out.append(sums[i] / frames)
        }
        if (!first && asMillis) out.append(" ms")
    }

    private fun ms(nanos: Long) = fmt(nanos / NANOS_PER_MS)

    private fun fmt(value: Double): String {
        val rounded = Math.round(value * 10)
        return "${rounded / 10}.${Math.abs(rounded % 10)}"
    }

    fun interface Listener {
        fun onRenderStats(summary: String)
    }
}
