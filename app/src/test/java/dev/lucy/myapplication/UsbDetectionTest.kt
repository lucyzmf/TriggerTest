package dev.lucy.myapplication

import org.junit.Test

/**
 * Simple test class for USB device detection logic
 */
class UsbDetectionTest {
    
    // Mock device class for testing
    data class MockUsbDevice(
        val deviceName: String,
        val vendorId: Int,
        val productId: Int
    )
    
    // Class that mimics the detection logic from MainActivity
    class UsbDeviceDetector {
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
        
        fun findSupportedDevice(devices: List<MockUsbDevice>): MockUsbDevice? {
            if (devices.isEmpty()) {
                return null
            }
            
            // Filter for supported USB serial devices
            val supportedDevices = devices.filter { device ->
                SUPPORTED_VENDORS.contains(device.vendorId)
            }
            
            return supportedDevices.firstOrNull()
        }
    }
    
    @Test
    fun testNoDevicesConnected() {
        val detector = UsbDeviceDetector()
        val devices = emptyList<MockUsbDevice>()
        val result = detector.findSupportedDevice(devices)
        
        // Verify no device was found
        assert(result == null)
    }
    
    @Test
    fun testSingleDeviceFound() {
        val detector = UsbDeviceDetector()
        val ftdiDevice = MockUsbDevice("FTDI FT232R", 0x0403, 0x6001)
        val devices = listOf(ftdiDevice)
        
        val result = detector.findSupportedDevice(devices)
        
        // Verify the FTDI device was found
        assert(result == ftdiDevice)
        assert(result?.vendorId == 0x0403)
    }
    
    @Test
    fun testMultipleDevicesPresent() {
        val detector = UsbDeviceDetector()
        val ftdiDevice = MockUsbDevice("FTDI FT232R", 0x0403, 0x6001)
        val cp210xDevice = MockUsbDevice("CP2102 USB to UART", 0x10C4, 0xEA60)
        val unsupportedDevice = MockUsbDevice("Unsupported Device", 0x1234, 0x5678)
        
        val devices = listOf(ftdiDevice, cp210xDevice, unsupportedDevice)
        val result = detector.findSupportedDevice(devices)
        
        // Verify a supported device was found (should be the first one)
        assert(result != null)
        assert(result?.vendorId == 0x0403)
        assert(result != unsupportedDevice)
    }
    
    @Test
    fun testOnlyUnsupportedDevicePresent() {
        val detector = UsbDeviceDetector()
        val unsupportedDevice = MockUsbDevice("Unsupported Device", 0x1234, 0x5678)
        
        val devices = listOf(unsupportedDevice)
        val result = detector.findSupportedDevice(devices)
        
        // Verify no supported device was found
        assert(result == null)
    }
}
