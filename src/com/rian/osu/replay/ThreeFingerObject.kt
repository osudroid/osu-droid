package com.rian.osu.replay

import com.rian.osu.beatmap.hitobject.HitObject

/**
 * An extension of [HitObject] containing three-finger data.
 */
data class ThreeFingerObject(
    /**
     * The [HitObject].
     */
    @JvmField
    val obj: HitObject,

    /**
     * The cursor instance index that aimed the object at the nearest time.
     *
     * If the object was missed, or if the object is a spinner, this is -1.
     */
    @JvmField
    val aimingCursorInstanceIndex: Int,

    /**
     * The cursor instance index that pressed the object at the nearest time.
     *
     * If the object was missed, or if the object is a spinner, this is -1.
     */
    @JvmField
    val pressingCursorInstanceIndex: Int
)