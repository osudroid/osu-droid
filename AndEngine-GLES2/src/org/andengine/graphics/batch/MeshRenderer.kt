package org.andengine.graphics.batch

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BlendMode
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.CompareFunction
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
 * Draws colored triangle/line meshes, untextured or with one texture (slider bodies, trails, storyboard shapes), in one
 * draw call each. A mesh is given as 2D positions (or positions with a color per vertex), is
 * transformed on the CPU by a 2D matrix, uploaded with [GpuBuffer.replace] and drawn with its own
 * program; the vertex shader only applies the projection passed to [draw].
 *
 * Every draw first flushes the [QuadBatcher], so the order on screen stays the order of the calls.
 * The depth test, depth function and depth mask come from the pipeline (see the `depthTest` and
 * `depthWrite` parameters), so depth-masked meshes need no GL calls from the caller.
 *
 * The draw leaves the GL state as it found it, depth test, depth function, depth mask, blend,
 * program and bindings included, so code after it that relies on the state it set up before keeps
 * working. A caller that wants the depth test off afterwards must disable it itself.
 *
 * The staging memory and the vertex buffer grow (to the next power of two) only when a mesh needs
 * more room, they are never created per frame. GPU resources are created at the first draw and
 * restore themselves after a context loss. GL thread only.
 */
object MeshRenderer {

    /** float2 position + 4 normalized bytes color. */
    const val VERTEX_SIZE_BYTES = 12

    /** Vertices the buffers hold before the first growth. */
    const val INITIAL_CAPACITY_VERTICES = 4096

    private const val COLORED_FLOATS = 6

    @JvmField
    val layout = VertexLayout(
        VERTEX_SIZE_BYTES,
        listOf(
            VertexAttribute(0, VertexFormat.Float2, 0),
            VertexAttribute(1, VertexFormat.UByte4Normalized, 8)
        )
    )

