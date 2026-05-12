package com.rian.andengine.timing

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.truncate

/**
 * Takes an [IClock] source and separates time reading on a per-frame level.
 *
 * The [currentTime] value will only change on initial construction and whenever [processFrame] is run.
 *
 * @param source A source [IClock] which will be used as the backing time source. If `null`, a [StopwatchClock] will
 * be created. When provided, the [currentTime] of [source] will be transferred instantly.
 * @param processSource If [source] is an [IFrameBasedClock], whether its [processFrame] method should be called
 * during this [FramedClock]'s [processFrame] call.
 */
open class FramedClock @JvmOverloads constructor(source: IClock? = null, private val processSource: Boolean = true) :
    IFrameBasedClock, ISourceChangeableClock {
    final override lateinit var source: IClock
        private set

    override var currentTime = 0f
        protected set

    protected open var lastFrameTime = 0f

    private val betweenFrameTimes = FloatArray(128)
    private var totalFramesProcessed = 0

    final override var framesPerSecond = 0f
        private set

    var jitter = 0f
        private set

    override val rate
        get() = source.rate

    protected val sourceTime
        get() = source.currentTime

    override val elapsedFrameTime
        get() = currentTime - lastFrameTime

    override val isRunning
        get() = source.isRunning

    private val _timeInfo = FrameTimeInfo()

    override val timeInfo
        get() = _timeInfo.apply {
            current = currentTime
            elapsed = elapsedFrameTime
        }

    private var timeUntilNextCalculation = 0f
    private var timeSinceLastCalculation = 0f
    private var framesSinceLastCalculation = 0

    private val fpsCalculationInterval = 0.25f

    init {
        changeSource(source)
    }

    override fun changeSource(source: IClock?) {
        this.source = source ?: StopwatchClock(true)
        currentTime = this.source.currentTime
        lastFrameTime = currentTime

        totalFramesProcessed = 0
        betweenFrameTimes.fill(0f)
        timeUntilNextCalculation = fpsCalculationInterval
        timeSinceLastCalculation = 0f
        framesSinceLastCalculation = 0
    }

    override fun processFrame() {
        updateTime()
        performBookkeeping(elapsedFrameTime)
    }

    /**
     * Updates the [currentTime] and [lastFrameTime] of this [FramedClock].
     */
    protected open fun updateTime() {
        if (processSource) {
            (source as? IFrameBasedClock)?.processFrame()
        }

        lastFrameTime = currentTime
        currentTime = sourceTime
    }

    /**
     * Performs bookkeeping for diagnostics like [framesPerSecond] and [jitter].
     *
     * @param delta The elapsed time for the current frame.
     */
    protected fun performBookkeeping(delta: Float) {
        betweenFrameTimes[totalFramesProcessed % betweenFrameTimes.size] = delta
        ++totalFramesProcessed

        framesSinceLastCalculation++
        timeUntilNextCalculation -= delta
        timeSinceLastCalculation += delta

        if (timeUntilNextCalculation <= 0) {
            timeUntilNextCalculation += fpsCalculationInterval

            if (framesSinceLastCalculation == 0 || timeSinceLastCalculation <= 0f) {
                framesPerSecond = 0f
                jitter = 0f
            } else {
                framesPerSecond = ceil(framesSinceLastCalculation / timeSinceLastCalculation)

                // Simple stddev
                var sum = 0f
                var sumOfSquares = 0f
                val sampleCount = min(totalFramesProcessed, betweenFrameTimes.size)

                for (i in 0 until sampleCount) {
                    val v = betweenFrameTimes[i]

                    sum += v
                    sumOfSquares += v * v
                }

                val average = sum / sampleCount
                val variance = sumOfSquares / sampleCount - average * average
                jitter = sqrt(max(0f, variance))
            }

            timeSinceLastCalculation = 0f
            framesSinceLastCalculation = 0
        }
    }

    override fun toString() = "${this::class.simpleName} (${truncate(currentTime * 1e3)}ms, $framesPerSecond FPS)"
}
