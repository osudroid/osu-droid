package org.andengine.graphics.shape

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.text.GlyphSlot
import org.andengine.opengl.font.FontManager

/**
 * Breaks UI shapes (rectangles, rounded rectangles, outlines, lines, triangles, circles) into
 * quads over the coverage masks of [ShapeMasks] and adds them to the [QuadBatcher] as glyph quads,
 * so shapes and text share draws. Geometry is in virtual units in the local coordinates of
 * `matrix` (column-major 4x4, only its 2D part is used); mask sizes are derived from it with
 * [FontManager.getRasterScale]. Shapes are not snapped to pixels. A color with alpha 0 (top byte of
 * the ABGR int, see [QuadBatcher.packColor]) adds nothing. The caller sets the projection. GL thread
 * only; allocation-free once the masks exist.
 */
object ShapeBatching {

    /**
     * The masks to use; null means [ShapeMasks.shared]. Set only on the GL thread before use (tests).
     */
    @JvmField
    var masks: ShapeMasks? = null

    private val qx = FloatArray(4)
    private val qy = FloatArray(4)
    private val qu = FloatArray(4)
    private val qv = FloatArray(4)
    private val qc = IntArray(4)

    private fun masks(): ShapeMasks = masks ?: ShapeMasks.shared

    private fun visible(rgba: Int): Boolean = (rgba ushr 24) != 0

    /** Mask pixel size for a virtual size, quantized so animated sizes do not mint a mask per frame. */
    private fun px(virtual: Float): Int =
        ShapeMasks.quantize(max(1, Math.round(virtual * FontManager.getRasterScale())))

    /** Axis aligned quad with the UV rectangle ([u0], [v0])-([u1], [v1]) and one color. */
    private fun box(
        slot: GlyphSlot, blend: BlendMode, m: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, rgba: Int
    ) {
        qx[0] = x0; qy[0] = y0; qu[0] = u0; qv[0] = v0
        qx[1] = x1; qy[1] = y0; qu[1] = u1; qv[1] = v0
        qx[2] = x1; qy[2] = y1; qu[2] = u1; qv[2] = v1
        qx[3] = x0; qy[3] = y1; qu[3] = u0; qv[3] = v1
        qc.fill(rgba)
        QuadBatcher.addGlyphQuad(slot, blend, m, qx, qy, qu, qv, qc)
    }

    /** Solid fill of a rectangle; empty rectangles are skipped. */
    private fun fill(slot: GlyphSlot, blend: BlendMode, m: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, rgba: Int) {
        if (x1 <= x0 || y1 <= y0) return
        val u = ShapeMasks.centerU(slot)
        val v = ShapeMasks.centerV(slot)
        box(slot, blend, m, x0, y0, x1, y1, u, v, u, v, rgba)
    }

    /**
     * A corner mask in the box ([x0], [y0])-([x1], [y1]); the mask has coverage toward its bottom
     * right, [flipX] / [flipY] mirror it so that the coverage points to the circle center.
     */
    private fun maskBox(
        mask: GlyphSlot, blend: BlendMode, m: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float, flipX: Boolean, flipY: Boolean, rgba: Int
    ) {
        val innerU = ShapeMasks.innerU(mask)
        val innerV = ShapeMasks.innerV(mask)
        val u0 = if (flipX) innerU else mask.u
        val u1 = if (flipX) mask.u else innerU
        val v0 = if (flipY) innerV else mask.v
        val v1 = if (flipY) mask.v else innerV
        box(mask, blend, m, x0, y0, x1, y1, u0, v0, u1, v1, rgba)
    }

    @JvmStatic
    fun rect(matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, rgba: Int, blend: BlendMode) {
        if (!visible(rgba)) return
        fill(masks().solid(), blend, matrix, x0, y0, x1, y1, rgba)
    }

