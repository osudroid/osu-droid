package org.andengine.graphics.gles

import android.opengl.GLES20

/** Test fake: records every call as `glName(arg1,arg2,...)`. */
class RecordingGL : GL32 {
    val calls: MutableList<String> = ArrayList()
    var versionString: String? = "OpenGL ES 3.2 test"
    var extensionsString: String? = ""
    var rendererString: String? = "RecordingGL"
    val intValues: MutableMap<Int, Int> = HashMap()
    /** Values returned by [glGetQueryObjectuiv], keyed by `pname`; 0 when absent. */
    val queryValues: MutableMap<Int, Int> = HashMap()
    /** The bytes of every non-null [glBufferData] upload, in call order. */
    val bufferDataBytes: MutableList<ByteArray> = ArrayList()
    private var nextId = 1

    override fun glGetString(name: Int): String? {
        calls += "glGetString($name)"
        return when (name) {
            GLES20.GL_VERSION -> versionString
            GLES20.GL_EXTENSIONS -> extensionsString
            GLES20.GL_RENDERER -> rendererString
            else -> null
        }
    }

    override fun glGetIntegerv(pname: Int, params: IntArray, offset: Int) {
        calls += "glGetIntegerv($pname,buf,$offset)"
        params[offset] = intValues[pname] ?: 0
    }

    /** Codes returned by [glGetError], first in first out; `GL_NO_ERROR` (0) once empty. */
    val errorQueue: ArrayDeque<Int> = ArrayDeque()

    override fun glGetError(): Int {
        calls += "glGetError()"
        return errorQueue.removeFirstOrNull() ?: 0
    }

    override fun glEnable(cap: Int) {
        calls += "glEnable($cap)"
    }

    override fun glDisable(cap: Int) {
        calls += "glDisable($cap)"
    }

