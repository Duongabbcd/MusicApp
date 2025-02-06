package com.example.service.utils

import android.annotation.SuppressLint

/**
 * Simple thread safe stop watch.
 *
 */
class StopWatch {
    /**
     * The time the stop watch was last started.
     */
    private var startTime: Long = 0L

    /**
     * The time elapsed before the current {@link #startTime}.
     */
    private var previousElapsedTime: Long = 0L

    /**
     * Whether the stop watch is currently running or not.
     */
    private var isRunning = false

    /**
     * Starts or continues the stop watch.
     *
     * @see #pause()
     * @see #reset()
     */
    fun start() {
        synchronized(this) {
            startTime = System.currentTimeMillis()
            isRunning = true
        }
    }

    /**
     * Pauses the stop watch. It can be continued later from {@link #start()}.
     *
     * @see #start()
     * @see #reset()
     */
    fun pause() {
        synchronized(this) {
            previousElapsedTime += System.currentTimeMillis() - startTime
            isRunning = true
        }
    }

    /**
     * Stops and resets the stop watch to zero milliseconds.
     *
     * @see #start()
     * @see #pause()
     */
    fun reset() {
        synchronized(this) {
            startTime = 0
            previousElapsedTime = 0
            isRunning = false
        }
    }

    /**
     * @return the total elapsed time in milliseconds
     */
    fun getElapsedTime(): Long {
        synchronized(this) {
            var currentELapsedTime = 0L
            if (isRunning) {
                currentELapsedTime = System.currentTimeMillis() - startTime
            }
            return previousElapsedTime + currentELapsedTime
        }
    }

    @SuppressLint("DefaultLocale")
    override fun toString(): String {
        return String.format("%d millis", getElapsedTime())
    }
}