    @JvmStatic
    fun gradientRect(
        matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float,
        topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int, blend: BlendMode
    ) {
        if (!visible(topLeft) && !visible(topRight) && !visible(bottomLeft) && !visible(bottomRight)) return
        if (x1 <= x0 || y1 <= y0) return
        val slot = masks().solid()
        qx[0] = x0; qy[0] = y0
        qx[1] = x1; qy[1] = y0
        qx[2] = x1; qy[2] = y1
        qx[3] = x0; qy[3] = y1
        qu.fill(ShapeMasks.centerU(slot))
        qv.fill(ShapeMasks.centerV(slot))
        qc[0] = topLeft; qc[1] = topRight; qc[2] = bottomRight; qc[3] = bottomLeft
        QuadBatcher.addGlyphQuad(slot, blend, matrix, qx, qy, qu, qv, qc)
    }

    @JvmStatic
    fun roundedRect(matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, radius: Float, rgba: Int, blend: BlendMode) {
        if (!visible(rgba)) return
        val r = min(radius, min(x1 - x0, y1 - y0) * 0.5f)
        if (!(r > 0f)) {
            rect(matrix, x0, y0, x1, y1, rgba, blend)
            return
        }
        val ms = masks()
        val corner = ms.corner(px(r))
        val solid = ms.solid()
        val ix0 = x0 + r
        val ix1 = x1 - r
        val iy0 = y0 + r
        val iy1 = y1 - r
        maskBox(corner, blend, matrix, x0, y0, ix0, iy0, false, false, rgba)
        maskBox(corner, blend, matrix, ix1, y0, x1, iy0, true, false, rgba)
        maskBox(corner, blend, matrix, x0, iy1, ix0, y1, false, true, rgba)
        maskBox(corner, blend, matrix, ix1, iy1, x1, y1, true, true, rgba)
        fill(solid, blend, matrix, ix0, y0, ix1, iy0, rgba)
        fill(solid, blend, matrix, ix0, iy1, ix1, y1, rgba)
        fill(solid, blend, matrix, x0, iy0, ix0, iy1, rgba)
        fill(solid, blend, matrix, ix1, iy0, x1, iy1, rgba)
        fill(solid, blend, matrix, ix0, iy0, ix1, iy1, rgba)
    }

    /**
     * The outline lies inside the box. By design it is never thicker than the corner radius, and an
     * outline without radius is limited to half the box; [lineWidth] is clamped accordingly.
     */
    @JvmStatic
    fun roundedOutline(
        matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float,
        radius: Float, lineWidth: Float, rgba: Int, blend: BlendMode
    ) {
        if (!visible(rgba) || !(lineWidth > 0f)) return
        val half = min(x1 - x0, y1 - y0) * 0.5f
        val r = max(0f, min(radius, half))
        val lw = min(lineWidth, if (r > 0f) r else half)
        if (!(lw > 0f)) return
        val ms = masks()
        val solid = ms.solid()
        if (r <= 0f) {
            fill(solid, blend, matrix, x0, y0, x1, y0 + lw, rgba)
            fill(solid, blend, matrix, x0, y1 - lw, x1, y1, rgba)
            fill(solid, blend, matrix, x0, y0 + lw, x0 + lw, y1 - lw, rgba)
            fill(solid, blend, matrix, x1 - lw, y0 + lw, x1, y1 - lw, rgba)
            return
        }
        val ring = ms.ring(px(r), px(lw))
        val ix0 = x0 + r
        val ix1 = x1 - r
        val iy0 = y0 + r
        val iy1 = y1 - r
        maskBox(ring, blend, matrix, x0, y0, ix0, iy0, false, false, rgba)
        maskBox(ring, blend, matrix, ix1, y0, x1, iy0, true, false, rgba)
        maskBox(ring, blend, matrix, x0, iy1, ix0, y1, false, true, rgba)
        maskBox(ring, blend, matrix, ix1, iy1, x1, y1, true, true, rgba)
        fill(solid, blend, matrix, ix0, y0, ix1, y0 + lw, rgba)
        fill(solid, blend, matrix, ix0, y1 - lw, ix1, y1, rgba)
        fill(solid, blend, matrix, x0, iy0, x0 + lw, iy1, rgba)
        fill(solid, blend, matrix, x1 - lw, iy0, x1, iy1, rgba)
    }

