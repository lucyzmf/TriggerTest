package dev.lucy.myapplication

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConnectionErrorManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockFile: File
    
    @Mock
    private lateinit var mockExternalFilesDir: File
    
    private lateinit var errorManager: ConnectionErrorManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup mock context
        `when`(mockContext.getExternalFilesDir(null)).thenReturn(mockExternalFilesDir)
        `when`(mockExternalFilesDir.exists()).thenReturn(true)
        
        // Create error manager with mock context
        errorManager = ConnectionErrorManager(mockContext)
    }
    
    @Test
    fun testInitialState() {
        // Initial state should be DISCONNECTED
        assertEquals(ConnectionErrorManager.ConnectionState.DISCONNECTED, errorManager.getState())
        assertEquals(ConnectionErrorManager.ErrorCode.NONE, errorManager.getError())
    }
    
    @Test
    fun testStateTransitions() {
        // Test state transitions
        errorManager.setState(ConnectionErrorManager.ConnectionState.CONNECTED)
        assertEquals(ConnectionErrorManager.ConnectionState.CONNECTED, errorManager.getState())
        
        errorManager.setState(ConnectionErrorManager.ConnectionState.ERROR, 
                             ConnectionErrorManager.ErrorCode.WRITE_FAILED)
        assertEquals(ConnectionErrorManager.ConnectionState.ERROR, errorManager.getState())
        assertEquals(ConnectionErrorManager.ErrorCode.WRITE_FAILED, errorManager.getError())
        
        errorManager.setState(ConnectionErrorManager.ConnectionState.DISCONNECTED)
        assertEquals(ConnectionErrorManager.ConnectionState.DISCONNECTED, errorManager.getState())
    }
    
    @Test
    fun testStateChangeListener() {
        val latch = CountDownLatch(1)
        var receivedState: ConnectionErrorManager.ConnectionState? = null
        var receivedError: ConnectionErrorManager.ErrorCode? = null
        
        // Set listener
        errorManager.setStateChangeListener { state, errorCode ->
            receivedState = state
            receivedError = errorCode
            latch.countDown()
        }
        
        // Change state
        errorManager.setState(ConnectionErrorManager.ConnectionState.ERROR, 
                             ConnectionErrorManager.ErrorCode.PERMISSION_DENIED)
        
        // Wait for listener to be called
        latch.await(1, TimeUnit.SECONDS)
        
        // Verify listener was called with correct values
        assertEquals(ConnectionErrorManager.ConnectionState.ERROR, receivedState)
        assertEquals(ConnectionErrorManager.ErrorCode.PERMISSION_DENIED, receivedError)
    }
    
    @Test
    fun testMultipleStateChanges() {
        val states = mutableListOf<ConnectionErrorManager.ConnectionState>()
        
        // Set listener
        errorManager.setStateChangeListener { state, _ ->
            states.add(state)
        }
        
        // Simulate multiple quick state changes
        errorManager.setState(ConnectionErrorManager.ConnectionState.CONNECTED)
        errorManager.setState(ConnectionErrorManager.ConnectionState.ERROR, 
                             ConnectionErrorManager.ErrorCode.WRITE_FAILED)
        errorManager.setState(ConnectionErrorManager.ConnectionState.DISCONNECTED)
        errorManager.setState(ConnectionErrorManager.ConnectionState.CONNECTED)
        
        // Verify all state changes were recorded
        assertEquals(4, states.size)
        assertEquals(ConnectionErrorManager.ConnectionState.CONNECTED, states[0])
        assertEquals(ConnectionErrorManager.ConnectionState.ERROR, states[1])
        assertEquals(ConnectionErrorManager.ConnectionState.DISCONNECTED, states[2])
        assertEquals(ConnectionErrorManager.ConnectionState.CONNECTED, states[3])
    }
}
