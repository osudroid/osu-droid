package com.reco1l.andengine.shape

import com.reco1l.andengine.component.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UIBox.*
import com.reco1l.andengine.shape.PaintStyle.*
import com.reco1l.andengine.shape.UICircle.Companion.calculateArcResolution
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UIBox : UIBufferedComponent<BoxVBO>() {

    /**
     * The style of painting for the box.
     */
    var paintStyle = Fill
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * The line width of the box if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f


    private val safeRadius
        get() = radius.coerceAtMost(min(width, height) / 2f).coerceAtLeast(0f)

    private val segments
        get() = if (safeRadius > 0f) calculateArcResolution(safeRadius, safeRadius, 90f) else 0


    override fun createBuffer(): BoxVBO {
        return BoxVBO(segments, paintStyle)
    }

    override fun canReuseBuffer(buffer: BoxVBO): Boolean {
        return buffer.segments == segments && buffer.paintStyle == paintStyle
    }

    override fun generateBufferCacheKey(): String {
        return "BoxVBO@$width,$height,$segments,$paintStyle"
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        if (paintStyle == Outline) {
            GLHelper.lineWidth(gl, lineWidth)
        }
    }


    class BoxVBO(
        val segments: Int,
        val paintStyle: PaintStyle
    ) : VertexBuffer(

        // Segments * Arc count + Center point + Closing point
        vertexCount = max(1, segments) * 4 + if (paintStyle == Fill) 2 else 1,

        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = when (paintStyle) {
            Fill -> GL_TRIANGLE_FAN
            Outline -> GL_LINE_STRIP
        }
    ) {
        fun update(entity: UIBox) {

            val width = entity.width
            val height = entity.height
            val segments = max(1, segments)

            var position = 0

            // Disposition: Arcs are drawn in a clockwise manner
            // [1][2]
            // [4][3]

            // Center
            if (paintStyle == Fill) {
                putVertex(position++, width / 2f, height / 2f)
            }

            val radius = entity.radius.coerceAtMost(min(width, height) / 2f).coerceAtLeast(0f)

            // [1]
            position = addArc(position, radius, radius, -90f, 0f, radius, radius, segments)
            // [2]
            position = addArc(position, width - radius, radius, 0f, 90f, radius, radius, segments)
            // [3]
            position = addArc(position, width - radius, height - radius, 90f, 180f, radius, radius, segments)
            // [4]
            position = addArc(position, radius, height - radius, 180f, 270f, radius, radius, segments)

            // Closing point
            putVertex(position, 0f, radius)
        }
    }

}