package com.reco1l.andengine.shape

import org.andengine.graphics.BlendMode
import org.andengine.graphics.ClearBuffers
import org.andengine.graphics.DepthState
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import com.edlplan.framework.utils.*
import com.reco1l.andengine.buffered.UIBufferedComponent
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.*
import org.andengine.graphics.DrawContext

// TODO: This class should be replaced with a more efficient BufferedEntity implementation.
class UITriangleMesh : UIComponent() {


    /**
     * The vertices of the mesh.
     */
    val vertices = FloatArraySlice()


    /**
     * How the mesh uses the depth buffer.
     */
    var depthState = DepthState.Disabled

    /**
     * The buffers cleared before the mesh draws.
     */
    var clearBuffers = ClearBuffers.None

    /**
     * The blend mode of the mesh; null draws with the one of the parent mesh or buffered component.
     */
    var blendMode: BlendMode? = UIBufferedComponent.DEFAULT_BLEND

    /**
     * Whether [vertices] hold (x, y, weight) per vertex instead of (x, y). The weight selects the
     * color between the two of [setGradient], ramping over the range of [setGradientRange].
     */
    var hasVertexWeights = false

    private val gradientFrom = floatArrayOf(1f, 1f, 1f, 1f)
    private val gradientTo = floatArrayOf(1f, 1f, 1f, 1f)
    private var gradientStart = 0f
    private var gradientEnd = 1f

    private var depthFrom = 0f
    private var depthTo = 0f

    // The gradient colors times the draw color, for the draw.
    private val drawFrom = FloatArray(4)
    private val drawTo = FloatArray(4)


    init {
        vertices.ary = FloatArray(0)
    }


    /**
     * Allows to set the content size of the mesh explicitly.
     */
    fun setContentSize(width: Float, height: Float) {
        contentWidth = width
        contentHeight = height
    }


    /**
     * Sets the colors (RGBA) of a mesh with [hasVertexWeights], at the start and at the end of the
     * gradient range (see [setGradientRange]). Both are multiplied with the color of the mesh (and
     * of its ancestors); white for both draws the mesh in its color alone.
     */
    fun setGradient(
        fromRed: Float, fromGreen: Float, fromBlue: Float, fromAlpha: Float,
        toRed: Float, toGreen: Float, toBlue: Float, toAlpha: Float
    ) {
        gradientFrom[0] = fromRed
        gradientFrom[1] = fromGreen
        gradientFrom[2] = fromBlue
        gradientFrom[3] = fromAlpha
        gradientTo[0] = toRed
        gradientTo[1] = toGreen
        gradientTo[2] = toBlue
        gradientTo[3] = toAlpha
    }

    /**
     * Sets the weights over which the gradient goes from its first to its second color, 0 to 1 by
     * default; outside of them the nearer color is kept.
     */
    fun setGradientRange(start: Float, end: Float) {
        gradientStart = start
        gradientEnd = end
    }


    /**
     * Sets the depth of a mesh with [hasVertexWeights] at weight 0 and at weight 1, in normalized
     * device coordinates (-1 nearest, 1 farthest); between them it is mixed by the weight. With a
     * depth test, the parts nearer to weight [to] then win where the mesh overlaps itself.
     */
    fun setDepthRange(from: Float, to: Float) {
        depthFrom = from
        depthTo = to
    }


    private val resolvedBlendMode: BlendMode
        get() = blendMode ?: when (val parent = parent) {
            is UIBufferedComponent<*> -> parent.blendMode
            is UITriangleMesh -> parent.blendMode
            else -> null
        } ?: UIBufferedComponent.DEFAULT_BLEND

    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        drawContext.clear(clearBuffers)

        // The depth state itself comes from the pipeline of the draw.
        if (depthState.test == null) {
            drawContext.disableDepthTest()
        }
    }

    override fun doDraw(drawContext: DrawContext, pCamera: Camera) {
        super.doDraw(drawContext, pCamera)

        if (vertices.length > 0) {
            if (hasVertexWeights) {
                MeshRenderer.drawGradient(
                    PrimitiveType.Triangles,
                    vertices.ary, vertices.offset, vertices.length / MeshRenderer.GRADIENT_FLOATS,
                    drawContext.modelViewGLMatrix, drawContext.projectionGLMatrix,
                    tinted(gradientFrom, drawFrom), tinted(gradientTo, drawTo),
                    gradientStart, gradientEnd,
                    depthFrom, depthTo,
                    resolvedBlendMode, depthState.test, depthState.write
                )
            } else {
                MeshRenderer.draw(
                    PrimitiveType.Triangles,
                    vertices.ary, vertices.offset, vertices.length / 2,
                    drawContext.modelViewGLMatrix, drawContext.projectionGLMatrix,
                    QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
                    resolvedBlendMode, depthState.test, depthState.write
                )
            }
        }

        // The mesh renderer restores the state it found; disable the depth test after drawing so
        // subsequent entities (sprites, circles, etc.) are not accidentally tested against values we wrote.
        if (depthState.test != null) {
            drawContext.disableDepthTest()
        }
    }

    private fun tinted(color: FloatArray, out: FloatArray): FloatArray {
        out[0] = (drawRed * color[0]).coerceIn(0f, 1f)
        out[1] = (drawGreen * color[1]).coerceIn(0f, 1f)
        out[2] = (drawBlue * color[2]).coerceIn(0f, 1f)
        out[3] = (drawAlpha * color[3]).coerceIn(0f, 1f)
        return out
    }

}
