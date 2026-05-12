package com.rian.andengine.timing

/**
 * A completely manual [IFrameBasedClock] implementation.
 *
 * Everything is settable.
 */
class ManualFramedClock : IFrameBasedClock {
    override var currentTime = 0f
    override var rate = 1f
    override var elapsedFrameTime = 0f
    override var framesPerSecond = 0f

    private val _timeInfo = FrameTimeInfo()

    override val timeInfo
        get() = _timeInfo.apply {
            current = currentTime
            elapsed = elapsedFrameTime
        }

    override var isRunning = false

    override fun processFrame() = Unit
}
