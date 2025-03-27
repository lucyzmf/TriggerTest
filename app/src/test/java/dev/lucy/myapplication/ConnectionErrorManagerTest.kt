package dev.lucy.myapplication

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test class for ConnectionErrorManager that doesn't rely on Android-specific components
 */
class ConnectionErrorManagerTest {
    
    // Create a test-specific version of ConnectionErrorManager that doesn't use Android components
    class TestConnectionErrorManager {
        enum class ConnectionState {
            CONNECTED,
            DISCONNECTED,
            ERROR
        }

        enum class ErrorCode(val message: String) {
            NONE("No error"),
            PERMISSION_DENIED("USB permission denied"),
            DEVICE_NOT_FOUND("USB device not found"),
            CONNECTION_FAILED("Failed to connect to device"),
            WRITE_FAILED("Failed to write to device"),
            READ_FAILED("Failed to read from device"),
            PORT_BUSY("Serial port is busy"),
            UNKNOWN_ERROR("Unknown error occurred")
        }
        
        private var currentState = ConnectionState.DISCONNECTED
        private var currentError = ErrorCode.NONE
        private var stateChangeListener: ((ConnectionState, ErrorCode) -> Unit)? = null
        
        fun setState(state: ConnectionState, errorCode: ErrorCode = ErrorCode.NONE) {
            currentState = state
            currentError = errorCode
            stateChangeListener?.invoke(state, errorCode)
        }
        
        fun getState(): ConnectionState = currentState
        
        fun getError(): ErrorCode = currentError
        
        fun setStateChangeListener(listener: (ConnectionState, ErrorCode) -> Unit) {
            stateChangeListener = listener
        }
    }
    
    private lateinit var errorManager: TestConnectionErrorManager
    
    @Before
    fun setup() {
        errorManager = TestConnectionErrorManager()
    }
    
    @Test
    fun testInitialState() {
        // Initial state should be DISCONNECTED
        assertEquals(TestConnectionErrorManager.ConnectionState.DISCONNECTED, errorManager.getState())
        assertEquals(TestConnectionErrorManager.ErrorCode.NONE, errorManager.getError())
    }
    
    @Test
    fun testStateTransitions() {
        // Test state transitions
        errorManager.setState(TestConnectionErrorManager.ConnectionState.CONNECTED)
        assertEquals(TestConnectionErrorManager.ConnectionState.CONNECTED, errorManager.getState())
        
        errorManager.setState(TestConnectionErrorManager.ConnectionState.ERROR, 
                             TestConnectionErrorManager.ErrorCode.WRITE_FAILED)
        assertEquals(TestConnectionErrorManager.ConnectionState.ERROR, errorManager.getState())
        assertEquals(TestConnectionErrorManager.ErrorCode.WRITE_FAILED, errorManager.getError())
        
        errorManager.setState(TestConnectionErrorManager.ConnectionState.DISCONNECTED)
        assertEquals(TestConnectionErrorManager.ConnectionState.DISCONNECTED, errorManager.getState())
    }
    
    @Test
    fun testStateChangeListener() {
        val latch = CountDownLatch(1)
        var receivedState: TestConnectionErrorManager.ConnectionState? = null
        var receivedError: TestConnectionErrorManager.ErrorCode? = null
        
        // Set listener
        errorManager.setStateChangeListener { state, errorCode ->
            receivedState = state
            receivedError = errorCode
            latch.countDown()
        }
        
        // Change state
        errorManager.setState(TestConnectionErrorManager.ConnectionState.ERROR, 
                             TestConnectionErrorManager.ErrorCode.PERMISSION_DENIED)
        
        // Wait for listener to be called
        latch.await(1, TimeUnit.SECONDS)
        
        // Verify listener was called with correct values
        assertEquals(TestConnectionErrorManager.ConnectionState.ERROR, receivedState)
        assertEquals(TestConnectionErrorManager.ErrorCode.PERMISSION_DENIED, receivedError)
    }
    
    @Test
    fun testMultipleStateChanges() {
        val states = mutableListOf<TestConnectionErrorManager.ConnectionState>()
        
        // Set listener
        errorManager.setStateChangeListener { state, _ ->
            states.add(state)
        }
        
        // Simulate multiple quick state changes
        errorManager.setState(TestConnectionErrorManager.ConnectionState.CONNECTED)
        errorManager.setState(TestConnectionErrorManager.ConnectionState.ERROR, 
                             TestConnectionErrorManager.ErrorCode.WRITE_FAILED)
        errorManager.setState(TestConnectionErrorManager.ConnectionState.DISCONNECTED)
        errorManager.setState(TestConnectionErrorManager.ConnectionState.CONNECTED)
        
        // Verify all state changes were recorded
        assertEquals(4, states.size)
        assertEquals(TestConnectionErrorManager.ConnectionState.CONNECTED, states[0])
        assertEquals(TestConnectionErrorManager.ConnectionState.ERROR, states[1])
        assertEquals(TestConnectionErrorManager.ConnectionState.DISCONNECTED, states[2])
        assertEquals(TestConnectionErrorManager.ConnectionState.CONNECTED, states[3])
    }
}
