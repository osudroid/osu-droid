package com.rian.andengine.timing

/**
 * Provides a set of methods and properties that can be used to accurately measure elapsed time.
 */
open class Stopwatch @JvmOverloads constructor(
    /**
     * The time source to use for this [Stopwatch]. Defaults to [System.nanoTime].
     */
    private val source: () -> Long = System::nanoTime
) {
    /**
     * Whether this [Stopwatch] is running.
     */
    var isRunning = false
        protected set

    private var elapsedNanos = 0L
    private var startTime = 0L

    /**
     * Starts this [Stopwatch]. If this [Stopwatch] is already running, this method does nothing.
     */
    open fun start() {
        if (!isRunning) {
            startTime = source()
            isRunning = true
        }
    }

    /**
     * Stops this [Stopwatch]. If this [Stopwatch] is already stopped, this method does nothing.
     */
    open fun stop() {
        if (isRunning) {
            elapsedNanos += source() - startTime
            isRunning = false
        }
    }

    /**
     * Resets this [Stopwatch] to its initial state. If this [Stopwatch] is running, it will be stopped.
     */
    open fun reset() {
        elapsedNanos = 0
        isRunning = false
    }

    /**
     * Restarts this [Stopwatch] by resetting it and then starting it.
     */
    open fun restart() {
        reset()
        start()
    }

    /**
     * The total elapsed time in seconds. If this [Stopwatch] is currently running, the elapsed time will include the
     * time since it was last started.
     */
    val elapsedSeconds: Float
        get() {
            val currentElapsed = if (isRunning) elapsedNanos + (source() - startTime) else elapsedNanos

            return currentElapsed / 1e9f
        }
}