package org.andengine.graphics.gles

import android.opengl.GLES32
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * The OpenGL ES 3.2 API: [GL31] plus every function of [android.opengl.GLES32], mirrored one to one.
 *
 * The rule about cached state in [GL30] applies.
 */
interface GL32 : GL31 {
    // OpenGL ES 3.2
    fun glBlendBarrier()
    fun glCopyImageSubData(
        srcName: Int,
        srcTarget: Int,
        srcLevel: Int,
        srcX: Int,
        srcY: Int,
        srcZ: Int,
        dstName: Int,
        dstTarget: Int,
        dstLevel: Int,
        dstX: Int,
        dstY: Int,
        dstZ: Int,
        srcWidth: Int,
        srcHeight: Int,
        srcDepth: Int
    )
    fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        count: Int,
        ids: IntArray,
        offset: Int,
        enabled: Boolean
    )
    fun glDebugMessageControl(source: Int, type: Int, severity: Int, count: Int, ids: IntBuffer, enabled: Boolean)
    fun glDebugMessageInsert(source: Int, type: Int, id: Int, severity: Int, length: Int, buf: String)
    fun glDebugMessageCallback(callback: GLES32.DebugProc?)
    fun glGetDebugMessageLog(
        count: Int,
        bufSize: Int,
        sources: IntArray,
        sourcesOffset: Int,
        types: IntArray,
        typesOffset: Int,
        ids: IntArray,
        idsOffset: Int,
        severities: IntArray,
        severitiesOffset: Int,
        lengths: IntArray,
        lengthsOffset: Int,
        messageLog: ByteArray,
        messageLogOffset: Int
    ): Int
    fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer,
        lengths: IntBuffer,
        messageLog: ByteBuffer
    ): Int
    fun glGetDebugMessageLog(
        count: Int,
        sources: IntArray,
        sourcesOffset: Int,
        types: IntArray,
        typesOffset: Int,
        ids: IntArray,
        idsOffset: Int,
        severities: IntArray,
        severitiesOffset: Int
    ): Array<String?>?
    fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer
    ): Array<String?>?
    fun glPushDebugGroup(source: Int, id: Int, length: Int, message: String)
    fun glPopDebugGroup()
    fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String)
    fun glGetObjectLabel(identifier: Int, name: Int): String
    fun glObjectPtrLabel(ptr: Long, label: String)
    fun glGetObjectPtrLabel(ptr: Long): String
    fun glGetPointerv(pname: Int): Long
    fun glEnablei(target: Int, index: Int)
    fun glDisablei(target: Int, index: Int)
    fun glBlendEquationi(buf: Int, mode: Int)
    fun glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int)
    fun glBlendFunci(buf: Int, src: Int, dst: Int)
    fun glBlendFuncSeparatei(buf: Int, srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean)
    fun glIsEnabledi(target: Int, index: Int): Boolean
    fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: Buffer, basevertex: Int)
    fun glDrawRangeElementsBaseVertex(
        mode: Int,
        start: Int,
        end: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        basevertex: Int
    )
    fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        instanceCount: Int,
        basevertex: Int
    )
    fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        offset: Int,
        instanceCount: Int,
        basevertex: Int
    )
    fun glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int)
    fun glPrimitiveBoundingBox(
        minX: Float,
        minY: Float,
        minZ: Float,
        minW: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        maxW: Float
    )
    fun glGetGraphicsResetStatus(): Int
    fun glReadnPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, bufSize: Int, data: Buffer)
    fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatArray, offset: Int)
    fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatBuffer)
    fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int)
    fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntBuffer)
    fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int)
    fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntBuffer)
    fun glMinSampleShading(value: Float)
    fun glPatchParameteri(pname: Int, value: Int)
    fun glTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glTexParameterIiv(target: Int, pname: Int, params: IntBuffer)
    fun glTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glTexParameterIuiv(target: Int, pname: Int, params: IntBuffer)
    fun glGetTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetTexParameterIiv(target: Int, pname: Int, params: IntBuffer)
    fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntBuffer)
    fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntArray, offset: Int)
    fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer)
    fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntArray, offset: Int)
    fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer)
    fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer)
    fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer)
    fun glTexBuffer(target: Int, internalformat: Int, buffer: Int)
    fun glTexBufferRange(target: Int, internalformat: Int, buffer: Int, offset: Int, size: Int)
    fun glTexStorage3DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        fixedsamplelocations: Boolean
    )
}
