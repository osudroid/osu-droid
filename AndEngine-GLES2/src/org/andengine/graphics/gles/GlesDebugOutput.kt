package org.andengine.graphics.gles

import android.opengl.GLES32
import android.os.SystemClock
import android.util.Log

/**
 * Routes `GL_DEBUG_OUTPUT` messages of the current context to logcat (tag [TAG]). High and medium
 * severity messages are logged as warnings, low severity as debug; notifications are dropped.
 *
 * The Android Java bindings declare [GLES32.glDebugMessageCallback] and several
 * `glGetDebugMessageLog` overloads, but some of their JNI stubs only throw
 * `UnsupportedOperationException("not yet implemented")` (seen for the callback and the `byte[]`
 * overload on Android 10 and 14). [install] probes them in order; with a polling path, [poll]
 * (called once per frame by `GlesEngineRenderer`) drains the context's debug message log. When no path
 * works but [GlesDebugOptions.debugOutput] is set, [poll] falls back to `glGetError` instead and
 * logs each distinct error code as a warning, at most once per [ERROR_LOG_INTERVAL_MS] per code.
 *
 * GL thread only.
 */
object GlesDebugOutput {

    const val TAG = "GlesDebug"

    /** Messages fetched per `glGetDebugMessageLog` call while polling. */
    private const val POLL_BATCH = 16

    /** Fallback when the driver reports no `GL_MAX_DEBUG_MESSAGE_LENGTH`. */
    private const val DEFAULT_MAX_MESSAGE_LENGTH = 1024

    private val POLL_MODES = arrayOf(Mode.POLL_BYTES, Mode.POLL_STRINGS)

    /** Most `glGetError` calls per [poll] in the fallback, read by tests; the error queue is drained up to this. */
    const val MAX_ERRORS_PER_POLL = 8

    /** Minimum time between two log lines for the same `glGetError` code, read by tests. */
    const val ERROR_LOG_INTERVAL_MS = 1000L

    /** Distinct `glGetError` codes whose last log time is remembered (ES defines five). */
    private const val ERROR_SLOTS = 8

    /** How messages of the current context reach logcat. */
    enum class Mode {
        /** No debug output (not installed, or not supported by the bindings). */
        DISABLED,
        /** `glDebugMessageCallback`. */
        CALLBACK,
        /** [poll] with the `int[]`/`byte[]` overload of `glGetDebugMessageLog`. */
        POLL_BYTES,
        /** [poll] with the `String[]`-returning overload of `glGetDebugMessageLog`. */
        POLL_STRINGS
    }

    /** Strong reference to the registered callback, so the object stays alive as long as GL may call it. */
    private var callback: GLES32.DebugProc? = null

    /** The GL that [poll] drains; non-null only in a polling [mode]. */
    private var pollGl: GL32? = null

    /** The GL that [poll] checks with `glGetError`; non-null only in the fallback. */
    private var errorGl: GL32? = null

    @JvmStatic
    var mode: Mode = Mode.DISABLED
        private set

    private val errorCodes = IntArray(ERROR_SLOTS)
    private val errorLoggedAt = LongArray(ERROR_SLOTS)
    private var errorCodeCount = 0

    private val sources = IntArray(POLL_BATCH)
    private val types = IntArray(POLL_BATCH)
    private val ids = IntArray(POLL_BATCH)
    private val severities = IntArray(POLL_BATCH)
    private val lengths = IntArray(POLL_BATCH)
    private var messageLog = ByteArray(0)

    /** Whether the current context delivers its messages through [poll], read by tests. */
    @JvmStatic
    val isPolling: Boolean get() = pollGl != null

    /**
     * Whether [poll] falls back to `glGetError` because the debug output bindings are unsupported,
     * read by tests.
     */
    @JvmStatic
    val isPollingErrors: Boolean get() = errorGl != null

