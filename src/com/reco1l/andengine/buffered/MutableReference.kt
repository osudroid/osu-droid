package com.reco1l.andengine.buffered

/**
 * A mutable reference wrapper that allows multiple components to share and update
 * the same reference to a buffer.
 *
 * Unlike [java.lang.ref.WeakReference], this reference is mutable and can be updated,
 * allowing all components that share this reference to see the changes.
 *
 * @param T The type of buffer being referenced
 */
class MutableReference<T>(private var value: T) {

    /**
     * Gets the current value.
     */
    fun get(): T = value

    /**
     * Sets a new value.
     */
    fun set(newValue: T) {
        value = newValue
    }

}
