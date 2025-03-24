Here's the step-by-step implementation plan broken into LLM prompts:

# Phase 1: Core Infrastructure
## Prompt 1: Project Setup and Dependency Management
In this new android project, add the UsbSerial library dependency (com.github.mik3y:usb-serial-android). Set up a basic activity with a vertical LinearLayout containing TextViews for: 
- Connection status
- Serial port name
- Current trigger number
- Wall time
- App time
Include a handler for USB permission requests. Create empty placeholder methods for:
- Serial port detection
- Connection management
- Trigger sending
- Time display updates

## Prompt 2: Serial Port Detection
```text
Implement automatic USB serial port detection:
1. Create a UsbManager instance
2. Filter connected devices for FTDI, CDC, and CP21xx interfaces
3. Select first available device
4. Display "Found device: [device name]" in status
5. Handle no-device scenario with "No serial device found" error
6. Add retry mechanism every 5 seconds
Include test cases for:
- Device found scenario
- No devices connected
- Multiple devices present
```

# Phase 2: Core Functionality
## Prompt 3: Serial Connection Management
```text
Implement serial connection lifecycle:
1. Request USB permission when device detected
2. Open connection at 9600 baud using 8N1 configuration
3. Handle connection success/failure callbacks
4. Implement keep-alive ping every 500ms
5. Detect disconnection via write errors
6. Add auto-reconnect logic with 5s interval
Test scenarios:
- Successful connection
- Permission denied
- Cable unplugged during operation
- Reconnection after failure
```

## Prompt 4: Trigger Sequence Implementation
```text
Create a thread-safe trigger generator:
1. Implement circular counter (1-256)
2. Add AtomicInteger for thread-safe increments
3. Create ScheduledExecutorService for 1s intervals
4. On each tick:
   a. Get next trigger value
   b. Convert to unsigned byte
   c. Write to serial port
   d. Update UI with new value
5. Handle serial write failures by stopping scheduler
Test cases:
- Sequence wraps at 256
- Timing accuracy ±50ms
- Thread safety under load
```

# Phase 3: Time Tracking
## Prompt 5: Time Display System
```text
Implement dual time tracking:
1. Wall time using System.currentTimeMillis()
2. App time using SystemClock.elapsedRealtime()
3. Create Handler with 100ms update interval
4. Format times as:
   - Wall: "HH:mm:ss.SSS"
   - App: "ss.SSS"s"
5. Synchronize time updates with trigger events
Add tests for:
- Time formatting correctness
- Update frequency
- Drift detection between clock sources
```

# Phase 4: Error Handling
## Prompt 6: Connection Error Management
```text
Implement error handling system:
1. Create error state machine with:
   - CONNECTED
   - DISCONNECTED
   - ERROR
2. Map error codes to user messages
3. Add visual indicators (color changes)
4. Implement exponential backoff for retries
5. Log errors to file with timestamps
Test cases:
- Simulated write failures
- Permission denial recovery
- Multiple quick disconnect/reconnect events
```

# Phase 5: Final Integration
## Prompt 7: System Integration
```text
Wire all components together:
1. Connect serial detection to connection manager
2. Link trigger generator to serial writer
3. Synchronize time displays with trigger events
4. Implement state restoration on configuration changes
5. Add purge buffer before/after each write
6. Verify end-to-en timing constraints
Final test scenarios:
- Full 256-cycle test
- Connection loss during sequence
- 24-hour stability test
- Cross-device compatibility check
```

Each phase builds on the previous with incremental complexity. The test cases ensure safety while maintaining progress. Let me know if you need adjustments to any component's scope or test coverage.
```