    /**
     * Enables synchronous debug output on the current context and picks the first delivery path the
     * Android bindings implement: the callback, then polling with either `glGetDebugMessageLog`
     * overload (each probed once here). When none works, debug output is disabled again and, if
     * [GlesDebugOptions.debugOutput] is set, [poll] checks `glGetError` instead. Once per context;
     * never throws because of an unimplemented binding.
     */
    @JvmStatic
    fun install(gl32: GL32) {
        pollGl = null
        errorGl = null
        errorCodeCount = 0
        mode = Mode.DISABLED

        // Capability toggles that the state cache does not track, so they go to the GL directly.
        gl32.glEnable(GLES32.GL_DEBUG_OUTPUT)
        gl32.glEnable(GLES32.GL_DEBUG_OUTPUT_SYNCHRONOUS)

        val proc = callback ?: GLES32.DebugProc { source, type, id, severity, message ->
            log(source, type, id, severity, message)
        }.also { callback = it }

        try {
            gl32.glDebugMessageCallback(proc)
            mode = Mode.CALLBACK
            Log.i(TAG, "debug output: callback")
            return
        } catch (e: RuntimeException) {
            Log.i(TAG, "debug output: glDebugMessageCallback unsupported (${e.message})")
        }

        val maxLength = IntArray(1)
        gl32.glGetIntegerv(GLES32.GL_MAX_DEBUG_MESSAGE_LENGTH, maxLength, 0)
        val length = if (maxLength[0] > 0) maxLength[0] else DEFAULT_MAX_MESSAGE_LENGTH
        if (messageLog.size < POLL_BATCH * length) messageLog = ByteArray(POLL_BATCH * length)

        // The probe is a real drain, so messages already logged (e.g. during context setup) are kept.
        for (candidate in POLL_MODES) {
            try {
                drain(gl32, candidate)
                pollGl = gl32
                mode = candidate
                Log.i(TAG, "debug output: polling glGetDebugMessageLog per frame ($candidate)")
                return
            } catch (e: RuntimeException) {
                Log.i(TAG, "debug output: glGetDebugMessageLog ($candidate) unsupported (${e.message})")
            }
        }

        disable(gl32)
        Log.i(TAG, "debug output: not supported by Android bindings, disabled")
        startErrorPolling(gl32)
    }

    /** Stops delivering messages; called for a context without debug output. */
    @JvmStatic
    fun uninstall() {
        pollGl = null
        errorGl = null
        mode = Mode.DISABLED
    }

    /**
     * Drains the debug message log of the current context, or checks `glGetError` in the fallback
     * ([isPollingErrors]). No-op otherwise. Never throws: a failing binding disables debug output
     * for the rest of the context (and switches to the `glGetError` fallback).
     */
    @JvmStatic
    fun poll() {
        val gl = pollGl
        if (gl == null) {
            val errors = errorGl ?: return
            pollErrors(errors)
            return
        }
        try {
            drain(gl, mode)
        } catch (e: RuntimeException) {
            disable(gl)
            Log.w(TAG, "debug output: glGetDebugMessageLog failed, disabled", e)
            startErrorPolling(gl)
        }
    }

    /** Switches [poll] to `glGetError`, unless debug output is off (release builds). */
    private fun startErrorPolling(gl: GL32) {
        if (!GlesDebugOptions.debugOutput) return
        errorGl = gl
        errorCodeCount = 0
        Log.i(TAG, "debug output: polling glGetError per frame instead")
    }

    private fun pollErrors(gl: GL32) {
        var now = -1L
        for (i in 0 until MAX_ERRORS_PER_POLL) {
            val error = gl.glGetError()
            if (error == GLES32.GL_NO_ERROR) return
            if (now < 0) now = SystemClock.uptimeMillis()
            if (shouldLogError(error, now)) Log.w(TAG, formatError(error))
        }
    }

    /** Rate limit per error code; a code seen for the first time is always logged. */
    private fun shouldLogError(error: Int, now: Long): Boolean {
        for (i in 0 until errorCodeCount) {
            if (errorCodes[i] != error) continue
            if (now - errorLoggedAt[i] < ERROR_LOG_INTERVAL_MS) return false
            errorLoggedAt[i] = now
            return true
        }
        // Unknown codes beyond the table share its last slot.
        val slot = if (errorCodeCount < ERROR_SLOTS) errorCodeCount++ else ERROR_SLOTS - 1
        errorCodes[slot] = error
        errorLoggedAt[slot] = now
        return true
    }

    /** The log line for a `glGetError` code. */
    private fun formatError(error: Int): String = "glGetError: ${errorName(error)} (${hex(error)})"

