package com.osudroid.testing

import android.opengl.GLES20
import java.lang.reflect.Proxy
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext

/**
 * A fake [GL32] that records every call as `glName(arg1,arg2,...)`, with arrays and buffers as `buf`.
 * It hands out increasing ids, reports every shader and program as compiled and linked, and keeps
 * the bytes of every array buffer upload.
 */
class RecordingGl(
    /** Returned for `GL_EXTENSIONS`. */
    var extensions: String = ""
) {
    val calls = ArrayList<String>()

    /** Data written to the array buffer, by glBufferData or glBufferSubData, in order. */
    val vertexUploads = ArrayList<ByteArray>()

    /** The rectangle of every glScissor call: x, y, width, height. */
    val scissors = ArrayList<List<Int>>()

    private var nextId = 100

    val gl: GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { proxy, method, args ->
        val name = method.name
        when (name) {
            "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
            "equals" -> return@newProxyInstance proxy === args!![0]
            "toString" -> return@newProxyInstance "RecordingGl"
        }
        calls += "$name(${args?.joinToString(",") { if (it is IntArray || it is FloatArray || it is Buffer) "buf" else "$it" } ?: ""})"
        when {
            name == "glGetIntegerv" -> (args!![1] as IntArray)[args[2] as Int] = when (args[0]) {
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                else -> 0
            }
            name.startsWith("glGen") && args?.size == 3 && args[1] is IntArray -> {
                val ids = args[1] as IntArray
                for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
            }
            (name == "glGetShaderiv" || name == "glGetProgramiv") && args!![2] is IntArray ->
                (args[2] as IntArray)[args[3] as Int] = 1
            name == "glScissor" -> scissors += listOf(args!![0] as Int, args[1] as Int, args[2] as Int, args[3] as Int)
            name == "glBufferData" && args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer ->
                vertexUploads += bytesOf(args[2] as ByteBuffer, args[1] as Int)
            name == "glBufferSubData" && args!![0] == GLES20.GL_ARRAY_BUFFER && args[3] is ByteBuffer ->
                vertexUploads += bytesOf(args[3] as ByteBuffer, args[2] as Int)
        }
        when {
            method.returnType == java.lang.Integer.TYPE && name.startsWith("glCreate") -> nextId++
            method.returnType == String::class.java -> when (args?.getOrNull(0)) {
                GLES20.GL_VERSION -> "OpenGL ES 3.2 test"
                GLES20.GL_EXTENSIONS -> extensions
                else -> ""
            }
            else -> defaultValue(method.returnType)
        }
    } as GL32

    /** Makes this fake the current context and forgets the calls its setup made. */
    fun install(): RecordingGl {
        GlesBackend.onContextCreated(gl)
        calls.clear()
        return this
    }

    /** The number of calls to the GL function [name]. */
    fun count(name: String): Int = calls.count { it.startsWith("$name(") }

    /** The index of the first call starting with [prefix], or -1. */
    fun indexOfFirst(prefix: String): Int = calls.indexOfFirst { it.startsWith(prefix) }

    /** The four corners (x, y) of every quad of the last vertex upload. */
    fun lastQuadCorners(): List<List<FloatArray>> = quadCornersOf(vertexUploads.last())

    /** The top-left and bottom-right corner of every quad of the last vertex upload: x0, y0, x1, y1. */
    fun lastQuads(): List<FloatArray> = lastQuadCorners().map { floatArrayOf(it[0][0], it[0][1], it[2][0], it[2][1]) }

    /** Whether a blend function of [src] and [dst] was set, for color and alpha alike. */
    fun setsBlend(src: Int, dst: Int): Boolean =
        calls.contains("glBlendFunc($src,$dst)") || calls.contains("glBlendFuncSeparate($src,$dst,$src,$dst)")

    /** The vertices of the last program quad upload: per vertex x, y, u, v, lx, ly and the color int. */
    fun lastProgramQuad(): ByteBuffer {
        val bytes = vertexUploads.last { it.size == 4 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES }
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
    }

    /** The last vertex upload, in the device's byte order. */
    fun lastUpload(): ByteBuffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())

    private fun bytesOf(data: ByteBuffer, size: Int): ByteArray {
        val copy = data.duplicate()
        val bytes = ByteArray(minOf(size, copy.remaining()))
        copy.get(bytes)
        return bytes
    }
}

/** The 4x4 identity matrix, column-major. Never written to. */
val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

/** The four corners (x, y) of every quad in [upload], a vertex upload of the quad batcher. */
fun quadCornersOf(upload: ByteArray): List<List<FloatArray>> {
    val buffer = ByteBuffer.wrap(upload).order(ByteOrder.nativeOrder())
    val stride = QuadBatcher.VERTEX_SIZE_BYTES
    val quads = ArrayList<List<FloatArray>>()
    while (buffer.remaining() >= stride * 4) {
        quads += List(4) {
            val x = buffer.float
            val y = buffer.float
            buffer.position(buffer.position() + stride - 8)
            floatArrayOf(x, y)
        }
    }
    return quads
}

/** Position and texture coordinate (x, y, u, v) of the 4 corners of every quad in [upload]. */
fun quadVerticesOf(upload: ByteArray): List<FloatArray> {
    val buffer = ByteBuffer.wrap(upload).order(ByteOrder.nativeOrder())
    val stride = QuadBatcher.VERTEX_SIZE_BYTES
    val quads = ArrayList<FloatArray>()
    while (buffer.remaining() >= stride * 4) {
        val quad = FloatArray(16)
        for (corner in 0 until 4) {
            val start = buffer.position()
            for (k in 0 until 4) quad[corner * 4 + k] = buffer.float
            buffer.position(start + stride)
        }
        quads += quad
    }
    return quads
}

/** The layer of every vertex in [upload], from its layer marker after the color. */
fun vertexLayersOf(upload: ByteArray): List<Int> {
    val buffer = ByteBuffer.wrap(upload).order(ByteOrder.nativeOrder())
    val stride = QuadBatcher.VERTEX_SIZE_BYTES
    return List(upload.size / stride) { buffer.getInt(it * stride + 20) and 0xFFFF }
}

/** Zero, false or null: what a fake returns for a call it does not answer. */
fun defaultValue(type: Class<*>): Any? = when (type) {
    java.lang.Boolean.TYPE -> false
    java.lang.Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Float.TYPE -> 0f
    java.lang.Double.TYPE -> 0.0
    String::class.java -> ""
    else -> null
}

/** Ends the render pass a test left open, so the next test starts outside a pass. */
fun endOpenPass() {
    val context = GlesRenderContext.instance
    if (context.isInPass) context.endPass()
}
