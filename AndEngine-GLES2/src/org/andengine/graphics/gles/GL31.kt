package org.andengine.graphics.gles

import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * The OpenGL ES 3.1 API: [GL30] plus every function of [android.opengl.GLES31], mirrored one to one.
 *
 * The rule about cached state in [GL30] applies.
 */
interface GL31 : GL30 {
    // OpenGL ES 3.1
    fun glDispatchCompute(numGroupsX: Int, numGroupsY: Int, numGroupsZ: Int)
    fun glDispatchComputeIndirect(indirect: Long)
    fun glDrawArraysIndirect(mode: Int, indirect: Long)
    fun glDrawElementsIndirect(mode: Int, type: Int, indirect: Long)
    fun glFramebufferParameteri(target: Int, pname: Int, param: Int)
    fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer)
    fun glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer)
    fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int
    fun glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String
    fun glGetProgramResourceiv(
        program: Int,
        programInterface: Int,
        index: Int,
        propCount: Int,
        props: IntArray,
        propsOffset: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        params: IntArray,
        paramsOffset: Int
    )
    fun glGetProgramResourceiv(
        program: Int,
        programInterface: Int,
        index: Int,
        propCount: Int,
        props: IntBuffer,
        bufSize: Int,
        length: IntBuffer,
        params: IntBuffer
    )
    fun glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int
    fun glUseProgramStages(pipeline: Int, stages: Int, program: Int)
    fun glActiveShaderProgram(pipeline: Int, program: Int)
    fun glCreateShaderProgramv(type: Int, strings: Array<String>): Int
    fun glBindProgramPipeline(pipeline: Int)
    fun glDeleteProgramPipelines(n: Int, pipelines: IntArray, offset: Int)
    fun glDeleteProgramPipelines(n: Int, pipelines: IntBuffer)
    fun glGenProgramPipelines(n: Int, pipelines: IntArray, offset: Int)
    fun glGenProgramPipelines(n: Int, pipelines: IntBuffer)
    fun glIsProgramPipeline(pipeline: Int): Boolean
    fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer)
    fun glProgramUniform1i(program: Int, location: Int, v0: Int)
    fun glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int)
    fun glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int)
    fun glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int)
    fun glProgramUniform1ui(program: Int, location: Int, v0: Int)
    fun glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int)
    fun glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int)
    fun glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int)
    fun glProgramUniform1f(program: Int, location: Int, v0: Float)
    fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float)
    fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float)
    fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float)
    fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int)
    fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntBuffer)
    fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int)
    fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatBuffer)
    fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int)
    fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatBuffer)
    fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int)
    fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatBuffer)
    fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int)
    fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatBuffer)
    fun glProgramUniformMatrix2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix2fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix3fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix4fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix2x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix2x3fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix3x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix3x2fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix2x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix2x4fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix4x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix4x2fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix3x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix3x4fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glProgramUniformMatrix4x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    )
    fun glProgramUniformMatrix4x3fv(program: Int, location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glValidateProgramPipeline(pipeline: Int)
    fun glGetProgramPipelineInfoLog(program: Int): String
    fun glBindImageTexture(unit: Int, texture: Int, level: Int, layered: Boolean, layer: Int, access: Int, format: Int)
    fun glGetBooleani_v(target: Int, index: Int, data: BooleanArray, offset: Int)
    fun glGetBooleani_v(target: Int, index: Int, data: IntBuffer)
    fun glMemoryBarrier(barriers: Int)
    fun glMemoryBarrierByRegion(barriers: Int)
    fun glTexStorage2DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        fixedsamplelocations: Boolean
    )
    fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatArray, offset: Int)
    fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatBuffer)
    fun glSampleMaski(maskNumber: Int, mask: Int)
    fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntBuffer)
    fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatArray, offset: Int)
    fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatBuffer)
    fun glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int)
    fun glVertexAttribFormat(attribindex: Int, size: Int, type: Int, normalized: Boolean, relativeoffset: Int)
    fun glVertexAttribIFormat(attribindex: Int, size: Int, type: Int, relativeoffset: Int)
    fun glVertexAttribBinding(attribindex: Int, bindingindex: Int)
    fun glVertexBindingDivisor(bindingindex: Int, divisor: Int)
}
