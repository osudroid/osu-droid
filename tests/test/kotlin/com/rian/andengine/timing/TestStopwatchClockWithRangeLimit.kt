package com.rian.andengine.timing

class TestStopwatchClockWithRangeLimit : StopwatchClock(true) {
    val minTime = 0f
    var maxTime = Float.POSITIVE_INFINITY

    override val currentTime: Float
        get() {
            val currentTime = super.currentTime
            val clamped = currentTime.coerceIn(minTime, maxTime)

            if (clamped == currentTime) {
                return clamped
            }

            if ((rate > 0 && clamped == maxTime) || (rate < 0 && clamped == minTime)) {
                stop()
            }

            return clamped
        }

    override fun seek(position: Float): Boolean {
        val clamped = position.coerceIn(minTime, maxTime)

        if (clamped != position) {
            // Emulate what a BASS track would do in this situation.
            if (position >= maxTime) {
                stop()
            }

            seek(clamped)
            return false
        }

        return super.seek(position)
    }
}