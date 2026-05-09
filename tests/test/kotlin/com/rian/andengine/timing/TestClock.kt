package com.rian.andengine.timing

open class TestClock : IAdjustableClock {
    override var currentTime = 0f
    override var rate = 1f

    override var isRunning = false
        protected set

    override fun reset() {
        currentTime = 0f
        isRunning = false
    }

    override fun start() {
        isRunning = true
    }

    override fun stop() {
        isRunning = false
    }

    override fun seek(position: Float): Boolean {
        currentTime = position
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}