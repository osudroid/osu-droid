package com.rian.andengine.timing

/**
 * A [FramedClock] which allows an offset to be added or subtracted from an underlying source [IClock]'s time.
 */
class FramedOffsetClock @JvmOverloads constructor(source: IClock?, processSource: Boolean = true) :
    FramedClock(source, processSource) {
    /**
     * The offset to be applied.
     */
    var offset = 0.0
        set(value) {
            lastFrameTime += value - field
            field = value
        }

    override var currentTime
        get() = super.currentTime + offset
        set(value) {
            super.currentTime = value
        }
}