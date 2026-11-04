package org.andengine.graphics

/** A linked shader program built from GLSL ES source. */
interface GpuProgram : GpuResource {

    /** Location of the uniform [name], or -1 if the program has no such active uniform. */
    fun uniformLocation(name: String): Int
}
