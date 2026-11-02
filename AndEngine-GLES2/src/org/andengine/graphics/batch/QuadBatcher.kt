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
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphSlot

/**
 * Collects textured, colored quads and draws them with as few draw calls as possible. Sprites (a
 * 2D texture) and glyphs (a layer of a [GlyphArray]) share one program and one vertex format, so
 * quads that share blend mode, sprite texture and glyph array go out in one indexed draw of up to
 * [MAX_QUADS] quads, sprites and glyphs in any mix. Corners are transformed on the CPU, so quads
 * with different model-view matrices still share a draw; the vertex shader only applies the
 * projection set with [setProjection].
 *
 * The pending quads are drawn ("flushed") when a sprite needs another 2D texture or a glyph another
 * array than the pending quads use, the blend mode or the projection changes, the batch is full, or
 * [flush] is called; switching between sprites and glyphs alone does not flush. So that the order
 * on screen stays the order of the calls, they are also flushed before anyone else changes GL
 * state through the [org.andengine.graphics.gles.GlesStateCache] (see
 * [GlesStateCache.beforeExternalChange]), and every other draw call must [flush] first. Such a flush leaves the state as it found it, so the code
 * that interrupted the batch draws with the state it set up.
 *
 * While a clip rectangle is set ([setClip]), every quad is cut to it on the CPU: one wholly outside
 * is dropped, an axis-aligned one that crosses an edge is shrunk to the clip with its texture
 * coordinates and corner colors interpolated, so quads in different clips still share a draw.
 * Only a quad that is turned (not axis-aligned) and crosses an edge is drawn with the GL scissor
 * set to the clip; such quads form batches of their own per clip rectangle. Quads without a
 * scissor of their own are drawn with the scissor test off, so a change of the GL scissor flushes
 * only quads that use it (see [GlesStateCache.beforeScissorChange]).
 *
 * The sprite texture is bound on slot [SPRITE_UNIT], the glyph array on [GLYPH_UNIT]; after every
 * draw [SPRITE_UNIT] is the active unit again, as legacy code expects.
 *
 * A flush draws into whatever framebuffer and viewport are current (see
 * [org.andengine.graphics.RenderContext.beginInlinePass]), so it can be interleaved with legacy drawing;
 * the scissor state it found is set again after its draw.
 * GPU resources are created at the first flush and restore themselves after a context loss.
 * GL thread only.
 */
object QuadBatcher {

    /** Most quads in one draw; 16-bit indices reach 4 * 4096 vertices. */
    const val MAX_QUADS = 4096

    /**
     * float2 position + float2 UV + 4 normalized bytes color + UShort layer + 2 bytes padding;
     * sprite vertices carry the layer [SPRITE_LAYER].
     */
    const val VERTEX_SIZE_BYTES = 24

    /** Layer of sprite vertices: they sample the sprite texture instead of a glyph layer. */
    const val SPRITE_LAYER = 0xFFFF

    /** Texture slot of the sprite texture; legacy code expects this unit to be active. */
    const val SPRITE_UNIT = 0

    /** Texture slot of the glyph array; a sampler of another type, so another unit. */
    const val GLYPH_UNIT = 1

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

    private const val CLIP_KEEP = 0
    private const val CLIP_DROP = 1
    private const val CLIP_SCISSOR = 2

    /** An unknown value in [GlesStateCache.saveScissorState]. */
    private const val UNKNOWN_STATE = -1

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
    const val REASON_SCISSOR = 5
    private val REASON_NAMES = arrayOf(
        "batch.flush.texture", "batch.flush.blend", "batch.flush.foreign", "batch.flush.full", "batch.flush.frame",
        "batch.flush.scissor"
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
            VertexAttribute(2, VertexFormat.UByte4Normalized, 16),
            VertexAttribute(3, VertexFormat.UShort1, 20)
        )
    )

    private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec4 aColor;
