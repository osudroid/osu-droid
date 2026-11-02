package org.andengine.graphics.gles

/** [GL32] backed by [android.opengl.GLES32]. */
class AndroidGL32 : AndroidGL31(), GL32 {
    override fun glObjectLabel(identifier: Int, name: Int, length: Int, label: String) = android.opengl.GLES32.glObjectLabel(identifier, name, length, label)
    override fun glDebugMessageCallback(callback: android.opengl.GLES32.DebugProc?) = android.opengl.GLES32.glDebugMessageCallback(callback)
    override fun glGetDebugMessageLog(
        count: Int, bufSize: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int, lengths: IntArray, lengthsOffset: Int,
        messageLog: ByteArray, messageLogOffset: Int
    ): Int = android.opengl.GLES32.glGetDebugMessageLog(
        count, bufSize, sources, sourcesOffset, types, typesOffset, ids, idsOffset,
        severities, severitiesOffset, lengths, lengthsOffset, messageLog, messageLogOffset
    )
    override fun glGetDebugMessageLog(
        count: Int,
        sources: IntArray, sourcesOffset: Int, types: IntArray, typesOffset: Int, ids: IntArray, idsOffset: Int,
        severities: IntArray, severitiesOffset: Int
    ): Array<String?>? = android.opengl.GLES32.glGetDebugMessageLog(
        count, sources, sourcesOffset, types, typesOffset, ids, idsOffset, severities, severitiesOffset
    )
    override fun glDrawElementsBaseVertex(mode: Int, count: Int, type: Int, indices: java.nio.Buffer, basevertex: Int) = android.opengl.GLES32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex)
    override fun glDrawElementsInstancedBaseVertex(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int, basevertex: Int) = android.opengl.GLES32.glDrawElementsInstancedBaseVertex(mode, count, type, offset, instanceCount, basevertex)
}
