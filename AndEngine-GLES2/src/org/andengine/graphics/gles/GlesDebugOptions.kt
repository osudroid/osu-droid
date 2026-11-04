package org.andengine.graphics.gles

/** Developer switches for the GL core. */
object GlesDebugOptions {

    /** Pretend the device only supports ES 3.0, to exercise the fallback paths. */
    @JvmField @Volatile var simulateEs30: Boolean = false

    /** Enable GL debug output / error checking where available. */
    @JvmField @Volatile var debugOutput: Boolean = false
}
