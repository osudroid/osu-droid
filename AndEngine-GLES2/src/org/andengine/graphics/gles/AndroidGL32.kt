package org.andengine.graphics.gles

import android.opengl.GLES32
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

/** [GL32] backed by the static [android.opengl.GLES32] methods. */
class AndroidGL32 : AndroidGL31(), GL32 {
    // OpenGL ES 3.2
    override fun glBlendBarrier() = GLES32.glBlendBarrier()
    override fun glCopyImageSubData(
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
    ) =
        GLES32.glCopyImageSubData(
            srcName,
            srcTarget,
            srcLevel,
            srcX,
            srcY,
            srcZ,
            dstName,
            dstTarget,
            dstLevel,
            dstX,
            dstY,
            dstZ,
            srcWidth,
            srcHeight,
            srcDepth
        )
    override fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        count: Int,
        ids: IntArray,
        offset: Int,
        enabled: Boolean
    ) = GLES32.glDebugMessageControl(source, type, severity, count, ids, offset, enabled)
    override fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        count: Int,
        ids: IntBuffer,
        enabled: Boolean
    ) = GLES32.glDebugMessageControl(source, type, severity, count, ids, enabled)
    override fun glDebugMessageInsert(source: Int, type: Int, id: Int, severity: Int, length: Int, buf: String) =
        GLES32.glDebugMessageInsert(source, type, id, severity, length, buf)
    override fun glDebugMessageCallback(callback: GLES32.DebugProc?) = GLES32.glDebugMessageCallback(callback)
    override fun glGetDebugMessageLog(
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
    ): Int =
        GLES32.glGetDebugMessageLog(
            count,
            bufSize,
            sources,
            sourcesOffset,
            types,
            typesOffset,
            ids,
            idsOffset,
            severities,
            severitiesOffset,
            lengths,
            lengthsOffset,
            messageLog,
            messageLogOffset
        )
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer,
        lengths: IntBuffer,
        messageLog: ByteBuffer
    ): Int = GLES32.glGetDebugMessageLog(count, sources, types, ids, severities, lengths, messageLog)
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntArray,
        sourcesOffset: Int,
        types: IntArray,
        typesOffset: Int,
        ids: IntArray,
        idsOffset: Int,
        severities: IntArray,
        severitiesOffset: Int
    ): Array<String?>? =
        GLES32.glGetDebugMessageLog(
            count,
            sources,
            sourcesOffset,
            types,
            typesOffset,
            ids,
            idsOffset,
            severities,
            severitiesOffset
        )
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer
    ): Array<String?>? = GLES32.glGetDebugMessageLog(count, sources, types, ids, severities)
    override fun glPushDebugGroup(source: Int, id: Int, length: Int, message: String) =
        GLES32.glPushDebugGroup(source, id, length, message)
    override fun glPopDebugGroup() = GLES32.glPopDebugGroup()
    override fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String) =
        GLES32.glObjectLabel(identifier, name, length, label)
    override fun glGetObjectLabel(identifier: Int, name: Int): String = GLES32.glGetObjectLabel(identifier, name)
    override fun glObjectPtrLabel(ptr: Long, label: String) = GLES32.glObjectPtrLabel(ptr, label)
    override fun glGetObjectPtrLabel(ptr: Long): String = GLES32.glGetObjectPtrLabel(ptr)
    override fun glGetPointerv(pname: Int): Long = GLES32.glGetPointerv(pname)
    override fun glEnablei(target: Int, index: Int) = GLES32.glEnablei(target, index)
    override fun glDisablei(target: Int, index: Int) = GLES32.glDisablei(target, index)
    override fun glBlendEquationi(buf: Int, mode: Int) = GLES32.glBlendEquationi(buf, mode)
    override fun glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int) =
        GLES32.glBlendEquationSeparatei(buf, modeRGB, modeAlpha)
    override fun glBlendFunci(buf: Int, src: Int, dst: Int) = GLES32.glBlendFunci(buf, src, dst)
    override fun glBlendFuncSeparatei(buf: Int, srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) =
        GLES32.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha)
    override fun glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean) =
        GLES32.glColorMaski(index, r, g, b, a)
    override fun glIsEnabledi(target: Int, index: Int): Boolean = GLES32.glIsEnabledi(target, index)
    override fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: Buffer, basevertex: Int) =
        GLES32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex)
    override fun glDrawRangeElementsBaseVertex(
        mode: Int,
        start: Int,
        end: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        basevertex: Int
    ) = GLES32.glDrawRangeElementsBaseVertex(mode, start, end, count, type, indices, basevertex)
    override fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        instanceCount: Int,
        basevertex: Int
    ) = GLES32.glDrawElementsInstancedBaseVertex(mode, count, type, indices, instanceCount, basevertex)
    override fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        offset: Int,
        instanceCount: Int,
        basevertex: Int
    ) = GLES32.glDrawElementsInstancedBaseVertex(mode, count, type, offset, instanceCount, basevertex)
    override fun glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int) =
        GLES32.glFramebufferTexture(target, attachment, texture, level)
    override fun glPrimitiveBoundingBox(
        minX: Float,
        minY: Float,
        minZ: Float,
        minW: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        maxW: Float
    ) = GLES32.glPrimitiveBoundingBox(minX, minY, minZ, minW, maxX, maxY, maxZ, maxW)
    override fun glGetGraphicsResetStatus(): Int = GLES32.glGetGraphicsResetStatus()
    override fun glReadnPixels(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        bufSize: Int,
        data: Buffer
    ) = GLES32.glReadnPixels(x, y, width, height, format, type, bufSize, data)
    override fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatArray, offset: Int) =
        GLES32.glGetnUniformfv(program, location, bufSize, params, offset)
    override fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatBuffer) =
        GLES32.glGetnUniformfv(program, location, bufSize, params)
    override fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int) =
        GLES32.glGetnUniformiv(program, location, bufSize, params, offset)
    override fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntBuffer) =
        GLES32.glGetnUniformiv(program, location, bufSize, params)
    override fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int) =
        GLES32.glGetnUniformuiv(program, location, bufSize, params, offset)
    override fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntBuffer) =
        GLES32.glGetnUniformuiv(program, location, bufSize, params)
    override fun glMinSampleShading(value: Float) = GLES32.glMinSampleShading(value)
    override fun glPatchParameteri(pname: Int, value: Int) = GLES32.glPatchParameteri(pname, value)
    override fun glTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glTexParameterIiv(target, pname, params, offset)
    override fun glTexParameterIiv(target: Int, pname: Int, params: IntBuffer) =
        GLES32.glTexParameterIiv(target, pname, params)
    override fun glTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glTexParameterIuiv(target, pname, params, offset)
    override fun glTexParameterIuiv(target: Int, pname: Int, params: IntBuffer) =
        GLES32.glTexParameterIuiv(target, pname, params)
    override fun glGetTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glGetTexParameterIiv(target, pname, params, offset)
    override fun glGetTexParameterIiv(target: Int, pname: Int, params: IntBuffer) =
        GLES32.glGetTexParameterIiv(target, pname, params)
    override fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glGetTexParameterIuiv(target, pname, params, offset)
    override fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntBuffer) =
        GLES32.glGetTexParameterIuiv(target, pname, params)
    override fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntArray, offset: Int) =
        GLES32.glSamplerParameterIiv(sampler, pname, param, offset)
    override fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer) =
        GLES32.glSamplerParameterIiv(sampler, pname, param)
    override fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntArray, offset: Int) =
        GLES32.glSamplerParameterIuiv(sampler, pname, param, offset)
    override fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer) =
        GLES32.glSamplerParameterIuiv(sampler, pname, param)
    override fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glGetSamplerParameterIiv(sampler, pname, params, offset)
    override fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer) =
        GLES32.glGetSamplerParameterIiv(sampler, pname, params)
    override fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntArray, offset: Int) =
        GLES32.glGetSamplerParameterIuiv(sampler, pname, params, offset)
    override fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer) =
        GLES32.glGetSamplerParameterIuiv(sampler, pname, params)
    override fun glTexBuffer(target: Int, internalformat: Int, buffer: Int) =
        GLES32.glTexBuffer(target, internalformat, buffer)
    override fun glTexBufferRange(target: Int, internalformat: Int, buffer: Int, offset: Int, size: Int) =
        GLES32.glTexBufferRange(target, internalformat, buffer, offset, size)
    override fun glTexStorage3DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        fixedsamplelocations: Boolean
    ) = GLES32.glTexStorage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations)
}
