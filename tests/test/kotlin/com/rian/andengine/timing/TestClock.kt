package com.rian.andengine.timing

open class TestClock : IAdjustableClock {
    final override var currentTime = 0f
    final override var rate = 1f

    final override var isRunning = false
        protected set

    final override fun reset() {
        currentTime = 0f
        isRunning = false
    }

    final override fun start() {
        isRunning = true
    }

    final override fun stop() {
        isRunning = false
    }

    override fun seek(position: Float): Boolean {
        currentTime = position
        return true
    }

    override fun resetSpeedAdjustments() {
        throw NotImplementedError()
    }
}