    override fun glUseProgram(program: Int) {
        calls += "glUseProgram($program)"
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        calls += "glBindBuffer($target,$buffer)"
    }

    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) {
        calls += "glBindBufferBase($target,$index,$buffer)"
    }

    override fun glBindVertexArray(array: Int) {
        calls += "glBindVertexArray($array)"
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        calls += "glBindFramebuffer($target,$framebuffer)"
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        calls += "glBindRenderbuffer($target,$renderbuffer)"
    }

    override fun glActiveTexture(texture: Int) {
        calls += "glActiveTexture($texture)"
    }

    override fun glBindTexture(target: Int, texture: Int) {
        calls += "glBindTexture($target,$texture)"
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        calls += "glBlendFunc($sfactor,$dfactor)"
    }

    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        calls += "glBlendFuncSeparate($srcRGB,$dstRGB,$srcAlpha,$dstAlpha)"
    }

    override fun glDepthFunc(func: Int) {
        calls += "glDepthFunc($func)"
    }

    override fun glDepthMask(flag: Boolean) {
        calls += "glDepthMask($flag)"
    }

    override fun glClearDepthf(depth: Float) {
        calls += "glClearDepthf($depth)"
    }

    override fun glCullFace(mode: Int) {
        calls += "glCullFace($mode)"
    }

    override fun glFrontFace(mode: Int) {
        calls += "glFrontFace($mode)"
    }

    override fun glLineWidth(width: Float) {
        calls += "glLineWidth($width)"
    }

    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        calls += "glScissor($x,$y,$width,$height)"
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        calls += "glViewport($x,$y,$width,$height)"
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        calls += "glClearColor($red,$green,$blue,$alpha)"
    }

    override fun glClear(mask: Int) {
        calls += "glClear($mask)"
    }

    override fun glEnableVertexAttribArray(index: Int) {
        calls += "glEnableVertexAttribArray($index)"
    }

    override fun glDisableVertexAttribArray(index: Int) {
        calls += "glDisableVertexAttribArray($index)"
    }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        calls += "glVertexAttribPointer($index,$size,$type,$normalized,$stride,$offset)"
    }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer) {
        calls += "glVertexAttribPointer($index,$size,$type,$normalized,$stride,buf)"
    }

    override fun glVertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
        calls += "glVertexAttrib4f($index,$x,$y,$z,$w)"
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        calls += "glPixelStorei($pname,$param)"
    }

    override fun glGenTextures(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenTextures($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenBuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenBuffers($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenVertexArrays(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenVertexArrays($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenFramebuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenFramebuffers($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenRenderbuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenRenderbuffers($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenSamplers(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenSamplers($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glGenQueries(n: Int, ids: IntArray, offset: Int) {
        calls += "glGenQueries($n,buf,$offset)"
        for (i in 0 until n) ids[offset + i] = nextId++
    }

    override fun glDeleteTextures(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteTextures($n,buf,$offset)"
    }

    override fun glDeleteBuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteBuffers($n,buf,$offset)"
    }

    override fun glDeleteVertexArrays(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteVertexArrays($n,buf,$offset)"
    }

    override fun glDeleteFramebuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteFramebuffers($n,buf,$offset)"
    }

    override fun glDeleteRenderbuffers(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteRenderbuffers($n,buf,$offset)"
    }

    override fun glDeleteSamplers(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteSamplers($n,buf,$offset)"
    }

    override fun glDeleteQueries(n: Int, ids: IntArray, offset: Int) {
        calls += "glDeleteQueries($n,buf,$offset)"
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: java.nio.Buffer?) {
        calls += "glTexImage2D($target,$level,$internalformat,$width,$height,$border,$format,$type,buf)"
    }

    override fun glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer?) {
        calls += "glTexSubImage2D($target,$level,$xoffset,$yoffset,$width,$height,$format,$type,buf)"
    }

    override fun glTexStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int) {
        calls += "glTexStorage2D($target,$levels,$internalformat,$width,$height)"
    }

    override fun glTexStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int) {
        calls += "glTexStorage3D($target,$levels,$internalformat,$width,$height,$depth)"
    }

    override fun glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, type: Int, pixels: java.nio.Buffer?) {
        calls += "glTexSubImage3D($target,$level,$xoffset,$yoffset,$zoffset,$width,$height,$depth,$format,$type,buf)"
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        calls += "glTexParameteri($target,$pname,$param)"
    }

    override fun glGenerateMipmap(target: Int) {
        calls += "glGenerateMipmap($target)"
    }

    override fun glBufferData(target: Int, size: Int, data: java.nio.Buffer?, usage: Int) {
        calls += "glBufferData($target,$size,buf,$usage)"
        if (data is java.nio.ByteBuffer) {
            val copy = data.duplicate()
            val bytes = ByteArray(minOf(size, copy.remaining()))
            copy.get(bytes)
            bufferDataBytes += bytes
        }
    }

    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: java.nio.Buffer?) {
        calls += "glBufferSubData($target,$offset,$size,buf)"
    }

    override fun glCreateShader(type: Int): Int {
        calls += "glCreateShader($type)"
        return nextId++
    }

    override fun glShaderSource(shader: Int, string: String) {
        calls += "glShaderSource($shader,$string)"
    }

    override fun glCompileShader(shader: Int) {
        calls += "glCompileShader($shader)"
    }

    override fun glGetShaderiv(shader: Int, pname: Int, params: IntArray, offset: Int) {
        calls += "glGetShaderiv($shader,$pname,buf,$offset)"
        params[offset] = 1
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        calls += "glGetShaderInfoLog($shader)"
        return ""
    }

    override fun glCreateProgram(): Int {
        calls += "glCreateProgram()"
        return nextId++
    }

    override fun glAttachShader(program: Int, shader: Int) {
        calls += "glAttachShader($program,$shader)"
    }

    override fun glLinkProgram(program: Int) {
        calls += "glLinkProgram($program)"
    }

    override fun glGetProgramiv(program: Int, pname: Int, params: IntArray, offset: Int) {
        calls += "glGetProgramiv($program,$pname,buf,$offset)"
        params[offset] = 1
    }

    override fun glGetProgramInfoLog(program: Int): String {
        calls += "glGetProgramInfoLog($program)"
        return ""
    }

    override fun glDeleteShader(shader: Int) {
        calls += "glDeleteShader($shader)"
    }

    override fun glDeleteProgram(program: Int) {
        calls += "glDeleteProgram($program)"
    }

    override fun glGetUniformLocation(program: Int, name: String): Int {
        calls += "glGetUniformLocation($program,$name)"
        return 0
    }

    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int {
        calls += "glGetUniformBlockIndex($program,$uniformBlockName)"
        return 0
    }

    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) {
        calls += "glUniformBlockBinding($program,$uniformBlockIndex,$uniformBlockBinding)"
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        calls += "glFramebufferTexture2D($target,$attachment,$textarget,$texture,$level)"
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        calls += "glFramebufferRenderbuffer($target,$attachment,$renderbuffertarget,$renderbuffer)"
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        calls += "glRenderbufferStorage($target,$internalformat,$width,$height)"
    }

    /** Returned by [glCheckFramebufferStatus]. */
    var framebufferStatus = GLES20.GL_FRAMEBUFFER_COMPLETE

    override fun glCheckFramebufferStatus(target: Int): Int {
        calls += "glCheckFramebufferStatus($target)"
        return framebufferStatus
    }

    override fun glBindSampler(unit: Int, sampler: Int) {
        calls += "glBindSampler($unit,$sampler)"
    }

    override fun glSamplerParameteri(sampler: Int, pname: Int, param: Int) {
        calls += "glSamplerParameteri($sampler,$pname,$param)"
    }

    override fun glBeginQuery(target: Int, id: Int) {
        calls += "glBeginQuery($target,$id)"
    }

    override fun glEndQuery(target: Int) {
        calls += "glEndQuery($target)"
    }

    override fun glGetQueryObjectuiv(id: Int, pname: Int, params: IntArray, offset: Int) {
        calls += "glGetQueryObjectuiv($id,$pname,buf,$offset)"
        params[offset] = queryValues[pname] ?: 0
    }

    override fun glFenceSync(condition: Int, flags: Int): Long {
        calls += "glFenceSync($condition,$flags)"
        return nextId++.toLong()
    }

    override fun glClientWaitSync(sync: Long, flags: Int, timeout: Long): Int {
        calls += "glClientWaitSync($sync,$flags,$timeout)"
        return 0
    }

    override fun glDeleteSync(sync: Long) {
        calls += "glDeleteSync($sync)"
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        calls += "glDrawArrays($mode,$first,$count)"
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) {
        calls += "glDrawElements($mode,$count,$type,$offset)"
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: java.nio.Buffer) {
        calls += "glDrawElements($mode,$count,$type,buf)"
    }

    override fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String) {
        calls += "glObjectLabel($identifier,$name,$length,$label)"
    }

    override fun glDebugMessageCallback(callback: android.opengl.GLES32.DebugProc?) {
        calls += "glDebugMessageCallback(buf)"
        if (debugCallbackUnsupported) throw UnsupportedOperationException("not yet implemented")
    }

    /** Makes [glDebugMessageCallback] throw like the Android bindings do. */
    var debugCallbackUnsupported = false

    /** Makes the `byte[]` overload of [glGetDebugMessageLog] throw like the Android bindings do. */
    var debugLogBytesUnsupported = false

    /** Makes the `String[]` overload of [glGetDebugMessageLog] throw like the Android bindings do. */
    var debugLogStringsUnsupported = false

    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int
    ): Array<String?>? {
        calls += "glGetDebugMessageLog($count,strings)"
        if (debugLogStringsUnsupported) throw UnsupportedOperationException("not yet implemented")
        val result = ArrayList<String?>()
        while (result.size < count && debugLog.isNotEmpty()) {
            val m = debugLog.removeFirst()
            val n = result.size
            sources[sourcesOffset + n] = m.source
            types[typesOffset + n] = m.type
            ids[idsOffset + n] = m.id
            severities[severitiesOffset + n] = m.severity
            result += m.text
        }
        return result.toTypedArray()
    }

    /** Pending debug messages returned by [glGetDebugMessageLog]: source, type, id, severity, text. */
    val debugLog: ArrayDeque<DebugMessage> = ArrayDeque()

    class DebugMessage(val source: Int, val type: Int, val id: Int, val severity: Int, val text: String)

    override fun glGetDebugMessageLog(
        count: Int, bufSize: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int, lengths: IntArray, lengthsOffset: Int,
        messageLog: ByteArray, messageLogOffset: Int
    ): Int {
        calls += "glGetDebugMessageLog($count,$bufSize)"
        if (debugLogBytesUnsupported) throw UnsupportedOperationException("not yet implemented")
        var n = 0
        var offset = messageLogOffset
        while (n < count && debugLog.isNotEmpty()) {
            val bytes = debugLog.first().text.toByteArray(Charsets.UTF_8)
            if (offset - messageLogOffset + bytes.size + 1 > bufSize) break
            val m = debugLog.removeFirst()
            sources[sourcesOffset + n] = m.source
            types[typesOffset + n] = m.type
            ids[idsOffset + n] = m.id
            severities[severitiesOffset + n] = m.severity
            lengths[lengthsOffset + n] = bytes.size + 1
            System.arraycopy(bytes, 0, messageLog, offset, bytes.size)
            messageLog[offset + bytes.size] = 0
            offset += bytes.size + 1
            n++
        }
        return n
    }

    override fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: java.nio.Buffer, basevertex: Int) {
        calls += "glDrawElementsBaseVertex($mode,$count,$type,buf,$basevertex)"
    }

    override fun glDrawElementsInstancedBaseVertex(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int, basevertex: Int) {
        calls += "glDrawElementsInstancedBaseVertex($mode,$count,$type,$offset,$instanceCount,$basevertex)"
    }
}
