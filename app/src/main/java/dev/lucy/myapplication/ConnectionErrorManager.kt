package dev.lucy.myapplication

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

/**
 * Connection error state machine that handles USB connection errors
 * with exponential backoff for retries and error logging.
 */
class ConnectionErrorManager(private val context: Context) {

    // Connection states
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    // Error codes and their user-friendly messages
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

    // Current state and error
    @Volatile
    private var currentState = ConnectionState.DISCONNECTED
    private var currentError = ErrorCode.NONE
    
    // Retry counter and max retries
    private val retryCount = AtomicInteger(0)
    private val maxRetries = 5
    
    // Listeners
    private var stateChangeListener: ((ConnectionState, ErrorCode) -> Unit)? = null
    
    // Log file
    private val logFile: File by lazy {
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        File(logDir, "connection_errors.log")
    }
    
    /**
     * Set the connection state and error code
     */
    fun setState(state: ConnectionState, errorCode: ErrorCode = ErrorCode.NONE) {
        // Prevent redundant state changes
        if (currentState == state && currentError == errorCode) {
            Log.w(TAG, "Ignoring redundant state update: state=$state, errorCode=$errorCode")
            return
        }

        val oldState = currentState
        currentState = state
        currentError = errorCode

        // Log the state change
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "$timestamp: State changed from $oldState to $state with error: $errorCode"
        Log.d(TAG, logMessage)

        // Log to file if it's an error
        if (state == ConnectionState.ERROR) {
            logErrorToFile(logMessage)
        }

        // Notify listener
        stateChangeListener?.invoke(state, errorCode)

        // Handle retries if in error state and retry count is within limit
        if (state == ConnectionState.ERROR && retryCount.get() < maxRetries) {
            Log.d(TAG, "State is ERROR, scheduling retry.")
            scheduleRetry()
        } else if (state != ConnectionState.ERROR) {
            // Reset retry count when not in error state
            retryCount.set(0)
        }
    }
    
    /**
     * Get the current connection state
     */
    fun getState(): ConnectionState {
        return currentState
    }
    
    /**
     * Get the current error code
     */
    fun getError(): ErrorCode {
        return currentError
    }
    
    /**
     * Set a listener for state changes
     */
    fun setStateChangeListener(listener: (ConnectionState, ErrorCode) -> Unit) {
        stateChangeListener = listener
    }
    
    /**
     * Schedule a retry with exponential backoff
     */
    private fun scheduleRetry() {
        val currentRetryCount = retryCount.getAndIncrement()

        if (currentRetryCount >= maxRetries) {
            Log.w(TAG, "Maximum retry count reached ($maxRetries), giving up.")
            return
        }

        // Exponential backoff (Base delay: 1000ms, max 30s)
        val delayMs = min(
            (1000 * 2.0.pow(currentRetryCount.toDouble())).toLong(),
            30000
        )

        Log.d(TAG, "Retry attempt #${currentRetryCount + 1} scheduled in ${delayMs}ms")

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (currentState == ConnectionState.CONNECTED) {
                Log.d(TAG, "Connection restored before retry #${currentRetryCount + 1}, stopping retries.")
                return@postDelayed
            }
            Log.d(TAG, "Executing retry #${currentRetryCount + 1}")

            // Instead of triggering a state change immediately, we can attempt reconnection logic
            attemptReconnection()
        }, delayMs)
    }

    private fun tryReconnect(): Boolean {
        // TODO: Implement actual USB connection logic here
        Log.d(TAG, "Trying to reconnect to the USB device...")

        return false // Change to true when reconnection is successful
    }

    private fun attemptReconnection() {
        // Logic to check if reconnection is possible
        Log.d(TAG, "Attempting reconnection...")

        val success = tryReconnect()  // Replace with actual reconnection logic

        if (success) {
            setState(ConnectionState.CONNECTED)
        } else if (retryCount.get() < maxRetries) {
            scheduleRetry()
        } else {
            Log.w(TAG, "Final retry failed, remaining in ERROR state.")
        }
    }

    fun cancelRetries() {
        Log.d(TAG, "Cancelling all retry attempts.")
        retryCount.set(0)
    }
    /**
     * Log error to file
     */
    private fun logErrorToFile(message: String) {
        try {
            FileOutputStream(logFile, true).use { output ->
                output.write("$message\n".toByteArray())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    companion object {
        private const val TAG = "ConnectionErrorManager"
    }
}
