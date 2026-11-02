package org.andengine.graphics.gles

/**
 * The OpenGL ES 3.0 entry points the engine's GL core uses, 1:1 with [android.opengl.GLES30].
 * Constants are not mirrored; callers use `GLES30.GL_*`. Further methods are added by the same rule.
 */
interface GL30 {
    fun glGetString(name: Int): String?
    fun glGetIntegerv(pname: Int, params: IntArray, offset: Int)
    fun glGetError(): Int
    fun glEnable(cap: Int)
    fun glDisable(cap: Int)
    fun glUseProgram(program: Int)
    fun glBindBuffer(target: Int, buffer: Int)
    fun glBindBufferBase(target: Int, index: Int, buffer: Int)
    fun glBindVertexArray(array: Int)
    fun glBindFramebuffer(target: Int, framebuffer: Int)
    fun glBindRenderbuffer(target: Int, renderbuffer: Int)
    fun glActiveTexture(texture: Int)
    fun glBindTexture(target: Int, texture: Int)
    fun glBlendFunc(sfactor: Int, dfactor: Int)
    fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun glDepthFunc(func: Int)
    fun glDepthMask(flag: Boolean)
    fun glClearDepthf(depth: Float)
    fun glCullFace(mode: Int)
    fun glFrontFace(mode: Int)
    fun glLineWidth(width: Float)
    fun glScissor(x: Int, y: Int, width: Int, height: Int)
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glClear(mask: Int)
    fun glEnableVertexAttribArray(index: Int)
    fun glDisableVertexAttribArray(index: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer)
    fun glVertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float)
    fun glPixelStorei(pname: Int, param: Int)
    fun glGenTextures(n: Int, ids: IntArray, offset: Int)
    fun glGenBuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenVertexArrays(n: Int, ids: IntArray, offset: Int)
    fun glGenFramebuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenRenderbuffers(n: Int, ids: IntArray, offset: Int)
    fun glGenSamplers(n: Int, ids: IntArray, offset: Int)
    fun glGenQueries(n: Int, ids: IntArray, offset: Int)
    fun glDeleteTextures(n: Int, ids: IntArray, offset: Int)
    fun glDeleteBuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteVertexArrays(n: Int, ids: IntArray, offset: Int)
    fun glDeleteFramebuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteRenderbuffers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteSamplers(n: Int, ids: IntArray, offset: Int)
    fun glDeleteQueries(n: Int, ids: IntArray, offset: Int)
    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: java.nio.Buffer?)
    fun glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer?)
    fun glTexStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun glTexStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int)
    fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, type: Int, pixels: java.nio.Buffer?)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    fun glGenerateMipmap(target: Int)
    fun glBufferData(target: Int, size: Int, data: java.nio.Buffer?, usage: Int)
    fun glBufferSubData(target: Int, offset: Int, size: Int, data: java.nio.Buffer?)
    fun glCreateShader(type: Int): Int
    fun glShaderSource(shader: Int, string: String)
    fun glCompileShader(shader: Int)
    fun glGetShaderiv(shader: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetShaderInfoLog(shader: Int): String
    fun glCreateProgram(): Int
    fun glAttachShader(program: Int, shader: Int)
    fun glLinkProgram(program: Int)
    fun glGetProgramiv(program: Int, pname: Int, params: IntArray, offset: Int)
    fun glGetProgramInfoLog(program: Int): String
    fun glDeleteShader(shader: Int)
    fun glDeleteProgram(program: Int)
    fun glGetUniformLocation(program: Int, name: String): Int
    fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int
    fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int)
    fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int)
    fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int)
    fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
    fun glCheckFramebufferStatus(target: Int): Int
    fun glBindSampler(unit: Int, sampler: Int)
    fun glSamplerParameteri(sampler: Int, pname: Int, param: Int)
    fun glBeginQuery(target: Int, id: Int)
    fun glEndQuery(target: Int)
    fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntArray, offset: Int)
    fun glFenceSync(condition: Int, flags: Int): Long
    fun glClientWaitSync(sync: Long, flags: Int, timeout: Long): Int
    fun glDeleteSync(sync: Long)
    fun glDrawArrays(mode: Int, first: Int, count: Int)
    fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int)
    fun glDrawElements(mode: Int, count: Int, type: Int, indices: java.nio.Buffer)
}
