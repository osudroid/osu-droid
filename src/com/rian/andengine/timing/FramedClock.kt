package com.rian.andengine.timing

import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.truncate

/**
 * Takes an [IClock] source and separates time reading on a per-frame level.
 *
 * The [currentTime] value will only change on initial construction and whenever [processFrame] is run.
 *
 * @param source A source [IClock] which will be used as the backing time source. If `null`, a [StopwatchClock] will
 * be created. When provided, the [currentTime] of [source] will be transferred instantly.
 * @param processSource Whether the source [IClock]'s [processFrame] method should be called during this [IClock]'s
 * [processFrame] call.
 */
open class FramedClock @JvmOverloads constructor(source: IClock? = null, private val processSource: Boolean = true) :
    IFrameBasedClock, ISourceChangeableClock {
    final override lateinit var source: IClock
        private set

    private val betweenFrameTimes = DoubleArray(128)
    private var totalFramesProcessed = 0

    final override var framesPerSecond = 0.0
        private set

    var jitter = 0.0
        private set

    override var currentTime = 0.0
        protected set

    protected open var lastFrameTime = 0.0

    override val rate by this.source::rate

    protected val sourceTime by this.source::currentTime

    override val elapsedFrameTime
        get() = currentTime - lastFrameTime

    override val isRunning by this.source::isRunning

    private var timeUntilNextCalculation = 0.0
    private var timeSinceLastCalculation = 0.0
    private var framesSinceLastCalculation = 0

    private val fpsCalculationInterval = 250

    override val timeInfo = FrameTimeInfo()

    init {
        changeSource(source)
    }

    override fun changeSource(source: IClock?) {
        this.source = source ?: StopwatchClock(true)
        currentTime = lastFrameTime - this.source.currentTime
    }

    override fun processFrame() {
        betweenFrameTimes[totalFramesProcessed % betweenFrameTimes.size] = currentTime - lastFrameTime
        ++totalFramesProcessed

        if (processSource) {
            (source as? IFrameBasedClock)?.processFrame()
        }

        if (timeUntilNextCalculation <= 0) {
            timeUntilNextCalculation += fpsCalculationInterval

            if (framesSinceLastCalculation == 0) {
                framesPerSecond = 0.0
                jitter = 0.0
            } else {
                framesPerSecond = ceil(framesSinceLastCalculation * 1000 / timeSinceLastCalculation)

                // Simple stddev
                var sum = 0.0
                var sumOfSquares = 0.0

                for (i in betweenFrameTimes.indices) {
                    val v = betweenFrameTimes[i]

                    sum += v
                    sumOfSquares += v * v
                }

                val average = sum / betweenFrameTimes.size
                val variance = sumOfSquares / betweenFrameTimes.size - average * average
                jitter = sqrt(variance)
            }

            timeSinceLastCalculation = 0.0
            framesSinceLastCalculation = 0
        }

        framesSinceLastCalculation++
        timeUntilNextCalculation -= elapsedFrameTime
        timeSinceLastCalculation += elapsedFrameTime

        lastFrameTime = currentTime
        currentTime = sourceTime

        timeInfo.current = currentTime
        timeInfo.elapsed = elapsedFrameTime
    }

    override fun toString() = "${this::class.simpleName} (${truncate(currentTime)}ms, $framesPerSecond FPS)"
}