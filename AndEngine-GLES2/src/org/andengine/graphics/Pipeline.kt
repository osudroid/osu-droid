package org.andengine.graphics

enum class BlendFactor {
    Zero,
    One,
    SrcAlpha,
    OneMinusSrcAlpha,
    DstAlpha,
    OneMinusDstAlpha,
    SrcColor,
    OneMinusSrcColor,
    DstColor,
    OneMinusDstColor
}

/** Blend factors for the color and the alpha channel. Immutable; compared by content. */
class BlendMode(
    val srcColor: BlendFactor,
    val dstColor: BlendFactor,
    val srcAlpha: BlendFactor,
    val dstAlpha: BlendFactor
) {

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is BlendMode && srcColor == other.srcColor && dstColor == other.dstColor &&
            srcAlpha == other.srcAlpha && dstAlpha == other.dstAlpha

    override fun hashCode(): Int =
        ((srcColor.hashCode() * 31 + dstColor.hashCode()) * 31 + srcAlpha.hashCode()) * 31 + dstAlpha.hashCode()

    override fun toString() = "BlendMode($srcColor, $dstColor, $srcAlpha, $dstAlpha)"

    companion object {

        /** Source replaces the destination. */
        @JvmField
        val Opaque = BlendMode(BlendFactor.One, BlendFactor.Zero, BlendFactor.One, BlendFactor.Zero)

        /** Straight-alpha "over"; the destination alpha accumulates coverage. */
        @JvmField
        val Alpha = BlendMode(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendFactor.One, BlendFactor.OneMinusSrcAlpha)

        /** Source color weighted by its alpha is added to the destination. */
        @JvmField
        val Additive = BlendMode(BlendFactor.SrcAlpha, BlendFactor.One, BlendFactor.One, BlendFactor.One)

        /** "Over" for colors that are already multiplied by their alpha. */
        @JvmField
        val Premultiplied = BlendMode(BlendFactor.One, BlendFactor.OneMinusSrcAlpha, BlendFactor.One, BlendFactor.OneMinusSrcAlpha)

        /**
         * [Alpha] with its color factors for the alpha channel too, as a single
         * `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)` sets them.
         */
        @JvmField
        val AlphaAllChannels = BlendMode(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha)

        /**
         * [Additive] with its color factors for the alpha channel too, as a single
         * `glBlendFunc(GL_SRC_ALPHA, GL_ONE)` sets them.
         */
        @JvmField
        val AdditiveAllChannels = BlendMode(BlendFactor.SrcAlpha, BlendFactor.One, BlendFactor.SrcAlpha, BlendFactor.One)
    }
}

enum class CompareFunction { Never, Less, LessOrEqual, Equal, Greater, GreaterOrEqual, NotEqual, Always }

enum class CullMode { None, Front, Back }

enum class PrimitiveType { Triangles, TriangleStrip, Lines, LineStrip, Points, TriangleFan, LineLoop }

/**
 * Everything fixed about a draw besides its resources: the [program], how vertices are read
 * ([layout]), blending ([blend] `null` = blending off), depth test ([depthTest] `null` = off),
 * depth writes, face culling and the primitive type. Immutable; build one per combination and
 * reuse it, the render context skips the state changes when the same instance is set again.
 */
class Pipeline(
    val program: GpuProgram,
    val layout: VertexLayout,
    val blend: BlendMode?,
    val depthTest: CompareFunction?,
    val depthWrite: Boolean,
    val cull: CullMode,
    val primitive: PrimitiveType
) {
    override fun toString() =
        "Pipeline(${program.label}, $layout, blend=$blend, depth=$depthTest/$depthWrite, cull=$cull, $primitive)"
}
