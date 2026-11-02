package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES32
import android.os.Build
import org.andengine.graphics.GpuProgram
import org.andengine.opengl.exception.GLESVersionUnsupportedException

/**
 * Shader program compiled and linked from GLSL ES source, recompiled after a context loss. Sources
 * declaring `#version 310 es` need ES 3.1 and `#version 320 es` ES 3.2. Each sampler uniform named
 * in [samplerUnits] is set to its texture unit after every link. GL thread only.
 *
 * A program the driver cannot build throws [GLESVersionUnsupportedException], so players see the
 * unsupported device dialog instead of a raw crash; the driver log stays in its cause.
 */
class GlesProgram(
    val vertexSource: String,
    val fragmentSource: String,
    override val label: String,
    val samplerUnits: Map<String, Int> = emptyMap()
) : GpuProgram, GlesRestorable {

    /** The GL program; 0 while there is none. */
    var programId = 0
        private set

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Programs

    private val uniformLocations = HashMap<String, Int>()

    init {
        build()
        GlesBackend.registry.register(this)
    }

    private fun build() {
        try {
            checkVersion(vertexSource, "vertex")
            checkVersion(fragmentSource, "fragment")
            create()
        } catch (e: IllegalStateException) {
            throw unsupported(e)
        }
    }

    private fun unsupported(cause: IllegalStateException): GLESVersionUnsupportedException {
        val required = maxOf(versionOf(vertexSource) ?: 300, versionOf(fragmentSource) ?: 300)
        val gl = GlesBackend.gl
        val glsl = gl.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION)
        val version = gl.glGetString(GLES20.GL_VERSION)
        return GLESVersionUnsupportedException(
            "GLSL ES ${required / 100}.${"%02d".format(required % 100)}",
            (glsl ?: "unknown") + (if (version != null) " ($version)" else ""),
            Build.MANUFACTURER + " " + Build.MODEL,
            Build.HARDWARE,
            Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")",
            cause
        )
    }

    private fun versionOf(source: String): Int? = VERSION_REGEX.find(source)?.groupValues?.get(1)?.toInt()

    private fun checkVersion(source: String, stage: String) {
        val version = versionOf(source) ?: return
        val caps = GlesBackend.caps
        val supported = when {
            version >= 320 -> caps.hasGl32
            version >= 310 -> caps.hasGl31
            else -> true
        }
        check(supported) {
            "Program '$label': the $stage shader needs GLSL ES $version, the context is ${caps.version}"
        }
    }

    private fun create() {
        val gl = GlesBackend.gl
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource, "vertex")
        val fragment = try {
            compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource, "fragment")
        } catch (e: IllegalStateException) {
            gl.glDeleteShader(vertex)
            throw e
        }

        val program = gl.glCreateProgram()
        gl.glAttachShader(program, vertex)
        gl.glAttachShader(program, fragment)
        gl.glLinkProgram(program)
        // Flagged for deletion; freed together with the program.
        gl.glDeleteShader(vertex)
        gl.glDeleteShader(fragment)

        val status = IntArray(1)
        gl.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = gl.glGetProgramInfoLog(program)
            gl.glDeleteProgram(program)
            throw IllegalStateException("Program '$label' failed to link: $log")
        }

        programId = program
        uniformLocations.clear()
        GlesResources.label(GLES32.GL_PROGRAM, program, label)
        if (samplerUnits.isNotEmpty()) {
            // Uniform values belong to the program in use.
            GlesBackend.state.useProgram(program)
            for ((name, unit) in samplerUnits) {
                val location = uniformLocation(name)
                if (location >= 0) gl.glUniform1i(location, unit)
            }
        }
    }

    private fun compile(type: Int, source: String, stage: String): Int {
        val gl = GlesBackend.gl
        val shader = gl.glCreateShader(type)
        gl.glShaderSource(shader, source)
        gl.glCompileShader(shader)
        val status = IntArray(1)
        gl.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = gl.glGetShaderInfoLog(shader)
            gl.glDeleteShader(shader)
            throw IllegalStateException("Program '$label': $stage shader failed to compile: $log")
        }
        return shader
    }

    override fun uniformLocation(name: String): Int {
        val cached = uniformLocations[name]
        if (cached != null) return cached
        val location = GlesBackend.gl.glGetUniformLocation(programId, name)
        uniformLocations[name] = location
        return location
    }

    /** Makes this the current program through the state cache. */
    fun use() = GlesBackend.state.useProgram(programId)

    override fun onContextLost() {
        programId = 0
        uniformLocations.clear()
    }

    override fun restore() = build()

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        val id = programId
        programId = 0
        uniformLocations.clear()
        if (id != 0) {
            GlesBackend.state.onProgramDeleted(id)
            GlesBackend.gl.glDeleteProgram(id)
        }
    }

    override fun toString() = "GlesProgram($label)"

    private companion object {
        val VERSION_REGEX = Regex("""^\s*#\s*version\s+(\d+)""", RegexOption.MULTILINE)
    }
}
