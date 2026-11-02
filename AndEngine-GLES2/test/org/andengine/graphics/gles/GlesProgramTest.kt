package org.andengine.graphics.gles

import org.andengine.opengl.exception.GLESVersionUnsupportedException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesProgramTest {

    private lateinit var gl: RecordingGL

    private val vertex = "#version 320 es\nvoid main() {}\n"
    private val fragment = "#version 320 es\nprecision mediump float;\nvoid main() {}\n"

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = RecordingGL()
        GlesBackend.onContextCreatedForTest(gl)
    }

    @After
    fun tearDown() {
        GlesDebugOptions.simulateEs30 = false
    }

    @Test
    fun compileFailure_reportsUnsupportedDeviceWithDriverLog() {
        gl.shaderCompileStatus = 0
        gl.shaderInfoLog = "ERROR: 0:18: 'x' : Syntax error"

        val e = assertThrows(GLESVersionUnsupportedException::class.java) {
            GlesProgram(vertex, fragment, "quad-batch")
        }

        assertEquals("GLSL ES 3.20", e.requiredVersion)
        assertTrue(e.detectedVersion, e.detectedVersion.contains("OpenGL ES 3.2 test"))
        val cause = e.cause!!
        assertTrue(cause.message!!, cause.message!!.contains("quad-batch"))
        assertTrue(cause.message!!, cause.message!!.contains("ERROR: 0:18: 'x' : Syntax error"))
    }

    @Test
    fun linkFailure_reportsUnsupportedDevice() {
        gl.programLinkStatus = 0
        gl.programInfoLog = "link error"

        val e = assertThrows(GLESVersionUnsupportedException::class.java) {
            GlesProgram(vertex, fragment, "mesh")
        }

        assertTrue(e.cause!!.message!!, e.cause!!.message!!.contains("link error"))
    }

    @Test
    fun shaderNewerThanContext_reportsUnsupportedDevice() {
        GlesDebugOptions.simulateEs30 = true
        GlesBackend.onContextCreatedForTest(gl)

        val e = assertThrows(GLESVersionUnsupportedException::class.java) {
            GlesProgram(vertex, fragment, "quad-batch")
        }

        assertEquals("GLSL ES 3.20", e.requiredVersion)
    }

    @Test
    fun shaderWithoutVersion_requiresGlslEs300() {
        gl.shaderCompileStatus = 0

        val e = assertThrows(GLESVersionUnsupportedException::class.java) {
            GlesProgram("void main() {}", "void main() {}", "plain")
        }

        assertEquals("GLSL ES 3.00", e.requiredVersion)
    }
}
