package dev.lucy.myapplication

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe trigger generator that produces trigger values in a circular sequence (1-256).
 */
class TriggerGenerator(private val triggerListener: TriggerListener) {
    
    // Interface for trigger event callbacks
    interface TriggerListener {
        fun onTriggerGenerated(triggerValue: Int)
        fun onTriggerError(error: String)
    }
    
    // Thread-safe counter for trigger values
    private val triggerCounter = AtomicInteger(0)
    
    // Scheduler for periodic trigger generation
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var scheduledFuture: ScheduledFuture<*>? = null
    
    // Flag to track if the generator is running
    @Volatile
    private var isRunning = false
    
    /**
     * Start generating triggers at 1-second intervals.
     */
    fun start() {
        if (isRunning) {
            println("TriggerGenerator: Already running, ignoring start() call.")
            return
        }

        println("TriggerGenerator: Starting trigger generator...")

        scheduledFuture = scheduler.scheduleWithFixedDelay(
            { generateTrigger() },
            0,
            1000,
            TimeUnit.MILLISECONDS
        )
        isRunning = true

    }

    /**
     * Stop generating triggers.
     */
    fun stop() {
        if (!isRunning) return
        
        scheduledFuture?.cancel(false)
        isRunning = false
    }
    
    /**
     * Clean up resources when no longer needed.
     */
    fun shutdown() {
        stop()
        scheduler.shutdown()
    }
    
    /**
     * Generate the next trigger value and notify the listener.
     */
    private fun generateTrigger() {
        try {
            // Record the exact time this trigger was generated
            val triggerTime = System.currentTimeMillis()

            // Atomically update the counter and wrap properly
            if (triggerCounter.compareAndSet(256, 0)) {
                triggerCounter.set(1)
            } else {
                triggerCounter.incrementAndGet()
            }

            val triggerValue = triggerCounter.get()

            // Notify listener of the new trigger value
            triggerListener.onTriggerGenerated(triggerValue)

            // Log the trigger time for debugging
            println("TriggerGenerator: Trigger $triggerValue generated at $triggerTime")
        } catch (e: Exception) {
            // Handle any errors
            triggerListener.onTriggerError("Error generating trigger: ${e.message}")
            stop()
        }
    }
    
    /**
     * Get the current trigger value without incrementing.
     */
    fun getCurrentTriggerValue(): Int {
        val value = triggerCounter.get() % 256
        return if (value == 0) 256 else value
    }
    
    /**
     * Set the current trigger value.
     */
    fun setCurrentValue(value: Int) {
        if (value in 1..256) {
            triggerCounter.set(if (value == 256) 0 else value)
        }
    }
    
    /**
     * Check if the generator is currently running.
     */
    fun isRunning(): Boolean {
        return isRunning
    }
    
    /**
     * Reset the trigger counter to 0.
     */
    fun reset() {
        triggerCounter.set(0)
    }
}
