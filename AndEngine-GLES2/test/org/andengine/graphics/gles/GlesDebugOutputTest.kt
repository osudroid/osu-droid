package org.andengine.graphics.gles

import android.opengl.GLES32
import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class GlesDebugOutputTest {

    @Before
    fun setUp() {
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        GlesDebugOptions.debugOutput = false
        GlesDebugOptions.simulateEs30 = false
        GlesDebugOutput.uninstall()
    }

    @Test
    fun format_namesSeveritySourceAndType() {
        assertEquals(
            "[HIGH] ERROR from API (id=1282): invalid enum",
            GlesDebugOutput.format(
                GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_ERROR, 1282,
                GLES32.GL_DEBUG_SEVERITY_HIGH, "invalid enum"
            )
        )
        assertEquals(
            "[LOW] PERFORMANCE from SHADER_COMPILER (id=7): ",
            GlesDebugOutput.format(
                GLES32.GL_DEBUG_SOURCE_SHADER_COMPILER, GLES32.GL_DEBUG_TYPE_PERFORMANCE, 7,
                GLES32.GL_DEBUG_SEVERITY_LOW, null
            )
        )
    }

    @Test
    fun format_unknownEnumsAsHex() {
        assertEquals("[0x1] 0x2 from 0x3 (id=0): m", GlesDebugOutput.format(3, 2, 0, 1, "m"))
    }

    @Test
    fun format_dropsNotifications() {
        assertNull(
            GlesDebugOutput.format(
                GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_OTHER, 0,
                GLES32.GL_DEBUG_SEVERITY_NOTIFICATION, "buffer info"
            )
        )
    }

    @Test
    fun isWarning_highAndMediumOnly() {
        assertTrue(GlesDebugOutput.isWarning(GLES32.GL_DEBUG_SEVERITY_HIGH))
        assertTrue(GlesDebugOutput.isWarning(GLES32.GL_DEBUG_SEVERITY_MEDIUM))
        assertFalse(GlesDebugOutput.isWarning(GLES32.GL_DEBUG_SEVERITY_LOW))
        assertFalse(GlesDebugOutput.isWarning(GLES32.GL_DEBUG_SEVERITY_NOTIFICATION))
    }

    @Test
    fun install_enablesOutputAndRegistersCallback() {
        val gl = RecordingGL()
        GlesDebugOutput.install(gl)

        assertTrue("glEnable(${GLES32.GL_DEBUG_OUTPUT})" in gl.calls)
        assertTrue("glEnable(${GLES32.GL_DEBUG_OUTPUT_SYNCHRONOUS})" in gl.calls)
        assertTrue("glDebugMessageCallback(buf)" in gl.calls)
        assertFalse(GlesDebugOutput.isPolling)
    }

    @Test
    fun unsupportedCallback_fallsBackToPolling() {
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        GlesDebugOutput.install(gl)
        assertTrue(GlesDebugOutput.isPolling)

        gl.debugLog += RecordingGL.DebugMessage(
            GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_ERROR, 1, GLES32.GL_DEBUG_SEVERITY_HIGH, "bad"
        )
        gl.debugLog += RecordingGL.DebugMessage(
            GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_OTHER, 2, GLES32.GL_DEBUG_SEVERITY_NOTIFICATION, "spam"
        )
        gl.debugLog += RecordingGL.DebugMessage(
            GLES32.GL_DEBUG_SOURCE_APPLICATION, GLES32.GL_DEBUG_TYPE_MARKER, 3, GLES32.GL_DEBUG_SEVERITY_LOW, "ünï"
        )
        GlesDebugOutput.poll()

        assertTrue(gl.debugLog.isEmpty())
        val logs = ShadowLog.getLogsForTag(GlesDebugOutput.TAG).filter { it.type != Log.INFO }
        assertEquals(2, logs.size)
        assertEquals(Log.WARN, logs[0].type)
        assertEquals("[HIGH] ERROR from API (id=1): bad", logs[0].msg)
        assertEquals(Log.DEBUG, logs[1].type)
        assertEquals("[LOW] MARKER from APPLICATION (id=3): ünï", logs[1].msg)
    }

    @Test
    fun poll_drainsMoreThanOneBatch() {
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        GlesDebugOutput.install(gl)
        repeat(40) {
            gl.debugLog += RecordingGL.DebugMessage(
                GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_ERROR, it, GLES32.GL_DEBUG_SEVERITY_MEDIUM, "m$it"
            )
        }
        GlesDebugOutput.poll()

        assertTrue(gl.debugLog.isEmpty())
        assertEquals(40, ShadowLog.getLogsForTag(GlesDebugOutput.TAG).count { it.type == Log.WARN })
    }

    @Test
    fun allBindingsUnsupported_disablesDebugOutput() {
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        gl.debugLogBytesUnsupported = true
        gl.debugLogStringsUnsupported = true
        GlesDebugOutput.install(gl)

        assertEquals(GlesDebugOutput.Mode.DISABLED, GlesDebugOutput.mode)
        assertFalse(GlesDebugOutput.isPolling)
        assertTrue("glDisable(${GLES32.GL_DEBUG_OUTPUT})" in gl.calls)
        assertTrue(
            ShadowLog.getLogsForTag(GlesDebugOutput.TAG)
                .any { it.msg == "debug output: not supported by Android bindings, disabled" }
        )

        gl.calls.clear()
        GlesDebugOutput.poll()
        assertTrue(gl.calls.isEmpty())
    }

    private fun unsupportedBindingsGl() = RecordingGL().apply {
        debugCallbackUnsupported = true
        debugLogBytesUnsupported = true
        debugLogStringsUnsupported = true
    }

    private fun errorWarnings() =
        ShadowLog.getLogsForTag(GlesDebugOutput.TAG).filter { it.type == Log.WARN }.map { it.msg }

    @Test
    fun allBindingsUnsupported_withoutDebugOutput_doesNotPollErrors() {
        val gl = unsupportedBindingsGl()
        gl.errorQueue += GLES32.GL_INVALID_OPERATION
        GlesDebugOutput.install(gl)

        assertFalse(GlesDebugOutput.isPollingErrors)
        gl.calls.clear()
        GlesDebugOutput.poll()
        assertTrue(gl.calls.isEmpty())
        assertTrue(errorWarnings().isEmpty())
    }

    @Test
    fun allBindingsUnsupported_withDebugOutput_pollsGlGetErrorRateLimited() {
        GlesDebugOptions.debugOutput = true
        val gl = unsupportedBindingsGl()
        GlesDebugOutput.install(gl)

        assertEquals(GlesDebugOutput.Mode.DISABLED, GlesDebugOutput.mode)
        assertFalse(GlesDebugOutput.isPolling)
        assertTrue(GlesDebugOutput.isPollingErrors)

        gl.errorQueue += listOf(GLES32.GL_INVALID_OPERATION, GLES32.GL_INVALID_ENUM, GLES32.GL_INVALID_OPERATION)
        gl.calls.clear()
        GlesDebugOutput.poll()

        // Drained until GL_NO_ERROR; the repeated code is logged once.
        assertEquals(4, gl.calls.count { it == "glGetError()" })
        assertEquals(
            listOf("glGetError: GL_INVALID_OPERATION (0x502)", "glGetError: GL_INVALID_ENUM (0x500)"),
            errorWarnings()
        )

        // Same code within the interval: suppressed.
        ShadowLog.clear()
        ShadowSystemClock.advanceBy(Duration.ofMillis(GlesDebugOutput.ERROR_LOG_INTERVAL_MS - 1))
        gl.errorQueue += GLES32.GL_INVALID_OPERATION
        GlesDebugOutput.poll()
        assertTrue(errorWarnings().isEmpty())

        // Once the interval has passed since the last log line: logged again.
        ShadowSystemClock.advanceBy(Duration.ofMillis(1))
        gl.errorQueue += GLES32.GL_INVALID_OPERATION
        GlesDebugOutput.poll()
        assertEquals(listOf("glGetError: GL_INVALID_OPERATION (0x502)"), errorWarnings())

        // No error: a single glGetError call and nothing logged.
        ShadowLog.clear()
        gl.calls.clear()
        GlesDebugOutput.poll()
        assertEquals(listOf("glGetError()"), gl.calls)
        assertTrue(errorWarnings().isEmpty())
    }

    @Test
    fun errorPolling_atMostMaxIterationsPerPoll() {
        GlesDebugOptions.debugOutput = true
        val gl = unsupportedBindingsGl()
        GlesDebugOutput.install(gl)
        repeat(20) { gl.errorQueue += GLES32.GL_INVALID_VALUE }
        gl.calls.clear()

        GlesDebugOutput.poll()

        assertEquals(GlesDebugOutput.MAX_ERRORS_PER_POLL, gl.calls.count { it == "glGetError()" })
        assertEquals(20 - GlesDebugOutput.MAX_ERRORS_PER_POLL, gl.errorQueue.size)
        assertEquals(listOf("glGetError: GL_INVALID_VALUE (0x501)"), errorWarnings())
    }

    @Test
    fun pollThrowingLater_withDebugOutput_fallsBackToGlGetError() {
        GlesDebugOptions.debugOutput = true
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        GlesDebugOutput.install(gl)
        assertEquals(GlesDebugOutput.Mode.POLL_BYTES, GlesDebugOutput.mode)
        assertFalse(GlesDebugOutput.isPollingErrors)

        gl.debugLogBytesUnsupported = true
        GlesDebugOutput.poll()
        assertEquals(GlesDebugOutput.Mode.DISABLED, GlesDebugOutput.mode)
        assertTrue(GlesDebugOutput.isPollingErrors)

        ShadowLog.clear()
        gl.errorQueue += GLES32.GL_OUT_OF_MEMORY
        GlesDebugOutput.poll()
        assertEquals(listOf("glGetError: GL_OUT_OF_MEMORY (0x505)"), errorWarnings())
    }

    @Test
    fun uninstall_stopsErrorPolling() {
        GlesDebugOptions.debugOutput = true
        val gl = unsupportedBindingsGl()
        GlesDebugOutput.install(gl)
        GlesDebugOutput.uninstall()

        gl.calls.clear()
        GlesDebugOutput.poll()
        assertFalse(GlesDebugOutput.isPollingErrors)
        assertTrue(gl.calls.isEmpty())
    }

    @Test
    fun bytesOverloadUnsupported_usesStringsOverload() {
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        gl.debugLogBytesUnsupported = true
        GlesDebugOutput.install(gl)
        assertEquals(GlesDebugOutput.Mode.POLL_STRINGS, GlesDebugOutput.mode)

        repeat(20) {
            gl.debugLog += RecordingGL.DebugMessage(
                GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_ERROR, it, GLES32.GL_DEBUG_SEVERITY_HIGH, "s$it"
            )
        }
        gl.debugLog += RecordingGL.DebugMessage(
            GLES32.GL_DEBUG_SOURCE_API, GLES32.GL_DEBUG_TYPE_OTHER, 99, GLES32.GL_DEBUG_SEVERITY_NOTIFICATION, "spam"
        )
        GlesDebugOutput.poll()

        assertTrue(gl.debugLog.isEmpty())
        val warnings = ShadowLog.getLogsForTag(GlesDebugOutput.TAG).filter { it.type == Log.WARN }
        assertEquals(20, warnings.size)
        assertEquals("[HIGH] ERROR from API (id=0): s0", warnings[0].msg)
    }

    @Test
    fun pollThrowingLater_disablesWithoutThrowing() {
        val gl = RecordingGL()
        gl.debugCallbackUnsupported = true
        GlesDebugOutput.install(gl)
        assertEquals(GlesDebugOutput.Mode.POLL_BYTES, GlesDebugOutput.mode)

        gl.debugLogBytesUnsupported = true
        GlesDebugOutput.poll()

        assertEquals(GlesDebugOutput.Mode.DISABLED, GlesDebugOutput.mode)
        assertTrue("glDisable(${GLES32.GL_DEBUG_OUTPUT})" in gl.calls)
        gl.calls.clear()
        GlesDebugOutput.poll()
        assertTrue(gl.calls.isEmpty())
    }

    @Test
    fun poll_noOpWithoutPolling() {
        val gl = RecordingGL()
        GlesDebugOutput.install(gl)
        gl.calls.clear()
        GlesDebugOutput.poll()
        assertTrue(gl.calls.isEmpty())
    }

    @Test
    fun backend_installsOnlyWhenEnabledOnEs32() {
        val gl = RecordingGL()
        GlesBackend.onContextCreated(gl)
        assertFalse("glDebugMessageCallback(buf)" in gl.calls)

        GlesDebugOptions.debugOutput = true
        val gl2 = RecordingGL()
        GlesBackend.onContextCreated(gl2)
        assertTrue("glDebugMessageCallback(buf)" in gl2.calls)

        GlesDebugOptions.simulateEs30 = true
        val gl3 = RecordingGL()
        gl3.extensionsString = "GL_KHR_debug"
        GlesBackend.onContextCreated(gl3)
        assertFalse("glDebugMessageCallback(buf)" in gl3.calls)
    }

    @Test
    fun backend_logsContextLine() {
        GlesDebugOptions.simulateEs30 = true
        GlesBackend.onContextCreated(RecordingGL())
        val line = ShadowLog.getLogsForTag("GlesBackend").single().msg
        assertEquals(
            "context: ES 3.0 (OpenGL ES 3.2 test) renderer=RecordingGL simulateEs30=true debugOutput=false",
            line
        )
    }
}
