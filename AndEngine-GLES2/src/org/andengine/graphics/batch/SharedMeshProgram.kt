package org.andengine.graphics.batch

/**
 * One [MeshProgram] shared by every user of a shader, for example all entities of one class: created
 * at the first [get], so nothing touches GL before the first draw. Once created, the program restores
 * itself after a context loss. GL thread only.
 */
class SharedMeshProgram(
    private val label: String,
    private val fragmentShader: String,
    private val uniforms: List<String> = emptyList(),
    private val external: Boolean = false
) {

    private var program: MeshProgram? = null

    /** The shared program, created at the first call. */
    fun get(): MeshProgram =
        program ?: MeshProgram(label, fragmentShader, uniforms, external).also { program = it }

    /** Disposes the program, if it was created; the next [get] creates it again. */
    fun release() {
        program?.releaseResources()
        program = null
    }
}
