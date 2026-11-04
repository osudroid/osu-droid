package org.andengine.graphics

/** A GPU-side object owned by the engine. Backend-neutral: no GL types here. */
interface GpuResource {

    /** Human readable name, used for debug labels and logs. */
    val label: String

    val isDisposed: Boolean

    /** Releases the GPU object. Further use is an error. */
    fun dispose()
}
