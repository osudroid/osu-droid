package org.andengine.graphics.batch

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import org.andengine.graphics.BlendMode
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.CullMode
import org.andengine.graphics.GpuBuffer
import org.andengine.graphics.GpuProgram
import org.andengine.graphics.Pipeline
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.Texture
import org.andengine.graphics.VertexAttribute
import org.andengine.graphics.VertexFormat
import org.andengine.graphics.VertexLayout
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesStateCache

/**
 * Collects textured, colored quads and draws them with as few draw calls as possible: quads that
 * share texture and blend mode go out in one indexed draw of up to [MAX_QUADS] quads. Corners are
 * transformed on the CPU, so quads with different model-view matrices still share a draw; the
 * vertex shader only applies the projection set with [setProjection].
 *
 * The pending quads are drawn ("flushed") when the texture or the blend mode changes, the
 * projection changes, the batch is full, or [flush] is called. So that the order on screen stays
 * the order of the calls, they are also flushed before anyone else changes GL state through the
 * [org.andengine.graphics.gles.GlesStateCache] (see [GlesStateCache.beforeExternalChange]), and every
 * other draw call must [flush] first. Such a flush leaves the state as it found it, so the code
 * that interrupted the batch draws with the state it set up.
 *
 * A flush draws into whatever framebuffer, viewport and scissor are current (see
 * [org.andengine.graphics.RenderContext.beginInlinePass]), so it can be interleaved with legacy drawing.
 * GPU resources are created at the first flush and restore themselves after a context loss.
 * GL thread only.
 */
object QuadBatcher {

    /** Most quads in one draw; 16-bit indices reach 4 * 4096 vertices. */
    const val MAX_QUADS = 4096

    /** float2 position + float2 UV + 4 normalized bytes color. */
    const val VERTEX_SIZE_BYTES = 20

    /**
     * Packs a color for [add]: ABGR (red in the low byte), each channel clamped to 0..1 and
     * rounded to a byte.
     */
    @JvmStatic
    fun packColor(red: Float, green: Float, blue: Float, alpha: Float): Int =
        (colorByte(alpha) shl 24) or (colorByte(blue) shl 16) or (colorByte(green) shl 8) or colorByte(red)

    private fun colorByte(value: Float): Int = (value.coerceIn(0f, 1f) * 255f + 0.5f).toInt()

    private const val INTS_PER_VERTEX = VERTEX_SIZE_BYTES / 4
    private const val INTS_PER_QUAD = INTS_PER_VERTEX * 4
    private const val QUAD_SIZE_BYTES = VERTEX_SIZE_BYTES * 4
    private const val INDICES_PER_QUAD = 6

    /** Registers a named [RenderStats] counter and returns its index. */
    fun interface CounterRegistration {
        fun register(name: String): Int
    }

    @JvmField
    val DEFAULT_COUNTER_REGISTRATION = CounterRegistration { RenderStats.registerCounter(it) }

    /** How the "batch.quads" counter is registered, at the first flush; replaceable for tests. */
    @JvmField
    var counterRegistration = DEFAULT_COUNTER_REGISTRATION

    private const val COUNTER_UNREGISTERED = -2
    private const val COUNTER_UNAVAILABLE = -1
    private var quadCounter = COUNTER_UNREGISTERED

    const val REASON_TEXTURE = 0
    const val REASON_BLEND = 1
    const val REASON_FOREIGN = 2
    const val REASON_FULL = 3
    const val REASON_FRAME = 4
    private val REASON_NAMES = arrayOf(
        "batch.flush.texture", "batch.flush.blend", "batch.flush.foreign", "batch.flush.full", "batch.flush.frame"
    )
    private val reasonCounters = IntArray(REASON_NAMES.size) { COUNTER_UNREGISTERED }

    /**
     * Index of the [RenderStats] counter of the quads drawn; -1 if registering it failed (then
     * nothing is counted), -2 before the first flush.
     */
    val quadCounterIndex: Int get() = quadCounter

    @JvmField
    val layout = VertexLayout(
        VERTEX_SIZE_BYTES,
        listOf(
            VertexAttribute(0, VertexFormat.Float2, 0),
            VertexAttribute(1, VertexFormat.Float2, 8),
            VertexAttribute(2, VertexFormat.UByte4Normalized, 16)
        )
    )

