package com.rian.andengine.timing

class TestNonAdjustableClock : IClock {
    override var currentTime = 0f
    override var rate = 1f
    override val isRunning = true
}