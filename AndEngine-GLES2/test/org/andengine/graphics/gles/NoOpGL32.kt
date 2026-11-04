package org.andengine.graphics.gles

import android.opengl.GLES32
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * Test base: every [GL32] function does nothing and returns zero, false, an empty string or null.
 * Fakes extend it and override only what they record or answer.
 */
open class NoOpGL32 : GL32 {
    // OpenGL ES 2.0
    override fun glActiveTexture(texture: Int) {}
    override fun glAttachShader(program: Int, shader: Int) {}
    override fun glBindAttribLocation(program: Int, index: Int, name: String) {}
    override fun glBindBuffer(target: Int, buffer: Int) {}
    override fun glBindFramebuffer(target: Int, framebuffer: Int) {}
    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {}
    override fun glBindTexture(target: Int, texture: Int) {}
    override fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) {}
    override fun glBlendEquation(mode: Int) {}
    override fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) {}
    override fun glBlendFunc(sfactor: Int, dfactor: Int) {}
    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {}
    override fun glBufferData(target: Int, size: Int, data: Buffer?, usage: Int) {}
    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer?) {}
    override fun glCheckFramebufferStatus(target: Int): Int = 0
    override fun glClear(mask: Int) {}
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {}
    override fun glClearDepthf(depth: Float) {}
    override fun glClearStencil(s: Int) {}
    override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {}
    override fun glCompileShader(shader: Int) {}
    override fun glCompressedTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    ) {}
    override fun glCompressedTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        imageSize: Int,
        data: Buffer?
    ) {}
    override fun glCopyTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) {}
    override fun glCopyTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {}
    override fun glCreateProgram(): Int = 0
    override fun glCreateShader(type: Int): Int = 0
    override fun glCullFace(mode: Int) {}
    override fun glDeleteBuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteBuffers(n: Int, buffers: IntBuffer) {}
    override fun glDeleteFramebuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteFramebuffers(n: Int, framebuffers: IntBuffer) {}
    override fun glDeleteProgram(program: Int) {}
    override fun glDeleteRenderbuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer) {}
    override fun glDeleteShader(shader: Int) {}
    override fun glDeleteTextures(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteTextures(n: Int, textures: IntBuffer) {}
    override fun glDepthFunc(func: Int) {}
    override fun glDepthMask(flag: Boolean) {}
    override fun glDepthRangef(zNear: Float, zFar: Float) {}
    override fun glDetachShader(program: Int, shader: Int) {}
    override fun glDisable(cap: Int) {}
    override fun glDisableVertexAttribArray(index: Int) {}
    override fun glDrawArrays(mode: Int, first: Int, count: Int) {}
    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) {}
    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Buffer) {}
    override fun glEnable(cap: Int) {}
    override fun glEnableVertexAttribArray(index: Int) {}
    override fun glFinish() {}
    override fun glFlush() {}
    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {}
    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {}
    override fun glFrontFace(mode: Int) {}
    override fun glGenBuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenBuffers(n: Int, buffers: IntBuffer) {}
    override fun glGenerateMipmap(target: Int) {}
    override fun glGenFramebuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenFramebuffers(n: Int, framebuffers: IntBuffer) {}
    override fun glGenRenderbuffers(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenRenderbuffers(n: Int, renderbuffers: IntBuffer) {}
    override fun glGenTextures(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenTextures(n: Int, textures: IntBuffer) {}
    override fun glGetActiveAttrib(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int,
        name: ByteArray,
        nameOffset: Int
    ) {}
    override fun glGetActiveAttrib(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = ""
    override fun glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String = ""
    override fun glGetActiveUniform(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int,
        name: ByteArray,
        nameOffset: Int
    ) {}
    override fun glGetActiveUniform(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = ""
    override fun glGetActiveUniform(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String = ""
    override fun glGetAttachedShaders(
        program: Int,
        maxcount: Int,
        count: IntArray,
        countOffset: Int,
        shaders: IntArray,
        shadersOffset: Int
    ) {}
    override fun glGetAttachedShaders(program: Int, maxcount: Int, count: IntBuffer, shaders: IntBuffer) {}
    override fun glGetAttribLocation(program: Int, name: String): Int = 0
    override fun glGetBooleanv(pname: Int, params: BooleanArray, offset: Int) {}
    override fun glGetBooleanv(pname: Int, params: IntBuffer) {}
    override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetError(): Int = 0
    override fun glGetFloatv(pname: Int, params: FloatArray, offset: Int) {}
    override fun glGetFloatv(pname: Int, params: FloatBuffer) {}
    override fun glGetFramebufferAttachmentParameteriv(
        target: Int,
        attachment: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) {}
    override fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer) {}
    override fun glGetIntegerv(pname: Int, params: IntArray, offset: Int) {}
    override fun glGetIntegerv(pname: Int, params: IntBuffer) {}
    override fun glGetProgramiv(program: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetProgramiv(program: Int, pname: Int, params: IntBuffer) {}
    override fun glGetProgramInfoLog(program: Int): String = ""
    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetShaderiv(shader: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetShaderiv(shader: Int, pname: Int, params: IntBuffer) {}
    override fun glGetShaderInfoLog(shader: Int): String = ""
    override fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntArray,
        rangeOffset: Int,
        precision: IntArray,
        precisionOffset: Int
    ) {}
    override fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntBuffer,
        precision: IntBuffer
    ) {}
    override fun glGetShaderSource(
        shader: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        source: ByteArray,
        sourceOffset: Int
    ) {}
    override fun glGetShaderSource(shader: Int): String = ""
    override fun glGetString(name: Int): String? = null
    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int) {}
    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {}
    override fun glGetTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetUniformfv(program: Int, location: Int, params: FloatArray, offset: Int) {}
    override fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer) {}
    override fun glGetUniformiv(program: Int, location: Int, params: IntArray, offset: Int) {}
    override fun glGetUniformiv(program: Int, location: Int, params: IntBuffer) {}
    override fun glGetUniformLocation(program: Int, name: String): Int = 0
    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatArray, offset: Int) {}
    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer) {}
    override fun glGetVertexAttribiv(index: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer) {}
    override fun glHint(target: Int, mode: Int) {}
    override fun glIsBuffer(buffer: Int): Boolean = false
    override fun glIsEnabled(cap: Int): Boolean = false
    override fun glIsFramebuffer(framebuffer: Int): Boolean = false
    override fun glIsProgram(program: Int): Boolean = false
    override fun glIsRenderbuffer(renderbuffer: Int): Boolean = false
    override fun glIsShader(shader: Int): Boolean = false
    override fun glIsTexture(texture: Int): Boolean = false
    override fun glLineWidth(width: Float) {}
    override fun glLinkProgram(program: Int) {}
    override fun glPixelStorei(pname: Int, param: Int) {}
    override fun glPolygonOffset(factor: Float, units: Float) {}
    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer) {}
    override fun glReleaseShaderCompiler() {}
    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {}
    override fun glSampleCoverage(value: Float, invert: Boolean) {}
    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {}
    override fun glShaderBinary(
        n: Int,
        shaders: IntArray,
        offset: Int,
        binaryformat: Int,
        binary: Buffer,
        length: Int
    ) {}
    override fun glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int) {}
    override fun glShaderSource(shader: Int, string: String) {}
    override fun glStencilFunc(func: Int, ref: Int, mask: Int) {}
    override fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {}
    override fun glStencilMask(mask: Int) {}
    override fun glStencilMaskSeparate(face: Int, mask: Int) {}
    override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) {}
    override fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {}
    override fun glTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {}
    override fun glTexParameterf(target: Int, pname: Int, param: Float) {}
    override fun glTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int) {}
    override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {}
    override fun glTexParameteri(target: Int, pname: Int, param: Int) {}
    override fun glTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glTexParameteriv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {}
    override fun glUniform1f(location: Int, x: Float) {}
    override fun glUniform1fv(location: Int, count: Int, v: FloatArray, offset: Int) {}
    override fun glUniform1fv(location: Int, count: Int, v: FloatBuffer) {}
    override fun glUniform1i(location: Int, x: Int) {}
    override fun glUniform1iv(location: Int, count: Int, v: IntArray, offset: Int) {}
    override fun glUniform1iv(location: Int, count: Int, v: IntBuffer) {}
    override fun glUniform2f(location: Int, x: Float, y: Float) {}
    override fun glUniform2fv(location: Int, count: Int, v: FloatArray, offset: Int) {}
    override fun glUniform2fv(location: Int, count: Int, v: FloatBuffer) {}
    override fun glUniform2i(location: Int, x: Int, y: Int) {}
    override fun glUniform2iv(location: Int, count: Int, v: IntArray, offset: Int) {}
    override fun glUniform2iv(location: Int, count: Int, v: IntBuffer) {}
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {}
    override fun glUniform3fv(location: Int, count: Int, v: FloatArray, offset: Int) {}
    override fun glUniform3fv(location: Int, count: Int, v: FloatBuffer) {}
    override fun glUniform3i(location: Int, x: Int, y: Int, z: Int) {}
    override fun glUniform3iv(location: Int, count: Int, v: IntArray, offset: Int) {}
    override fun glUniform3iv(location: Int, count: Int, v: IntBuffer) {}
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {}
    override fun glUniform4fv(location: Int, count: Int, v: FloatArray, offset: Int) {}
    override fun glUniform4fv(location: Int, count: Int, v: FloatBuffer) {}
    override fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int) {}
    override fun glUniform4iv(location: Int, count: Int, v: IntArray, offset: Int) {}
    override fun glUniform4iv(location: Int, count: Int, v: IntBuffer) {}
    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUseProgram(program: Int) {}
    override fun glValidateProgram(program: Int) {}
    override fun glVertexAttrib1f(indx: Int, x: Float) {}
    override fun glVertexAttrib1fv(indx: Int, values: FloatArray, offset: Int) {}
    override fun glVertexAttrib1fv(indx: Int, values: FloatBuffer) {}
    override fun glVertexAttrib2f(indx: Int, x: Float, y: Float) {}
    override fun glVertexAttrib2fv(indx: Int, values: FloatArray, offset: Int) {}
    override fun glVertexAttrib2fv(indx: Int, values: FloatBuffer) {}
    override fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float) {}
    override fun glVertexAttrib3fv(indx: Int, values: FloatArray, offset: Int) {}
    override fun glVertexAttrib3fv(indx: Int, values: FloatBuffer) {}
    override fun glVertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {}
    override fun glVertexAttrib4fv(indx: Int, values: FloatArray, offset: Int) {}
    override fun glVertexAttrib4fv(indx: Int, values: FloatBuffer) {}
    override fun glVertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Int
    ) {}
    override fun glVertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: Buffer
    ) {}
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {}

    // OpenGL ES 3.0
    override fun glReadBuffer(mode: Int) {}
    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, indices: Buffer) {}
    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, offset: Int) {}
    override fun glTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {}
    override fun glTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        format: Int,
        type: Int,
        offset: Int
    ) {}
    override fun glTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {}
    override fun glTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        type: Int,
        offset: Int
    ) {}
    override fun glCopyTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {}
    override fun glCompressedTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    ) {}
    override fun glCompressedTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        imageSize: Int,
        offset: Int
    ) {}
    override fun glCompressedTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        imageSize: Int,
        data: Buffer?
    ) {}
    override fun glCompressedTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        imageSize: Int,
        offset: Int
    ) {}
    override fun glGenQueries(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenQueries(n: Int, ids: IntBuffer) {}
    override fun glDeleteQueries(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteQueries(n: Int, ids: IntBuffer) {}
    override fun glIsQuery(id: Int): Boolean = false
    override fun glBeginQuery(target: Int, id: Int) {}
    override fun glEndQuery(target: Int) {}
    override fun glGetQueryiv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetQueryiv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer) {}
    override fun glUnmapBuffer(target: Int): Boolean = false
    override fun glGetBufferPointerv(target: Int, pname: Int): Buffer? = null
    override fun glDrawBuffers(n: Int, bufs: IntArray, offset: Int) {}
    override fun glDrawBuffers(n: Int, bufs: IntBuffer) {}
    override fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {}
    override fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) {}
    override fun glBlitFramebuffer(
        srcX0: Int,
        srcY0: Int,
        srcX1: Int,
        srcY1: Int,
        dstX0: Int,
        dstY0: Int,
        dstX1: Int,
        dstY1: Int,
        mask: Int,
        filter: Int
    ) {}
    override fun glRenderbufferStorageMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int
    ) {}
    override fun glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int) {}
    override fun glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer? = null
    override fun glFlushMappedBufferRange(target: Int, offset: Int, length: Int) {}
    override fun glBindVertexArray(array: Int) {}
    override fun glDeleteVertexArrays(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteVertexArrays(n: Int, arrays: IntBuffer) {}
    override fun glGenVertexArrays(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenVertexArrays(n: Int, arrays: IntBuffer) {}
    override fun glIsVertexArray(array: Int): Boolean = false
    override fun glGetIntegeri_v(target: Int, index: Int, data: IntArray, offset: Int) {}
    override fun glGetIntegeri_v(target: Int, index: Int, data: IntBuffer) {}
    override fun glBeginTransformFeedback(primitiveMode: Int) {}
    override fun glEndTransformFeedback() {}
    override fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) {}
    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) {}
    override fun glTransformFeedbackVaryings(program: Int, varyings: Array<String>, bufferMode: Int) {}
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int,
        name: ByteArray,
        nameOffset: Int
    ) {}
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: Byte
    ) {}
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: ByteBuffer
    ) {}
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = ""
    override fun glGetTransformFeedbackVarying(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String = ""
    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Buffer) {}
    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int) {}
    override fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer) {}
    override fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer) {}
    override fun glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int) {}
    override fun glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int) {}
    override fun glVertexAttribI4iv(index: Int, v: IntArray, offset: Int) {}
    override fun glVertexAttribI4iv(index: Int, v: IntBuffer) {}
    override fun glVertexAttribI4uiv(index: Int, v: IntArray, offset: Int) {}
    override fun glVertexAttribI4uiv(index: Int, v: IntBuffer) {}
    override fun glGetUniformuiv(program: Int, location: Int, params: IntArray, offset: Int) {}
    override fun glGetUniformuiv(program: Int, location: Int, params: IntBuffer) {}
    override fun glGetFragDataLocation(program: Int, name: String): Int = 0
    override fun glUniform1ui(location: Int, v0: Int) {}
    override fun glUniform2ui(location: Int, v0: Int, v1: Int) {}
    override fun glUniform3ui(location: Int, v0: Int, v1: Int, v2: Int) {}
    override fun glUniform4ui(location: Int, v0: Int, v1: Int, v2: Int, v3: Int) {}
    override fun glUniform1uiv(location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glUniform1uiv(location: Int, count: Int, value: IntBuffer) {}
    override fun glUniform2uiv(location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glUniform2uiv(location: Int, count: Int, value: IntBuffer) {}
    override fun glUniform3uiv(location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glUniform3uiv(location: Int, count: Int, value: IntBuffer) {}
    override fun glUniform4uiv(location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glUniform4uiv(location: Int, count: Int, value: IntBuffer) {}
    override fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int) {}
    override fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer) {}
    override fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int) {}
    override fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer) {}
    override fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatArray, offset: Int) {}
    override fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer) {}
    override fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int) {}
    override fun glGetStringi(name: Int, index: Int): String? = null
    override fun glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int) {}
    override fun glGetUniformIndices(
        program: Int,
        uniformNames: Array<String>,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int
    ) {}
    override fun glGetUniformIndices(program: Int, uniformNames: Array<String>, uniformIndices: IntBuffer) {}
    override fun glGetActiveUniformsiv(
        program: Int,
        uniformCount: Int,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int,
        pname: Int,
        params: IntArray,
        paramsOffset: Int
    ) {}
    override fun glGetActiveUniformsiv(
        program: Int,
        uniformCount: Int,
        uniformIndices: IntBuffer,
        pname: Int,
        params: IntBuffer
    ) {}
    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = 0
    override fun glGetActiveUniformBlockiv(
        program: Int,
        uniformBlockIndex: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) {}
    override fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer) {}
    override fun glGetActiveUniformBlockName(
        program: Int,
        uniformBlockIndex: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        uniformBlockName: ByteArray,
        uniformBlockNameOffset: Int
    ) {}
    override fun glGetActiveUniformBlockName(
        program: Int,
        uniformBlockIndex: Int,
        length: Buffer,
        uniformBlockName: Buffer
    ) {}
    override fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = ""
    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) {}
    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int) {}
    override fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indices: Buffer, instanceCount: Int) {}
    override fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indicesOffset: Int, instanceCount: Int) {}
    override fun glFenceSync(condition: Int, flags: Int): Long = 0L
    override fun glIsSync(sync: Long): Boolean = false
    override fun glDeleteSync(sync: Long) {}
    override fun glClientWaitSync(sync: Long, flags: Int, timeout: Long): Int = 0
    override fun glWaitSync(sync: Long, flags: Int, timeout: Long) {}
    override fun glGetInteger64v(pname: Int, params: LongArray, offset: Int) {}
    override fun glGetInteger64v(pname: Int, params: LongBuffer) {}
    override fun glGetSynciv(
        sync: Long,
        pname: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        values: IntArray,
        valuesOffset: Int
    ) {}
    override fun glGetSynciv(sync: Long, pname: Int, bufSize: Int, length: IntBuffer, values: IntBuffer) {}
    override fun glGetInteger64i_v(target: Int, index: Int, data: LongArray, offset: Int) {}
    override fun glGetInteger64i_v(target: Int, index: Int, data: LongBuffer) {}
    override fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongArray, offset: Int) {}
    override fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer) {}
    override fun glGenSamplers(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenSamplers(count: Int, samplers: IntBuffer) {}
    override fun glDeleteSamplers(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteSamplers(count: Int, samplers: IntBuffer) {}
    override fun glIsSampler(sampler: Int): Boolean = false
    override fun glBindSampler(unit: Int, sampler: Int) {}
    override fun glSamplerParameteri(sampler: Int, pname: Int, param: Int) {}
    override fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntArray, offset: Int) {}
    override fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer) {}
    override fun glSamplerParameterf(sampler: Int, pname: Int, param: Float) {}
    override fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatArray, offset: Int) {}
    override fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer) {}
    override fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer) {}
    override fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatArray, offset: Int) {}
    override fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer) {}
    override fun glVertexAttribDivisor(index: Int, divisor: Int) {}
    override fun glBindTransformFeedback(target: Int, id: Int) {}
    override fun glDeleteTransformFeedbacks(n: Int, ids: IntArray, offset: Int) {}
    override fun glDeleteTransformFeedbacks(n: Int, ids: IntBuffer) {}
    override fun glGenTransformFeedbacks(n: Int, ids: IntArray, offset: Int) {}
    override fun glGenTransformFeedbacks(n: Int, ids: IntBuffer) {}
    override fun glIsTransformFeedback(id: Int): Boolean = false
    override fun glPauseTransformFeedback() {}
    override fun glResumeTransformFeedback() {}
    override fun glGetProgramBinary(
        program: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        binaryFormat: IntArray,
        binaryFormatOffset: Int,
        binary: Buffer
    ) {}
    override fun glGetProgramBinary(
        program: Int,
        bufSize: Int,
        length: IntBuffer,
        binaryFormat: IntBuffer,
        binary: Buffer
    ) {}
    override fun glProgramBinary(program: Int, binaryFormat: Int, binary: Buffer, length: Int) {}
    override fun glProgramParameteri(program: Int, pname: Int, value: Int) {}
    override fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntArray, offset: Int) {}
    override fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer) {}
    override fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntArray,
        offset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {}
    override fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntBuffer,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {}
    override fun glTexStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int) {}
    override fun glTexStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int) {}
    override fun glGetInternalformativ(
        target: Int,
        internalformat: Int,
        pname: Int,
        bufSize: Int,
        params: IntArray,
        offset: Int
    ) {}
    override fun glGetInternalformativ(target: Int, internalformat: Int, pname: Int, bufSize: Int, params: IntBuffer) {}
    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, offset: Int) {}

    // OpenGL ES 3.1
    override fun glDispatchCompute(numGroupsX: Int, numGroupsY: Int, numGroupsZ: Int) {}
    override fun glDispatchComputeIndirect(indirect: Long) {}
    override fun glDrawArraysIndirect(mode: Int, indirect: Long) {}
    override fun glDrawElementsIndirect(mode: Int, type: Int, indirect: Long) {}
    override fun glFramebufferParameteri(target: Int, pname: Int, param: Int) {}
    override fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetProgramInterfaceiv(
        program: Int,
        programInterface: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) {}
    override fun glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer) {}
    override fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int = 0
    override fun glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String = ""
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
    ) {}
    override fun glGetProgramResourceiv(
        program: Int,
        programInterface: Int,
        index: Int,
        propCount: Int,
        props: IntBuffer,
        bufSize: Int,
        length: IntBuffer,
        params: IntBuffer
    ) {}
    override fun glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int = 0
    override fun glUseProgramStages(pipeline: Int, stages: Int, program: Int) {}
    override fun glActiveShaderProgram(pipeline: Int, program: Int) {}
    override fun glCreateShaderProgramv(type: Int, strings: Array<String>): Int = 0
    override fun glBindProgramPipeline(pipeline: Int) {}
    override fun glDeleteProgramPipelines(n: Int, pipelines: IntArray, offset: Int) {}
    override fun glDeleteProgramPipelines(n: Int, pipelines: IntBuffer) {}
    override fun glGenProgramPipelines(n: Int, pipelines: IntArray, offset: Int) {}
    override fun glGenProgramPipelines(n: Int, pipelines: IntBuffer) {}
    override fun glIsProgramPipeline(pipeline: Int): Boolean = false
    override fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer) {}
    override fun glProgramUniform1i(program: Int, location: Int, v0: Int) {}
    override fun glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int) {}
    override fun glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int) {}
    override fun glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int) {}
    override fun glProgramUniform1ui(program: Int, location: Int, v0: Int) {}
    override fun glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int) {}
    override fun glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int) {}
    override fun glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int) {}
    override fun glProgramUniform1f(program: Int, location: Int, v0: Float) {}
    override fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float) {}
    override fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float) {}
    override fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float) {}
    override fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform1iv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform2iv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform3iv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform4iv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform1uiv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform2uiv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform3uiv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntArray, offset: Int) {}
    override fun glProgramUniform4uiv(program: Int, location: Int, count: Int, value: IntBuffer) {}
    override fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) {}
    override fun glProgramUniform1fv(program: Int, location: Int, count: Int, value: FloatBuffer) {}
    override fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) {}
    override fun glProgramUniform2fv(program: Int, location: Int, count: Int, value: FloatBuffer) {}
    override fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) {}
    override fun glProgramUniform3fv(program: Int, location: Int, count: Int, value: FloatBuffer) {}
    override fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatArray, offset: Int) {}
    override fun glProgramUniform4fv(program: Int, location: Int, count: Int, value: FloatBuffer) {}
    override fun glProgramUniformMatrix2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix2x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix2x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix3x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix3x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix2x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix2x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix4x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix4x2fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix3x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix3x4fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glProgramUniformMatrix4x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatArray,
        offset: Int
    ) {}
    override fun glProgramUniformMatrix4x3fv(
        program: Int,
        location: Int,
        count: Int,
        transpose: Boolean,
        value: FloatBuffer
    ) {}
    override fun glValidateProgramPipeline(pipeline: Int) {}
    override fun glGetProgramPipelineInfoLog(program: Int): String = ""
    override fun glBindImageTexture(
        unit: Int,
        texture: Int,
        level: Int,
        layered: Boolean,
        layer: Int,
        access: Int,
        format: Int
    ) {}
    override fun glGetBooleani_v(target: Int, index: Int, data: BooleanArray, offset: Int) {}
    override fun glGetBooleani_v(target: Int, index: Int, data: IntBuffer) {}
    override fun glMemoryBarrier(barriers: Int) {}
    override fun glMemoryBarrierByRegion(barriers: Int) {}
    override fun glTexStorage2DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        fixedsamplelocations: Boolean
    ) {}
    override fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatArray, offset: Int) {}
    override fun glGetMultisamplefv(pname: Int, index: Int, `val`: FloatBuffer) {}
    override fun glSampleMaski(maskNumber: Int, mask: Int) {}
    override fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntBuffer) {}
    override fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatArray, offset: Int) {}
    override fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatBuffer) {}
    override fun glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int) {}
    override fun glVertexAttribFormat(
        attribindex: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        relativeoffset: Int
    ) {}
    override fun glVertexAttribIFormat(attribindex: Int, size: Int, type: Int, relativeoffset: Int) {}
    override fun glVertexAttribBinding(attribindex: Int, bindingindex: Int) {}
    override fun glVertexBindingDivisor(bindingindex: Int, divisor: Int) {}

    // OpenGL ES 3.2
    override fun glBlendBarrier() {}
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
    ) {}
    override fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        count: Int,
        ids: IntArray,
        offset: Int,
        enabled: Boolean
    ) {}
    override fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        count: Int,
        ids: IntBuffer,
        enabled: Boolean
    ) {}
    override fun glDebugMessageInsert(source: Int, type: Int, id: Int, severity: Int, length: Int, buf: String) {}
    override fun glDebugMessageCallback(callback: GLES32.DebugProc?) {}
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
    ): Int = 0
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer,
        lengths: IntBuffer,
        messageLog: ByteBuffer
    ): Int = 0
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
    ): Array<String?>? = null
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntBuffer,
        types: IntBuffer,
        ids: IntBuffer,
        severities: IntBuffer
    ): Array<String?>? = null
    override fun glPushDebugGroup(source: Int, id: Int, length: Int, message: String) {}
    override fun glPopDebugGroup() {}
    override fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String) {}
    override fun glGetObjectLabel(identifier: Int, name: Int): String = ""
    override fun glObjectPtrLabel(ptr: Long, label: String) {}
    override fun glGetObjectPtrLabel(ptr: Long): String = ""
    override fun glGetPointerv(pname: Int): Long = 0L
    override fun glEnablei(target: Int, index: Int) {}
    override fun glDisablei(target: Int, index: Int) {}
    override fun glBlendEquationi(buf: Int, mode: Int) {}
    override fun glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int) {}
    override fun glBlendFunci(buf: Int, src: Int, dst: Int) {}
    override fun glBlendFuncSeparatei(buf: Int, srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {}
    override fun glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean) {}
    override fun glIsEnabledi(target: Int, index: Int): Boolean = false
    override fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: Buffer, basevertex: Int) {}
    override fun glDrawRangeElementsBaseVertex(
        mode: Int,
        start: Int,
        end: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        basevertex: Int
    ) {}
    override fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        indices: Buffer,
        instanceCount: Int,
        basevertex: Int
    ) {}
    override fun glDrawElementsInstancedBaseVertex(
        mode: Int,
        count: Int,
        type: Int,
        offset: Int,
        instanceCount: Int,
        basevertex: Int
    ) {}
    override fun glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int) {}
    override fun glPrimitiveBoundingBox(
        minX: Float,
        minY: Float,
        minZ: Float,
        minW: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        maxW: Float
    ) {}
    override fun glGetGraphicsResetStatus(): Int = 0
    override fun glReadnPixels(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        bufSize: Int,
        data: Buffer
    ) {}
    override fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatArray, offset: Int) {}
    override fun glGetnUniformfv(program: Int, location: Int, bufSize: Int, params: FloatBuffer) {}
    override fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int) {}
    override fun glGetnUniformiv(program: Int, location: Int, bufSize: Int, params: IntBuffer) {}
    override fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntArray, offset: Int) {}
    override fun glGetnUniformuiv(program: Int, location: Int, bufSize: Int, params: IntBuffer) {}
    override fun glMinSampleShading(value: Float) {}
    override fun glPatchParameteri(pname: Int, value: Int) {}
    override fun glTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glTexParameterIiv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glTexParameterIuiv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetTexParameterIiv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetTexParameterIiv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetTexParameterIuiv(target: Int, pname: Int, params: IntBuffer) {}
    override fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntArray, offset: Int) {}
    override fun glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer) {}
    override fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntArray, offset: Int) {}
    override fun glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer) {}
    override fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer) {}
    override fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntArray, offset: Int) {}
    override fun glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer) {}
    override fun glTexBuffer(target: Int, internalformat: Int, buffer: Int) {}
    override fun glTexBufferRange(target: Int, internalformat: Int, buffer: Int, offset: Int, size: Int) {}
    override fun glTexStorage3DMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        fixedsamplelocations: Boolean
    ) {}
}
