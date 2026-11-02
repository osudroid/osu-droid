package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.BlendFactor
import org.andengine.graphics.BlendMode
import org.andengine.graphics.CompareFunction
import org.andengine.graphics.CullMode
import org.andengine.graphics.Pipeline
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.VertexFormat

/** Turns a neutral [Pipeline] into GL state, through the [GlesStateCache]. */
object GlesPipeline {

    /**
     * Sets program, blend, depth and cull state of [p]. The primitive type is not GL state; see
     * [primitiveMode].
     */
    fun apply(p: Pipeline, state: GlesStateCache) {
        val program = p.program as GlesProgram
        check(!program.isDisposed) { "Pipeline uses the disposed program '${program.label}'" }
        state.useProgram(program.programId)

        val blend = p.blend
        if (blend == null) {
            state.setBlendEnabled(false)
        } else {
            state.setBlendEnabled(true)
            state.blendFuncSeparate(
                blendFactor(blend.srcColor),
                blendFactor(blend.dstColor),
                blendFactor(blend.srcAlpha),
                blendFactor(blend.dstAlpha)
            )
        }

        val depth = p.depthTest
        if (depth == null) {
            state.setDepthTestEnabled(false)
        } else {
            state.setDepthTestEnabled(true)
            state.depthFunc(compareFunction(depth))
        }
        state.depthMask(p.depthWrite)

        when (p.cull) {
            CullMode.None -> state.setCullFaceEnabled(false)
            CullMode.Front -> {
                state.setCullFaceEnabled(true)
                state.cullFace(GLES20.GL_FRONT)
            }
            CullMode.Back -> {
                state.setCullFaceEnabled(true)
                state.cullFace(GLES20.GL_BACK)
            }
        }
    }

    fun primitiveMode(type: PrimitiveType): Int = when (type) {
        PrimitiveType.Triangles -> GLES20.GL_TRIANGLES
        PrimitiveType.TriangleStrip -> GLES20.GL_TRIANGLE_STRIP
        PrimitiveType.Lines -> GLES20.GL_LINES
        PrimitiveType.LineStrip -> GLES20.GL_LINE_STRIP
        PrimitiveType.Points -> GLES20.GL_POINTS
    }

    fun blendFactor(factor: BlendFactor): Int = when (factor) {
        BlendFactor.Zero -> GLES20.GL_ZERO
        BlendFactor.One -> GLES20.GL_ONE
        BlendFactor.SrcAlpha -> GLES20.GL_SRC_ALPHA
        BlendFactor.OneMinusSrcAlpha -> GLES20.GL_ONE_MINUS_SRC_ALPHA
        BlendFactor.DstAlpha -> GLES20.GL_DST_ALPHA
        BlendFactor.OneMinusDstAlpha -> GLES20.GL_ONE_MINUS_DST_ALPHA
        BlendFactor.SrcColor -> GLES20.GL_SRC_COLOR
        BlendFactor.OneMinusSrcColor -> GLES20.GL_ONE_MINUS_SRC_COLOR
        BlendFactor.DstColor -> GLES20.GL_DST_COLOR
        BlendFactor.OneMinusDstColor -> GLES20.GL_ONE_MINUS_DST_COLOR
    }

    /** The [BlendFactor] of the GL blend factor constant [glFactor]; null for one without a counterpart. */
    fun blendFactorOf(glFactor: Int): BlendFactor? = when (glFactor) {
        GLES20.GL_ZERO -> BlendFactor.Zero
        GLES20.GL_ONE -> BlendFactor.One
        GLES20.GL_SRC_ALPHA -> BlendFactor.SrcAlpha
        GLES20.GL_ONE_MINUS_SRC_ALPHA -> BlendFactor.OneMinusSrcAlpha
        GLES20.GL_DST_ALPHA -> BlendFactor.DstAlpha
        GLES20.GL_ONE_MINUS_DST_ALPHA -> BlendFactor.OneMinusDstAlpha
        GLES20.GL_SRC_COLOR -> BlendFactor.SrcColor
        GLES20.GL_ONE_MINUS_SRC_COLOR -> BlendFactor.OneMinusSrcColor
        GLES20.GL_DST_COLOR -> BlendFactor.DstColor
        GLES20.GL_ONE_MINUS_DST_COLOR -> BlendFactor.OneMinusDstColor
        else -> null
    }

    /**
     * The blend mode that `glBlendFunc(src, dst)` sets up: the same factors for color and alpha.
     * Null if either factor has no [BlendFactor]. Allocates; cache the result.
     */
    fun blendModeOf(src: Int, dst: Int): BlendMode? {
        val source = blendFactorOf(src) ?: return null
        val destination = blendFactorOf(dst) ?: return null
        return BlendMode(source, destination, source, destination)
    }

    /** The [CompareFunction] of a `glDepthFunc` argument, or null if [function] is not one.
     * Allocation free. */
    fun compareFunctionOf(function: Int): CompareFunction? = when (function) {
        GLES20.GL_NEVER -> CompareFunction.Never
        GLES20.GL_LESS -> CompareFunction.Less
        GLES20.GL_LEQUAL -> CompareFunction.LessOrEqual
        GLES20.GL_EQUAL -> CompareFunction.Equal
        GLES20.GL_GREATER -> CompareFunction.Greater
        GLES20.GL_GEQUAL -> CompareFunction.GreaterOrEqual
        GLES20.GL_NOTEQUAL -> CompareFunction.NotEqual
        GLES20.GL_ALWAYS -> CompareFunction.Always
        else -> null
    }

    fun compareFunction(function: CompareFunction): Int = when (function) {
        CompareFunction.Never -> GLES20.GL_NEVER
        CompareFunction.Less -> GLES20.GL_LESS
        CompareFunction.LessOrEqual -> GLES20.GL_LEQUAL
        CompareFunction.Equal -> GLES20.GL_EQUAL
        CompareFunction.Greater -> GLES20.GL_GREATER
        CompareFunction.GreaterOrEqual -> GLES20.GL_GEQUAL
        CompareFunction.NotEqual -> GLES20.GL_NOTEQUAL
        CompareFunction.Always -> GLES20.GL_ALWAYS
    }

    /** Component count of [format]. */
    fun componentCount(format: VertexFormat): Int = when (format) {
        VertexFormat.Float1 -> 1
        VertexFormat.Float2 -> 2
        VertexFormat.Float3 -> 3
        VertexFormat.Float4 -> 4
        VertexFormat.UByte4Normalized -> 4
        VertexFormat.UShort1 -> 1
    }

    /** GL component type of [format]. */
    fun componentType(format: VertexFormat): Int = when (format) {
        VertexFormat.UByte4Normalized -> GLES20.GL_UNSIGNED_BYTE
        VertexFormat.UShort1 -> GLES20.GL_UNSIGNED_SHORT
        else -> GLES20.GL_FLOAT
    }

    fun isNormalized(format: VertexFormat): Boolean = format == VertexFormat.UByte4Normalized
}
