package org.andengine.graphics.gles

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * The OpenGL ES 2.0 and 3.0 API: every function of [android.opengl.GLES20] and [android.opengl.GLES30],
 * mirrored one to one with the same names, overloads and parameters. Constants are not mirrored; callers
 * use `GLES30.GL_*`.
 *
 * State the engine caches (current program, bound buffers, vertex array, textures, samplers and
 * framebuffer, blend, depth and cull state, enable flags, viewport, scissor ...) must be changed through
 * [GlesBackend.state] ([GlesStateCache]); calling these functions directly for it leaves the cache stale.
 */
interface GL30 {
    // OpenGL ES 2.0
    fun glActiveTexture(texture: Int)
    fun glAttachShader(program: Int, shader: Int)
    fun glBindAttribLocation(program: Int, index: Int, name: String)
    fun glBindBuffer(target: Int, buffer: Int)
    fun glBindFramebuffer(target: Int, framebuffer: Int)
    fun glBindRenderbuffer(target: Int, renderbuffer: Int)
    fun glBindTexture(target: Int, texture: Int)
    fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glBlendEquation(mode: Int)
    fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int)
    fun glBlendFunc(sfactor: Int, dfactor: Int)
    fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun glBufferData(target: Int, size: Int, data: Buffer?, usage: Int)
    fun glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer?)
    fun glCheckFramebufferStatus(target: Int): Int
    fun glClear(mask: Int)
    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glClearDepthf(depth: Float)
    fun glClearStencil(s: Int)
    fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    fun glCompileShader(shader: Int)
    fun glCompressedTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    )
    fun glCompressedTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        imageSize: Int,
        data: Buffer?
    )
    fun glCopyTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    )
    fun glCopyTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
    fun glCreateProgram(): Int
    fun glCreateShader(type: Int): Int
    fun glCullFace(mode: Int)
    fun glDeleteBuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteBuffers(n: Int, buffers: IntBuffer)
    fun glDeleteFramebuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteFramebuffers(n: Int, framebuffers: IntBuffer)
    fun glDeleteProgram(program: Int)
    fun glDeleteRenderbuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer)
    fun glDeleteShader(shader: Int)
    fun glDeleteTextures(n: Int, ids: IntArray, offset: Int)
    fun glDeleteTextures(n: Int, textures: IntBuffer)
    fun glDepthFunc(func: Int)
    fun glDepthMask(flag: Boolean)
    fun glDepthRangef(zNear: Float, zFar: Float)
    fun glDetachShader(program: Int, shader: Int)
    fun glDisable(cap: Int)
    fun glDisableVertexAttribArray(index: Int)
    fun glDrawArrays(mode: Int, first: Int, count: Int)
    fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int)
    fun glDrawElements(mode: Int, count: Int, type: Int, indices: Buffer)
    fun glEnable(cap: Int)
    fun glEnableVertexAttribArray(index: Int)
    fun glFinish()
    fun glFlush()
    fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int)
    fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int)
    fun glFrontFace(mode: Int)
    fun glGenBuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenBuffers(n: Int, buffers: IntBuffer)
    fun glGenerateMipmap(target: Int)
    fun glGenFramebuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenFramebuffers(n: Int, framebuffers: IntBuffer)
    fun glGenRenderbuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenRenderbuffers(n: Int, renderbuffers: IntBuffer)
    fun glGenTextures(n: Int, ids: IntArray, offset: Int)
    fun glGenTextures(n: Int, textures: IntBuffer)
    fun glGetActiveAttrib(
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
    )
    fun glGetActiveAttrib(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String
    fun glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String
    fun glGetActiveUniform(
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
    )
    fun glGetActiveUniform(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String
    fun glGetActiveUniform(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String
    fun glGetAttachedShaders(
        program: Int,
        maxcount: Int,
        count: IntArray,
        countOffset: Int,
        shaders: IntArray,
        shadersOffset: Int
    )
    fun glGetAttachedShaders(program: Int, maxcount: Int, count: IntBuffer, shaders: IntBuffer)
    fun glGetAttribLocation(program: Int, name: String): Int
    fun glGetBooleanv(pname: Int, params: BooleanArray, offset: Int)
    fun glGetBooleanv(pname: Int, params: IntBuffer)
    fun glGetBufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer)
    fun glGetError(): Int
    fun glGetFloatv(pname: Int, params: FloatArray, offset: Int)
    fun glGetFloatv(pname: Int, params: FloatBuffer)
    fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer)
    fun glGetIntegerv(pname: Int, params: IntArray, offset: Int)
    fun glGetIntegerv(pname: Int, params: IntBuffer)
    fun glGetProgramiv(program: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetProgramiv(program: Int, pname: Int, params: IntBuffer)
    fun glGetProgramInfoLog(program: Int): String
    fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer)
    fun glGetShaderiv(shader: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetShaderiv(shader: Int, pname: Int, params: IntBuffer)
    fun glGetShaderInfoLog(shader: Int): String
    fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntArray,
        rangeOffset: Int,
        precision: IntArray,
        precisionOffset: Int
    )
    fun glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: IntBuffer, precision: IntBuffer)
    fun glGetShaderSource(
        shader: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        source: ByteArray,
        sourceOffset: Int
    )
    fun glGetShaderSource(shader: Int): String
    fun glGetString(name: Int): String?
    fun glGetTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int)
    fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer)
    fun glGetTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer)
    fun glGetUniformfv(program: Int, location: Int, params: FloatArray, offset: Int)
    fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer)
    fun glGetUniformiv(program: Int, location: Int, params: IntArray, offset: Int)
    fun glGetUniformiv(program: Int, location: Int, params: IntBuffer)
    fun glGetUniformLocation(program: Int, name: String): Int
    fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatArray, offset: Int)
    fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer)
    fun glGetVertexAttribiv(index: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer)
    fun glHint(target: Int, mode: Int)
    fun glIsBuffer(buffer: Int): Boolean
    fun glIsEnabled(cap: Int): Boolean
    fun glIsFramebuffer(framebuffer: Int): Boolean
    fun glIsProgram(program: Int): Boolean
    fun glIsRenderbuffer(renderbuffer: Int): Boolean
    fun glIsShader(shader: Int): Boolean
    fun glIsTexture(texture: Int): Boolean
    fun glLineWidth(width: Float)
    fun glLinkProgram(program: Int)
    fun glPixelStorei(pname: Int, param: Int)
    fun glPolygonOffset(factor: Float, units: Float)
    fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer)
    fun glReleaseShaderCompiler()
    fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
    fun glSampleCoverage(value: Float, invert: Boolean)
    fun glScissor(x: Int, y: Int, width: Int, height: Int)
    fun glShaderBinary(n: Int, shaders: IntArray, offset: Int, binaryformat: Int, binary: Buffer, length: Int)
    fun glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int)
    fun glShaderSource(shader: Int, string: String)
    fun glStencilFunc(func: Int, ref: Int, mask: Int)
    fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int)
    fun glStencilMask(mask: Int)
    fun glStencilMaskSeparate(face: Int, mask: Int)
    fun glStencilOp(fail: Int, zfail: Int, zpass: Int)
    fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int)
    fun glTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    )
    fun glTexParameterf(target: Int, pname: Int, param: Float)
    fun glTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int)
    fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    fun glTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glTexParameteriv(target: Int, pname: Int, params: IntBuffer)
    fun glTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    )
    fun glUniform1f(location: Int, x: Float)
    fun glUniform1fv(location: Int, count: Int, v: FloatArray, offset: Int)
    fun glUniform1fv(location: Int, count: Int, v: FloatBuffer)
    fun glUniform1i(location: Int, x: Int)
    fun glUniform1iv(location: Int, count: Int, v: IntArray, offset: Int)
    fun glUniform1iv(location: Int, count: Int, v: IntBuffer)
    fun glUniform2f(location: Int, x: Float, y: Float)
    fun glUniform2fv(location: Int, count: Int, v: FloatArray, offset: Int)
    fun glUniform2fv(location: Int, count: Int, v: FloatBuffer)
    fun glUniform2i(location: Int, x: Int, y: Int)
    fun glUniform2iv(location: Int, count: Int, v: IntArray, offset: Int)
    fun glUniform2iv(location: Int, count: Int, v: IntBuffer)
    fun glUniform3f(location: Int, x: Float, y: Float, z: Float)
    fun glUniform3fv(location: Int, count: Int, v: FloatArray, offset: Int)
    fun glUniform3fv(location: Int, count: Int, v: FloatBuffer)
    fun glUniform3i(location: Int, x: Int, y: Int, z: Int)
    fun glUniform3iv(location: Int, count: Int, v: IntArray, offset: Int)
    fun glUniform3iv(location: Int, count: Int, v: IntBuffer)
    fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float)
    fun glUniform4fv(location: Int, count: Int, v: FloatArray, offset: Int)
    fun glUniform4fv(location: Int, count: Int, v: FloatBuffer)
    fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int)
    fun glUniform4iv(location: Int, count: Int, v: IntArray, offset: Int)
    fun glUniform4iv(location: Int, count: Int, v: IntBuffer)
    fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUseProgram(program: Int)
    fun glValidateProgram(program: Int)
    fun glVertexAttrib1f(indx: Int, x: Float)
    fun glVertexAttrib1fv(indx: Int, values: FloatArray, offset: Int)
    fun glVertexAttrib1fv(indx: Int, values: FloatBuffer)
    fun glVertexAttrib2f(indx: Int, x: Float, y: Float)
    fun glVertexAttrib2fv(indx: Int, values: FloatArray, offset: Int)
    fun glVertexAttrib2fv(indx: Int, values: FloatBuffer)
    fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float)
    fun glVertexAttrib3fv(indx: Int, values: FloatArray, offset: Int)
    fun glVertexAttrib3fv(indx: Int, values: FloatBuffer)
    fun glVertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float)
    fun glVertexAttrib4fv(indx: Int, values: FloatArray, offset: Int)
    fun glVertexAttrib4fv(indx: Int, values: FloatBuffer)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: Buffer)
    fun glViewport(x: Int, y: Int, width: Int, height: Int)

    // OpenGL ES 3.0
    fun glReadBuffer(mode: Int)
    fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, indices: Buffer)
    fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, offset: Int)
    fun glTexImage3D(
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
    )
    fun glTexImage3D(
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
    )
    fun glTexSubImage3D(
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
    )
    fun glTexSubImage3D(
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
    )
    fun glCopyTexSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
    fun glCompressedTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    )
    fun glCompressedTexImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        imageSize: Int,
        offset: Int
    )
    fun glCompressedTexSubImage3D(
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
    )
    fun glCompressedTexSubImage3D(
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
    )
    fun glGenQueries(n: Int, ids: IntArray, offset: Int)
    fun glGenQueries(n: Int, ids: IntBuffer)
    fun glDeleteQueries(n: Int, ids: IntArray, offset: Int)
    fun glDeleteQueries(n: Int, ids: IntBuffer)
    fun glIsQuery(id: Int): Boolean
    fun glBeginQuery(target: Int, id: Int)
    fun glEndQuery(target: Int)
    fun glGetQueryiv(target: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetQueryiv(target: Int, pname: Int, params: IntBuffer)
    fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer)
    fun glUnmapBuffer(target: Int): Boolean
    fun glGetBufferPointerv(target: Int, pname: Int): Buffer?
    fun glDrawBuffers(n: Int, bufs: IntArray, offset: Int)
    fun glDrawBuffers(n: Int, bufs: IntBuffer)
    fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer)
    fun glBlitFramebuffer(
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
    )
    fun glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int)
    fun glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int)
    fun glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer?
    fun glFlushMappedBufferRange(target: Int, offset: Int, length: Int)
    fun glBindVertexArray(array: Int)
    fun glDeleteVertexArrays(n: Int, ids: IntArray, offset: Int)
    fun glDeleteVertexArrays(n: Int, arrays: IntBuffer)
    fun glGenVertexArrays(n: Int, ids: IntArray, offset: Int)
    fun glGenVertexArrays(n: Int, arrays: IntBuffer)
    fun glIsVertexArray(array: Int): Boolean
    fun glGetIntegeri_v(target: Int, index: Int, data: IntArray, offset: Int)
    fun glGetIntegeri_v(target: Int, index: Int, data: IntBuffer)
    fun glBeginTransformFeedback(primitiveMode: Int)
    fun glEndTransformFeedback()
    fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int)
    fun glBindBufferBase(target: Int, index: Int, buffer: Int)
    fun glTransformFeedbackVaryings(program: Int, varyings: Array<String>, bufferMode: Int)
    fun glGetTransformFeedbackVarying(
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
    )
    fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: Byte
    )
    fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: ByteBuffer
    )
    fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String
    fun glGetTransformFeedbackVarying(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String
    fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Buffer)
    fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)
    fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer)
    fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer)
    fun glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int)
    fun glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int)
    fun glVertexAttribI4iv(index: Int, v: IntArray, offset: Int)
    fun glVertexAttribI4iv(index: Int, v: IntBuffer)
    fun glVertexAttribI4uiv(index: Int, v: IntArray, offset: Int)
    fun glVertexAttribI4uiv(index: Int, v: IntBuffer)
    fun glGetUniformuiv(program: Int, location: Int, params: IntArray, offset: Int)
    fun glGetUniformuiv(program: Int, location: Int, params: IntBuffer)
    fun glGetFragDataLocation(program: Int, name: String): Int
    fun glUniform1ui(location: Int, v0: Int)
    fun glUniform2ui(location: Int, v0: Int, v1: Int)
    fun glUniform3ui(location: Int, v0: Int, v1: Int, v2: Int)
    fun glUniform4ui(location: Int, v0: Int, v1: Int, v2: Int, v3: Int)
    fun glUniform1uiv(location: Int, count: Int, value: IntArray, offset: Int)
    fun glUniform1uiv(location: Int, count: Int, value: IntBuffer)
    fun glUniform2uiv(location: Int, count: Int, value: IntArray, offset: Int)
    fun glUniform2uiv(location: Int, count: Int, value: IntBuffer)
    fun glUniform3uiv(location: Int, count: Int, value: IntArray, offset: Int)
    fun glUniform3uiv(location: Int, count: Int, value: IntBuffer)
    fun glUniform4uiv(location: Int, count: Int, value: IntArray, offset: Int)
    fun glUniform4uiv(location: Int, count: Int, value: IntBuffer)
    fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int)
    fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer)
    fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int)
    fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer)
    fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatArray, offset: Int)
    fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer)
    fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int)
    fun glGetStringi(name: Int, index: Int): String?
    fun glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int)
    fun glGetUniformIndices(
        program: Int,
        uniformNames: Array<String>,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int
    )
    fun glGetUniformIndices(program: Int, uniformNames: Array<String>, uniformIndices: IntBuffer)
    fun glGetActiveUniformsiv(
        program: Int,
        uniformCount: Int,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int,
        pname: Int,
        params: IntArray,
        paramsOffset: Int
    )
    fun glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer)
    fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int
    fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer)
    fun glGetActiveUniformBlockName(
        program: Int,
        uniformBlockIndex: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        uniformBlockName: ByteArray,
        uniformBlockNameOffset: Int
    )
    fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer)
    fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String
    fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int)
    fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int)
    fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indices: Buffer, instanceCount: Int)
    fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indicesOffset: Int, instanceCount: Int)
    fun glFenceSync(condition: Int, flags: Int): Long
    fun glIsSync(sync: Long): Boolean
    fun glDeleteSync(sync: Long)
    fun glClientWaitSync(sync: Long, flags: Int, timeout: Long): Int
    fun glWaitSync(sync: Long, flags: Int, timeout: Long)
    fun glGetInteger64v(pname: Int, params: LongArray, offset: Int)
    fun glGetInteger64v(pname: Int, params: LongBuffer)
    fun glGetSynciv(
        sync: Long,
        pname: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        values: IntArray,
        valuesOffset: Int
    )
    fun glGetSynciv(sync: Long, pname: Int, bufSize: Int, length: IntBuffer, values: IntBuffer)
    fun glGetInteger64i_v(target: Int, index: Int, data: LongArray, offset: Int)
    fun glGetInteger64i_v(target: Int, index: Int, data: LongBuffer)
    fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongArray, offset: Int)
    fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer)
    fun glGenSamplers(n: Int, ids: IntArray, offset: Int)
    fun glGenSamplers(count: Int, samplers: IntBuffer)
    fun glDeleteSamplers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteSamplers(count: Int, samplers: IntBuffer)
    fun glIsSampler(sampler: Int): Boolean
    fun glBindSampler(unit: Int, sampler: Int)
    fun glSamplerParameteri(sampler: Int, pname: Int, param: Int)
    fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntArray, offset: Int)
    fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer)
    fun glSamplerParameterf(sampler: Int, pname: Int, param: Float)
    fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatArray, offset: Int)
    fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer)
    fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer)
    fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatArray, offset: Int)
    fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer)
    fun glVertexAttribDivisor(index: Int, divisor: Int)
    fun glBindTransformFeedback(target: Int, id: Int)
    fun glDeleteTransformFeedbacks(n: Int, ids: IntArray, offset: Int)
    fun glDeleteTransformFeedbacks(n: Int, ids: IntBuffer)
    fun glGenTransformFeedbacks(n: Int, ids: IntArray, offset: Int)
    fun glGenTransformFeedbacks(n: Int, ids: IntBuffer)
    fun glIsTransformFeedback(id: Int): Boolean
    fun glPauseTransformFeedback()
    fun glResumeTransformFeedback()
    fun glGetProgramBinary(
        program: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        binaryFormat: IntArray,
        binaryFormatOffset: Int,
        binary: Buffer
    )
    fun glGetProgramBinary(program: Int, bufSize: Int, length: IntBuffer, binaryFormat: IntBuffer, binary: Buffer)
    fun glProgramBinary(program: Int, binaryFormat: Int, binary: Buffer, length: Int)
    fun glProgramParameteri(program: Int, pname: Int, value: Int)
    fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntArray, offset: Int)
    fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer)
    fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntArray,
        offset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
    fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntBuffer,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
    fun glTexStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun glTexStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int)
    fun glGetInternalformativ(target: Int, internalformat: Int, pname: Int, bufSize: Int, params: IntArray, offset: Int)
    fun glGetInternalformativ(target: Int, internalformat: Int, pname: Int, bufSize: Int, params: IntBuffer)
    fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, offset: Int)
}
