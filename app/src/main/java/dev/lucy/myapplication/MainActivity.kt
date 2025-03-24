package dev.lucy.myapplication

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    // UI elements
    private lateinit var connectionStatusTextView: TextView
    private lateinit var serialPortNameTextView: TextView
    private lateinit var triggerNumberTextView: TextView
    private lateinit var wallTimeTextView: TextView
    private lateinit var appTimeTextView: TextView
    
    // USB related variables
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private val ACTION_USB_PERMISSION = "dev.lucy.myapplication.USB_PERMISSION"
    
    // Handler for updating time
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTimeDisplays()
            handler.postDelayed(this, 1000) // Update every second
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
                            updateConnectionStatus("USB Permission denied")
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI elements
        connectionStatusTextView = findViewById(R.id.connection_status)
        serialPortNameTextView = findViewById(R.id.serial_port_name)
        triggerNumberTextView = findViewById(R.id.trigger_number)
        wallTimeTextView = findViewById(R.id.wall_time)
        appTimeTextView = findViewById(R.id.app_time)
        
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
        
        // Set up the send trigger button
        findViewById<Button>(R.id.send_trigger_button).setOnClickListener {
            // Send a trigger with an incremented number
            val currentTrigger = triggerNumberTextView.text.toString()
                .replace("Trigger: ", "").toIntOrNull() ?: 0
            sendTrigger(currentTrigger + 1)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver and stop handler
        unregisterReceiver(usbPermissionReceiver)
        handler.removeCallbacks(timeUpdateRunnable)
    }
    
    // Method for serial port detection
    private fun detectSerialPorts() {
        updateConnectionStatus("Searching for devices...")
        
        // Get the list of connected USB devices
        val deviceList = usbManager.deviceList
        
        if (deviceList.isEmpty()) {
            updateConnectionStatus("No USB devices found")
            return
        }
        
        // Look for the first available USB device
        for ((_, device) in deviceList) {
            usbDevice = device
            updateConnectionStatus("Device found: ${device.deviceName}")
            updateSerialPortName(device.deviceName)
            
            // Request permission for the device
            requestUsbPermission(device)
            break
        }
    }
    
    // Method for connection management
    private fun connectToSerialPort(device: UsbDevice) {
        updateConnectionStatus("Connecting to ${device.deviceName}...")
        
        // In a real implementation, you would:
        // 1. Get a UsbDeviceConnection from the UsbManager
        // 2. Find the appropriate interface and endpoint
        // 3. Create a UsbSerialDevice instance
        // 4. Open the connection and set parameters (baud rate, etc.)
        
        // For now, we'll just simulate a successful connection
        updateConnectionStatus("Connected to ${device.deviceName}")
        
        // In a real implementation, you would start reading from the serial port here
    }
    
    // Method for trigger sending
    private fun sendTrigger(triggerNumber: Int) {
        if (usbDevice == null) {
            updateConnectionStatus("Cannot send trigger: No device connected")
            return
        }
        
        // In a real implementation, you would:
        // 1. Format the trigger command as a byte array
        // 2. Write the bytes to the serial port
        
        // For now, we'll just update the UI
        updateTriggerNumber(triggerNumber)
        updateConnectionStatus("Trigger ${triggerNumber} sent")
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
    
    private fun updateSerialPortName(name: String) {
        serialPortNameTextView.text = "Port: $name"
    }
    
    private fun updateTriggerNumber(number: Int) {
        triggerNumberTextView.text = "Trigger: $number"
    }
    
    private fun updateTimeDisplays() {
        // Update wall time
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        wallTimeTextView.text = "Wall Time: $currentTime"
        
        // Update app time (placeholder)
        appTimeTextView.text = "App Time: $currentTime"
    }
}
