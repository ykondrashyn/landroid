# Development Notes

## Current Status

### M0: Baseline Verification ✅
- Android SDK not available in current environment (Android build not verified yet)
- Documented limitation and proceeded with desktop implementation

### M1: Desktop Bootstrap ✅
- ✅ Created :app-desktop module with Swing/AWT UI
- ✅ Implemented fixed-timestep game loop (60 Hz)
- ✅ Added keyboard controls (arrows/WASD for steering/thrust, ESC to quit)
- ✅ Uber JAR packaging task available
- ✅ Configurable seed via --seed CLI argument

### M2: Proper Separation & Reuse ✅
- ✅ Introduced INamer interface to decouple naming from Android resources
- ✅ Created :core-sim module (pure Kotlin)
- ✅ Moved Vec2, Physics (Entity/Body/Constraint/Simulator), Universe, Autopilot to :core-sim
- ✅ Introduced KColor to replace Compose Color in core
- ✅ Added adapters in :app for Compose interop (toComposeColor, Vec2 adapters)
- ✅ Desktop app now runs shared Universe with DesktopNamer
- ✅ Basic unit tests for :core-sim

### M3: MCP Bridge ✅
- ✅ Created :control-mcp module with JDK HttpServer (127.0.0.1:8080)
- ✅ Endpoints: /health, /observe, /act, /step, /reset
- ✅ Artificial time stepping for determinism
- ✅ POST JSON body support for /act and /step (query params also supported)
- ✅ Autopilot controlled via CLI flag (--autopilot=true|false, default false)
- ✅ Richer /observe payload (autopilot flag, landing info, nearest body)
- ✅ Locale.US numeric formatting (dot decimals)
- ✅ Configurable seed and port via CLI args (--seed, --port)

### M4: Documentation & Scripts ✅
- ✅ README updated with module descriptions and run commands
- ✅ DEVELOPMENT.md tracks progress
- ✅ Created run scripts:
  - scripts/run-desktop.sh (with --seed support)
  - scripts/run-mcp.sh (with --seed, --autopilot, --port support)
  - scripts/build-all.sh (builds all modules and creates distributions)

## Project Complete

All milestones (M0-M4) have been completed:
1. ✅ VCS Setup: Fork created, remotes configured
2. ✅ M0: Baseline verified (Android SDK limitation documented)
3. ✅ M1: Desktop JAR mode working with Swing UI
4. ✅ M2: Core simulation extracted to :core-sim (pure Kotlin)
5. ✅ M3: MCP HTTP server with observe/act/step/reset API
6. ✅ M4: Documentation and run scripts complete

## Architecture

The multimode architecture separates concerns as follows:
- **Core simulation logic** (:core-sim) - platform-independent
- **Platform adapters** (Android vs Desktop) - UI/rendering adapters
- **Control systems** (Human keyboard vs MCP HTTP API)
- **Application wiring** (Android APK vs Desktop JAR vs MCP server)

This separation allows the same game logic to run on all platforms with appropriate input/output adapters.

## Environment Setup (Android)
To build Android locally:
1. Install Android SDK
2. Set ANDROID_HOME or update local.properties with sdk.dir
3. Run `./gradlew :app:installDebug`

## Optional Future Enhancements
- Add more unit tests for core physics/simulation
- Expand MCP API with additional observation fields
- Add Android build support when SDK becomes available
- Implement Compose Desktop UI (currently using Swing)
