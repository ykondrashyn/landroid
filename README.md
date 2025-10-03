# Space Multimode

A multiplatform fork of the Android 16 Space easter egg with three run modes:

- **Mode A (Android)**: Original APK with touch controls
- **Mode B (Desktop JAR)**: Runnable JVM/Compose Desktop app with keyboard/mouse controls
- **Mode C (Desktop + MCP)**: Desktop build with optional localhost API for observe/act/step/reset

## Original Source

Based on the Android 16 easter egg: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-s2-release/packages/EasterEgg

Upstream repository: https://github.com/queuejw/Space.git

## Architecture

This project uses a ports-and-adapters style with these modules (current):

- `:core-sim` → Pure Kotlin simulation (Vec2, Physics, Universe, Autopilot, RNG/Naming)
- `:app` → Android app (Compose UI adapters for rendering)
- `:app-desktop` → Swing Desktop app using shared `:core-sim`
- `:control-mcp` → Headless HTTP server exposing observe/act/step/reset on localhost

(Planned later: additional adapter/control modules as needed.)

## Build & Run

### Android Mode
```bash
./gradlew :app-android:installDebug
```

### Desktop Mode
```bash
# Run directly
./gradlew :app-desktop:run

# Build uber JAR
./gradlew :app-desktop:packageUberJarForCurrentOS
java -jar app-desktop/build/libs/app-desktop-all.jar --seed=42
```

### Desktop + MCP Mode
```bash
# Run MCP server (HTTP on 127.0.0.1:8080)
./gradlew :control-mcp:run

# Basic checks (in another terminal)
curl -s http://127.0.0.1:8080/health
curl -s http://127.0.0.1:8080/observe
curl -s "http://127.0.0.1:8080/act?thrust=1&angle=0.0"
curl -s "http://127.0.0.1:8080/step?dt=0.016"
curl -s "http://127.0.0.1:8080/reset?seed=42"
```

## MCP API

MCP HTTP endpoints (localhost only):

- `GET /observe` → current state (time, ship pose/velocity, body count)
- `POST/GET /act?thrust=0..1&angle=<radians>` → set thrust magnitude and/or ship angle
- `POST/GET /step?dt=<seconds>` → advance simulation time by dt
- `POST/GET /reset?seed=<long>` → reset universe with seed
- `GET /health` → {"status":"ok"}

Notes:
- Deterministic: fixed-timestep stepping with artificial time inside the server
- Local-only bind (127.0.0.1) for safety

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
