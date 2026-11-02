package org.andengine.graphics.batch

import org.andengine.graphics.BlendMode
import org.andengine.graphics.CullMode
import org.andengine.graphics.GpuProgram
import org.andengine.graphics.Pipeline
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.RenderContext
import org.andengine.graphics.VertexLayout
import org.andengine.graphics.gles.GlesGraphicsDevice

/**
 * A program of its own for [MeshRenderer.drawProgram]: the caller supplies the fragment shader, the
 * engine the standard vertex shader [VERTEX_SHADER], the vertex upload and the GL state.
 *
 * The fragment shader is complete GLSL (`#version 300 es` first) and may read:
 * - `in vec2 vTexCoord`, the texture coordinate of the vertex,
 * - `in vec2 vLocal`, the local coordinate of the vertex (0..1 across a quad, as the caller gives it),
 * - `in vec4 vColor`, the draw color (straight alpha, as passed to the draw),
 * - `uniform sampler2D uTexture` on unit 0, or with [external] `uniform samplerExternalOES uTexture`;
 *   an external shader must also declare `#extension GL_OES_EGL_image_external_essl3 : require`,
 * and writes `out vec4 fragColor`.
 *
 * [uniforms] names the caller's own uniforms. Each must be declared exactly once in the fragment
 * shader, outside comments, as `float`, `vec2`, `vec3` or `vec4` (a precision qualifier is allowed);
 * the declared type decides how many components are sent. Values are stored by [set] without
 * allocating and sent before every draw, so they need to be set only when they change. Uniforms of
 * other types are not supported.
 *
 * The GL program is created at the first draw and restores itself after a context loss (uniform
 * locations are looked up again, the stored values are sent again at the next draw). GL thread only.
 *
 * Lifetime: once created, the GL program is held by the engine's GL resource registry (so that it
 * can be restored after a context loss) until [releaseResources]. A [MeshProgram] is meant to be
 * long-lived and shared: keep one per shader (e.g. in a companion object), never one per entity or
 * per draw, or the registry keeps every one of them.
 */
class MeshProgram(
    val label: String,
    val fragmentShader: String,
    uniforms: List<String>,
    val external: Boolean = false
) {

    private val names: Array<String> = uniforms.toTypedArray()
    private val components = IntArray(names.size)
    private val values = FloatArray(names.size * 4)

    private var program: GpuProgram? = null

    /** Per blend mode, the pipelines by primitive. */
    private val pipelines = HashMap<BlendMode, Array<Pipeline?>>()

    /** Number of cached pipelines. */
    var pipelineCount = 0
        private set

    init {
        if (external) {
            require(fragmentShader.contains(EXTERNAL_EXTENSION) && fragmentShader.contains("samplerExternalOES")) {
                "External program '$label' must declare '#extension $EXTERNAL_EXTENSION : require' and a samplerExternalOES"
            }
        }
        val code = stripComments(fragmentShader)
        for (i in names.indices) {
            val name = names[i]
            require(names.indexOf(name) == i) { "Program '$label' names the uniform '$name' twice" }
            val declarations = Regex(
                """\buniform\s+(?:(?:lowp|mediump|highp)\s+)?(\w+)\s+${Regex.escape(name)}\s*;"""
            ).findAll(code).toList()
            require(declarations.size <= 1) {
                "Program '$label': the uniform '$name' is declared ${declarations.size} times in the fragment shader"
            }
            val declaration = declarations.firstOrNull()?.takeIf { it.groupValues[1] in UNIFORM_TYPES }
            requireNotNull(declaration) {
                "Program '$label': the uniform '$name' is not declared as float, vec2, vec3 or vec4 in the fragment shader"
            }
            components[i] = when (declaration.groupValues[1]) {
                "float" -> 1
                "vec2" -> 2
                "vec3" -> 3
                else -> 4
            }
        }
    }

    /**
     * Stores the value of [uniform] (one of the names given at construction); only as many
     * components as the uniform has are used. Applied at the next draw.
     */
    fun set(uniform: String, x: Float, y: Float = 0f, z: Float = 0f, w: Float = 0f) {
        var index = -1
        for (i in names.indices) {
            if (names[i] == uniform) {
                index = i
                break
            }
        }
        require(index >= 0) { "Program '$label' has no uniform '$uniform'" }
        val base = index * 4
        values[base] = x
        values[base + 1] = y
        values[base + 2] = z
        values[base + 3] = w
    }

    /** The GL program, created at the first call. For [MeshRenderer]. */
    fun obtainProgram(): GpuProgram {
        var p = program
        if (p == null) {
            p = GlesGraphicsDevice.instance.createProgram(VERTEX_SHADER, fragmentShader, label, SAMPLER_UNITS)
            program = p
            pipelines.clear()
            pipelineCount = 0
        }
        return p
    }

    /** The cached pipeline of this program for [layout], [blend] and [primitive]. For [MeshRenderer]. */
    fun pipelineFor(layout: VertexLayout, blend: BlendMode, primitive: PrimitiveType): Pipeline {
        val p = obtainProgram()
        var slots = pipelines[blend]
        if (slots == null) {
            slots = arrayOfNulls(PRIMITIVES.size)
            pipelines[blend] = slots
        }
        var pipeline = slots[primitive.ordinal]
        if (pipeline == null) {
            pipeline = Pipeline(p, layout, blend, null, false, CullMode.None, primitive)
            slots[primitive.ordinal] = pipeline
            pipelineCount++
        }
        return pipeline
    }

    /** Sends the stored uniform values; the pipeline of this program must be set on [context]. */
    fun applyUniforms(context: RenderContext) {
        val p = program ?: return
        for (i in names.indices) {
            context.setUniformFloat(p.uniformLocation(names[i]), components[i], values, i * 4)
        }
    }

    /** Disposes the GL program; the next draw creates it again. For shutdown and tests. */
    fun releaseResources() {
        pipelines.clear()
        pipelineCount = 0
        program?.dispose()
        program = null
    }

    override fun toString() = "MeshProgram($label)"

    companion object {

        /** The sampler of the fragment shader, on texture unit 0. */
        const val SAMPLER = "uTexture"

        private const val EXTERNAL_EXTENSION = "GL_OES_EGL_image_external_essl3"

        private val SAMPLER_UNITS = mapOf(SAMPLER to 0)

        private val PRIMITIVES = PrimitiveType.values()

        private val UNIFORM_TYPES = setOf("float", "vec2", "vec3", "vec4")

        private val COMMENT = Regex("""/\*.*?\*/|//[^\n]*""", RegexOption.DOT_MATCHES_ALL)

        /** [source] without its `//` and `/* */` comments, so that commented-out declarations do not count. */
        private fun stripComments(source: String): String = COMMENT.replace(source, " ")

        /**
         * The vertex shader of every [MeshProgram], for the vertex layout of
         * [MeshRenderer.programLayout]: the position is already transformed on the CPU, only
         * `uProjection` is applied.
         */
        const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec2 aLocal;
layout(location = 3) in vec4 aColor;
uniform mat4 uProjection;
out vec2 vTexCoord;
out vec2 vLocal;
out vec4 vColor;
void main() {
    vTexCoord = aTexCoord;
    vLocal = aLocal;
    vColor = aColor;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
"""
    }
}