    /** A line of [lineWidth] centered on the segment, with flat ends. */
    @JvmStatic
    fun line(matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, lineWidth: Float, rgba: Int, blend: BlendMode) {
        segment(matrix, x0, y0, x1, y1, lineWidth, 0f, rgba, blend)
    }

    /**
     * The outline of a triangle: its three edges as lines of [lineWidth] centered on them, each
     * extended by half the width at both ends so the corners are filled.
     */
    @JvmStatic
    fun triangleOutline(
        matrix: FloatArray, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float,
        lineWidth: Float, rgba: Int, blend: BlendMode
    ) {
        val extend = lineWidth * 0.5f
        segment(matrix, ax, ay, bx, by, lineWidth, extend, rgba, blend)
        segment(matrix, bx, by, cx, cy, lineWidth, extend, rgba, blend)
        segment(matrix, cx, cy, ax, ay, lineWidth, extend, rgba, blend)
    }

    private fun segment(
        matrix: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float,
        lineWidth: Float, extend: Float, rgba: Int, blend: BlendMode
    ) {
        if (!visible(rgba)) return
        val dx = x1 - x0
        val dy = y1 - y0
        val length = sqrt(dx * dx + dy * dy)
        if (!(length > 0f) || !(lineWidth > 0f)) return
        val nx = -dy / length * lineWidth * 0.5f
        val ny = dx / length * lineWidth * 0.5f
        val ex = dx / length * extend
        val ey = dy / length * extend
        val slot = masks().solid()
        qx[0] = x0 - ex + nx; qy[0] = y0 - ey + ny
        qx[1] = x1 + ex + nx; qy[1] = y1 + ey + ny
        qx[2] = x1 + ex - nx; qy[2] = y1 + ey - ny
        qx[3] = x0 - ex - nx; qy[3] = y0 - ey - ny
        qu.fill(ShapeMasks.centerU(slot))
        qv.fill(ShapeMasks.centerV(slot))
        qc.fill(rgba)
        QuadBatcher.addGlyphQuad(slot, blend, matrix, qx, qy, qu, qv, qc)
    }

    /** A triangle as a quad whose fourth corner repeats the third. */
    @JvmStatic
    fun triangle(matrix: FloatArray, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, rgba: Int, blend: BlendMode) {
        if (!visible(rgba)) return
        val slot = masks().solid()
        qx[0] = ax; qy[0] = ay
        qx[1] = bx; qy[1] = by
        qx[2] = cx; qy[2] = cy
        qx[3] = cx; qy[3] = cy
        qu.fill(ShapeMasks.centerU(slot))
        qv.fill(ShapeMasks.centerV(slot))
        qc.fill(rgba)
        QuadBatcher.addGlyphQuad(slot, blend, matrix, qx, qy, qu, qv, qc)
    }

    @JvmStatic
    fun circle(matrix: FloatArray, cx: Float, cy: Float, radius: Float, rgba: Int, blend: BlendMode) {
        if (!visible(rgba) || !(radius > 0f)) return
        quarters(masks().corner(px(radius)), matrix, cx, cy, radius, rgba, blend)
    }

    /** A ring whose outer edge is [radius] and whose band is [lineWidth] wide. */
    @JvmStatic
    fun ring(matrix: FloatArray, cx: Float, cy: Float, radius: Float, lineWidth: Float, rgba: Int, blend: BlendMode) {
        if (!visible(rgba) || !(radius > 0f) || !(lineWidth > 0f)) return
        quarters(masks().ring(px(radius), px(min(lineWidth, radius))), matrix, cx, cy, radius, rgba, blend)
    }

