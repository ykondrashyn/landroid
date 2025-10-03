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

### Build All Modules
```bash
# Build all modules and create distributions
./scripts/build-all.sh
```

### Android Mode
```bash
./gradlew :app:installDebug
# Note: Requires Android SDK (not available in current environment)
```

### Desktop Mode
```bash
# Run using script (recommended)
./scripts/run-desktop.sh --seed=42

# Or run directly with Gradle
./gradlew :app-desktop:run --args="--seed=42"

# Or build uber JAR and run standalone
./gradlew :app-desktop:packageUberJarForCurrentOS
java -jar app-desktop/build/libs/app-desktop-all.jar --seed=42
```

### Desktop + MCP Mode
```bash
# Run MCP server using script (recommended)
./scripts/run-mcp.sh --seed=42 --autopilot=false --port=8080

# Or run directly with Gradle
./gradlew :control-mcp:run --args="--seed=42 --autopilot=false --port=8080"

# Basic checks (in another terminal)
curl -s http://127.0.0.1:8080/health
curl -s http://127.0.0.1:8080/observe

# Act (GET query or POST JSON)
curl -s "http://127.0.0.1:8080/act?thrust=1&angle=0.0"
curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"thrust":1,"angle":0.0}' http://127.0.0.1:8080/act

# Step (GET query or POST JSON)
curl -s "http://127.0.0.1:8080/step?dt=0.016"
curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"dt":0.5}' http://127.0.0.1:8080/step

# Reset (GET)
curl -s "http://127.0.0.1:8080/reset?seed=42"
```

## MCP API

MCP HTTP endpoints (localhost only):

- `GET /observe` → current state (time, ship pose/velocity, body count)
- `POST or GET /act` → set thrust magnitude and/or ship angle
  - GET query: `?thrust=0..1&angle=<radians>`
  - POST JSON: `{"thrust": <0..1>, "angle": <radians>}`
- `POST or GET /step` → advance simulation time by dt (seconds)
  - GET query: `?dt=<seconds>`
  - POST JSON: `{"dt": <seconds>}`
- `POST/GET /reset?seed=<long>` → reset universe with seed
- `GET /health` → {"status":"ok"}

Notes:
- Autopilot is controlled only at launch via CLI flag: `--autopilot=true|false` (default false)
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
