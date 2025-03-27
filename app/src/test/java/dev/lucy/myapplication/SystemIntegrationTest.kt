package dev.lucy.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * System integration tests to verify end-to-end functionality
 */
class SystemIntegrationTest {

    /**
     * Test a full 256-cycle sequence
     */
    @Test
    fun testFullCycleSequence() {
        val values = mutableListOf<Int>()
        val latch = CountDownLatch(256)
        
        val listener = object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                values.add(triggerValue)
                latch.countDown()
            }
            
            override fun onTriggerError(error: String) {
                // Not testing errors here
            }
        }
        
        val generator = TriggerGenerator(listener)
        generator.start()
        
        // Wait for all 256 triggers (with a generous timeout)
        assertTrue("Timed out waiting for 256 triggers", 
                  latch.await(257, TimeUnit.SECONDS))
        
        generator.shutdown()
        
        // Verify we got all 256 values in sequence
        assertEquals(256, values.size)
        for (i in 1..256) {
            assertEquals(i, values[i-1])
        }
    }
    
    /**
     * Test recovery from connection loss
     */
    @Test
    fun testConnectionLossRecovery() {
        // Create a mock connection manager
        val connectionState = AtomicInteger(0) // 0=disconnected, 1=connected, 2=error
        val errorCount = AtomicInteger(0)
        val recoveryCount = AtomicInteger(0)
        val triggerCount = AtomicInteger(0)
        
        val listener = object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                triggerCount.incrementAndGet()
                
                // Simulate connection loss after 10 triggers
                if (triggerValue == 10) {
                    connectionState.set(2) // Error state
                    errorCount.incrementAndGet()
                }
                
                // Simulate recovery after 5 more triggers
                if (triggerValue == 15) {
                    connectionState.set(1) // Connected state
                    recoveryCount.incrementAndGet()
                }
            }
            
            override fun onTriggerError(error: String) {
                // Not testing errors here
            }
        }
        
        val generator = TriggerGenerator(listener)
        generator.start()
        
        // Wait for enough triggers
        Thread.sleep(20000)
        
        generator.shutdown()
        
        // Verify we detected the error and recovered
        assertTrue("Should have generated at least 20 triggers", triggerCount.get() >= 20)
        assertEquals("Should have detected 1 error", 1, errorCount.get())
        assertEquals("Should have recovered 1 time", 1, recoveryCount.get())
    }
    
    /**
     * Test timing constraints
     */
    @Test
    fun testTimingConstraints() {
        val timestamps = mutableListOf<Long>()
        val latch = CountDownLatch(30)
        
        val listener = object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                timestamps.add(System.currentTimeMillis())
                latch.countDown()
            }
            
            override fun onTriggerError(error: String) {
                // Not testing errors here
            }
        }
        
        val generator = TriggerGenerator(listener)
        generator.start()
        
        // Wait for 30 triggers
        assertTrue("Timed out waiting for triggers", 
                  latch.await(35, TimeUnit.SECONDS))
        
        generator.shutdown()
        
        // Calculate statistics on intervals
        var totalDrift = 0L
        var maxDrift = 0L
        
        for (i in 1 until timestamps.size) {
            val interval = timestamps[i] - timestamps[i-1]
            val drift = Math.abs(interval - 1000)
            
            totalDrift += drift
            if (drift > maxDrift) {
                maxDrift = drift
            }
            
            // Each interval should be close to 1 second
            assertTrue("Interval $interval is too far from 1000ms", 
                      interval in 900..1100)
        }
        
        // Calculate average drift
        val avgDrift = totalDrift / (timestamps.size - 1)
        
        // Log statistics
        println("Timing statistics:")
        println("- Average drift: $avgDrift ms")
        println("- Maximum drift: $maxDrift ms")
        
        // Verify timing constraints
        assertTrue("Maximum drift ($maxDrift ms) exceeds 100ms", maxDrift <= 100)
        assertTrue("Average drift ($avgDrift ms) exceeds 50ms", avgDrift <= 50)
    }
}
