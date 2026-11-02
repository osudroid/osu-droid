package org.andengine.graphics.gles

/** OpenGL ES 3.2 entry points, 1:1 with [android.opengl.GLES32]. */
interface GL32 : GL31 {
    fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String)
    fun glDebugMessageCallback(callback: android.opengl.GLES32.DebugProc?)
    fun glGetDebugMessageLog(
        count: Int, bufSize: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int, lengths: IntArray, lengthsOffset: Int,
        messageLog: ByteArray, messageLogOffset: Int
    ): Int
    fun glGetDebugMessageLog(
        count: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int
    ): Array<String?>?
    fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: java.nio.Buffer, basevertex: Int)
    /** The only base-vertex draw with a byte offset into the bound element buffer in the Android bindings. */
    fun glDrawElementsInstancedBaseVertex(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int, basevertex: Int)
}
