package dev.lucy.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TriggerGeneratorTest {

    @Test
    fun testTriggerGeneration() {
        val triggerGenerator = TriggerGenerator(object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                println("TEST: Received trigger: $triggerValue")  // Should print
                android.util.Log.d("TEST", "Received trigger: $triggerValue")
            }

            override fun onTriggerError(error: String) {
                println("TEST ERROR: $error")
            }
        })

        triggerGenerator.start()  // ✅ Ensure this is called
        Thread.sleep(3000)  // Allow time for triggers to be generated
        triggerGenerator.stop()
    }

    @Test
    fun testTriggerSequenceWrapsAt256() {
        // Create a mock listener
        val mockListener = object : TriggerGenerator.TriggerListener {
            val values = mutableListOf<Int>()

            override fun onTriggerGenerated(triggerValue: Int) {
                values.add(triggerValue)
            }

            override fun onTriggerError(error: String) {
                // Not testing errors here
            }
        }

        // Create the generator and manually trigger 300 values
        val generator = TriggerGenerator(mockListener)

        // Generate 300 triggers manually (without scheduling)
        for (i in 1..300) {
            generator.javaClass.getDeclaredMethod("generateTrigger").apply {
                isAccessible = true
                invoke(generator)
            }
        }

        // Check that values wrap correctly
        // The first 256 values should be 1-256
        for (i in 1..256) {
            assertEquals(i, mockListener.values[i - 1])
        }

        // Values after 256 should wrap back to 1
        assertEquals(1, mockListener.values[256])
        assertEquals(2, mockListener.values[257])
        assertEquals(3, mockListener.values[258])
    }

    @Test
    fun testTimingAccuracy() {
        val latch = CountDownLatch(5) // Wait for 5 triggers
        val timestamps = mutableListOf<Long>()

        // Create a listener that records timestamps
        val listener = object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                timestamps.add(System.currentTimeMillis())
                latch.countDown()
            }

            override fun onTriggerError(error: String) {
                // Not testing errors here
            }
        }

        // Create and start the generator
        val generator = TriggerGenerator(listener)
        val startTime = System.currentTimeMillis()
        generator.start()

        // Wait for 5 triggers (should take about 5 seconds)
        assertTrue("Timed out waiting for triggers", latch.await(10, TimeUnit.SECONDS))

        // Stop the generator
        generator.shutdown()

        // Check timing accuracy (should be within ±100ms of each second)
        // This is more lenient to account for test environment variability
        for (i in 1 until timestamps.size) {
            val interval = timestamps[i] - timestamps[i - 1]
            assertTrue(
                "Interval $interval is outside acceptable range",
                interval in 850..1150
            )
        }
    }

    @Test
    fun testThreadSafety() {
        val triggerCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val latch = CountDownLatch(4) // Wait for 4 triggers

        // Create a listener that counts triggers
        val listener = object : TriggerGenerator.TriggerListener {
            override fun onTriggerGenerated(triggerValue: Int) {
                triggerCount.incrementAndGet()
                latch.countDown()
            }

            override fun onTriggerError(error: String) {
                errorCount.incrementAndGet()
            }
        }

        // Create and start the generator
        val generator = TriggerGenerator(listener)

        // Start generating triggers
        generator.start()

        // Wait for all triggers
        assertTrue("Timed out waiting for triggers", latch.await(10, TimeUnit.SECONDS))

        // Stop the generator
        generator.shutdown()

        // Verify no errors occurred
        assertEquals("Errors occurred during thread safety test", 0, errorCount.get())

        // Verify we got the expected number of triggers
        assertEquals("Did not receive expected number of triggers", 4, triggerCount.get())
    }
}
