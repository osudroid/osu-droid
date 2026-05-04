package com.rian.andengine.timing

/**
 * Provides a set of methods and properties that can be used to accurately measure elapsed time.
 */
open class Stopwatch {
    /**
     * Whether this [Stopwatch] is running.
     */
    var isRunning = false
        protected set

    private var elapsedNanos = 0L
    private var startTime = 0L

    open fun start() {
        if (!isRunning) {
            startTime = System.nanoTime()
            isRunning = true
        }
    }

    open fun stop() {
        if (isRunning) {
            elapsedNanos += System.nanoTime() - startTime
            isRunning = false
        }
    }

    open fun reset() {
        elapsedNanos = 0
        isRunning = false
    }

    open fun restart() {
        reset()
        start()
    }

    val elapsedMilliseconds: Double
        get() {
            val currentElapsed = if (isRunning) elapsedNanos + (System.nanoTime() - startTime) else elapsedNanos

            return currentElapsed / 1000000.0
        }
}