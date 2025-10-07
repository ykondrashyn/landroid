# Space Multimode

A multiplatform fork of the Android 16 Space easter egg with three run modes:

- **Mode A (Android)**: Original APK with touch controls
- **Mode B (Desktop JAR)**: Runnable JVM/Compose Desktop app with keyboard/mouse controls
- **Mode C (Desktop + MCP)**: Desktop build with optional localhost API for observe/act/step/reset

## Original Source

Based on the Android 16 easter egg: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-s2-release/packages/EasterEgg

Originally inspired by https://github.com/queuejw/Space.git — this project is now a standalone fork with additional MCP capabilities and desktop features.

## Architecture

This project uses a modular architecture with the following modules:

- `:core-sim` → Pure Kotlin simulation (entities, physics, update loop)
- `:naming-shared` → Shared planet/star naming logic for deterministic generation across platforms
- `:app` → Android APK with touch controls
- `:app-desktop` → Desktop JAR with keyboard controls and optional MCP server
- `:control-mcp` → (Legacy) Standalone headless MCP server (use `app-desktop --mcp --headless` instead)

### Module Structure
```
landroid/
├── app/                    # Android app
├── app-desktop/            # Desktop app (Compose Desktop)
│   └── src/main/kotlin/ru/queuejw/space/desktop/
│       ├── Main.kt         # Entry point with mode branching
│       ├── AppConfig.kt    # CLI argument parsing
│       ├── Logger.kt       # Structured logging
│       ├── mcp/            # MCP HTTP server for desktop
│       └── ui/             # Compose UI components (shared with Android)
├── core-sim/               # Shared simulation logic
├── naming-shared/          # Shared naming data (planets, stars, activities)
└── control-mcp/            # (Legacy standalone MCP server)
```

## Build & Run

### MCP Integration (AI Assistant Control)

The Space game can be controlled by AI assistants like Claude or Augment via the Model Context Protocol (MCP).

**Architecture:**
1. Space game runs as HTTP server (localhost:18080)
2. MCP bridge (`scripts/mcp_bridge.py`) translates MCP protocol ↔ HTTP REST
3. AI assistant connects to bridge via stdio

**Setup:**

1. Install `uv` (Python package manager):
```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

2. Start the Space game server (realtime mode recommended):
```bash
./gradlew :app-desktop:run -Pmcp=true -Prealtime=true -Pheadless=true -Pport=18080 -Pseed=42
```

3. Configure your AI assistant by adding to your MCP config file:
```json
{
  "mcpServers": {
    "Space": {
      "command": "uv",
      "args": ["run", "--with", "mcp", "python3", "/absolute/path/to/landroid/scripts/mcp_bridge.py"],
      "env": {
        "SPACE_MCP_PORT": "18080"
      }
    }
  }
}
```

**Notes:**
- Replace `/absolute/path/to/landroid` with the actual path to this repository
- `uv` automatically installs the `mcp` package when needed (no manual pip install required)
- If `uv` is not in your PATH, use the full path: `~/.local/bin/uv` or `$HOME/.local/bin/uv`

4. The AI assistant can now use tools like:
   - `observe()` - Get probe state, nearest planet, altitude, gravity
   - `act(thrust, angle)` - Apply thrust
   - `reset(seed)` - Start new simulation

**Example interaction:**
```
User: "Can you land the probe on the nearest planet?"
AI: [calls observe() to see current state]
AI: [calls act(thrust=1.0, angle=3.14) to brake]
AI: [continues observing and acting until safe landing]
```

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
# Run from Gradle with UI rendering and MCP enabled (no keyboard control)
./gradlew :app-desktop:run --args="--mcp --seed=42"

# From uber JAR with UI rendering and MCP enabled (no keyboard control)
java -jar app-desktop/build/compose/jars/SpaceDesktop-all.jar --mcp --seed=42

# Headless (no rendering) with MCP
java -jar app-desktop/build/compose/jars/SpaceDesktop-all.jar --mcp --headless --seed=42
```

Behavior:
- With `--mcp` and no `--headless`: full graphics like Android (planets, ship, trails, telemetry), but all keyboard input is disabled. Control ONLY via the MCP API.
- With `--mcp --headless`: no graphics; simulation controlled entirely by MCP.

## MCP API (HTTP)

When `--mcp` is enabled, a localhost HTTP server exposes endpoints on `http://127.0.0.1:8080` (configurable with `--port=<N>`):

- `GET /health` → `{ "status": "ok" }`
- `GET /observe[?nearestN=N]` → `{ now, ship{ x,y,vx,vy,angle,fuel,fuelCapacity,hull,hullCapacity }, nearestPlanet{ id,x,y,radius,mass,canLand }|null, nearestPlanetId, nearestPlanets:[{ id,x,y,radius,distance,altitude,gx,gy,vx,vy,mass }], landing:{ planetId,planetName,angle,text }|null, altitude, gravity{ gx,gy }, bodies }
- `GET /act?thrust=0..1&angle=radians` → set thrust magnitude and absolute angle (validates range)
- `GET /step?dt=seconds` → advance simulation by dt seconds (default 1/60, max 10)
- `GET /reset?seed=long` → re-seed and reset the universe
- `GET /world` → world description `{ now, universeRange, config{ realtime,timeScale }, star{ name,x,y,vx,vy,radius,mass,class,deadly,collides,canLand:false }, planets:[{ id,name,x,y,vx,vy,radius,mass,collides,canLand,explored,description,atmosphere,flora,fauna }], bodies }`

Notes:
- In MCP+UI non-realtime mode, the desktop app does not advance time on its own; use `/step` to drive the simulation.
- In MCP+UI realtime mode (`--realtime`), the simulation advances automatically at `--hz` (default 60); `/step` is informational and returns current time.
- Keyboard and on-screen controls are disabled in MCP+UI mode to ensure control only via API.
- Thread-safe: uses read-write locks to synchronize universe access between API calls; UI reads are unsynchronized (low-risk for floats).

Example usage:
```bash
# Start MCP+UI mode (using -P properties for convenience)
./gradlew :app-desktop:run -Pmcp=true -Pseed=42 -Pport=18080

# In another terminal, control the simulation (curl)
curl http://127.0.0.1:18080/health
curl http://127.0.0.1:18080/observe
curl "http://127.0.0.1:18080/act?thrust=0.5&angle=1.57"
curl "http://127.0.0.1:18080/step?dt=0.016"
curl "http://127.0.0.1:18080/reset?seed=123"

# Or use the Python helper (no external deps)
python3 scripts/mcp_client.py --port 18080 health
python3 scripts/mcp_client.py --port 18080 observe
python3 scripts/mcp_client.py --port 18080 act --thrust 1 --angle 0
python3 scripts/mcp_client.py --port 18080 step --dt 1
python3 scripts/mcp_client.py --port 18080 loop --thrust 1 --angle 0 --dt 0.1 --steps 50
```

# Realtime mode (auto-advancing simulation)
./gradlew :app-desktop:run -Pmcp=true -Prealtime=true -Phz=60 -Pport=18080
# Agent should NOT call /step in realtime; instead, observe->act->sleep to simulate processing delay
python3 scripts/mcp_client.py --port 18080 drive-rt --thrust 1 --angle 0 --omega 0.2 --think 0.05 --steps 200


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
