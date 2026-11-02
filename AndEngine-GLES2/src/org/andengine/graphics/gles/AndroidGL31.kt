package org.andengine.graphics.gles

import android.opengl.GLES31
import java.nio.FloatBuffer
import java.nio.IntBuffer

/** [GL31] backed by the static [android.opengl.GLES31] methods. */
open class AndroidGL31 : AndroidGL30(), GL31 {
    // OpenGL ES 3.1
    override fun glDispatchCompute(numGroupsX: Int, numGroupsY: Int, numGroupsZ: Int) =
        GLES31.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ)
    override fun glDispatchComputeIndirect(indirect: Long) = GLES31.glDispatchComputeIndirect(indirect)
    override fun glDrawArraysIndirect(mode: Int, indirect: Long) = GLES31.glDrawArraysIndirect(mode, indirect)
    override fun glDrawElementsIndirect(mode: Int, type: Int, indirect: Long) =
        GLES31.glDrawElementsIndirect(mode, type, indirect)
    override fun glFramebufferParameteri(target: Int, pname: Int, param: Int) =
        GLES31.glFramebufferParameteri(target, pname, param)
    override fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES31.glGetFramebufferParameteriv(target, pname, params, offset)
    override fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer) =
        GLES31.glGetFramebufferParameteriv(target, pname, params)
    override fun glGetProgramInterfaceiv(
        program: Int,
        programInterface: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) = GLES31.glGetProgramInterfaceiv(program, programInterface, pname, params, offset)
    override fun glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer) =
        GLES31.glGetProgramInterfaceiv(program, programInterface, pname, params)
    override fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int =
        GLES31.glGetProgramResourceIndex(program, programInterface, name)
    override fun glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String =
        GLES31.glGetProgramResourceName(program, programInterface, index)
    override fun glGetProgramResourceiv(
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
    ) =
        GLES31.glGetProgramResourceiv(
            program,
            programInterface,
            index,
            propCount,
            props,
            propsOffset,
            bufSize,
            length,
            lengthOffset,
            params,
            paramsOffset
        )
    override fun glGetProgramResourceiv(
        program: Int,
        programInterface: Int,
        index: Int,
        propCount: Int,
        props: IntBuffer,
        bufSize: Int,
        length: IntBuffer,
        params: IntBuffer
    ) = GLES31.glGetProgramResourceiv(program, programInterface, index, propCount, props, bufSize, length, params)
    override fun glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int =
        GLES31.glGetProgramResourceLocation(program, programInterface, name)
    override fun glUseProgramStages(pipeline: Int, stages: Int, program: Int) =
        GLES31.glUseProgramStages(pipeline, stages, program)
    override fun glActiveShaderProgram(pipeline: Int, program: Int) = GLES31.glActiveShaderProgram(pipeline, program)
    override fun glCreateShaderProgramv(type: Int, strings: Array<String>): Int =
        GLES31.glCreateShaderProgramv(type, strings)
    override fun glBindProgramPipeline(pipeline: Int) = GLES31.glBindProgramPipeline(pipeline)
    override fun glDeleteProgramPipelines(n: Int, pipelines: IntArray, offset: Int) =
        GLES31.glDeleteProgramPipelines(n, pipelines, offset)
    override fun glDeleteProgramPipelines(n: Int, pipelines: IntBuffer) = GLES31.glDeleteProgramPipelines(n, pipelines)
    override fun glGenProgramPipelines(n: Int, pipelines: IntArray, offset: Int) =
        GLES31.glGenProgramPipelines(n, pipelines, offset)
    override fun glGenProgramPipelines(n: Int, pipelines: IntBuffer) = GLES31.glGenProgramPipelines(n, pipelines)
    override fun glIsProgramPipeline(pipeline: Int): Boolean = GLES31.glIsProgramPipeline(pipeline)
    override fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntArray, offset: Int) =
        GLES31.glGetProgramPipelineiv(pipeline, pname, params, offset)
    override fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer) =
        GLES31.glGetProgramPipelineiv(pipeline, pname, params)
    override fun glProgramUniform1i(program: Int, location: Int, v0: Int) =
        GLES31.glProgramUniform1i(program, location, v0)
    override fun glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int) =
        GLES31.glProgramUniform2i(program, location, v0, v1)
    override fun glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int) =
        GLES31.glProgramUniform3i(program, location, v0, v1, v2)
    override fun glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int) =
        GLES31.glProgramUniform4i(program, location, v0, v1, v2, v3)
    override fun glProgramUniform1ui(program: Int, location: Int, v0: Int) =
        GLES31.glProgramUniform1ui(program, location, v0)
    override fun glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int) =
        GLES31.glProgramUniform2ui(program, location, v0, v1)
    override fun glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int) =
        GLES31.glProgramUniform3ui(program, location, v0, v1, v2)
    override fun glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int) =
        GLES31.glProgramUniform4ui(program, location, v0, v1, v2, v3)
    override fun glProgramUniform1f(program: Int, location: Int, v0: Float) =
        GLES31.glProgramUniform1f(program, location, v0)
    override fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float) =
        GLES31.glProgramUniform2f(program, location, v0, v1)
    override fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float) =
        GLES31.glProgramUniform3f(program, location, v0, v1, v2)
    override fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float) =
        GLES31.glProgramUniform4f(program, location, v0, v1, v2, v3)
    override fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform1iv(program, location, count, value, offset)
    override fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform1iv(program, location, count, value)
    override fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform2iv(program, location, count, value, offset)
    override fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform2iv(program, location, count, value)
    override fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform3iv(program, location, count, value, offset)
    override fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform3iv(program, location, count, value)
    override fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform4iv(program, location, count, value, offset)
    override fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform4iv(program, location, count, value)
    override fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform1uiv(program, location, count, value, offset)
    override fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform1uiv(program, location, count, value)
    override fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform2uiv(program, location, count, value, offset)
    override fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform2uiv(program, location, count, value)
    override fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform3uiv(program, location, count, value, offset)
    override fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform3uiv(program, location, count, value)
    override fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) =
        GLES31.glProgramUniform4uiv(program, location, count, value, offset)
    override fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntBuffer) =
        GLES31.glProgramUniform4uiv(program, location, count, value)
    override fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) =
        GLES31.glProgramUniform1fv(program, location, count, value, offset)
    override fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatBuffer) =
        GLES31.glProgramUniform1fv(program, location, count, value)
    override fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) =
        GLES31.glProgramUniform2fv(program, location, count, value, offset)
    override fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatBuffer) =
        GLES31.glProgramUniform2fv(program, location, count, value)
    override fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) =
        GLES31.glProgramUniform3fv(program, location, count, value, offset)
    override fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatBuffer) =
        GLES31.glProgramUniform3fv(program, location, count, value)
    override fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) =
        GLES31.glProgramUniform4fv(program, location, count, value, offset)
    override fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatBuffer) =
        GLES31.glProgramUniform4fv(program, location, count, value)
    override fun glProgramUniformMatrix2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix2fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix2fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix3fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix3fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix4fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix4fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix2x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix2x3fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix2x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix2x3fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix3x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix3x2fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix3x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix3x2fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix2x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix2x4fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix2x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix2x4fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix4x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix4x2fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix4x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix4x2fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix3x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix3x4fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix3x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix3x4fv(program, location, count, transpose, value)
    override fun glProgramUniformMatrix4x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) = GLES31.glProgramUniformMatrix4x3fv(program, location, count, transpose, value, offset)
    override fun glProgramUniformMatrix4x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) = GLES31.glProgramUniformMatrix4x3fv(program, location, count, transpose, value)
    override fun glValidateProgramPipeline(pipeline: Int) = GLES31.glValidateProgramPipeline(pipeline)
    override fun glGetProgramPipelineInfoLog(program: Int): String = GLES31.glGetProgramPipelineInfoLog(program)
    override fun glBindImageTexture(
        unit: Int,
        texture: Int,
        level: Int,
        layered: Boolean,
        layer: Int,
        access: Int,
        format: Int
    ) = GLES31.glBindImageTexture(unit, texture, level, layered, layer, access, format)
    override fun glGetBooleani_v(target: Int, index: Int, data: BooleanArray, offset: Int) =
        GLES31.glGetBooleani_v(target, index, data, offset)
    override fun glGetBooleani_v(target: Int, index: Int, data: IntBuffer) = GLES31.glGetBooleani_v(target, index, data)
    override fun glMemoryBarrier(barriers: Int) = GLES31.glMemoryBarrier(barriers)
    override fun glMemoryBarrierByRegion(barriers: Int) = GLES31.glMemoryBarrierByRegion(barriers)
    override fun glTexStorage2DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        fixedsamplelocations: Boolean
    ) = GLES31.glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations)
    override fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatArray, offset: Int) =
        GLES31.glGetMultisamplefv(pname, index, `val`, offset)
    override fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatBuffer) =
        GLES31.glGetMultisamplefv(pname, index, `val`)
    override fun glSampleMaski(maskNumber: Int, mask: Int) = GLES31.glSampleMaski(maskNumber, mask)
    override fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntArray, offset: Int) =
        GLES31.glGetTexLevelParameteriv(target, level, pname, params, offset)
    override fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntBuffer) =
        GLES31.glGetTexLevelParameteriv(target, level, pname, params)
    override fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatArray, offset: Int) =
        GLES31.glGetTexLevelParameterfv(target, level, pname, params, offset)
    override fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatBuffer) =
        GLES31.glGetTexLevelParameterfv(target, level, pname, params)
    override fun glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int) =
        GLES31.glBindVertexBuffer(bindingindex, buffer, offset, stride)
    override fun glVertexAttribFormat(
        attribindex: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        relativeoffset: Int
    ) = GLES31.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset)
    override fun glVertexAttribIFormat(attribindex: Int, size: Int, type: Int, relativeoffset: Int) =
        GLES31.glVertexAttribIFormat(attribindex, size, type, relativeoffset)
    override fun glVertexAttribBinding(attribindex: Int, bindingindex: Int) =
        GLES31.glVertexAttribBinding(attribindex, bindingindex)
    override fun glVertexBindingDivisor(bindingindex: Int, divisor: Int) =
        GLES31.glVertexBindingDivisor(bindingindex, divisor)
}
