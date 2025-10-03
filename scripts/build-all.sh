#!/usr/bin/env bash
# Build all modules and create distribution artifacts

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "========================================="
echo "Building all Space Multimode modules"
echo "========================================="
echo ""

echo "[1/4] Building :core-sim (pure Kotlin simulation)..."
./gradlew :core-sim:build --console=plain
echo "✓ :core-sim built successfully"
echo ""

echo "[2/4] Building :app-desktop (Swing desktop app)..."
./gradlew :app-desktop:build --console=plain
echo "✓ :app-desktop built successfully"
echo ""

echo "[3/4] Building :control-mcp (MCP HTTP server)..."
./gradlew :control-mcp:build --console=plain
echo "✓ :control-mcp built successfully"
echo ""

echo "[4/4] Packaging desktop uber JAR..."
./gradlew :app-desktop:packageUberJarForCurrentOS --console=plain
echo "✓ Desktop uber JAR created"
echo ""

echo "========================================="
echo "Build complete!"
echo "========================================="
echo ""
echo "Artifacts:"
echo "  Desktop JAR: app-desktop/build/libs/app-desktop-all.jar"
echo "  MCP distribution: control-mcp/build/distributions/"
echo ""
echo "Run commands:"
echo "  ./scripts/run-desktop.sh [--seed=42]"
echo "  ./scripts/run-mcp.sh [--seed=42] [--autopilot=false]"
echo ""
echo "Note: Android build requires Android SDK (not available in current environment)"

