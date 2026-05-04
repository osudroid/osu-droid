package com.rian.andengine.timing

/**
 * A completely manual [IFrameBasedClock] implementation. Everything is settable.
 */
class ManualFramedClock : ManualClock(), IFrameBasedClock {
    override val elapsedFrameTime = 0.0
    override val framesPerSecond = 0.0
    override val timeInfo = FrameTimeInfo()
    override fun processFrame() = Unit
}