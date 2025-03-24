Here's a comprehensive `todo.md` checklist organized by implementation phase:

```markdown
# Serial Trigger Test App Checklist

## Phase 1: Core Infrastructure
### Project Setup
- [ ] Create new Android project (min API 26)
- [ ] Add UsbSerial dependency to build.gradle
- [ ] Implement basic activity layout with:
  - [ ] Connection status TextView
  - [ ] Serial port name TextView
  - [ ] Trigger number TextView
  - [ ] Wall time TextView
  - [ ] App time TextView
- [ ] Create USB permission request handler
- [ ] Add placeholder methods stubs

### Serial Detection
- [ ] Implement UsbManager initialization
- [ ] Create device detection logic for:
  - [ ] FTDI devices
  - [ ] CDC devices
  - [ ] CP21xx devices
- [ ] Implement device selection logic
- [ ] Add no-device error handling
- [ ] Create 5-second retry loop
- [ ] Test cases:
  - [ ] Single device detection
  - [ ] No devices connected
  - [ ] Multiple devices present

## Phase 2: Core Functionality
### Connection Management
- [ ] Implement USB permission workflow
- [ ] Create serial connection with 9600 baud/8N1
- [ ] Add connection state callbacks
- [ ] Implement keep-alive ping system
- [ ] Develop disconnection detection
- [ ] Create auto-reconnect logic
- [ ] Test cases:
  - [ ] Successful connection lifecycle
  - [ ] Permission denial recovery
  - [ ] Cable unplug/replug during operation

### Trigger System
- [ ] Implement circular 1-256 counter
- [ ] Add AtomicInteger for thread safety
- [ ] Create ScheduledExecutorService
- [ ] Build serial write logic
- [ ] Connect UI update triggers
- [ ] Test cases:
  - [ ] Sequence wrap at 256
  - [ ] Timing accuracy validation
  - [ ] Concurrent access stress test

## Phase 3: Time Tracking
### Time Display
- [ ] Implement wall time tracking
- [ ] Create app elapsed time counter
- [ ] Build Handler-based updater
- [ ] Format time displays:
  - [ ] HH:mm:ss.SSS (wall)
  - [ ] ss.SSS"s (app)
- [ ] Synchronize with trigger events
- [ ] Test cases:
  - [ ] Time formatting validation
  - [ ] Update frequency check
  - [ ] Clock drift detection

## Phase 4: Error Handling
### Error System
- [ ] Implement state machine:
  - [ ] CONNECTED
  - [ ] DISCONNECTED
  - [ ] ERROR
- [ ] Create error message mappings
- [ ] Add visual status indicators
- [ ] Implement exponential backoff
- [ ] Build error logging system
- [ ] Test cases:
  - [ ] Simulated write failures
  - [ ] Rapid disconnect/reconnect
  - [ ] Configuration error recovery

## Phase 5: Final Integration
### System Wiring
- [ ] Connect detection to connection mgmt
- [ ] Link trigger generator to serial IO
- [ ] Synchronize time displays
- [ ] Implement state preservation
- [ ] Add buffer purge logic
- [ ] Final test cases:
  - [ ] Full 256-cycle validation
  - [ ] Connection loss during sequence
  - [ ] 24-hour stability test
  - [ ] Cross-device compatibility

## Comprehensive Testing
- [ ] Verify all error messages are human-readable
- [ ] Test on multiple USB-serial adapters
- [ ] Validate timing under system load
- [ ] Check battery usage characteristics
- [ ] Verify no memory leaks
- [ ] Confirm correct behavior on:
  - [ ] App minimize/restore
  - [ ] Device rotation
  - [ ] Low battery conditions
  - [ ] Flight mode toggle

## Deployment Prep
- [ ] Build release APK
- [ ] Create install documentation
- [ ] Write user quick-start guide
- [ ] Prepare test report template
- [ ] Generate validation checklist
```

This checklist follows the implementation plan while adding practical verification steps. Each checkbox represents a testable, deliverable unit of work. Mark items as complete only after passing their associated test cases.