    private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec4 aColor;
uniform mat4 uProjection;
out vec4 vColor;
void main() {
    vColor = aColor;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
"""

    private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec4 vColor;
out vec4 fragColor;
void main() {
    fragColor = vColor;
}
"""

    /** float2 position + float2 uv + 4 normalized bytes color. */
    const val TEXTURED_VERTEX_SIZE_BYTES = 20

    private const val TEXTURED_FLOATS = 4

    @JvmField
    val texturedLayout = VertexLayout(
        TEXTURED_VERTEX_SIZE_BYTES,
        listOf(
            VertexAttribute(0, VertexFormat.Float2, 0),
            VertexAttribute(1, VertexFormat.Float2, 8),
            VertexAttribute(2, VertexFormat.UByte4Normalized, 16)
        )
    )

    private const val TEXTURED_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aColor;
uniform mat4 uProjection;
out vec2 vUv;
out vec4 vColor;
void main() {
    vUv = aUv;
    vColor = aColor;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
"""

    private const val TEXTURED_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vUv;
in vec4 vColor;
out vec4 fragColor;
void main() {
    fragColor = texture(uTexture, vUv) * vColor;
}
"""

    private const val TEXTURE_UNIT = 0

    private const val COUNTER_UNREGISTERED = -2
    private const val COUNTER_UNAVAILABLE = -1
    private var vertexCounter = COUNTER_UNREGISTERED

    private val primitives = PrimitiveType.values()

    // Depth states: off and the compare functions.
    private val depthStates = CompareFunction.values().size + 1

    private val littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    /** Program, vertex buffer, staging and pipelines of one vertex format. */
    private class Kit(
        val vertexSize: Int,
        val layout: VertexLayout,
        val vertexShader: String,
        val fragmentShader: String,
        val name: String,
        val samplerUnits: Map<String, Int>
    ) {
        var program: GpuProgram? = null
        var vertexBuffer: GpuBuffer? = null

        /** Per blend mode, the pipelines by depth state, depth write and primitive. */
        val pipelines = HashMap<BlendMode, Array<Pipeline?>>()
        var pipelineTotal = 0

        /** Staging for the vertex upload; CPU memory, kept across [releaseResources]. */
        var staging: ByteBuffer? = null

        val capacityVertices: Int get() = (staging?.capacity() ?: 0) / vertexSize
    }

    private val plain = Kit(VERTEX_SIZE_BYTES, layout, VERTEX_SHADER, FRAGMENT_SHADER, "mesh", emptyMap())
    private val textured = Kit(
        TEXTURED_VERTEX_SIZE_BYTES, texturedLayout, TEXTURED_VERTEX_SHADER, TEXTURED_FRAGMENT_SHADER, "mesh-textured",
        mapOf("uTexture" to TEXTURE_UNIT)
    )

    private val savedState = IntArray(GlesStateCache.DRAW_STATE_SIZE)

    /** Vertices the staging memory holds; 0 before the first draw. */
    @JvmStatic
    val capacityVertices: Int get() = plain.capacityVertices

    private val kits = arrayOf(plain, textured)

    /** Number of cached pipelines, textured meshes included. */
    val pipelineCount: Int get() = kits.sumOf { it.pipelineTotal }

    /** Vertices the staging memory of the textured meshes holds; 0 before the first one. */
    @JvmStatic
    val texturedCapacityVertices: Int get() = textured.capacityVertices

    /**
     * Index of the [RenderStats] counter "mesh.vertices"; -1 if registering it failed, -2 before the
     * first draw.
     */
    val vertexCounterIndex: Int get() = vertexCounter

    /**
     * Draws [vertexCount] vertices as [primitive]. Their positions (x, y pairs) are read from
     * [positions] starting at [offset], transformed by the 2D part of the column-major model-view
     * [matrix], and all drawn with the ABGR packed color [rgba] (see [QuadBatcher.packColor]).
     * [projection] is the column-major 4x4 projection. [depthTest] is the depth compare function or
     * null for no depth test; [depthWrite] whether the depth buffer is written.
     */
    @JvmStatic
    fun draw(
        primitive: PrimitiveType,
        positions: FloatArray,
        offset: Int,
        vertexCount: Int,
        matrix: FloatArray,
        projection: FloatArray,
        rgba: Int,
        blend: BlendMode,
        depthTest: CompareFunction?,
        depthWrite: Boolean
    ) {
        if (vertexCount <= 0) return
        require(offset >= 0 && offset + vertexCount * 2 <= positions.size) {
            "Positions of $vertexCount vertices from $offset exceed ${positions.size} values"
        }
        val bytes = reserve(plain, vertexCount)
        val color = if (littleEndian) rgba else Integer.reverseBytes(rgba)
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        var src = offset
        var dst = 0
        for (i in 0 until vertexCount) {
            val x = positions[src]
            val y = positions[src + 1]
            bytes.putFloat(dst, a * x + c * y + tx)
            bytes.putFloat(dst + 4, b * x + d * y + ty)
            bytes.putInt(dst + 8, color)
            src += 2
            dst += VERTEX_SIZE_BYTES
        }
        submit(plain, primitive, vertexCount, projection, null, blend, depthTest, depthWrite)
    }

    /**
     * Like [draw], but [vertices] holds (x, y, r, g, b, a) floats per vertex from [offset] on, each
     * vertex with its own color (components clamped to 0..1).
     */
    @JvmStatic
    fun drawColored(
        primitive: PrimitiveType,
        vertices: FloatArray,
        offset: Int,
        vertexCount: Int,
        matrix: FloatArray,
        projection: FloatArray,
        blend: BlendMode,
        depthTest: CompareFunction?,
        depthWrite: Boolean
    ) {
        if (vertexCount <= 0) return
        require(offset >= 0 && offset + vertexCount * COLORED_FLOATS <= vertices.size) {
            "Vertices of $vertexCount vertices from $offset exceed ${vertices.size} values"
        }
        val bytes = reserve(plain, vertexCount)
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        var src = offset
        var dst = 0
        for (i in 0 until vertexCount) {
            val x = vertices[src]
            val y = vertices[src + 1]
            val abgr = QuadBatcher.packColor(vertices[src + 2], vertices[src + 3], vertices[src + 4], vertices[src + 5])
            bytes.putFloat(dst, a * x + c * y + tx)
            bytes.putFloat(dst + 4, b * x + d * y + ty)
            bytes.putInt(dst + 8, if (littleEndian) abgr else Integer.reverseBytes(abgr))
            src += COLORED_FLOATS
            dst += VERTEX_SIZE_BYTES
        }
        submit(plain, primitive, vertexCount, projection, null, blend, depthTest, depthWrite)
    }

    /**
     * Like [draw], but [vertices] holds (x, y, u, v) floats per vertex from [offset] on, and the
     * mesh is drawn with [texture] on unit 0, multiplied by the one color [rgba]. A legacy texture
     * that is not on the GPU draws nothing.
     */
    @JvmStatic
    fun drawTextured(
        primitive: PrimitiveType,
        vertices: FloatArray,
        offset: Int,
        vertexCount: Int,
        matrix: FloatArray,
        projection: FloatArray,
        texture: Texture,
        rgba: Int,
        blend: BlendMode,
        depthTest: CompareFunction?,
        depthWrite: Boolean
    ) {
        if (vertexCount <= 0) return
        require(offset >= 0 && offset + vertexCount * TEXTURED_FLOATS <= vertices.size) {
            "Vertices of $vertexCount vertices from $offset exceed ${vertices.size} values"
        }
        if (!BatchTexture.isLoaded(texture)) return
        val bytes = reserve(textured, vertexCount)
        val color = if (littleEndian) rgba else Integer.reverseBytes(rgba)
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        var src = offset
        var dst = 0
        for (i in 0 until vertexCount) {
            val x = vertices[src]
            val y = vertices[src + 1]
            bytes.putFloat(dst, a * x + c * y + tx)
            bytes.putFloat(dst + 4, b * x + d * y + ty)
            bytes.putFloat(dst + 8, vertices[src + 2])
            bytes.putFloat(dst + 12, vertices[src + 3])
            bytes.putInt(dst + 16, color)
            src += TEXTURED_FLOATS
            dst += TEXTURED_VERTEX_SIZE_BYTES
        }
        submit(textured, primitive, vertexCount, projection, texture, blend, depthTest, depthWrite)
    }

    /**
     * Draws the pending quads first, then makes sure the buffers of [kit] hold [vertexCount]
     * vertices (growing them if needed) and returns the staging memory to fill from byte 0.
     */
    private fun reserve(kit: Kit, vertexCount: Int): ByteBuffer {
        QuadBatcher.flush()
        ensureResources(kit, vertexCount)
        return kit.staging!!
    }

    private fun submit(
        kit: Kit,
        primitive: PrimitiveType,
        vertexCount: Int,
        projection: FloatArray,
        texture: Texture?,
        blend: BlendMode,
        depthTest: CompareFunction?,
        depthWrite: Boolean
    ) {
        require(projection.size >= 16) { "A projection needs 16 values, got ${projection.size}" }
        val state = GlesBackend.state
        // Before the upload binds the mesh buffer, so the binding found before the draw comes back.
        state.saveDrawState(savedState)
        val bytes = kit.staging!!
        val vb = kit.vertexBuffer!!
        bytes.clear()
        // New storage per draw: rewriting storage that earlier draws still read makes the driver wait.
        vb.replace(bytes, vertexCount * kit.vertexSize)
        val pipeline = pipelineFor(kit, blend, depthTest, depthWrite, primitive)
        val context = GlesRenderContext.instance
        val ownPass = !context.isInPass
        if (ownPass) context.beginInlinePass()
        try {
            context.setPipeline(pipeline)
            context.setUniformMatrix4(pipeline.program.uniformLocation("uProjection"), projection)
            if (texture != null) context.setTexture(TEXTURE_UNIT, texture, null)
            context.setVertexBuffer(vb)
            context.draw(0, vertexCount)
        } finally {
            if (ownPass) context.endPass()
            state.restoreDrawState(savedState)
            // The render context may think its pipeline is still applied.
            context.invalidatePipeline()
        }
        var counter = vertexCounter
        if (counter == COUNTER_UNREGISTERED) counter = registerVertexCounter()
        if (counter >= 0) RenderStats.count(counter, vertexCount)
    }

    /** Statistics must never stop the renderer: a failed registration only disables counting. */
    private fun registerVertexCounter(): Int {
        val index = try {
            RenderStats.registerCounter("mesh.vertices")
        } catch (e: RuntimeException) {
            COUNTER_UNAVAILABLE
        }
        vertexCounter = index
        return index
    }

    private fun pipelineFor(kit: Kit, blend: BlendMode, depthTest: CompareFunction?, depthWrite: Boolean, primitive: PrimitiveType): Pipeline {
        var slots = kit.pipelines[blend]
        if (slots == null) {
            slots = arrayOfNulls(depthStates * 2 * primitives.size)
            kit.pipelines[blend] = slots
        }
        val depthIndex = if (depthTest == null) 0 else depthTest.ordinal + 1
        val index = (depthIndex * 2 + (if (depthWrite) 1 else 0)) * primitives.size + primitive.ordinal
        var pipeline = slots[index]
        if (pipeline == null) {
            pipeline = Pipeline(kit.program!!, kit.layout, blend, depthTest, depthWrite, CullMode.None, primitive)
            slots[index] = pipeline
            kit.pipelineTotal++
        }
        return pipeline
    }

    /**
     * Creates the program and the vertex buffer of [kit], or grows its staging memory and buffer to
     * the next power of two when [vertexCount] vertices do not fit.
     */
    private fun ensureResources(kit: Kit, vertexCount: Int) {
        val oldCapacity = kit.capacityVertices
        var capacity = if (oldCapacity == 0) INITIAL_CAPACITY_VERTICES else oldCapacity
        if (vertexCount > capacity) capacity = Integer.highestOneBit(vertexCount - 1) shl 1
        val bufferBytes = capacity * kit.vertexSize

        val oldBuffer = kit.vertexBuffer
        val needProgram = kit.program == null
        val needBuffer = oldBuffer == null || oldBuffer.sizeBytes < bufferBytes
        if (!needProgram && !needBuffer && capacity == oldCapacity) return

        // Built into locals and published together, so a failure leaves nothing half built.
        var newProgram: GpuProgram? = null
        var newBuffer: GpuBuffer? = null
        try {
            if (needProgram) {
                newProgram = GlesGraphicsDevice.instance.createProgram(kit.vertexShader, kit.fragmentShader, kit.name, kit.samplerUnits)
            }
            if (needBuffer) {
                newBuffer = GlesGraphicsDevice.instance.createBuffer(BufferTarget.Vertex, BufferUsage.Stream, bufferBytes, "${kit.name}-vertices")
            }
        } catch (e: RuntimeException) {
            newBuffer?.dispose()
            newProgram?.dispose()
            throw e
        }
        if (capacity != oldCapacity) {
            kit.staging = ByteBuffer.allocateDirect(bufferBytes).order(ByteOrder.nativeOrder())
        }
        if (newProgram != null) {
            kit.program = newProgram
            // Pipelines hold the old program.
            kit.pipelines.clear()
            kit.pipelineTotal = 0
        }
        if (newBuffer != null) {
            oldBuffer?.dispose()
            kit.vertexBuffer = newBuffer
        }
    }

    /** Disposes the GPU resources; the next draw creates them again. For shutdown and tests. */
    @JvmStatic
    fun releaseResources() {
        for (kit in kits) {
            kit.pipelines.clear()
            kit.pipelineTotal = 0
            kit.program?.dispose()
            kit.vertexBuffer?.dispose()
            kit.program = null
            kit.vertexBuffer = null
        }
        vertexCounter = COUNTER_UNREGISTERED
    }
}