    /**
     * A filled fan over an elliptical arc with radii [rx], [ry] around ([cx], [cy]): one triangle
     * per segment between the center and two neighbouring arc points. The [segments] points run
     * evenly from [startAngle] to [endAngle] (degrees, 0 pointing up, clockwise on screen).
     */
    @JvmStatic
    fun sector(
        matrix: FloatArray, cx: Float, cy: Float, rx: Float, ry: Float,
        startAngle: Float, endAngle: Float, segments: Int, rgba: Int, blend: BlendMode
    ) {
        if (!visible(rgba) || segments < 2) return
        val start = toRadians(startAngle - 90f)
        val delta = (toRadians(endAngle - 90f) - start) / max(1, segments - 1)
        val slot = masks().solid()
        qu.fill(ShapeMasks.centerU(slot))
        qv.fill(ShapeMasks.centerV(slot))
        qc.fill(rgba)
        qx[0] = cx; qy[0] = cy
        var pcos = cos(start)
        var psin = sin(start)
        for (j in 1 until segments) {
            val angle = start + j * delta
            val c = cos(angle)
            val s = sin(angle)
            qx[1] = cx + rx * pcos; qy[1] = cy + ry * psin
            qx[2] = cx + rx * c; qy[2] = cy + ry * s
            qx[3] = qx[2]; qy[3] = qy[2]
            QuadBatcher.addGlyphQuad(slot, blend, matrix, qx, qy, qu, qv, qc)
            pcos = c
            psin = s
        }
    }

    /**
     * The outline of an elliptical arc as a strip of quads, [lineWidth] wide and centered on the arc
     * (radius +- half the width). On ellipses (rx != ry) the band is offset radially per axis, not along the
     * curve normal. Points as in [sector]. [lineWidth] is in the units of the
     * geometry, the caller converts from pixels.
     */
    @JvmStatic
    fun arc(
        matrix: FloatArray, cx: Float, cy: Float, rx: Float, ry: Float,
        startAngle: Float, endAngle: Float, segments: Int, lineWidth: Float, rgba: Int, blend: BlendMode
    ) {
        if (!visible(rgba) || segments < 2 || !(lineWidth > 0f)) return
        val half = lineWidth * 0.5f
        val inX = max(0f, rx - half)
        val inY = max(0f, ry - half)
        val outX = rx + half
        val outY = ry + half
        val slot = masks().solid()
        qu.fill(ShapeMasks.centerU(slot))
        qv.fill(ShapeMasks.centerV(slot))
        qc.fill(rgba)
        val start = toRadians(startAngle - 90f)
        val delta = (toRadians(endAngle - 90f) - start) / max(1, segments - 1)
        var pc = cos(start)
        var ps = sin(start)
        for (j in 1 until segments) {
            val angle = start + j * delta
            val c = cos(angle)
            val s = sin(angle)
            qx[0] = cx + inX * pc; qy[0] = cy + inY * ps
            qx[1] = cx + outX * pc; qy[1] = cy + outY * ps
            qx[2] = cx + outX * c; qy[2] = cy + outY * s
            qx[3] = cx + inX * c; qy[3] = cy + inY * s
            QuadBatcher.addGlyphQuad(slot, blend, matrix, qx, qy, qu, qv, qc)
            pc = c
            ps = s
        }
    }

    private fun toRadians(degrees: Float): Float = degrees * Math.PI.toFloat() / 180

    private fun quarters(mask: GlyphSlot, m: FloatArray, cx: Float, cy: Float, r: Float, rgba: Int, blend: BlendMode) {
        maskBox(mask, blend, m, cx - r, cy - r, cx, cy, false, false, rgba)
        maskBox(mask, blend, m, cx, cy - r, cx + r, cy, true, false, rgba)
        maskBox(mask, blend, m, cx - r, cy, cx, cy + r, false, true, rgba)
        maskBox(mask, blend, m, cx, cy, cx + r, cy + r, true, true, rgba)
    }
}
