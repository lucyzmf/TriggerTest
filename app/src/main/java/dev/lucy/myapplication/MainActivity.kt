package dev.lucy.myapplication

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TriggerGenerator.TriggerListener {
    
    // UI elements
    private lateinit var connectionStatusTextView: TextView
    private lateinit var serialPortNameTextView: TextView
    private lateinit var triggerNumberTextView: TextView
    private lateinit var wallTimeTextView: TextView
    private lateinit var appTimeTextView: TextView
    private lateinit var sendTriggerButton: Button
    private lateinit var statusIndicator: View
    
    // USB related variables
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null
    private val ACTION_USB_PERMISSION = "dev.lucy.myapplication.USB_PERMISSION"
    
    // Error handling
    private lateinit var errorManager: ConnectionErrorManager
    
    // Handlers for updating time and device detection
    private val handler = Handler(Looper.getMainLooper())
    private val deviceDetectionHandler = Handler(Looper.getMainLooper())
    
    // Trigger generator
    private lateinit var triggerGenerator: TriggerGenerator
    
    // Time tracking variables
    private val appStartTime = SystemClock.elapsedRealtime()
    private var lastTriggerTime = 0L
    private var lastTriggerWallTime = 0L
    private var lastTriggerAppTime = 0L
    
    // Time update runnable with faster refresh rate
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTimeDisplays()
            handler.postDelayed(this, 100) // Update every 100ms for smoother display
        }
    }
    
    // Runnable for periodic device detection
    private val deviceDetectionRunnable = object : Runnable {
        override fun run() {
            if (usbDevice == null) {
                detectSerialPorts()
            }
            deviceDetectionHandler.postDelayed(this, 5000) // Retry every 5 seconds
        }
    }
    
    // USB broadcast receiver for permission, attachment, and detachment
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(
                            UsbManager.EXTRA_DEVICE,
                            UsbDevice::class.java
                        )
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                // Permission granted, proceed with connection
                                connectToSerialPort(it)
                            }
                        } else {
                            // Permission denied
                            errorManager.setState(
                                ConnectionErrorManager.ConnectionState.ERROR,
                                ConnectionErrorManager.ErrorCode.PERMISSION_DENIED
                            )
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java
                    )
                    device?.let {
                        updateConnectionStatus("USB Device attached: ${it.deviceName}")
                        usbDevice = it
                        requestUsbPermission(it)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java
                    )
                    device?.let {
                        if (it == usbDevice) {
                            updateConnectionStatus("USB Device detached: ${it.deviceName}")
                            updateSerialPortName("None")
                            usbDevice = null
                        }
                    }
                }
            }
        }
    }
    
    // List of supported USB vendor IDs
    private val SUPPORTED_VENDORS = setOf(
        0x0403, // FTDI
        0x10C4, // Silicon Labs CP210x
        0x067B, // Prolific
        0x1A86, // QinHeng CH340/CH341
        0x2341, // Arduino
        0x16C0, // Teensyduino
        0x03EB, // Atmel Lufa
        0x1EAF, // Leaflabs
        0x0D28, // ARM mbed
        0x0483, // STMicroelectronics
        0x2E8A  // Raspberry Pi Pico
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI elements
        connectionStatusTextView = findViewById(R.id.connection_status)
        serialPortNameTextView = findViewById(R.id.serial_port_name)
        triggerNumberTextView = findViewById(R.id.trigger_number)
        wallTimeTextView = findViewById(R.id.wall_time)
        appTimeTextView = findViewById(R.id.app_time)
        sendTriggerButton = findViewById(R.id.send_trigger_button)
        statusIndicator = findViewById(R.id.status_indicator)
        
        // Initialize trigger generator
        triggerGenerator = TriggerGenerator(this)
        
        // Initialize error manager
        errorManager = ConnectionErrorManager(this)
        errorManager.setStateChangeListener { state, errorCode ->
            runOnUiThread {
                updateConnectionUI(state, errorCode)
                
                // Handle reconnection attempts
                if (state == ConnectionErrorManager.ConnectionState.DISCONNECTED) {
                    detectSerialPorts()
                }
            }
        }
        
        // Initialize USB manager
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        
        // Register USB permission receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        ContextCompat.registerReceiver(this, usbPermissionReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        
        // Check if the activity was started by a USB device being attached
        val intent = intent
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val attachedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            attachedDevice?.let {
                updateConnectionStatus("USB Device attached: ${it.deviceName}")
                usbDevice = it
                requestUsbPermission(it)
            }
        }
        
        // Start time updates
        handler.post(timeUpdateRunnable)
        
        // Initial UI setup
        updateConnectionStatus("Disconnected")
        updateSerialPortName("None")
        updateTriggerNumber(0)
        
        // Start USB device detection
        detectSerialPorts()
        
        // Start periodic device detection
        deviceDetectionHandler.post(deviceDetectionRunnable)
        
        // Set up the send trigger button
        findViewById<Button>(R.id.send_trigger_button).setOnClickListener {
            if (triggerGenerator.getCurrentTriggerValue() == 0) {
                // Start the trigger sequence
                triggerGenerator.start()
                findViewById<Button>(R.id.send_trigger_button).text = "Stop Triggers"
            } else {
                // Stop the trigger sequence
                triggerGenerator.stop()
                findViewById<Button>(R.id.send_trigger_button).text = "Start Triggers"
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver and stop handlers
        unregisterReceiver(usbPermissionReceiver)
        handler.removeCallbacks(timeUpdateRunnable)
        deviceDetectionHandler.removeCallbacks(deviceDetectionRunnable)
        
        // Clean up trigger generator
        triggerGenerator.shutdown()
        
        // Close USB connection
        closeSerialPort()
    }
    
    // Method for serial port detection
    private fun detectSerialPorts() {
        updateConnectionStatus("Searching for devices...")
        
        // Get the list of connected USB devices
        val deviceList = usbManager.deviceList
        
        if (deviceList.isEmpty()) {
            errorManager.setState(
                ConnectionErrorManager.ConnectionState.DISCONNECTED,
                ConnectionErrorManager.ErrorCode.DEVICE_NOT_FOUND
            )
            return
        }
        
        // Filter for supported USB serial devices
        val supportedDevices = deviceList.values.filter { device ->
            SUPPORTED_VENDORS.contains(device.vendorId)
        }
        
        if (supportedDevices.isEmpty()) {
            errorManager.setState(
                ConnectionErrorManager.ConnectionState.DISCONNECTED,
                ConnectionErrorManager.ErrorCode.DEVICE_NOT_FOUND
            )
            return
        }
        
        // Log all found devices for testing purposes
        for (device in supportedDevices) {
            android.util.Log.d("USB_DETECTION", 
                "Found device: ${device.deviceName}, " +
                "VendorID: ${device.vendorId}, " +
                "ProductID: ${device.productId}")
        }
        
        // Select the first available supported device
        val selectedDevice = supportedDevices.first()
        usbDevice = selectedDevice
        
        val deviceInfo = "Found device: ${selectedDevice.deviceName} " +
                         "(VID: ${selectedDevice.vendorId.toString(16)}, " +
                         "PID: ${selectedDevice.productId.toString(16)})"
        
        updateConnectionStatus(deviceInfo)
        updateSerialPortName(selectedDevice.deviceName)
        
        // Request permission for the device
        requestUsbPermission(selectedDevice)
    }
    
    // Method for connection management
    private fun connectToSerialPort(device: UsbDevice) {
        updateConnectionStatus("Connecting to ${device.deviceName}...")
        
        try {
            // Find all available drivers for the attached device
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                errorManager.setState(
                    ConnectionErrorManager.ConnectionState.ERROR,
                    ConnectionErrorManager.ErrorCode.DEVICE_NOT_FOUND
                )
                return
            }
            
            // Open a connection to the first available driver
            val driver = availableDrivers[0]
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                errorManager.setState(
                    ConnectionErrorManager.ConnectionState.ERROR,
                    ConnectionErrorManager.ErrorCode.CONNECTION_FAILED
                )
                return
            }
            
            // Get the first port (most devices have just one)
            val port = driver.ports[0]
            
            // Open the port and configure it
            try {
                port.open(connection)
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                
                // Store the connection and port for later use
                usbConnection = connection
                usbSerialPort = port
                
                // Update state to connected
                errorManager.setState(ConnectionErrorManager.ConnectionState.CONNECTED)
                
                updateConnectionStatus("Connected to ${device.deviceName}")
                updateSerialPortName("${driver.javaClass.simpleName} (${port.portNumber})")
            } catch (e: IOException) {
                errorManager.setState(
                    ConnectionErrorManager.ConnectionState.ERROR,
                    ConnectionErrorManager.ErrorCode.PORT_BUSY
                )
                try {
                    port.close()
                } catch (e2: IOException) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            errorManager.setState(
                ConnectionErrorManager.ConnectionState.ERROR,
                ConnectionErrorManager.ErrorCode.UNKNOWN_ERROR
            )
        }
    }
    
    // Close the serial port connection
    private fun closeSerialPort() {
        usbSerialPort?.let {
            try {
                it.close()
            } catch (e: IOException) {
                // Ignore
            }
            usbSerialPort = null
        }
        
        usbConnection = null
    }
    
    // Method for trigger sending
    private fun sendTrigger(triggerNumber: Int) {
        if (usbSerialPort == null) {
            errorManager.setState(
                ConnectionErrorManager.ConnectionState.DISCONNECTED,
                ConnectionErrorManager.ErrorCode.DEVICE_NOT_FOUND
            )
            return
        }
        
        try {
            // Record timestamps before sending trigger
            lastTriggerTime = System.currentTimeMillis()
            lastTriggerWallTime = lastTriggerTime
            lastTriggerAppTime = SystemClock.elapsedRealtime()
            
            // Convert trigger number to a byte
            val triggerByte = byteArrayOf(triggerNumber.toByte())
            
            // Write the byte to the serial port
            usbSerialPort?.write(triggerByte, 1000)
            
            // Update UI
            updateTriggerNumber(triggerNumber)
            updateConnectionStatus("Trigger ${triggerNumber} sent at ${formatTimestamp(lastTriggerTime)}")
            
            // Force immediate time display update
            updateTimeDisplays()
        } catch (e: IOException) {
            errorManager.setState(
                ConnectionErrorManager.ConnectionState.ERROR,
                ConnectionErrorManager.ErrorCode.WRITE_FAILED
            )
            triggerGenerator.stop()
            updateSendTriggerButtonState(false)
        }
    }
    
    // Helper method to format timestamps consistently
    private fun formatTimestamp(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timeMillis))
    }
    
    // Method to request USB permission
    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    // UI update methods
    private fun updateConnectionStatus(status: String) {
        connectionStatusTextView.text = "Status: $status"
    }
    
    // Update UI based on connection state
    private fun updateConnectionUI(state: ConnectionErrorManager.ConnectionState, errorCode: ConnectionErrorManager.ErrorCode) {
        when (state) {
            ConnectionErrorManager.ConnectionState.CONNECTED -> {
                connectionStatusTextView.setTextColor(Color.GREEN)
                statusIndicator.setBackgroundColor(Color.GREEN)
                updateConnectionStatus("Connected")
                updateSendTriggerButtonState(true)
            }
            ConnectionErrorManager.ConnectionState.DISCONNECTED -> {
                connectionStatusTextView.setTextColor(Color.GRAY)
                statusIndicator.setBackgroundColor(Color.GRAY)
                updateConnectionStatus("Disconnected: ${errorCode.message}")
                updateSendTriggerButtonState(false)
            }
            ConnectionErrorManager.ConnectionState.ERROR -> {
                connectionStatusTextView.setTextColor(Color.RED)
                statusIndicator.setBackgroundColor(Color.RED)
                updateConnectionStatus("Error: ${errorCode.message}")
                updateSendTriggerButtonState(false)
            }
        }
    }
    
    // Update send trigger button state
    private fun updateSendTriggerButtonState(enabled: Boolean) {
        sendTriggerButton.isEnabled = enabled
        if (!enabled && triggerGenerator.getCurrentTriggerValue() != 0) {
            triggerGenerator.stop()
            sendTriggerButton.text = "Start Triggers"
        }
    }
    
    private fun updateSerialPortName(name: String) {
        serialPortNameTextView.text = "Port: $name"
    }
    
    private fun updateTriggerNumber(number: Int) {
        triggerNumberTextView.text = "Trigger: $number"
    }
    
    private fun updateTimeDisplays() {
        // Update wall time using System.currentTimeMillis()
        val wallTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val formattedWallTime = dateFormat.format(Date(wallTimeMillis))
        wallTimeTextView.text = "Wall Time: $formattedWallTime"
        
        // Update app time using SystemClock.elapsedRealtime()
        val appTimeMillis = SystemClock.elapsedRealtime() - appStartTime
        val seconds = appTimeMillis / 1000
        val millis = appTimeMillis % 1000
        appTimeTextView.text = String.format("App Time: %d.%03d s", seconds, millis)
        
        // Calculate and log drift if we have trigger timestamps
        if (lastTriggerTime > 0) {
            val currentWallTime = System.currentTimeMillis()
            val currentAppTime = SystemClock.elapsedRealtime()
            
            val wallTimeDelta = currentWallTime - lastTriggerWallTime
            val appTimeDelta = currentAppTime - lastTriggerAppTime
            val drift = wallTimeDelta - appTimeDelta
            
            if (Math.abs(drift) > 10) { // Only log significant drift (>10ms)
                android.util.Log.d("TIME_DRIFT", 
                    "Drift detected: ${drift}ms (Wall: ${wallTimeDelta}ms, App: ${appTimeDelta}ms)")
            }
        }
    }
    
    // TriggerGenerator.TriggerListener implementation
    override fun onTriggerGenerated(triggerValue: Int) {
        runOnUiThread {
            sendTrigger(triggerValue)
        }
    }
    
    override fun onTriggerError(error: String) {
        runOnUiThread {
            updateConnectionStatus(error)
        }
    }
}
