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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    
    // USB permission broadcast receiver
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
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
        registerReceiver(usbPermissionReceiver, filter)
        
        // Start time updates
        handler.post(timeUpdateRunnable)
        
        // Initial UI setup
        updateConnectionStatus("Disconnected")
        updateSerialPortName("None")
        updateTriggerNumber(0)
        
        // Start USB device detection
        detectSerialPorts()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver and stop handler
        unregisterReceiver(usbPermissionReceiver)
        handler.removeCallbacks(timeUpdateRunnable)
    }
    
    // Placeholder method for serial port detection
    private fun detectSerialPorts() {
        // TODO: Implement serial port detection
        updateConnectionStatus("Searching for devices...")
    }
    
    // Placeholder method for connection management
    private fun connectToSerialPort(device: UsbDevice) {
        // TODO: Implement connection to serial port
        updateConnectionStatus("Connecting...")
    }
    
    // Placeholder method for trigger sending
    private fun sendTrigger(triggerNumber: Int) {
        // TODO: Implement trigger sending
        updateTriggerNumber(triggerNumber)
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
