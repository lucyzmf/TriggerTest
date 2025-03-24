package dev.lucy.myapplication

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.HashMap

@RunWith(MockitoJUnitRunner::class)
class UsbDetectionTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockUsbManager: UsbManager
    
    @Mock
    private lateinit var mockFtdiDevice: UsbDevice
    
    @Mock
    private lateinit var mockCp210xDevice: UsbDevice
    
    @Mock
    private lateinit var mockUnsupportedDevice: UsbDevice
    
    private lateinit var deviceMap: HashMap<String, UsbDevice>
    
    @Before
    fun setup() {
        // Set up FTDI device
        `when`(mockFtdiDevice.vendorId).thenReturn(0x0403) // FTDI
        `when`(mockFtdiDevice.productId).thenReturn(0x6001) // FT232R
        `when`(mockFtdiDevice.deviceName).thenReturn("FTDI FT232R")
        
        // Set up CP210x device
        `when`(mockCp210xDevice.vendorId).thenReturn(0x10C4) // Silicon Labs
        `when`(mockCp210xDevice.productId).thenReturn(0xEA60) // CP2102
        `when`(mockCp210xDevice.deviceName).thenReturn("CP2102 USB to UART")
        
        // Set up unsupported device
        `when`(mockUnsupportedDevice.vendorId).thenReturn(0x1234) // Random unsupported vendor
        `when`(mockUnsupportedDevice.productId).thenReturn(0x5678)
        `when`(mockUnsupportedDevice.deviceName).thenReturn("Unsupported Device")
        
        // Set up context to return USB manager
        `when`(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(mockUsbManager)
    }
    
    @Test
    fun testNoDevicesConnected() {
        // Empty device map
        deviceMap = HashMap()
        `when`(mockUsbManager.deviceList).thenReturn(deviceMap)
        
        // Create a detector that will use our mocked objects
        val detector = UsbDeviceDetector(mockContext)
        val result = detector.findSupportedDevice()
        
        // Verify no device was found
        assert(result == null)
    }
    
    @Test
    fun testSingleDeviceFound() {
        // Map with one supported device
        deviceMap = HashMap()
        deviceMap["FTDI"] = mockFtdiDevice
        `when`(mockUsbManager.deviceList).thenReturn(deviceMap)
        
        // Create a detector that will use our mocked objects
        val detector = UsbDeviceDetector(mockContext)
        val result = detector.findSupportedDevice()
        
        // Verify the FTDI device was found
        assert(result == mockFtdiDevice)
        assert(result?.vendorId == 0x0403)
    }
    
    @Test
    fun testMultipleDevicesPresent() {
        // Map with multiple devices
        deviceMap = HashMap()
        deviceMap["FTDI"] = mockFtdiDevice
        deviceMap["CP210x"] = mockCp210xDevice
        deviceMap["Unsupported"] = mockUnsupportedDevice
        `when`(mockUsbManager.deviceList).thenReturn(deviceMap)
        
        // Create a detector that will use our mocked objects
        val detector = UsbDeviceDetector(mockContext)
        val result = detector.findSupportedDevice()
        
        // Verify a supported device was found (should be the first one)
        assert(result != null)
        assert(result?.vendorId == 0x0403 || result?.vendorId == 0x10C4)
        assert(result != mockUnsupportedDevice)
    }
    
    @Test
    fun testOnlyUnsupportedDevicePresent() {
        // Map with only unsupported device
        deviceMap = HashMap()
        deviceMap["Unsupported"] = mockUnsupportedDevice
        `when`(mockUsbManager.deviceList).thenReturn(deviceMap)
        
        // Create a detector that will use our mocked objects
        val detector = UsbDeviceDetector(mockContext)
        val result = detector.findSupportedDevice()
        
        // Verify no supported device was found
        assert(result == null)
    }
}

/**
 * Simple class to encapsulate USB device detection logic for testing
 */
class UsbDeviceDetector(private val context: Context) {
    
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
    
    fun findSupportedDevice(): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        
        if (deviceList.isEmpty()) {
            return null
        }
        
        // Filter for supported USB serial devices
        val supportedDevices = deviceList.values.filter { device ->
            SUPPORTED_VENDORS.contains(device.vendorId)
        }
        
        return supportedDevices.firstOrNull()
    }
}
