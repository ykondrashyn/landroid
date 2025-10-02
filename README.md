# Space Multimode

A multiplatform fork of the Android 16 Space easter egg with three run modes:

- **Mode A (Android)**: Original APK with touch controls
- **Mode B (Desktop JAR)**: Runnable JVM/Compose Desktop app with keyboard/mouse controls
- **Mode C (Desktop + MCP)**: Desktop build with optional localhost API for observe/act/step/reset

## Original Source

Based on the Android 16 easter egg: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-s2-release/packages/EasterEgg

Upstream repository: https://github.com/queuejw/Space.git

## Architecture

This project uses a ports-and-adapters architecture with the following modules:

- `:core-sim` → Pure Kotlin simulation (entities, physics, update loop, scenarios)
- `:core-ports` → Interfaces for Renderer, Input, Clock, Storage, Telemetry
- `:platform-android` → Android-specific adapters for rendering/input/timing
- `:platform-desktop` → Compose Desktop adapters for rendering/input/timing
- `:control-human` → Maps touch/keyboard to high-level Actions via ActionBus
- `:control-mcp` → Optional localhost server (WebSocket/HTTP) with observe/act/step/reset
- `:app-android` → Android APK (core + android adapters + human control)
- `:app-desktop` → Desktop JAR (core + desktop adapters + human control + optional MCP)

## Build & Run

### Android Mode
```bash
./gradlew :app-android:installDebug
```

### Desktop Mode
```bash
# Run directly
./gradlew :app-desktop:run

# Build JAR
./gradlew :app-desktop:packageUberJarForCurrentOS
java -jar app-desktop/build/compose/jars/SpaceDesktop-all.jar --seed=42
```

### Desktop + MCP Mode
```bash
# With MCP API
java -jar SpaceDesktop-all.jar --mcp --seed=42

# Headless (no rendering)
java -jar SpaceDesktop-all.jar --mcp --headless --seed=42
```

## MCP API

When `--mcp` flag is enabled, a localhost WebSocket server provides:

- `observe` → Get current game state (ship pose/velocity, planets, score)
- `act` → Send actions `{ rotate: -1..1, thrust: 0..1 }`
- `step` → Advance N simulation frames
- `reset` → Reset with optional seed

Server binds to `127.0.0.1` with rate limiting and input validation.

## Determinism

All modes use fixed timestep + seeded RNG for deterministic behavior:
- Same seed → same outcome across Android and Desktop
- Scenarios stored in `scenarios/` JSON for reproducible runs

## Development

### Version Control
- `main` branch for stable releases
- Feature branches: `feat/m1-desktop-bootstrap`, `refactor/m2-core-split`, etc.
- Conventional Commits style: `feat(desktop):`, `refactor(core):`, `docs(readme):`

### Testing
- Unit tests in `:core-sim` for physics invariants
- Golden regression tests with tolerance checks across platforms
- MCP contract tests for API schema validation

<img src='/.github/1.jpg' width='300' alt="1"> <img src='/.github/2.jpg' width='300' alt="2">
