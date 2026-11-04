package org.andengine.graphics

/** Tells when the GPU has finished all commands issued before [insert]. */
interface GpuFence : GpuResource {

    /** Places the fence after all commands issued so far, replacing the previous one. */
    fun insert()

    /** True once the GPU passed the fence; also true if none was inserted or the context was lost. */
    fun isSignaled(): Boolean
}
