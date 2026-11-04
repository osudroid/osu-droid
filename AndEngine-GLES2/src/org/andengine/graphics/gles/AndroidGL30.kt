package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/** [GL30] backed by the static [android.opengl.GLES20] and [android.opengl.GLES30] methods. */
open class AndroidGL30 : GL30 {
    // OpenGL ES 2.0
    override fun glActiveTexture(texture: Int) = GLES20.glActiveTexture(texture)
    override fun glAttachShader(program: Int, shader: Int) = GLES20.glAttachShader(program, shader)
    override fun glBindAttribLocation(program: Int, index: Int, name: String) =
        GLES20.glBindAttribLocation(program, index, name)
    override fun glBindBuffer(target: Int, buffer: Int) = GLES20.glBindBuffer(target, buffer)
    override fun glBindFramebuffer(target: Int, framebuffer: Int) = GLES20.glBindFramebuffer(target, framebuffer)
    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) = GLES20.glBindRenderbuffer(target, renderbuffer)
    override fun glBindTexture(target: Int, texture: Int) = GLES20.glBindTexture(target, texture)
    override fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) =
        GLES20.glBlendColor(red, green, blue, alpha)
    override fun glBlendEquation(mode: Int) = GLES20.glBlendEquation(mode)
    override fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) =
        GLES20.glBlendEquationSeparate(modeRGB, modeAlpha)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = GLES20.glBlendFunc(sfactor, dfactor)
    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) =
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    override fun glBufferData(target: Int, size: Int, data: Buffer?, usage: Int) =
        GLES20.glBufferData(target, size, data, usage)
    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer?) =
        GLES20.glBufferSubData(target, offset, size, data)
    override fun glCheckFramebufferStatus(target: Int): Int = GLES20.glCheckFramebufferStatus(target)
    override fun glClear(mask: Int) = GLES20.glClear(mask)
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) =
        GLES20.glClearColor(red, green, blue, alpha)
    override fun glClearDepthf(depth: Float) = GLES20.glClearDepthf(depth)
    override fun glClearStencil(s: Int) = GLES20.glClearStencil(s)
    override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) =
        GLES20.glColorMask(red, green, blue, alpha)
    override fun glCompileShader(shader: Int) = GLES20.glCompileShader(shader)
    override fun glCompressedTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    ) = GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
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
    ) = GLES20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
    override fun glCopyTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) = GLES20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    override fun glCopyTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) = GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    override fun glCreateProgram(): Int = GLES20.glCreateProgram()
    override fun glCreateShader(type: Int): Int = GLES20.glCreateShader(type)
    override fun glCullFace(mode: Int) = GLES20.glCullFace(mode)
    override fun glDeleteBuffers(n: Int, ids: IntArray, offset: Int) = GLES20.glDeleteBuffers(n, ids, offset)
    override fun glDeleteBuffers(n: Int, buffers: IntBuffer) = GLES20.glDeleteBuffers(n, buffers)
    override fun glDeleteFramebuffers(n: Int, ids: IntArray, offset: Int) = GLES20.glDeleteFramebuffers(n, ids, offset)
    override fun glDeleteFramebuffers(n: Int, framebuffers: IntBuffer) = GLES20.glDeleteFramebuffers(n, framebuffers)
    override fun glDeleteProgram(program: Int) = GLES20.glDeleteProgram(program)
    override fun glDeleteRenderbuffers(n: Int, ids: IntArray, offset: Int) =
        GLES20.glDeleteRenderbuffers(n, ids, offset)
    override fun glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer) =
        GLES20.glDeleteRenderbuffers(n, renderbuffers)
    override fun glDeleteShader(shader: Int) = GLES20.glDeleteShader(shader)
    override fun glDeleteTextures(n: Int, ids: IntArray, offset: Int) = GLES20.glDeleteTextures(n, ids, offset)
    override fun glDeleteTextures(n: Int, textures: IntBuffer) = GLES20.glDeleteTextures(n, textures)
    override fun glDepthFunc(func: Int) = GLES20.glDepthFunc(func)
    override fun glDepthMask(flag: Boolean) = GLES20.glDepthMask(flag)
    override fun glDepthRangef(zNear: Float, zFar: Float) = GLES20.glDepthRangef(zNear, zFar)
    override fun glDetachShader(program: Int, shader: Int) = GLES20.glDetachShader(program, shader)
    override fun glDisable(cap: Int) = GLES20.glDisable(cap)
    override fun glDisableVertexAttribArray(index: Int) = GLES20.glDisableVertexAttribArray(index)
    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GLES20.glDrawArrays(mode, first, count)
    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) =
        GLES20.glDrawElements(mode, count, type, offset)
    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Buffer) =
        GLES20.glDrawElements(mode, count, type, indices)
    override fun glEnable(cap: Int) = GLES20.glEnable(cap)
    override fun glEnableVertexAttribArray(index: Int) = GLES20.glEnableVertexAttribArray(index)
    override fun glFinish() = GLES20.glFinish()
    override fun glFlush() = GLES20.glFlush()
    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) =
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) =
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    override fun glFrontFace(mode: Int) = GLES20.glFrontFace(mode)
    override fun glGenBuffers(n: Int, ids: IntArray, offset: Int) = GLES20.glGenBuffers(n, ids, offset)
    override fun glGenBuffers(n: Int, buffers: IntBuffer) = GLES20.glGenBuffers(n, buffers)
    override fun glGenerateMipmap(target: Int) = GLES20.glGenerateMipmap(target)
    override fun glGenFramebuffers(n: Int, ids: IntArray, offset: Int) = GLES20.glGenFramebuffers(n, ids, offset)
    override fun glGenFramebuffers(n: Int, framebuffers: IntBuffer) = GLES20.glGenFramebuffers(n, framebuffers)
    override fun glGenRenderbuffers(n: Int, ids: IntArray, offset: Int) = GLES20.glGenRenderbuffers(n, ids, offset)
    override fun glGenRenderbuffers(n: Int, renderbuffers: IntBuffer) = GLES20.glGenRenderbuffers(n, renderbuffers)
    override fun glGenTextures(n: Int, ids: IntArray, offset: Int) = GLES20.glGenTextures(n, ids, offset)
    override fun glGenTextures(n: Int, textures: IntBuffer) = GLES20.glGenTextures(n, textures)
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
    ) =
        GLES20.glGetActiveAttrib(
            program,
            index,
            bufsize,
            length,
            lengthOffset,
            size,
            sizeOffset,
            type,
            typeOffset,
            name,
            nameOffset
        )
    override fun glGetActiveAttrib(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = GLES20.glGetActiveAttrib(program, index, size, sizeOffset, type, typeOffset)
    override fun glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String =
        GLES20.glGetActiveAttrib(program, index, size, type)
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
    ) =
        GLES20.glGetActiveUniform(
            program,
            index,
            bufsize,
            length,
            lengthOffset,
            size,
            sizeOffset,
            type,
            typeOffset,
            name,
            nameOffset
        )
    override fun glGetActiveUniform(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = GLES20.glGetActiveUniform(program, index, size, sizeOffset, type, typeOffset)
    override fun glGetActiveUniform(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String =
        GLES20.glGetActiveUniform(program, index, size, type)
    override fun glGetAttachedShaders(
        program: Int,
        maxcount: Int,
        count: IntArray,
        countOffset: Int,
        shaders: IntArray,
        shadersOffset: Int
    ) = GLES20.glGetAttachedShaders(program, maxcount, count, countOffset, shaders, shadersOffset)
    override fun glGetAttachedShaders(program: Int, maxcount: Int, count: IntBuffer, shaders: IntBuffer) =
        GLES20.glGetAttachedShaders(program, maxcount, count, shaders)
    override fun glGetAttribLocation(program: Int, name: String): Int = GLES20.glGetAttribLocation(program, name)
    override fun glGetBooleanv(pname: Int, params: BooleanArray, offset: Int) =
        GLES20.glGetBooleanv(pname, params, offset)
    override fun glGetBooleanv(pname: Int, params: IntBuffer) = GLES20.glGetBooleanv(pname, params)
    override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetBufferParameteriv(target, pname, params, offset)
    override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetBufferParameteriv(target, pname, params)
    override fun glGetError(): Int = GLES20.glGetError()
    override fun glGetFloatv(pname: Int, params: FloatArray, offset: Int) = GLES20.glGetFloatv(pname, params, offset)
    override fun glGetFloatv(pname: Int, params: FloatBuffer) = GLES20.glGetFloatv(pname, params)
    override fun glGetFramebufferAttachmentParameteriv(
        target: Int,
        attachment: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) = GLES20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params, offset)
    override fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
    override fun glGetIntegerv(pname: Int, params: IntArray, offset: Int) = GLES20.glGetIntegerv(pname, params, offset)
    override fun glGetIntegerv(pname: Int, params: IntBuffer) = GLES20.glGetIntegerv(pname, params)
    override fun glGetProgramiv(program: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetProgramiv(program, pname, params, offset)
    override fun glGetProgramiv(program: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetProgramiv(program, pname, params)
    override fun glGetProgramInfoLog(program: Int): String = GLES20.glGetProgramInfoLog(program)
    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetRenderbufferParameteriv(target, pname, params, offset)
    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetRenderbufferParameteriv(target, pname, params)
    override fun glGetShaderiv(shader: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetShaderiv(shader, pname, params, offset)
    override fun glGetShaderiv(shader: Int, pname: Int, params: IntBuffer) = GLES20.glGetShaderiv(shader, pname, params)
    override fun glGetShaderInfoLog(shader: Int): String = GLES20.glGetShaderInfoLog(shader)
    override fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntArray,
        rangeOffset: Int,
        precision: IntArray,
        precisionOffset: Int
    ) = GLES20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, rangeOffset, precision, precisionOffset)
    override fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntBuffer,
        precision: IntBuffer
    ) = GLES20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)
    override fun glGetShaderSource(
        shader: Int,
        bufsize: Int,
        length: IntArray,
        lengthOffset: Int,
        source: ByteArray,
        sourceOffset: Int
    ) = GLES20.glGetShaderSource(shader, bufsize, length, lengthOffset, source, sourceOffset)
    override fun glGetShaderSource(shader: Int): String = GLES20.glGetShaderSource(shader)
    override fun glGetString(name: Int): String? = GLES20.glGetString(name)
    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int) =
        GLES20.glGetTexParameterfv(target, pname, params, offset)
    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer) =
        GLES20.glGetTexParameterfv(target, pname, params)
    override fun glGetTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetTexParameteriv(target, pname, params, offset)
    override fun glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetTexParameteriv(target, pname, params)
    override fun glGetUniformfv(program: Int, location: Int, params: FloatArray, offset: Int) =
        GLES20.glGetUniformfv(program, location, params, offset)
    override fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer) =
        GLES20.glGetUniformfv(program, location, params)
    override fun glGetUniformiv(program: Int, location: Int, params: IntArray, offset: Int) =
        GLES20.glGetUniformiv(program, location, params, offset)
    override fun glGetUniformiv(program: Int, location: Int, params: IntBuffer) =
        GLES20.glGetUniformiv(program, location, params)
    override fun glGetUniformLocation(program: Int, name: String): Int = GLES20.glGetUniformLocation(program, name)
    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatArray, offset: Int) =
        GLES20.glGetVertexAttribfv(index, pname, params, offset)
    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer) =
        GLES20.glGetVertexAttribfv(index, pname, params)
    override fun glGetVertexAttribiv(index: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glGetVertexAttribiv(index, pname, params, offset)
    override fun glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer) =
        GLES20.glGetVertexAttribiv(index, pname, params)
    override fun glHint(target: Int, mode: Int) = GLES20.glHint(target, mode)
    override fun glIsBuffer(buffer: Int): Boolean = GLES20.glIsBuffer(buffer)
    override fun glIsEnabled(cap: Int): Boolean = GLES20.glIsEnabled(cap)
    override fun glIsFramebuffer(framebuffer: Int): Boolean = GLES20.glIsFramebuffer(framebuffer)
    override fun glIsProgram(program: Int): Boolean = GLES20.glIsProgram(program)
    override fun glIsRenderbuffer(renderbuffer: Int): Boolean = GLES20.glIsRenderbuffer(renderbuffer)
    override fun glIsShader(shader: Int): Boolean = GLES20.glIsShader(shader)
    override fun glIsTexture(texture: Int): Boolean = GLES20.glIsTexture(texture)
    override fun glLineWidth(width: Float) = GLES20.glLineWidth(width)
    override fun glLinkProgram(program: Int) = GLES20.glLinkProgram(program)
    override fun glPixelStorei(pname: Int, param: Int) = GLES20.glPixelStorei(pname, param)
    override fun glPolygonOffset(factor: Float, units: Float) = GLES20.glPolygonOffset(factor, units)
    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer) =
        GLES20.glReadPixels(x, y, width, height, format, type, pixels)
    override fun glReleaseShaderCompiler() = GLES20.glReleaseShaderCompiler()
    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) =
        GLES20.glRenderbufferStorage(target, internalformat, width, height)
    override fun glSampleCoverage(value: Float, invert: Boolean) = GLES20.glSampleCoverage(value, invert)
    override fun glScissor(x: Int, y: Int, width: Int, height: Int) = GLES20.glScissor(x, y, width, height)
    override fun glShaderBinary(
        n: Int,
        shaders: IntArray,
        offset: Int,
        binaryformat: Int,
        binary: Buffer,
        length: Int
    ) = GLES20.glShaderBinary(n, shaders, offset, binaryformat, binary, length)
    override fun glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int) =
        GLES20.glShaderBinary(n, shaders, binaryformat, binary, length)
    override fun glShaderSource(shader: Int, string: String) = GLES20.glShaderSource(shader, string)
    override fun glStencilFunc(func: Int, ref: Int, mask: Int) = GLES20.glStencilFunc(func, ref, mask)
    override fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) =
        GLES20.glStencilFuncSeparate(face, func, ref, mask)
    override fun glStencilMask(mask: Int) = GLES20.glStencilMask(mask)
    override fun glStencilMaskSeparate(face: Int, mask: Int) = GLES20.glStencilMaskSeparate(face, mask)
    override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) = GLES20.glStencilOp(fail, zfail, zpass)
    override fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) =
        GLES20.glStencilOpSeparate(face, fail, zfail, zpass)
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
    ) = GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
    override fun glTexParameterf(target: Int, pname: Int, param: Float) = GLES20.glTexParameterf(target, pname, param)
    override fun glTexParameterfv(target: Int, pname: Int, params: FloatArray, offset: Int) =
        GLES20.glTexParameterfv(target, pname, params, offset)
    override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer) =
        GLES20.glTexParameterfv(target, pname, params)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GLES20.glTexParameteri(target, pname, param)
    override fun glTexParameteriv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES20.glTexParameteriv(target, pname, params, offset)
    override fun glTexParameteriv(target: Int, pname: Int, params: IntBuffer) =
        GLES20.glTexParameteriv(target, pname, params)
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
    ) = GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
    override fun glUniform1f(location: Int, x: Float) = GLES20.glUniform1f(location, x)
    override fun glUniform1fv(location: Int, count: Int, v: FloatArray, offset: Int) =
        GLES20.glUniform1fv(location, count, v, offset)
    override fun glUniform1fv(location: Int, count: Int, v: FloatBuffer) = GLES20.glUniform1fv(location, count, v)
    override fun glUniform1i(location: Int, x: Int) = GLES20.glUniform1i(location, x)
    override fun glUniform1iv(location: Int, count: Int, v: IntArray, offset: Int) =
        GLES20.glUniform1iv(location, count, v, offset)
    override fun glUniform1iv(location: Int, count: Int, v: IntBuffer) = GLES20.glUniform1iv(location, count, v)
    override fun glUniform2f(location: Int, x: Float, y: Float) = GLES20.glUniform2f(location, x, y)
    override fun glUniform2fv(location: Int, count: Int, v: FloatArray, offset: Int) =
        GLES20.glUniform2fv(location, count, v, offset)
    override fun glUniform2fv(location: Int, count: Int, v: FloatBuffer) = GLES20.glUniform2fv(location, count, v)
    override fun glUniform2i(location: Int, x: Int, y: Int) = GLES20.glUniform2i(location, x, y)
    override fun glUniform2iv(location: Int, count: Int, v: IntArray, offset: Int) =
        GLES20.glUniform2iv(location, count, v, offset)
    override fun glUniform2iv(location: Int, count: Int, v: IntBuffer) = GLES20.glUniform2iv(location, count, v)
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = GLES20.glUniform3f(location, x, y, z)
    override fun glUniform3fv(location: Int, count: Int, v: FloatArray, offset: Int) =
        GLES20.glUniform3fv(location, count, v, offset)
    override fun glUniform3fv(location: Int, count: Int, v: FloatBuffer) = GLES20.glUniform3fv(location, count, v)
    override fun glUniform3i(location: Int, x: Int, y: Int, z: Int) = GLES20.glUniform3i(location, x, y, z)
    override fun glUniform3iv(location: Int, count: Int, v: IntArray, offset: Int) =
        GLES20.glUniform3iv(location, count, v, offset)
    override fun glUniform3iv(location: Int, count: Int, v: IntBuffer) = GLES20.glUniform3iv(location, count, v)
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) =
        GLES20.glUniform4f(location, x, y, z, w)
    override fun glUniform4fv(location: Int, count: Int, v: FloatArray, offset: Int) =
        GLES20.glUniform4fv(location, count, v, offset)
    override fun glUniform4fv(location: Int, count: Int, v: FloatBuffer) = GLES20.glUniform4fv(location, count, v)
    override fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int) = GLES20.glUniform4i(location, x, y, z, w)
    override fun glUniform4iv(location: Int, count: Int, v: IntArray, offset: Int) =
        GLES20.glUniform4iv(location, count, v, offset)
    override fun glUniform4iv(location: Int, count: Int, v: IntBuffer) = GLES20.glUniform4iv(location, count, v)
    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES20.glUniformMatrix2fv(location, count, transpose, value, offset)
    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES20.glUniformMatrix2fv(location, count, transpose, value)
    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES20.glUniformMatrix3fv(location, count, transpose, value, offset)
    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES20.glUniformMatrix3fv(location, count, transpose, value)
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES20.glUniformMatrix4fv(location, count, transpose, value, offset)
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES20.glUniformMatrix4fv(location, count, transpose, value)
    override fun glUseProgram(program: Int) = GLES20.glUseProgram(program)
    override fun glValidateProgram(program: Int) = GLES20.glValidateProgram(program)
    override fun glVertexAttrib1f(indx: Int, x: Float) = GLES20.glVertexAttrib1f(indx, x)
    override fun glVertexAttrib1fv(indx: Int, values: FloatArray, offset: Int) =
        GLES20.glVertexAttrib1fv(indx, values, offset)
    override fun glVertexAttrib1fv(indx: Int, values: FloatBuffer) = GLES20.glVertexAttrib1fv(indx, values)
    override fun glVertexAttrib2f(indx: Int, x: Float, y: Float) = GLES20.glVertexAttrib2f(indx, x, y)
    override fun glVertexAttrib2fv(indx: Int, values: FloatArray, offset: Int) =
        GLES20.glVertexAttrib2fv(indx, values, offset)
    override fun glVertexAttrib2fv(indx: Int, values: FloatBuffer) = GLES20.glVertexAttrib2fv(indx, values)
    override fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float) = GLES20.glVertexAttrib3f(indx, x, y, z)
    override fun glVertexAttrib3fv(indx: Int, values: FloatArray, offset: Int) =
        GLES20.glVertexAttrib3fv(indx, values, offset)
    override fun glVertexAttrib3fv(indx: Int, values: FloatBuffer) = GLES20.glVertexAttrib3fv(indx, values)
    override fun glVertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) =
        GLES20.glVertexAttrib4f(index, x, y, z, w)
    override fun glVertexAttrib4fv(indx: Int, values: FloatArray, offset: Int) =
        GLES20.glVertexAttrib4fv(indx, values, offset)
    override fun glVertexAttrib4fv(indx: Int, values: FloatBuffer) = GLES20.glVertexAttrib4fv(indx, values)
    override fun glVertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Int
    ) = GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset)
    override fun glVertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: Buffer
    ) = GLES20.glVertexAttribPointer(index, size, type, normalized, stride, ptr)
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = GLES20.glViewport(x, y, width, height)

    // OpenGL ES 3.0
    override fun glReadBuffer(mode: Int) = GLES30.glReadBuffer(mode)
    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, indices: Buffer) =
        GLES30.glDrawRangeElements(mode, start, end, count, type, indices)
    override fun glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, type: Int, offset: Int) =
        GLES30.glDrawRangeElements(mode, start, end, count, type, offset)
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
    ) = GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels)
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
    ) = GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset)
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
    ) = GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
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
    ) = GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset)
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
    ) = GLES30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
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
    ) = GLES30.glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, imageSize, data)
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
    ) = GLES30.glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, imageSize, offset)
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
    ) =
        GLES30.glCompressedTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            width,
            height,
            depth,
            format,
            imageSize,
            data
        )
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
    ) =
        GLES30.glCompressedTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            width,
            height,
            depth,
            format,
            imageSize,
            offset
        )
    override fun glGenQueries(n: Int, ids: IntArray, offset: Int) = GLES30.glGenQueries(n, ids, offset)
    override fun glGenQueries(n: Int, ids: IntBuffer) = GLES30.glGenQueries(n, ids)
    override fun glDeleteQueries(n: Int, ids: IntArray, offset: Int) = GLES30.glDeleteQueries(n, ids, offset)
    override fun glDeleteQueries(n: Int, ids: IntBuffer) = GLES30.glDeleteQueries(n, ids)
    override fun glIsQuery(id: Int): Boolean = GLES30.glIsQuery(id)
    override fun glBeginQuery(target: Int, id: Int) = GLES30.glBeginQuery(target, id)
    override fun glEndQuery(target: Int) = GLES30.glEndQuery(target)
    override fun glGetQueryiv(target: Int, pname: Int, params: IntArray, offset: Int) =
        GLES30.glGetQueryiv(target, pname, params, offset)
    override fun glGetQueryiv(target: Int, pname: Int, params: IntBuffer) = GLES30.glGetQueryiv(target, pname, params)
    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntArray, offset: Int) =
        GLES30.glGetQueryObjectuiv(id, pname, params, offset)
    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer) =
        GLES30.glGetQueryObjectuiv(id, pname, params)
    override fun glUnmapBuffer(target: Int): Boolean = GLES30.glUnmapBuffer(target)
    override fun glGetBufferPointerv(target: Int, pname: Int): Buffer? = GLES30.glGetBufferPointerv(target, pname)
    override fun glDrawBuffers(n: Int, bufs: IntArray, offset: Int) = GLES30.glDrawBuffers(n, bufs, offset)
    override fun glDrawBuffers(n: Int, bufs: IntBuffer) = GLES30.glDrawBuffers(n, bufs)
    override fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix2x3fv(location, count, transpose, value, offset)
    override fun glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix2x3fv(location, count, transpose, value)
    override fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix3x2fv(location, count, transpose, value, offset)
    override fun glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix3x2fv(location, count, transpose, value)
    override fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix2x4fv(location, count, transpose, value, offset)
    override fun glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix2x4fv(location, count, transpose, value)
    override fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix4x2fv(location, count, transpose, value, offset)
    override fun glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix4x2fv(location, count, transpose, value)
    override fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix3x4fv(location, count, transpose, value, offset)
    override fun glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix3x4fv(location, count, transpose, value)
    override fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix4x3fv(location, count, transpose, value, offset)
    override fun glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer) =
        GLES30.glUniformMatrix4x3fv(location, count, transpose, value)
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
    ) = GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
    override fun glRenderbufferStorageMultisample(
        target: Int,
        samples: Int,
        internalformat: Int,
        width: Int,
        height: Int
    ) = GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
    override fun glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int) =
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer)
    override fun glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer? =
        GLES30.glMapBufferRange(target, offset, length, access)
    override fun glFlushMappedBufferRange(target: Int, offset: Int, length: Int) =
        GLES30.glFlushMappedBufferRange(target, offset, length)
    override fun glBindVertexArray(array: Int) = GLES30.glBindVertexArray(array)
    override fun glDeleteVertexArrays(n: Int, ids: IntArray, offset: Int) = GLES30.glDeleteVertexArrays(n, ids, offset)
    override fun glDeleteVertexArrays(n: Int, arrays: IntBuffer) = GLES30.glDeleteVertexArrays(n, arrays)
    override fun glGenVertexArrays(n: Int, ids: IntArray, offset: Int) = GLES30.glGenVertexArrays(n, ids, offset)
    override fun glGenVertexArrays(n: Int, arrays: IntBuffer) = GLES30.glGenVertexArrays(n, arrays)
    override fun glIsVertexArray(array: Int): Boolean = GLES30.glIsVertexArray(array)
    override fun glGetIntegeri_v(target: Int, index: Int, data: IntArray, offset: Int) =
        GLES30.glGetIntegeri_v(target, index, data, offset)
    override fun glGetIntegeri_v(target: Int, index: Int, data: IntBuffer) = GLES30.glGetIntegeri_v(target, index, data)
    override fun glBeginTransformFeedback(primitiveMode: Int) = GLES30.glBeginTransformFeedback(primitiveMode)
    override fun glEndTransformFeedback() = GLES30.glEndTransformFeedback()
    override fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int) =
        GLES30.glBindBufferRange(target, index, buffer, offset, size)
    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) = GLES30.glBindBufferBase(target, index, buffer)
    override fun glTransformFeedbackVaryings(program: Int, varyings: Array<String>, bufferMode: Int) =
        GLES30.glTransformFeedbackVaryings(program, varyings, bufferMode)
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
    ) =
        GLES30.glGetTransformFeedbackVarying(
            program,
            index,
            bufsize,
            length,
            lengthOffset,
            size,
            sizeOffset,
            type,
            typeOffset,
            name,
            nameOffset
        )
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: Byte
    ) = GLES30.glGetTransformFeedbackVarying(program, index, bufsize, length, size, type, name)
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        bufsize: Int,
        length: IntBuffer,
        size: IntBuffer,
        type: IntBuffer,
        name: ByteBuffer
    ) = GLES30.glGetTransformFeedbackVarying(program, index, bufsize, length, size, type, name)
    override fun glGetTransformFeedbackVarying(
        program: Int,
        index: Int,
        size: IntArray,
        sizeOffset: Int,
        type: IntArray,
        typeOffset: Int
    ): String = GLES30.glGetTransformFeedbackVarying(program, index, size, sizeOffset, type, typeOffset)
    override fun glGetTransformFeedbackVarying(program: Int, index: Int, size: IntBuffer, type: IntBuffer): String =
        GLES30.glGetTransformFeedbackVarying(program, index, size, type)
    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Buffer) =
        GLES30.glVertexAttribIPointer(index, size, type, stride, pointer)
    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int) =
        GLES30.glVertexAttribIPointer(index, size, type, stride, offset)
    override fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntArray, offset: Int) =
        GLES30.glGetVertexAttribIiv(index, pname, params, offset)
    override fun glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer) =
        GLES30.glGetVertexAttribIiv(index, pname, params)
    override fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntArray, offset: Int) =
        GLES30.glGetVertexAttribIuiv(index, pname, params, offset)
    override fun glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer) =
        GLES30.glGetVertexAttribIuiv(index, pname, params)
    override fun glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int) =
        GLES30.glVertexAttribI4i(index, x, y, z, w)
    override fun glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int) =
        GLES30.glVertexAttribI4ui(index, x, y, z, w)
    override fun glVertexAttribI4iv(index: Int, v: IntArray, offset: Int) = GLES30.glVertexAttribI4iv(index, v, offset)
    override fun glVertexAttribI4iv(index: Int, v: IntBuffer) = GLES30.glVertexAttribI4iv(index, v)
    override fun glVertexAttribI4uiv(index: Int, v: IntArray, offset: Int) =
        GLES30.glVertexAttribI4uiv(index, v, offset)
    override fun glVertexAttribI4uiv(index: Int, v: IntBuffer) = GLES30.glVertexAttribI4uiv(index, v)
    override fun glGetUniformuiv(program: Int, location: Int, params: IntArray, offset: Int) =
        GLES30.glGetUniformuiv(program, location, params, offset)
    override fun glGetUniformuiv(program: Int, location: Int, params: IntBuffer) =
        GLES30.glGetUniformuiv(program, location, params)
    override fun glGetFragDataLocation(program: Int, name: String): Int = GLES30.glGetFragDataLocation(program, name)
    override fun glUniform1ui(location: Int, v0: Int) = GLES30.glUniform1ui(location, v0)
    override fun glUniform2ui(location: Int, v0: Int, v1: Int) = GLES30.glUniform2ui(location, v0, v1)
    override fun glUniform3ui(location: Int, v0: Int, v1: Int, v2: Int) = GLES30.glUniform3ui(location, v0, v1, v2)
    override fun glUniform4ui(location: Int, v0: Int, v1: Int, v2: Int, v3: Int) =
        GLES30.glUniform4ui(location, v0, v1, v2, v3)
    override fun glUniform1uiv(location: Int, count: Int, value: IntArray, offset: Int) =
        GLES30.glUniform1uiv(location, count, value, offset)
    override fun glUniform1uiv(location: Int, count: Int, value: IntBuffer) =
        GLES30.glUniform1uiv(location, count, value)
    override fun glUniform2uiv(location: Int, count: Int, value: IntArray, offset: Int) =
        GLES30.glUniform2uiv(location, count, value, offset)
    override fun glUniform2uiv(location: Int, count: Int, value: IntBuffer) =
        GLES30.glUniform2uiv(location, count, value)
    override fun glUniform3uiv(location: Int, count: Int, value: IntArray, offset: Int) =
        GLES30.glUniform3uiv(location, count, value, offset)
    override fun glUniform3uiv(location: Int, count: Int, value: IntBuffer) =
        GLES30.glUniform3uiv(location, count, value)
    override fun glUniform4uiv(location: Int, count: Int, value: IntArray, offset: Int) =
        GLES30.glUniform4uiv(location, count, value, offset)
    override fun glUniform4uiv(location: Int, count: Int, value: IntBuffer) =
        GLES30.glUniform4uiv(location, count, value)
    override fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int) =
        GLES30.glClearBufferiv(buffer, drawbuffer, value, offset)
    override fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer) =
        GLES30.glClearBufferiv(buffer, drawbuffer, value)
    override fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntArray, offset: Int) =
        GLES30.glClearBufferuiv(buffer, drawbuffer, value, offset)
    override fun glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer) =
        GLES30.glClearBufferuiv(buffer, drawbuffer, value)
    override fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatArray, offset: Int) =
        GLES30.glClearBufferfv(buffer, drawbuffer, value, offset)
    override fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer) =
        GLES30.glClearBufferfv(buffer, drawbuffer, value)
    override fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int) =
        GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil)
    override fun glGetStringi(name: Int, index: Int): String? = GLES30.glGetStringi(name, index)
    override fun glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int) =
        GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)
    override fun glGetUniformIndices(
        program: Int,
        uniformNames: Array<String>,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int
    ) = GLES30.glGetUniformIndices(program, uniformNames, uniformIndices, uniformIndicesOffset)
    override fun glGetUniformIndices(program: Int, uniformNames: Array<String>, uniformIndices: IntBuffer) =
        GLES30.glGetUniformIndices(program, uniformNames, uniformIndices)
    override fun glGetActiveUniformsiv(
        program: Int,
        uniformCount: Int,
        uniformIndices: IntArray,
        uniformIndicesOffset: Int,
        pname: Int,
        params: IntArray,
        paramsOffset: Int
    ) =
        GLES30.glGetActiveUniformsiv(
            program,
            uniformCount,
            uniformIndices,
            uniformIndicesOffset,
            pname,
            params,
            paramsOffset
        )
    override fun glGetActiveUniformsiv(
        program: Int,
        uniformCount: Int,
        uniformIndices: IntBuffer,
        pname: Int,
        params: IntBuffer
    ) = GLES30.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params)
    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int =
        GLES30.glGetUniformBlockIndex(program, uniformBlockName)
    override fun glGetActiveUniformBlockiv(
        program: Int,
        uniformBlockIndex: Int,
        pname: Int,
        params: IntArray,
        offset: Int
    ) = GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params, offset)
    override fun glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer) =
        GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
    override fun glGetActiveUniformBlockName(
        program: Int,
        uniformBlockIndex: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        uniformBlockName: ByteArray,
        uniformBlockNameOffset: Int
    ) =
        GLES30.glGetActiveUniformBlockName(
            program,
            uniformBlockIndex,
            bufSize,
            length,
            lengthOffset,
            uniformBlockName,
            uniformBlockNameOffset
        )
    override fun glGetActiveUniformBlockName(
        program: Int,
        uniformBlockIndex: Int,
        length: Buffer,
        uniformBlockName: Buffer
    ) = GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)
    override fun glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String =
        GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex)
    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) =
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int) =
        GLES30.glDrawArraysInstanced(mode, first, count, instanceCount)
    override fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indices: Buffer, instanceCount: Int) =
        GLES30.glDrawElementsInstanced(mode, count, type, indices, instanceCount)
    override fun glDrawElementsInstanced(mode: Int, count: Int, type: Int, indicesOffset: Int, instanceCount: Int) =
        GLES30.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount)
    override fun glFenceSync(condition: Int, flags: Int): Long = GLES30.glFenceSync(condition, flags)
    override fun glIsSync(sync: Long): Boolean = GLES30.glIsSync(sync)
    override fun glDeleteSync(sync: Long) = GLES30.glDeleteSync(sync)
    override fun glClientWaitSync(sync: Long, flags: Int, timeout: Long): Int =
        GLES30.glClientWaitSync(sync, flags, timeout)
    override fun glWaitSync(sync: Long, flags: Int, timeout: Long) = GLES30.glWaitSync(sync, flags, timeout)
    override fun glGetInteger64v(pname: Int, params: LongArray, offset: Int) =
        GLES30.glGetInteger64v(pname, params, offset)
    override fun glGetInteger64v(pname: Int, params: LongBuffer) = GLES30.glGetInteger64v(pname, params)
    override fun glGetSynciv(
        sync: Long,
        pname: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        values: IntArray,
        valuesOffset: Int
    ) = GLES30.glGetSynciv(sync, pname, bufSize, length, lengthOffset, values, valuesOffset)
    override fun glGetSynciv(sync: Long, pname: Int, bufSize: Int, length: IntBuffer, values: IntBuffer) =
        GLES30.glGetSynciv(sync, pname, bufSize, length, values)
    override fun glGetInteger64i_v(target: Int, index: Int, data: LongArray, offset: Int) =
        GLES30.glGetInteger64i_v(target, index, data, offset)
    override fun glGetInteger64i_v(target: Int, index: Int, data: LongBuffer) =
        GLES30.glGetInteger64i_v(target, index, data)
    override fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongArray, offset: Int) =
        GLES30.glGetBufferParameteri64v(target, pname, params, offset)
    override fun glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer) =
        GLES30.glGetBufferParameteri64v(target, pname, params)
    override fun glGenSamplers(n: Int, ids: IntArray, offset: Int) = GLES30.glGenSamplers(n, ids, offset)
    override fun glGenSamplers(count: Int, samplers: IntBuffer) = GLES30.glGenSamplers(count, samplers)
    override fun glDeleteSamplers(n: Int, ids: IntArray, offset: Int) = GLES30.glDeleteSamplers(n, ids, offset)
    override fun glDeleteSamplers(count: Int, samplers: IntBuffer) = GLES30.glDeleteSamplers(count, samplers)
    override fun glIsSampler(sampler: Int): Boolean = GLES30.glIsSampler(sampler)
    override fun glBindSampler(unit: Int, sampler: Int) = GLES30.glBindSampler(unit, sampler)
    override fun glSamplerParameteri(sampler: Int, pname: Int, param: Int) =
        GLES30.glSamplerParameteri(sampler, pname, param)
    override fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntArray, offset: Int) =
        GLES30.glSamplerParameteriv(sampler, pname, param, offset)
    override fun glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer) =
        GLES30.glSamplerParameteriv(sampler, pname, param)
    override fun glSamplerParameterf(sampler: Int, pname: Int, param: Float) =
        GLES30.glSamplerParameterf(sampler, pname, param)
    override fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatArray, offset: Int) =
        GLES30.glSamplerParameterfv(sampler, pname, param, offset)
    override fun glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer) =
        GLES30.glSamplerParameterfv(sampler, pname, param)
    override fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntArray, offset: Int) =
        GLES30.glGetSamplerParameteriv(sampler, pname, params, offset)
    override fun glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer) =
        GLES30.glGetSamplerParameteriv(sampler, pname, params)
    override fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatArray, offset: Int) =
        GLES30.glGetSamplerParameterfv(sampler, pname, params, offset)
    override fun glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer) =
        GLES30.glGetSamplerParameterfv(sampler, pname, params)
    override fun glVertexAttribDivisor(index: Int, divisor: Int) = GLES30.glVertexAttribDivisor(index, divisor)
    override fun glBindTransformFeedback(target: Int, id: Int) = GLES30.glBindTransformFeedback(target, id)
    override fun glDeleteTransformFeedbacks(n: Int, ids: IntArray, offset: Int) =
        GLES30.glDeleteTransformFeedbacks(n, ids, offset)
    override fun glDeleteTransformFeedbacks(n: Int, ids: IntBuffer) = GLES30.glDeleteTransformFeedbacks(n, ids)
    override fun glGenTransformFeedbacks(n: Int, ids: IntArray, offset: Int) =
        GLES30.glGenTransformFeedbacks(n, ids, offset)
    override fun glGenTransformFeedbacks(n: Int, ids: IntBuffer) = GLES30.glGenTransformFeedbacks(n, ids)
    override fun glIsTransformFeedback(id: Int): Boolean = GLES30.glIsTransformFeedback(id)
    override fun glPauseTransformFeedback() = GLES30.glPauseTransformFeedback()
    override fun glResumeTransformFeedback() = GLES30.glResumeTransformFeedback()
    override fun glGetProgramBinary(
        program: Int,
        bufSize: Int,
        length: IntArray,
        lengthOffset: Int,
        binaryFormat: IntArray,
        binaryFormatOffset: Int,
        binary: Buffer
    ) = GLES30.glGetProgramBinary(program, bufSize, length, lengthOffset, binaryFormat, binaryFormatOffset, binary)
    override fun glGetProgramBinary(
        program: Int,
        bufSize: Int,
        length: IntBuffer,
        binaryFormat: IntBuffer,
        binary: Buffer
    ) = GLES30.glGetProgramBinary(program, bufSize, length, binaryFormat, binary)
    override fun glProgramBinary(program: Int, binaryFormat: Int, binary: Buffer, length: Int) =
        GLES30.glProgramBinary(program, binaryFormat, binary, length)
    override fun glProgramParameteri(program: Int, pname: Int, value: Int) =
        GLES30.glProgramParameteri(program, pname, value)
    override fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntArray, offset: Int) =
        GLES30.glInvalidateFramebuffer(target, numAttachments, attachments, offset)
    override fun glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer) =
        GLES30.glInvalidateFramebuffer(target, numAttachments, attachments)
    override fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntArray,
        offset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) = GLES30.glInvalidateSubFramebuffer(target, numAttachments, attachments, offset, x, y, width, height)
    override fun glInvalidateSubFramebuffer(
        target: Int,
        numAttachments: Int,
        attachments: IntBuffer,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) = GLES30.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height)
    override fun glTexStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int) =
        GLES30.glTexStorage2D(target, levels, internalformat, width, height)
    override fun glTexStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int) =
        GLES30.glTexStorage3D(target, levels, internalformat, width, height, depth)
    override fun glGetInternalformativ(
        target: Int,
        internalformat: Int,
        pname: Int,
        bufSize: Int,
        params: IntArray,
        offset: Int
    ) = GLES30.glGetInternalformativ(target, internalformat, pname, bufSize, params, offset)
    override fun glGetInternalformativ(target: Int, internalformat: Int, pname: Int, bufSize: Int, params: IntBuffer) =
        GLES30.glGetInternalformativ(target, internalformat, pname, bufSize, params)
    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, offset: Int) =
        GLES30.glReadPixels(x, y, width, height, format, type, offset)
}
