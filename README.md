# USB Trigger Application

## Overview
This Android application provides a reliable way to send trigger signals via USB serial connections. It's designed for research and experimental setups where precise timing and reliable communication with external hardware is required.

## Features
- **USB Serial Communication**: Supports common USB-to-Serial adapters (FTDI, CP210x, CH340, etc.)
- **Automatic Device Detection**: Scans and connects to compatible USB devices
- **Trigger Sequence Generation**: Sends numbered triggers (1-256) at precise intervals
- **Dual Time Tracking**: Monitors both wall clock time and application time
- **Error Handling**: Comprehensive error detection with automatic recovery
- **Visual Feedback**: Status indicators for connection state and trigger events
- **State Preservation**: Maintains state across device rotations and configuration changes

## Requirements
- Android device with USB Host support
- Android 13.0 (API 33) or higher
- USB OTG adapter (if using with a device that has USB-C or micro-USB port)
- Compatible USB-to-Serial adapter

## Supported USB Serial Adapters
- FTDI (FT232R, FT2232H, FT4232H, FT232H, FT230X)
- Silicon Labs CP210x series
- Prolific PL2303 series
- CH340/CH341 converters
- CDC-ACM compliant devices (Arduino, Teensy, etc.)

## Usage
1. Connect a compatible USB serial adapter to your Android device
2. Launch the application
3. Grant USB permission when prompted
4. Once connected (indicated by green status), press "Start Triggers"
5. The application will send numbered triggers (1-256) at 1-second intervals
6. Press "Stop Triggers" to halt the sequence

## Technical Details
- Baud rate: 9600
- Data bits: 8
- Stop bits: 1
- Parity: None
- Trigger format: Single byte representing the trigger number (1-256)
- Buffer management: Automatic purging before/after each write

## Timing Precision
- Triggers are generated at 1-second intervals with <50ms average drift
- Time displays update at 100ms intervals for smooth visualization
- Timestamps are recorded with millisecond precision

## Error Handling
The application implements a state machine with three states:
- CONNECTED: Successfully connected to a USB device
- DISCONNECTED: No USB device connected
- ERROR: Connection error occurred

When errors occur, the application attempts to recover using exponential backoff.

## Development
This application is built using:
- Kotlin
- Android USB Host API
- USB Serial for Android library (https://github.com/mik3y/usb-serial-for-android)

## License
[MIT License](LICENSE)