    private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec4 aColor;
uniform mat4 uProjection;
out vec2 vTexCoord;
out vec4 vColor;
void main() {
    vTexCoord = aTexCoord;
    vColor = aColor;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
"""

    private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vTexCoord;
in vec4 vColor;
out vec4 fragColor;
void main() {
    fragColor = texture(uTexture, vTexCoord) * vColor;
}
"""

    /** The pending vertices: position and UV as raw float bits, then the color, per vertex. */
    private val vertices = IntArray(MAX_QUADS * INTS_PER_QUAD)
    private var quadCount = 0
    private var texture: Texture? = null
    private var blend: BlendMode? = null

    private val projection = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    // The color int is ABGR (red in the low byte); in memory the bytes must be R, G, B, A.
    private val littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    private var program: GpuProgram? = null
    private var vertexBuffer: GpuBuffer? = null
    private var indexBuffer: GpuBuffer? = null
    private val pipelines = HashMap<BlendMode, Pipeline>()

    /** The state found by an outside flush, set again after it; see [GlesStateCache.saveDrawState]. */
    private val savedState = IntArray(GlesStateCache.DRAW_STATE_SIZE)

    /** Flushes before a foreign state change; armed on the state cache while quads wait. */
    private val flushBeforeChange = Runnable { flush() }
    private var armedState: GlesStateCache? = null

    /** Staging for the vertex upload; CPU memory, kept across [releaseResources]. */
    private var uploadBytes: ByteBuffer? = null
    private var uploadInts: IntBuffer? = null

    /** True while quads wait to be drawn. */
    @JvmStatic
    val hasPending: Boolean get() = quadCount > 0

    /** Number of cached pipelines, one per blend mode drawn with. */
    val pipelineCount: Int get() = pipelines.size

    /**
     * Sets the projection (column-major 4x4) for the following quads. A matrix with different
     * content flushes the pending quads first; the values are copied.
     */
    @JvmStatic
    fun setProjection(m: FloatArray) {
        require(m.size >= 16) { "A projection needs 16 values, got ${m.size}" }
        val p = projection
        var same = true
        for (i in 0 until 16) {
            if (p[i] != m[i]) {
                same = false
                break
            }
        }
        if (same) return
        drawPending(REASON_FOREIGN)
        System.arraycopy(m, 0, p, 0, 16)
    }

    /**
     * Adds the rectangle ([x0], [y0])-([x1], [y1]) in local coordinates, transformed by [matrix]
     * (column-major 4x4 model-view, only its 2D affine part is used), with the texture
     * rectangle ([u0], [v0])-([u1], [v1]) and the ABGR packed color [rgba] on all corners.
     * Skipped without any state change while [texture] is not loaded (see [BatchTexture.isLoaded]).
     */
    @JvmStatic
    fun add(
        texture: Texture,
        blend: BlendMode,
        matrix: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        rgba: Int
    ) {
        if (!BatchTexture.isLoaded(texture)) return
        prepare(texture, blend)
        arm()
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        val color = vertexColor(rgba)
        var i = quadCount * INTS_PER_QUAD
        i = put(i, a * x0 + c * y0 + tx, b * x0 + d * y0 + ty, u0, v0, color)
        i = put(i, a * x1 + c * y0 + tx, b * x1 + d * y0 + ty, u1, v0, color)
        i = put(i, a * x1 + c * y1 + tx, b * x1 + d * y1 + ty, u1, v1, color)
        put(i, a * x0 + c * y1 + tx, b * x0 + d * y1 + ty, u0, v1, color)
        quadCount++
    }

    /**
     * Adds a free quad whose 4 corners ([x], [y], already in the projection's coordinates) are
     * given in order around the quad, with their texture coordinates [u], [v] and the ABGR packed
     * color [rgba] on all corners. Skipped like [add] while [texture] is not loaded.
     */
    @JvmStatic
    fun addQuad(texture: Texture, blend: BlendMode, x: FloatArray, y: FloatArray, u: FloatArray, v: FloatArray, rgba: Int) {
        if (!BatchTexture.isLoaded(texture)) return
        prepare(texture, blend)
        arm()
        val color = vertexColor(rgba)
        var i = quadCount * INTS_PER_QUAD
        for (k in 0 until 4) {
            i = put(i, x[k], y[k], u[k], v[k], color)
        }
        quadCount++
    }

    /**
     * [addQuad] for corners in local coordinates: they are transformed by the 2D part of the
     * column-major model-view [matrix] first, as in [add].
     */
    fun addQuad(
        texture: Texture,
        blend: BlendMode,
        matrix: FloatArray,
        x: FloatArray, y: FloatArray, u: FloatArray, v: FloatArray,
        rgba: Int
    ) {
        if (!BatchTexture.isLoaded(texture)) return
        prepare(texture, blend)
        arm()
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        val color = vertexColor(rgba)
        var i = quadCount * INTS_PER_QUAD
        for (k in 0 until 4) {
            val lx = x[k]
            val ly = y[k]
            i = put(i, a * lx + c * ly + tx, b * lx + d * ly + ty, u[k], v[k], color)
        }
        quadCount++
    }

    /**
     * Draws the pending quads, if any, and sets the GL state the draw changed back to what it was
     * before, so code that set up state for a draw of its own can call this right before drawing.
     * A no-op without pending quads.
     */
    @JvmStatic
    fun flush() = flushFor(REASON_FOREIGN)

    /** [flush] for the end of a frame; counted as the "batch.flush.frame" reason. */
    @JvmStatic
    fun flushFrame() = flushFor(REASON_FRAME)

    private fun flushFor(reason: Int) {
        if (quadCount == 0) return
        val state = GlesBackend.state
        state.saveDrawState(savedState)
        try {
            drawPending(reason)
        } finally {
            state.restoreDrawState(savedState)
            // The render context may think its pipeline is still applied.
            GlesRenderContext.instance.invalidatePipeline()
        }
    }

    /** Arms [flushBeforeChange] on the current state cache, which is new after a context loss. */
    private fun arm() {
        val state = GlesBackend.state
        if (state.beforeExternalChange !== flushBeforeChange) {
            state.beforeExternalChange = flushBeforeChange
            armedState = state
        }
    }

    private fun disarm() {
        val state = armedState ?: return
        armedState = null
        // The cache clears the hook itself before running it.
        if (state.beforeExternalChange === flushBeforeChange) state.beforeExternalChange = null
    }

    /** Draws the pending quads; the batch's own state changes must not flush again. */
    private fun drawPending(reason: Int) {
        val count = quadCount
        if (count == 0) return
        countFlush(reason)
        disarm()
        val tex = texture!!
        val pipelineBlend = blend!!
        // Dropped even if the draw fails, so a broken batch is not retried with every add.
        quadCount = 0

        ensureResources()
        val vb = vertexBuffer!!
        val ints = uploadInts!!
        ints.clear()
        ints.put(vertices, 0, count * INTS_PER_QUAD)
        val bytes = uploadBytes!!
        bytes.clear()
        // New storage per flush: the previous draw may still read the old content.
        vb.orphan()
        vb.update(0, bytes, count * QUAD_SIZE_BYTES)

        val pipeline = pipelineFor(pipelineBlend)
        val context = GlesRenderContext.instance
        val ownPass = !context.isInPass
        if (ownPass) context.beginInlinePass()
        try {
            context.setPipeline(pipeline)
            context.setUniformMatrix4(pipeline.program.uniformLocation("uProjection"), projection)
            context.setTexture(0, tex, null)
            context.setVertexBuffer(vb)
            context.setIndexBuffer(indexBuffer!!)
            context.drawIndexed(0, count * INDICES_PER_QUAD)
        } finally {
            if (ownPass) context.endPass()
        }
        var counter = quadCounter
        if (counter == COUNTER_UNREGISTERED) counter = registerQuadCounter()
        if (counter >= 0) RenderStats.count(counter, count)
    }

    /** Counts one flush that draws, for [reason]; free while [RenderStats] is disabled. */
    private fun countFlush(reason: Int) {
        if (!RenderStats.enabled) return
        if (reasonCounters[0] == COUNTER_UNREGISTERED) registerReasonCounters()
        val counter = reasonCounters[reason]
        if (counter >= 0) RenderStats.count(counter, 1)
    }

    private fun registerReasonCounters() {
        try {
            for (i in reasonCounters.indices) reasonCounters[i] = RenderStats.registerCounter(REASON_NAMES[i])
        } catch (e: RuntimeException) {
            reasonCounters.fill(COUNTER_UNAVAILABLE)
        }
    }

    /**
     * Index of the [RenderStats] counter of the flushes for [reason] (one of the REASON_*
     * values: texture, blend, foreign, full, frame); -1 if registering failed, -2 before the
     * first counted flush.
     */
    fun flushCounterIndex(reason: Int): Int = reasonCounters[reason]

    /** Statistics must never stop the batcher: a failed registration only disables counting. */
    private fun registerQuadCounter(): Int {
        val index = try {
            counterRegistration.register("batch.quads")
        } catch (e: RuntimeException) {
            COUNTER_UNAVAILABLE
        }
        quadCounter = index
        return index
    }

    /**
     * Drops the pending quads and disposes the GPU resources; the next flush creates them again.
     * For shutdown and tests.
     */
    @JvmStatic
    fun releaseResources() {
        quadCount = 0
        disarm()
        texture = null
        blend = null
        pipelines.clear()
        program?.dispose()
        vertexBuffer?.dispose()
        indexBuffer?.dispose()
        program = null
        vertexBuffer = null
        indexBuffer = null
        quadCounter = COUNTER_UNREGISTERED
        reasonCounters.fill(COUNTER_UNREGISTERED)
    }

    private fun prepare(texture: Texture, blend: BlendMode) {
        if (quadCount == MAX_QUADS) {
            drawPending(REASON_FULL)
        } else if (quadCount > 0) {
            if (texture !== this.texture) {
                drawPending(REASON_TEXTURE)
            } else if (blend !== this.blend && blend != this.blend) {
                drawPending(REASON_BLEND)
            }
        }
        this.texture = texture
        this.blend = blend
    }

    private fun vertexColor(abgr: Int): Int = if (littleEndian) abgr else Integer.reverseBytes(abgr)

    private fun put(index: Int, x: Float, y: Float, u: Float, v: Float, color: Int): Int {
        val a = vertices
        a[index] = java.lang.Float.floatToRawIntBits(x)
        a[index + 1] = java.lang.Float.floatToRawIntBits(y)
        a[index + 2] = java.lang.Float.floatToRawIntBits(u)
        a[index + 3] = java.lang.Float.floatToRawIntBits(v)
        a[index + 4] = color
        return index + INTS_PER_VERTEX
    }

    private fun pipelineFor(blend: BlendMode): Pipeline {
        val cached = pipelines[blend]
        if (cached != null) return cached
        val created = Pipeline(program!!, layout, blend, null, false, CullMode.None, PrimitiveType.Triangles)
        pipelines[blend] = created
        return created
    }

    private fun ensureResources() {
        if (program != null) return
        val device = GlesGraphicsDevice.instance
        // Built into locals and published together, so a failure leaves nothing half built
        // (and nothing registered) and the next flush starts over.
        var newProgram: GpuProgram? = null
        var newVertices: GpuBuffer? = null
        var newIndices: GpuBuffer? = null
        try {
            newProgram = device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER, "quad-batch")
            newVertices = device.createBuffer(BufferTarget.Vertex, BufferUsage.Stream, MAX_QUADS * QUAD_SIZE_BYTES, "quad-batch-vertices")
            newIndices = createIndexBuffer()
        } catch (e: RuntimeException) {
            newIndices?.dispose()
            newVertices?.dispose()
            newProgram?.dispose()
            throw e
        }

        if (uploadBytes == null) {
            val bytes = ByteBuffer.allocateDirect(MAX_QUADS * QUAD_SIZE_BYTES).order(ByteOrder.nativeOrder())
            uploadBytes = bytes
            uploadInts = bytes.asIntBuffer()
        }
        program = newProgram
        vertexBuffer = newVertices
        indexBuffer = newIndices
    }

    private fun createIndexBuffer(): GpuBuffer {
        val device = GlesGraphicsDevice.instance

        val indexBytes = MAX_QUADS * INDICES_PER_QUAD * 2
        val indices = ByteBuffer.allocateDirect(indexBytes).order(ByteOrder.nativeOrder())
        val shorts = indices.asShortBuffer()
        for (q in 0 until MAX_QUADS) {
            val first = q * 4
            shorts.put(first.toShort())
            shorts.put((first + 1).toShort())
            shorts.put((first + 2).toShort())
            shorts.put((first + 2).toShort())
            shorts.put((first + 3).toShort())
            shorts.put(first.toShort())
        }
        // Static: the registry uploads the indices again after a context loss.
        val buffer = device.createBuffer(BufferTarget.Index, BufferUsage.Static, indexBytes, "quad-batch-indices")
        try {
            buffer.update(0, indices, indexBytes)
        } catch (e: RuntimeException) {
            buffer.dispose()
            throw e
        }
        return buffer
    }
}
