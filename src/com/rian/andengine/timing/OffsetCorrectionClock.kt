package com.rian.andengine.timing

/**
 * A [FramedOffsetClock] which applies a correction to the offset based on the current playback rate.
 */
class OffsetCorrectionClock(source: IClock?) : FramedOffsetClock(source) {
    private var _offset = 0f

    override var offset
        get() = _offset
        set(value) {
            if (_offset == value) {
                return
            }

            _offset = value
            updateOffset()
        }

    /**
     * The rate-adjusted offset of this [OffsetCorrectionClock].
     */
    val rateAdjustedOffset
        get() = super.offset

    override fun processFrame() {
        super.processFrame()
        updateOffset()
    }

    private fun updateOffset() {
        // we always want to apply the same real-time offset, so it should be adjusted by the difference in playback
        // rate (from realtime) to achieve this.
        super.offset = offset * rate
    }
}