layout(location = 3) in float aLayer;
uniform mat4 uProjection;
out vec2 vTexCoord;
out vec4 vColor;
flat out float vLayer;
void main() {
    vTexCoord = aTexCoord;
    vColor = aColor;
    vLayer = aLayer;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
"""

    // The layer is flat, so all fragments of one quad take the same branch. Highp, as mediump
    // need not hold the sprite marker exactly. Sampling inside the branch is safe because the
    // selector is constant per primitive (derivatives stay uniform); keep it flat and do not
    // switch to per-vertex selection.
    private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;
precision mediump sampler2DArray;
uniform sampler2D uSprite;
uniform sampler2DArray uGlyphs;
in highp vec2 vTexCoord;
in vec4 vColor;
flat in highp float vLayer;
out vec4 fragColor;
void main() {
    if (vLayer > 65534.5) {
        fragColor = texture(uSprite, vTexCoord) * vColor;
    } else {
        fragColor = vec4(vColor.rgb, vColor.a * texture(uGlyphs, vec3(vTexCoord, vLayer)).r);
    }
}
"""

    private val SAMPLER_UNITS = mapOf("uSprite" to SPRITE_UNIT, "uGlyphs" to GLYPH_UNIT)

    /**
     * The pending vertices: position and UV as raw float bits, the color, then the layer, per
     * vertex.
     */
    private val vertices = IntArray(MAX_QUADS * INTS_PER_QUAD)
    private var quadCount = 0

    /** The 2D texture of the pending sprites; null while no sprite is pending. */
    private var texture: Texture? = null

    /** The array of the pending glyphs; null while no glyph is pending. */
    private var glyphArray: GlyphArray? = null
    private var blend: BlendMode? = null

    /** True while the pending quads are drawn with the GL scissor set to the pending box. */
    private var pendingScissored = false
    private var pendingScissorX = 0
    private var pendingScissorY = 0
    private var pendingScissorWidth = 0
    private var pendingScissorHeight = 0

    /** The clip rectangle in GL window pixels; see [setClip]. */
    private var clipActive = false
    private var clipX = 0
    private var clipY = 0
    private var clipWidth = 0
    private var clipHeight = 0

    /**
     * The clip in the coordinates of the corners (the projection's input); only valid while
     * [clipInWorld]. Computed for the surface size [clipSurfaceWidth] x [clipSurfaceHeight]; a
     * width of -1 makes the next clipped quad compute it again.
     */
    private var clipMinX = 0f
    private var clipMaxX = 0f
    private var clipMinY = 0f
    private var clipMaxY = 0f
    private var clipInWorld = false
    private var clipSurfaceWidth = -1
    private var clipSurfaceHeight = -1

    // The corners of the quad being added, before they are written: position, UV and the ABGR
    // color (only filled when every corner has its own).
    private val cornerX = FloatArray(4)
    private val cornerY = FloatArray(4)
    private val cornerU = FloatArray(4)
    private val cornerV = FloatArray(4)
    private val cornerColor = IntArray(4)

    // The attributes before clamping, which the clamped corners are interpolated from.
    private val sourceU = FloatArray(4)
    private val sourceV = FloatArray(4)
    private val sourceColor = IntArray(4)

    /** The scissor found by a draw of the batch, set again after it. */
    private val savedScissor = IntArray(GlesStateCache.SCISSOR_STATE_SIZE)

    private val projection = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    // The color int is ABGR (red in the low byte); in memory the bytes must be R, G, B, A.
    private val littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    /** [SPRITE_LAYER] as the last int of a vertex. */
    private val spriteLayer = layerInt(SPRITE_LAYER)

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
        clipSurfaceWidth = -1
    }

    /**
     * Cuts the following quads to the rectangle of [width] x [height] pixels from ([x], [y]) in GL
     * window pixels (origin bottom-left, as `glScissor`), until [clearClip] or the next call. Does
     * not flush: quads added before keep the clip they were added with. The pixels are mapped to
     * the corners' coordinates through the projection and the surface size, as the glyph pixel
     * snapping does; without a surface size or with a projection that is not a plain orthographic
     * scale and offset, clipped quads are drawn with the GL scissor instead. The viewport is assumed
     * to cover the whole surface; inside a pass onto a target of another size the clip would be wrong.
     */
    @JvmStatic
    fun setClip(x: Int, y: Int, width: Int, height: Int) {
        require(width >= 0 && height >= 0) { "A clip needs a size of at least 0, got $width x $height" }
        clipActive = true
        clipX = x
        clipY = y
        clipWidth = width
        clipHeight = height
        clipSurfaceWidth = -1
    }

    /** Stops cutting the following quads; see [setClip]. Does not flush. */
    @JvmStatic
    fun clearClip() {
        clipActive = false
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
        if (!isDrawable(texture)) return
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        corner(0, a * x0 + c * y0 + tx, b * x0 + d * y0 + ty, u0, v0)
        corner(1, a * x1 + c * y0 + tx, b * x1 + d * y0 + ty, u1, v0)
        corner(2, a * x1 + c * y1 + tx, b * x1 + d * y1 + ty, u1, v1)
        corner(3, a * x0 + c * y1 + tx, b * x0 + d * y1 + ty, u0, v1)
        addSpriteCorners(texture, blend, rgba)
    }

    /**
     * Adds a free quad whose 4 corners ([x], [y], already in the projection's coordinates) are
     * given in order around the quad, with their texture coordinates [u], [v] and the ABGR packed
     * color [rgba] on all corners. Skipped like [add] while [texture] is not loaded.
     */
    @JvmStatic
    fun addQuad(texture: Texture, blend: BlendMode, x: FloatArray, y: FloatArray, u: FloatArray, v: FloatArray, rgba: Int) {
        if (!isDrawable(texture)) return
        for (k in 0 until 4) corner(k, x[k], y[k], u[k], v[k])
        addSpriteCorners(texture, blend, rgba)
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
        if (!isDrawable(texture)) return
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        for (k in 0 until 4) {
            val lx = x[k]
            val ly = y[k]
            corner(k, a * lx + c * ly + tx, b * lx + d * ly + ty, u[k], v[k])
        }
        addSpriteCorners(texture, blend, rgba)
    }

    /**
     * [addQuad] with the corners passed as values, for callers that add many quads in a row
     * (storyboards, particles): the corners ([x0], [y0]) to ([x3], [y3]) in local coordinates in
     * the order of [addQuad], transformed by the 2D part of the column-major model-view [matrix];
     * the texture rectangle ([u0], [v0])-([u1], [v1]) runs from corner 0 to corner 2 (swap the
     * ends to mirror). A quad that continues the pending batch (same texture and blend mode, no
     * clip, room left) is written straight away; any other goes the general way of [addQuad].
     */
    @JvmStatic
    fun addQuad(
        texture: Texture,
        blend: BlendMode,
        matrix: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        rgba: Int
    ) {
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        val px0 = a * x0 + c * y0 + tx
        val py0 = b * x0 + d * y0 + ty
        val px1 = a * x1 + c * y1 + tx
        val py1 = b * x1 + d * y1 + ty
        val px2 = a * x2 + c * y2 + tx
        val py2 = b * x2 + d * y2 + ty
        val px3 = a * x3 + c * y3 + tx
        val py3 = b * x3 + d * y3 + ty

        val count = quadCount
        if (count > 0 && count < MAX_QUADS && texture === this.texture && blend === this.blend &&
            !clipActive && !pendingScissored
        ) {
            // Continues the pending batch: already checked, keyed and armed when it started.
            val color = vertexColor(rgba)
            val layer = spriteLayer
            var i = count * INTS_PER_QUAD
            i = put(i, px0, py0, u0, v0, color, layer)
            i = put(i, px1, py1, u1, v0, color, layer)
            i = put(i, px2, py2, u1, v1, color, layer)
            put(i, px3, py3, u0, v1, color, layer)
            quadCount = count + 1
            return
        }

        if (!isDrawable(texture)) return
        corner(0, px0, py0, u0, v0)
        corner(1, px1, py1, u1, v0)
        corner(2, px2, py2, u1, v1)
        corner(3, px3, py3, u0, v1)
        addSpriteCorners(texture, blend, rgba)
    }

    /**
     * Adds the glyph rectangle ([x0], [y0])-([x1], [y1]) in local coordinates, transformed by
     * [matrix] like [add], with the UVs and layer of [slot] and the ABGR packed color [rgba]. Only
     * the alpha of the glyph image is used. The transformed top-left corner is then rounded to a
     * whole physical pixel (through the current projection and the surface size) and all corners
     * move by the same amount, so the size stays; without a surface size nothing is rounded.
     */
    @JvmStatic
    fun addGlyph(slot: GlyphSlot, blend: BlendMode, matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, rgba: Int) {
        // Not uploaded to the current texture (yet): its texels are undefined, draw nothing.
        if (!slot.uploaded) return
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]

        var ox = a * x0 + c * y0 + tx
        var oy = b * x0 + d * y0 + ty
        val sw = GlesBackend.surfaceWidth
        val sh = GlesBackend.surfaceHeight
        val p = projection
        if (sw > 0 && sh > 0 && p[0] != 0f && p[5] != 0f) {
            val ndcX = p[0] * ox + p[4] * oy + p[12]
            val ndcY = p[1] * ox + p[5] * oy + p[13]
            val px = (ndcX + 1f) * 0.5f * sw
            val py = (ndcY + 1f) * 0.5f * sh
            val snappedX = Math.round(px) * 2f / sw - 1f
            val snappedY = Math.round(py) * 2f / sh - 1f
            ox += (snappedX - ndcX) / p[0]
            oy += (snappedY - ndcY) / p[5]
        }

        val w0 = a * (x1 - x0)
        val w1 = b * (x1 - x0)
        val h0 = c * (y1 - y0)
        val h1 = d * (y1 - y0)
        corner(0, ox, oy, slot.u, slot.v)
        corner(1, ox + w0, oy + w1, slot.u2, slot.v)
        corner(2, ox + w0 + h0, oy + w1 + h1, slot.u2, slot.v2)
        corner(3, ox + h0, oy + h1, slot.u, slot.v2)
        addGlyphCorners(slot, blend, vertexColor(rgba), false)
    }

    /**
     * Adds a free glyph quad: the 4 corners ([x], [y] in local coordinates, in the order of
     * [addQuad]) are transformed by the 2D part of the column-major model-view [matrix], with the
     * UVs [u], [v] and the ABGR packed color of that corner in [rgba]. Unlike [addGlyph] nothing is
     * snapped to whole pixels. Only the alpha of the glyph image is used. Skipped while [slot] is not
     * uploaded.
     */
    @JvmStatic
    fun addGlyphQuad(
        slot: GlyphSlot,
        blend: BlendMode,
        matrix: FloatArray,
        x: FloatArray, y: FloatArray, u: FloatArray, v: FloatArray,
        rgba: IntArray
    ) {
        if (!slot.uploaded) return
        val a = matrix[0]
        val b = matrix[1]
        val c = matrix[4]
        val d = matrix[5]
        val tx = matrix[12]
        val ty = matrix[13]
        for (k in 0 until 4) {
            val lx = x[k]
            val ly = y[k]
            corner(k, a * lx + c * ly + tx, b * lx + d * ly + ty, u[k], v[k])
            cornerColor[k] = rgba[k]
        }
        addGlyphCorners(slot, blend, 0, true)
    }

    private fun corner(k: Int, x: Float, y: Float, u: Float, v: Float) {
        cornerX[k] = x
        cornerY[k] = y
        cornerU[k] = u
        cornerV[k] = v
    }

    /** Clips the corners and adds them as a sprite quad of [texture]. */
    private fun addSpriteCorners(texture: Texture, blend: BlendMode, rgba: Int) {
        val clip = clipCorners(false)
        if (clip == CLIP_DROP) return
        val scissored = clip == CLIP_SCISSOR
        prepare(texture, blend, scissored)
        arm(scissored)
        writeCorners(vertexColor(rgba), false, spriteLayer)
    }

    /**
     * Clips the corners and adds them as a glyph quad of [slot], with the vertex [color] on every
     * corner or, if [perCornerColor], the colors in [cornerColor].
     */
    private fun addGlyphCorners(slot: GlyphSlot, blend: BlendMode, color: Int, perCornerColor: Boolean) {
        val clip = clipCorners(perCornerColor)
        if (clip == CLIP_DROP) return
        val scissored = clip == CLIP_SCISSOR
        prepareGlyph(slot.array, blend, scissored)
        arm(scissored)
        writeCorners(color, perCornerColor, layerInt(slot.layer))
    }

    private fun writeCorners(color: Int, perCornerColor: Boolean, layer: Int) {
        var i = quadCount * INTS_PER_QUAD
        for (k in 0 until 4) {
            val c = if (perCornerColor) vertexColor(cornerColor[k]) else color
            i = put(i, cornerX[k], cornerY[k], cornerU[k], cornerV[k], c, layer)
        }
        quadCount++
    }

    /**
     * Cuts the corners to the clip, if one is set: [CLIP_KEEP] when they are inside or were
     * clamped to it in place, [CLIP_DROP] when the quad lies wholly outside, [CLIP_SCISSOR] when it
     * crosses an edge but cannot be cut on the CPU.
     */
    private fun clipCorners(perCornerColor: Boolean): Int {
        if (!clipActive) return CLIP_KEEP
        updateClipSpace()
        if (!clipInWorld) return CLIP_SCISSOR
        val x = cornerX
        val y = cornerY
        val minX = Math.min(Math.min(x[0], x[1]), Math.min(x[2], x[3]))
        val maxX = Math.max(Math.max(x[0], x[1]), Math.max(x[2], x[3]))
        val minY = Math.min(Math.min(y[0], y[1]), Math.min(y[2], y[3]))
        val maxY = Math.max(Math.max(y[0], y[1]), Math.max(y[2], y[3]))
        if (minX >= clipMinX && maxX <= clipMaxX && minY >= clipMinY && maxY <= clipMaxY) return CLIP_KEEP
        if (maxX <= clipMinX || minX >= clipMaxX || maxY <= clipMinY || minY >= clipMaxY) return CLIP_DROP
        // Axis-aligned with the edge from corner 0 to corner 1 along x, or along y (turned by a
        // multiple of 90 degrees).
        val alongX = x[0] == x[3] && x[1] == x[2] && y[0] == y[1] && y[2] == y[3]
        if (!alongX && !(x[0] == x[1] && x[2] == x[3] && y[0] == y[3] && y[1] == y[2])) return CLIP_SCISSOR
        clampCorners(alongX, perCornerColor)
        return CLIP_KEEP
    }

    /**
     * Moves the corners of an axis-aligned quad into the clip and interpolates their UVs (and
     * colors) bilinearly between the original corners, so the visible part looks as before.
     */
    private fun clampCorners(alongX: Boolean, perCornerColor: Boolean) {
        val x = cornerX
        val y = cornerY
        val u = cornerU
        val v = cornerV
        for (k in 0 until 4) {
            sourceU[k] = u[k]
            sourceV[k] = v[k]
            sourceColor[k] = cornerColor[k]
        }
        val x0 = x[0]
        val y0 = y[0]
        // Lengths of the edges from corner 0 to corner 1 (s) and to corner 3 (t).
        val sLength = if (alongX) x[1] - x0 else y[1] - y0
        val tLength = if (alongX) y[3] - y0 else x[3] - x0
        for (k in 0 until 4) {
            val px = x[k].coerceIn(clipMinX, clipMaxX)
            val py = y[k].coerceIn(clipMinY, clipMaxY)
            x[k] = px
            y[k] = py
            val s = if (sLength == 0f) 0f else (if (alongX) px - x0 else py - y0) / sLength
            val t = if (tLength == 0f) 0f else (if (alongX) py - y0 else px - x0) / tLength
            val w0 = (1f - s) * (1f - t)
            val w1 = s * (1f - t)
            val w2 = s * t
            val w3 = (1f - s) * t
            u[k] = sourceU[0] * w0 + sourceU[1] * w1 + sourceU[2] * w2 + sourceU[3] * w3
            v[k] = sourceV[0] * w0 + sourceV[1] * w1 + sourceV[2] * w2 + sourceV[3] * w3
            if (perCornerColor) cornerColor[k] = mixColor(w0, w1, w2, w3)
        }
    }

    /** The [sourceColor]s weighted per byte, rounded; all ABGR. */
    private fun mixColor(w0: Float, w1: Float, w2: Float, w3: Float): Int {
        val c = sourceColor
        var result = 0
        var shift = 0
        while (shift < 32) {
            val value = ((c[0] ushr shift) and 0xFF) * w0 + ((c[1] ushr shift) and 0xFF) * w1 +
                ((c[2] ushr shift) and 0xFF) * w2 + ((c[3] ushr shift) and 0xFF) * w3
            result = result or ((value + 0.5f).toInt().coerceIn(0, 255) shl shift)
            shift += 8
        }
        return result
    }

    /**
     * Maps the pixel clip into the corners' coordinates for the current projection and surface
     * size, unless already done for them.
     */
    private fun updateClipSpace() {
        val sw = GlesBackend.surfaceWidth
        val sh = GlesBackend.surfaceHeight
        if (sw == clipSurfaceWidth && sh == clipSurfaceHeight) return
        clipSurfaceWidth = sw
        clipSurfaceHeight = sh
        val p = projection
        // Only a scale and an offset per axis can be inverted per axis.
        clipInWorld = sw > 0 && sh > 0 && p[0] != 0f && p[5] != 0f &&
            p[1] == 0f && p[4] == 0f && p[3] == 0f && p[7] == 0f && p[15] == 1f
        if (!clipInWorld) return
        val left = (clipX.toFloat() / sw * 2f - 1f - p[12]) / p[0]
        val right = ((clipX.toFloat() + clipWidth) / sw * 2f - 1f - p[12]) / p[0]
        val bottom = (clipY.toFloat() / sh * 2f - 1f - p[13]) / p[5]
        val top = ((clipY.toFloat() + clipHeight) / sh * 2f - 1f - p[13]) / p[5]
        clipMinX = Math.min(left, right)
        clipMaxX = Math.max(left, right)
        clipMinY = Math.min(bottom, top)
        clipMaxY = Math.max(bottom, top)
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
    fun flushFrame() {
        flushFor(REASON_FRAME)
        BatchTexture.endFrame()
    }

    /**
     * [BatchTexture.isLoaded], skipped for the texture of the pending quads: it was checked when
     * they started, and textures are only loaded or unloaded between batches, not within one.
     */
    private fun isDrawable(texture: Texture): Boolean =
        (texture === this.texture && quadCount > 0) || BatchTexture.isLoaded(texture)

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

    /**
     * Arms [flushBeforeChange] on the current state cache, which is new after a context loss; for
     * [scissored] quads also before a scissor change.
     */
    private fun arm(scissored: Boolean) {
        val state = GlesBackend.state
        if (state.beforeExternalChange !== flushBeforeChange) {
            state.beforeExternalChange = flushBeforeChange
            armedState = state
        }
        if (scissored && state.beforeScissorChange !== flushBeforeChange) {
            state.beforeScissorChange = flushBeforeChange
            armedState = state
        }
    }

    private fun disarm() {
        val state = armedState ?: return
        armedState = null
        // The cache clears a hook itself before running it.
        if (state.beforeExternalChange === flushBeforeChange) state.beforeExternalChange = null
        if (state.beforeScissorChange === flushBeforeChange) state.beforeScissorChange = null
    }

    /** Draws the pending quads; the batch's own state changes must not flush again. */
    private fun drawPending(reason: Int) {
        val count = quadCount
        if (count == 0) return
        countFlush(reason)
        disarm()
        val pipelineBlend = blend!!
        val array = glyphArray
        val spriteTexture = texture
        val scissored = pendingScissored
        // Dropped even if the draw fails, so a broken batch is not retried with every add.
        quadCount = 0
        texture = null
        glyphArray = null
        pendingScissored = false

        ensureResources()
        val vb = vertexBuffer!!
        val ints = uploadInts!!
        ints.clear()
        ints.put(vertices, 0, count * INTS_PER_QUAD)
        val bytes = uploadBytes!!
        bytes.clear()
        // New storage per flush: rewriting storage that earlier draws still read makes the
        // driver wait or copy the whole buffer on every flush.
        vb.replace(bytes, count * QUAD_SIZE_BYTES)
        val pipeline = pipelineFor(pipelineBlend)
        val context = GlesRenderContext.instance
        val state = GlesBackend.state
        val scissor = savedScissor
        state.saveScissorState(scissor, 0)
        // Quads were cut on the CPU, or need the scissor of their clip.
        if (scissored) {
            state.setScissorEnabled(true)
            state.scissor(pendingScissorX, pendingScissorY, pendingScissorWidth, pendingScissorHeight)
        } else {
            state.setScissorEnabled(false)
        }
        val ownPass = !context.isInPass
        if (ownPass) context.beginInlinePass()
        try {
            context.setPipeline(pipeline)
            context.setUniformMatrix4(pipeline.program.uniformLocation("uProjection"), projection)
            // Glyphs first, so the sprite unit ends up active. A unit without quads of its kind in
            // this batch keeps what is bound there; it is not sampled.
            if (array != null) context.setTexture(GLYPH_UNIT, array.texture, null)
            if (spriteTexture != null) context.setTexture(SPRITE_UNIT, spriteTexture, null)
            context.setVertexBuffer(vb)
            context.setIndexBuffer(indexBuffer!!)
            context.drawIndexed(0, count * INDICES_PER_QUAD)
        } finally {
            if (ownPass) {
                context.endPass()
            } else {
                // Legacy code binds its textures on the active unit and expects it to be unit 0.
                state.activeTexture(SPRITE_UNIT)
            }
            state.restoreScissorState(scissor, 0)
            // Not known before: off, as GL starts, so later draws are not cut by this clip.
            if (scissored && scissor[0] == UNKNOWN_STATE) state.setScissorEnabled(false)
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
     * values: texture, blend, foreign, full, frame, scissor); -1 if registering failed, -2 before the
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
        BatchTexture.endFrame()
        texture = null
        glyphArray = null
        blend = null
        pendingScissored = false
        clipActive = false
        clipSurfaceWidth = -1
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

    /**
     * Makes room for a glyph of [array]: flushes when full, or for another array, blend mode or
     * scissor ([scissored] means the current clip).
     */
    private fun prepareGlyph(array: GlyphArray, blend: BlendMode, scissored: Boolean) {
        if (quadCount == MAX_QUADS) {
            drawPending(REASON_FULL)
        } else if (quadCount > 0) {
            val pending = glyphArray
            if (pending != null && pending !== array) {
                drawPending(REASON_TEXTURE)
            } else {
                flushForKey(blend, scissored)
            }
        }
        glyphArray = array
        setKey(blend, scissored)
    }

    /**
     * Makes room for a sprite of [texture]: flushes when full, or for another texture, blend mode
     * or scissor ([scissored] means the current clip).
     */
    private fun prepare(texture: Texture, blend: BlendMode, scissored: Boolean) {
        if (quadCount == MAX_QUADS) {
            drawPending(REASON_FULL)
        } else if (quadCount > 0) {
            val pending = this.texture
            if (pending != null && pending !== texture) {
                drawPending(REASON_TEXTURE)
            } else {
                flushForKey(blend, scissored)
            }
        }
        this.texture = texture
        setKey(blend, scissored)
    }

    /** Flushes the pending quads if their blend mode or scissor differs. */
    private fun flushForKey(blend: BlendMode, scissored: Boolean) {
        if (blend !== this.blend && blend != this.blend) {
            drawPending(REASON_BLEND)
        } else if (scissored != pendingScissored || scissored && !pendingScissorIsClip()) {
            drawPending(REASON_SCISSOR)
        }
    }

    private fun pendingScissorIsClip(): Boolean =
        pendingScissorX == clipX && pendingScissorY == clipY &&
            pendingScissorWidth == clipWidth && pendingScissorHeight == clipHeight

    private fun setKey(blend: BlendMode, scissored: Boolean) {
        this.blend = blend
        pendingScissored = scissored
        if (scissored) {
            pendingScissorX = clipX
            pendingScissorY = clipY
            pendingScissorWidth = clipWidth
            pendingScissorHeight = clipHeight
        }
    }

    private fun vertexColor(abgr: Int): Int = if (littleEndian) abgr else Integer.reverseBytes(abgr)

    /** [layer] as the last int of a vertex: the UShort first in memory, then the padding. */
    private fun layerInt(layer: Int): Int = if (littleEndian) layer else layer shl 16

    @Suppress("NOTHING_TO_INLINE")
    private inline fun put(index: Int, x: Float, y: Float, u: Float, v: Float, color: Int, layer: Int): Int {
        val a = vertices
        a[index] = java.lang.Float.floatToRawIntBits(x)
        a[index + 1] = java.lang.Float.floatToRawIntBits(y)
        a[index + 2] = java.lang.Float.floatToRawIntBits(u)
        a[index + 3] = java.lang.Float.floatToRawIntBits(v)
        a[index + 4] = color
        a[index + 5] = layer
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
            newProgram = device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER, "quad-batch", SAMPLER_UNITS)
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
