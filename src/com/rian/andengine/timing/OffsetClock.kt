package com.rian.andengine.timing

open class OffsetClock(protected var source: IClock) : IClock {
    var offset = 0.0

    override val currentTime
        get() = source.currentTime + offset

    override val rate by source::rate
    override val isRunning by source::isRunning
}