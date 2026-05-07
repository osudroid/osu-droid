package com.rian.andengine.timing

/**
 * A decoupling [IClock] allows taking an existing [IClock] which has seek limitations and provides a controllable
 * encapsulated view of that [IClock] with the ability to seek and track time outside the normally seekable bounds.
 *
 * Put simply, it will take a Track which can only track time from `0..trackLength` and allow both negative seeks and
 * seeks beyond `trackLength`. It will also allow time to continue counting beyond the end of the track even when not
 * explicitly seeked.
 *
 * There are a few things to note about this implementation:
 * - Changing the source [IClock] via [changeSource] will always take on the new source's running state and current time,
 * regardless of decoupled state.
 * - It is always assumed that after a [reset] on the source, it will be able to track time.
 * - It is assumed that a source is generally able to start tracking from zero. Special handling ensures that when
 * arriving at zero from negative time, the source will attempt to be started once so it can take over. Note that no
 * such special handling is assured for when the source has a maximum allowable time, since it is not known what that
 * time is.
 */
open class DecouplingFramedClock @JvmOverloads constructor(source: IClock? = null) : ISourceChangeableClock,
    IAdjustableClock, IFrameBasedClock {
    /**
     * Whether to allow operation in decoupled mode. Defaults to `true`.
     *
     * When set to `false`, this [DecouplingFramedClock] will operate in a transparent pass-through mode.
     */
    var allowDecoupling = true

    final override var isRunning = false
        private set

    final override var currentTime = 0f
        private set

    final override var elapsedFrameTime = 0f
        private set

    override val framesPerSecond = 0f

    /**
     * We maintain an internal running state so that when we notice the source clock has stopped, we can continue to run
     * in a decoupled mode (and know if we should be running or not).
     */
    private var shouldBeRunning = false

    /**
     * We need to track our internal time separately from the exposed currentTime to make sure the exposed value is only
     * ever updated on [processFrame].
     */
    private var _currentTime = 0f

    /**
     * Tracks the current time of [realtimeReferenceClock] one [processFrame] ago.
     */
    private var lastReferenceTime: Float? = null

    /**
     * Whether the last [seek] operation failed. This denotes that we need to [start] in decoupled mode (if possible).
     */
    private var lastSeekFailed = false

    /**
     * This clock is used when we are decoupling from the source.
     */
    private val realtimeReferenceClock: IClock = StopwatchClock(true)

    private lateinit var adjustableSourceClock: IAdjustableClock

    /**
     * Denotes a state where a negative seek stopped the source clock and entered decoupled mode, meaning that after
     * crossing into positive time again we should attempt to start and use the source clock.
     */
    private var pendingSourceRestartAfterNegativeSeek = false

    override val timeInfo = FrameTimeInfo()

    init {
        changeSource(source)
    }

    override fun processFrame() {
        val lastTime = currentTime

        (source as? IFrameBasedClock)?.processFrame()

        val referenceTime = realtimeReferenceClock.currentTime

        try {
            // If the source is running, there is never a need for any decoupling logic.
            if (source.isRunning) {
                _currentTime = source.currentTime
                shouldBeRunning = true
                return
            }

            // If we are not allowed to decouple, we should also just pass-through the source time.
            if (!allowDecoupling) {
                _currentTime = source.currentTime
                shouldBeRunning = false
                return
            }

            // We then want to check whether our internal running state permits time to elapse in decoupled mode.
            if (!shouldBeRunning) {
                return
            }

            // We can only begin tracking time from the second frame, as we need an elapsed real time reference.
            val lastReferenceTime = lastReferenceTime ?: return
            val elapsedReferenceTime = (referenceTime - lastReferenceTime) * rate

            _currentTime += elapsedReferenceTime

            // When crossing into positive time, we should attempt to start and use the source clock.
            // Note that this carries the common assumption that the source clock *should* be able to run from zero.
            if (pendingSourceRestartAfterNegativeSeek && currentTime >= 0) {
                pendingSourceRestartAfterNegativeSeek = false

                // We still need to check the seek was successful, else we might have already exceeded valid length of
                // the source.
                lastSeekFailed = !adjustableSourceClock.seek(currentTime)

                if (!lastSeekFailed) {
                    adjustableSourceClock.start()
                }

                // Don't use the source's time until next frame, as our decoupled time is likely more accurate
                // (starting a clock may have slight discrepancies).
            }
        } finally {
            isRunning = shouldBeRunning
            lastReferenceTime = referenceTime
            currentTime = _currentTime
            elapsedFrameTime = currentTime - lastTime

            timeInfo.current = currentTime
            timeInfo.elapsed = elapsedFrameTime
        }
    }

    //region ISourceChangeableClock implementation

    final override lateinit var source: IClock
        private set

    override fun changeSource(source: IClock?) {
        val source = source ?: StopwatchClock(true)

        if (source !is IAdjustableClock) {
            throw IllegalArgumentException("Clock must be of type IAdjustableClock")
        }

        this.source = source
        adjustableSourceClock = source
        _currentTime = adjustableSourceClock.currentTime
        shouldBeRunning = adjustableSourceClock.isRunning
        lastSeekFailed = false
    }

    //endregion

    //region IAdjustableClock implementation

    override fun reset() {
        adjustableSourceClock.reset()
        pendingSourceRestartAfterNegativeSeek = false
        shouldBeRunning = false
        lastSeekFailed = false
        _currentTime = 0f
    }

    override fun start() {
        if (shouldBeRunning) {
            return
        }

        // If the previous seek failed, avoid calling `Start` on the source clock. Doing so would potentially cause it
        // to start from an incorrect location (i.e., 0 in the case where we are tracking negative time).
        if (lastSeekFailed && allowDecoupling) {
            shouldBeRunning = true
            return
        }

        adjustableSourceClock.start()
        shouldBeRunning = adjustableSourceClock.isRunning || allowDecoupling
    }

    override fun stop() {
        adjustableSourceClock.stop()
        shouldBeRunning = false
    }

    override fun seek(position: Float): Boolean {
        lastSeekFailed = !adjustableSourceClock.seek(position)

        if (!lastSeekFailed) {
            // Transfer attempt to transfer decoupled running state to source in the case we succeeded.
            if (shouldBeRunning && !source.isRunning) {
                adjustableSourceClock.start()
            }
        } else {
            if (!allowDecoupling) {
                return false
            }

            // Ensure the underlying clock is stopped as we enter decoupled mode.
            adjustableSourceClock.stop()
            pendingSourceRestartAfterNegativeSeek = position < 0
        }

        _currentTime = position
        return true
    }

    override fun resetSpeedAdjustments() {
        adjustableSourceClock.resetSpeedAdjustments()
    }

    override var rate by adjustableSourceClock::rate

    //endregion
}