    private fun errorName(error: Int): String = when (error) {
        GLES32.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
        GLES32.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
        GLES32.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
        GLES32.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
        GLES32.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
        else -> "UNKNOWN"
    }

    private fun disable(gl: GL32) {
        pollGl = null
        mode = Mode.DISABLED
        try {
            gl.glDisable(GLES32.GL_DEBUG_OUTPUT_SYNCHRONOUS)
            gl.glDisable(GLES32.GL_DEBUG_OUTPUT)
        } catch (_: RuntimeException) {
        }
    }

    private fun drain(gl: GL32, mode: Mode) {
        while (true) {
            val count = when (mode) {
                Mode.POLL_BYTES -> drainBytes(gl)
                Mode.POLL_STRINGS -> drainStrings(gl)
                else -> return
            }
            if (count < POLL_BATCH) return
        }
    }

    private fun drainBytes(gl: GL32): Int {
        val count = gl.glGetDebugMessageLog(
            POLL_BATCH, messageLog.size,
            sources, 0, types, 0, ids, 0, severities, 0, lengths, 0,
            messageLog, 0
        )
        var offset = 0
        for (i in 0 until count) {
            val length = lengths[i]
            if (length <= 0 || offset + length > messageLog.size) break
            // The reported length includes the terminating null character.
            val textLength = if (messageLog[offset + length - 1].toInt() == 0) length - 1 else length
            if (severities[i] != GLES32.GL_DEBUG_SEVERITY_NOTIFICATION) {
                log(sources[i], types[i], ids[i], severities[i], String(messageLog, offset, textLength, Charsets.UTF_8))
            }
            offset += length
        }
        return count
    }

    /** Allocates the returned array on every call; only used when the byte[] overload is missing. */
    private fun drainStrings(gl: GL32): Int {
        val messages = gl.glGetDebugMessageLog(POLL_BATCH, sources, 0, types, 0, ids, 0, severities, 0)
            ?: return 0
        val count = minOf(messages.size, POLL_BATCH)
        for (i in 0 until count) {
            log(sources[i], types[i], ids[i], severities[i], messages[i])
        }
        return count
    }

    private fun log(source: Int, type: Int, id: Int, severity: Int, message: String?) {
        val text = format(source, type, id, severity, message) ?: return
        if (isWarning(severity)) Log.w(TAG, text) else Log.d(TAG, text)
    }

    /** Whether a message of this severity is logged as a warning, read by tests. */
    @JvmStatic
    fun isWarning(severity: Int): Boolean =
        severity == GLES32.GL_DEBUG_SEVERITY_HIGH || severity == GLES32.GL_DEBUG_SEVERITY_MEDIUM

    /**
     * The log line for a debug message, or null when the message is filtered out (notifications).
     * Tests call it directly.
     */
    @JvmStatic
    fun format(source: Int, type: Int, id: Int, severity: Int, message: String?): String? {
        if (severity == GLES32.GL_DEBUG_SEVERITY_NOTIFICATION) return null
        return "[${severityName(severity)}] ${typeName(type)} from ${sourceName(source)} (id=$id): ${message.orEmpty()}"
    }

    private fun severityName(severity: Int): String = when (severity) {
        GLES32.GL_DEBUG_SEVERITY_HIGH -> "HIGH"
        GLES32.GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
        GLES32.GL_DEBUG_SEVERITY_LOW -> "LOW"
        else -> hex(severity)
    }

    private fun typeName(type: Int): String = when (type) {
        GLES32.GL_DEBUG_TYPE_ERROR -> "ERROR"
        GLES32.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR"
        GLES32.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR"
        GLES32.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
        GLES32.GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
        GLES32.GL_DEBUG_TYPE_MARKER -> "MARKER"
        GLES32.GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP"
        GLES32.GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP"
        GLES32.GL_DEBUG_TYPE_OTHER -> "OTHER"
        else -> hex(type)
    }

    private fun sourceName(source: Int): String = when (source) {
        GLES32.GL_DEBUG_SOURCE_API -> "API"
        GLES32.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM"
        GLES32.GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER"
        GLES32.GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY"
        GLES32.GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
        GLES32.GL_DEBUG_SOURCE_OTHER -> "OTHER"
        else -> hex(source)
    }

    private fun hex(value: Int): String = "0x" + Integer.toHexString(value)
}
