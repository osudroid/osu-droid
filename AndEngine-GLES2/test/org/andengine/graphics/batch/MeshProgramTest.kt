package org.andengine.graphics.batch

import android.opengl.GLES20
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BlendMode
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.texture.ITexture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeshProgramTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture
    private val programs = ArrayList<MeshProgram>()

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private val quad = floatArrayOf(
        0f, 0f, 0f, 0f, 0f, 0f,
        1f, 0f, 1f, 0f, 1f, 0f,
        0f, 1f, 0f, 1f, 0f, 1f
    )

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "t") as GlesTexture
        QuadBatcher.setProjection(identity)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        programs.forEach { it.releaseResources() }
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        texture.dispose()
    }

    private fun program(fragment: String = PLAIN_FRAGMENT, uniforms: List<String> = emptyList(), external: Boolean = false) =
        MeshProgram("test", fragment, uniforms, external).also { programs += it }

    private fun drawProgram(
        program: MeshProgram,
        tex: Texture = texture,
        vertices: FloatArray = quad,
        matrix: FloatArray = identity,
        rgba: Int = -1
    ) = MeshRenderer.drawProgram(
        program, PrimitiveType.Triangles, vertices, 0, vertices.size / 6, matrix, identity, tex, rgba, BlendMode.Alpha
    )

    private fun lastVertices(): ByteBuffer =
        ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    @Test
    fun meshProgram_compilesWithStandardVertexShader() {
        val p = program()
        val matrix = identity.copyOf()
        matrix[0] = 2f
        matrix[12] = 10f
        drawProgram(p, vertices = floatArrayOf(1f, 2f, 0.25f, 0.75f, 0.5f, 1f), matrix = matrix, rgba = 0x44332211)

        val sources = gl.calls.filter { it.startsWith("glShaderSource") }
        assertEquals(gl.calls.toString(), 2, sources.size)
        val vertex = sources[0]
        assertTrue(vertex, vertex.contains("#version 300 es"))
        for (name in listOf("uProjection", "vTexCoord", "vColor", "vLocal")) assertTrue(name, vertex.contains(name))
        assertTrue(sources[1].contains("fragColor = texture(uTexture, vTexCoord)"))
        // The sampler is set to unit 0 after the link.
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1i(0,0)"))

        val bytes = lastVertices()
        assertEquals(MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES, bytes.capacity())
        assertEquals(12f, bytes.getFloat(0), 1e-5f)
        assertEquals(2f, bytes.getFloat(4), 1e-5f)
        assertEquals(0.25f, bytes.getFloat(8), 0f)
        assertEquals(0.75f, bytes.getFloat(12), 0f)
        assertEquals(0.5f, bytes.getFloat(16), 0f)
        assertEquals(1f, bytes.getFloat(20), 0f)
        assertEquals(0x11, bytes.get(24).toInt() and 0xFF)
        assertEquals(0x44, bytes.get(27).toInt() and 0xFF)
        assertEquals(1, gl.calls.count { it.startsWith("glDrawArrays(${GLES20.GL_TRIANGLES},0,1)") })
        assertTrue(gl.uniformMatrices.isNotEmpty())

        // The program and its pipeline are made once.
        val creates = gl.calls.count { it == "glCreateProgram()" }
        drawProgram(p)
        assertEquals(creates, gl.calls.count { it == "glCreateProgram()" })
        assertEquals(1, p.pipelineCount)
    }

    @Test
    fun programQuad_packsCornersWithLocalCoordinatesAsStrip() {
        val p = program()
        val matrix = identity.copyOf()
        matrix[12] = 5f
        // Top-left, bottom-left, top-right, bottom-right.
        val positions = floatArrayOf(0f, 0f, 0f, 20f, 10f, 0f, 10f, 20f)
        val uvs = floatArrayOf(0.1f, 0.2f, 0.1f, 0.8f, 0.9f, 0.2f, 0.9f, 0.8f)
        MeshRenderer.drawProgramQuad(p, positions, uvs, matrix, identity, texture, 0x44332211, BlendMode.Alpha)

        val bytes = lastVertices()
        val stride = MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES
        assertEquals(4 * stride, bytes.capacity())
        val local = floatArrayOf(0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f)
        for (i in 0 until 4) {
            val o = i * stride
            assertEquals(positions[i * 2] + 5f, bytes.getFloat(o), 1e-5f)
            assertEquals(positions[i * 2 + 1], bytes.getFloat(o + 4), 1e-5f)
            assertEquals(uvs[i * 2], bytes.getFloat(o + 8), 0f)
            assertEquals(uvs[i * 2 + 1], bytes.getFloat(o + 12), 0f)
            assertEquals(local[i * 2], bytes.getFloat(o + 16), 0f)
            assertEquals(local[i * 2 + 1], bytes.getFloat(o + 20), 0f)
            assertEquals(0x11, bytes.get(o + 24).toInt() and 0xFF)
            assertEquals(0x44, bytes.get(o + 27).toInt() and 0xFF)
        }
        assertEquals(gl.calls.toString(), 1, gl.calls.count { it.startsWith("glDrawArrays(${GLES20.GL_TRIANGLE_STRIP},0,4)") })
    }

    @Test
    fun meshProgram_uniformsAppliedAtDraw() {
        val p = program(UNIFORM_FRAGMENT, listOf("uRadius", "uOffset", "uTint"))
        p.set("uRadius", 0.5f)
        p.set("uOffset", 1f, 2f)
        p.set("uTint", 1f, 0.5f, 0.25f, 0.125f)
        drawProgram(p)

        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        val use = gl.calls.subList(0, draw).indexOfLast { it.startsWith("glUseProgram") }
        val radius = gl.calls.indexOf("glUniform1f(0,0.5)")
        val offset = gl.calls.indexOf("glUniform2f(0,1.0,2.0)")
        val tint = gl.calls.indexOf("glUniform4f(0,1.0,0.5,0.25,0.125)")
        assertTrue(gl.calls.toString(), radius in (use + 1) until draw)
        assertTrue(gl.calls.toString(), offset in (use + 1) until draw)
        assertTrue(gl.calls.toString(), tint in (use + 1) until draw)

        gl.calls.clear()
        p.set("uRadius", 0.75f)
        drawProgram(p)
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1f(0,0.75)"))

        assertThrows<IllegalArgumentException> { p.set("uMissing", 1f) }
        assertThrows<IllegalArgumentException> { program(UNIFORM_FRAGMENT, listOf("uNotDeclared")) }
    }

    @Test
    fun meshProgram_uniformDeclarationsIgnoreComments() {
        // A commented-out declaration is no declaration, and does not count as a second one.
        val commented = UNIFORM_FRAGMENT.replace(
            "uniform float uRadius;",
            "// uniform vec4 uRadius;\nuniform float uRadius; /* uniform vec2 uRadius; */"
        )
        val p = program(commented, listOf("uRadius"))
        p.set("uRadius", 0.5f)
        drawProgram(p)
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1f(0,0.5)"))

        val onlyCommented = UNIFORM_FRAGMENT.replace("uniform float uRadius;", "/* uniform float uRadius; */")
        assertThrows<IllegalArgumentException> { program(onlyCommented, listOf("uRadius")) }
        val lineCommented = UNIFORM_FRAGMENT.replace("uniform float uRadius;", "// uniform float uRadius;")
        assertThrows<IllegalArgumentException> { program(lineCommented, listOf("uRadius")) }
    }

    @Test
    fun meshProgram_uniformDeclaredTwice_rejected() {
        val twice = UNIFORM_FRAGMENT.replace("uniform float uRadius;", "uniform float uRadius;\nuniform vec2 uRadius;")
        assertThrows<IllegalArgumentException> { program(twice, listOf("uRadius")) }
        // A declaration of an unsupported type is rejected as well.
        val matrix = UNIFORM_FRAGMENT.replace("uniform float uRadius;", "uniform mat4 uRadius;")
        assertThrows<IllegalArgumentException> { program(matrix, listOf("uRadius")) }
    }

    @Test
    fun meshProgramDraw_flushesBatchFirst() {
        val p = program()
        QuadBatcher.add(texture, BlendMode.Alpha, identity, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        assertTrue(QuadBatcher.hasPending)
        drawProgram(p)
        assertFalse(QuadBatcher.hasPending)
        val quadDraw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val mesh = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        assertTrue(gl.calls.toString(), quadDraw in 0 until mesh)
    }

    @Test
    fun externalTexture_boundWithOesTarget() {
        val legacy = fakeLegacyTexture(4, 4) { 7 }
        val view = BatchTexture.externalOf(legacy)
        val p = program(EXTERNAL_FRAGMENT, external = true)
        drawProgram(p, tex = view)

        val bind = gl.calls.indexOf("glBindTexture($TEXTURE_EXTERNAL_OES,7)")
        assertTrue(gl.calls.toString(), bind >= 0)
        val active = gl.calls.subList(0, bind).lastOrNull { it.startsWith("glActiveTexture") }
        assertTrue(active == null || active == "glActiveTexture(${GLES20.GL_TEXTURE0})")
        assertFalse(gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertTrue(gl.calls.toString(), gl.calls.any { it.startsWith("glDrawArrays") })

        // Unloaded external textures draw nothing.
        gl.calls.clear()
        val unloaded = BatchTexture.externalOf(fakeLegacyTexture(4, 4) { 0 })
        drawProgram(p, tex = unloaded)
        assertFalse(gl.calls.any { it.startsWith("glDrawArrays") })

        // An external program must declare the extension and the external sampler.
        assertThrows<IllegalArgumentException> { program(PLAIN_FRAGMENT, external = true) }
    }

    @Test
    fun meshProgram_restoredAfterContextLoss() {
        val p = program(UNIFORM_FRAGMENT, listOf("uRadius", "uOffset", "uTint"))
        p.set("uRadius", 0.5f)
        drawProgram(p)

        gl.calls.clear()
        GlesBackend.onContextCreatedForTest(gl)
        assertEquals(gl.calls.toString(), 1, gl.calls.count { it == "glCreateProgram()" })
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1i(0,0)"))

        gl.calls.clear()
        drawProgram(p)
        // Locations are looked up again on the new program, and the stored values are applied.
        assertTrue(gl.calls.toString(), gl.calls.any { it.startsWith("glGetUniformLocation") && it.endsWith(",uRadius)") })
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1f(0,0.5)"))
        assertTrue(gl.calls.any { it.startsWith("glDrawArrays") })
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return
            throw e
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }

    private fun fakeLegacyTexture(width: Int, height: Int, id: () -> Int): ITexture {
        lateinit var self: ITexture
        self = Proxy.newProxyInstance(ITexture::class.java.classLoader, arrayOf(ITexture::class.java)) { _, method, args ->
            when (method.name) {
                "getHardwareTextureID" -> id()
                "getWidth" -> width
                "getHeight" -> height
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args!![0]
                "toString" -> "FakeTexture"
                else -> throw UnsupportedOperationException(method.name)
            }
        } as ITexture
        return self
    }

    private companion object {
        const val TEXTURE_EXTERNAL_OES = 0x8D65

        const val PLAIN_FRAGMENT = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vTexCoord;
in vec4 vColor;
in vec2 vLocal;
out vec4 fragColor;
void main() {
    fragColor = texture(uTexture, vTexCoord) * vColor;
}
"""

        const val UNIFORM_FRAGMENT = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
uniform float uRadius;
uniform highp vec2 uOffset;
uniform vec4 uTint;
in vec2 vTexCoord;
in vec4 vColor;
in vec2 vLocal;
out vec4 fragColor;
void main() {
    float d = distance(vLocal + uOffset, vec2(0.5));
    fragColor = texture(uTexture, vTexCoord) * vColor * uTint * step(d, uRadius);
}
"""

        const val EXTERNAL_FRAGMENT = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTexture;
in vec2 vTexCoord;
in vec4 vColor;
in vec2 vLocal;
out vec4 fragColor;
void main() {
    fragColor = texture(uTexture, vTexCoord) * vColor;
}
"""
    }
}
