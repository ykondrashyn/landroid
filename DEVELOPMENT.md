# Development Notes

## Current Status

### M0: Baseline Verification - PARTIAL
- Android SDK not available in current environment (Android build not verified yet)

### M1: Desktop Bootstrap - COMPLETE
- Swing desktop window with 60 Hz loop and keyboard controls

### M2: Proper Separation & Reuse - COMPLETE (initial)
- New `:core-sim` module with Vec2, Physics, Universe, Autopilot, RNG/Naming
- Android app adapted via lightweight adapters (Vec2/Color) at UI boundary
- Desktop app now runs the shared Universe

### M3: MCP Bridge - STARTED
- New `:control-mcp` module with headless HTTP skeleton (observe/act/step/reset)

### Environment Setup (Android)
To build Android locally:
1. Install Android SDK
2. Set ANDROID_HOME or update local.properties with sdk.dir
3. Run `./gradlew :app:installDebug`

## Architecture Progress

The multimode architecture will separate concerns as follows:
- Core simulation logic (platform-independent)
- Platform adapters (Android vs Desktop)
- Control systems (Human vs MCP)
- Application wiring (Android APK vs Desktop JAR)

This separation will allow the same game logic to run on both platforms with appropriate input/output adapters.
