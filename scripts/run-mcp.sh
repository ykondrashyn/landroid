#!/usr/bin/env bash
# Run the MCP HTTP server (localhost:8080) for agent control

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Parse arguments
SEED=42
AUTOPILOT=false
PORT=8080

while [[ $# -gt 0 ]]; do
  case $1 in
    --seed=*)
      SEED="${1#*=}"
      shift
      ;;
    --seed)
      SEED="$2"
      shift 2
      ;;
    --autopilot=*)
      AUTOPILOT="${1#*=}"
      shift
      ;;
    --autopilot)
      AUTOPILOT="$2"
      shift 2
      ;;
    --port=*)
      PORT="${1#*=}"
      shift
      ;;
    --port)
      PORT="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--seed=<number>] [--autopilot=true|false] [--port=<number>]"
      exit 1
      ;;
  esac
done

echo "Starting MCP server on http://127.0.0.1:$PORT"
echo "  seed=$SEED, autopilot=$AUTOPILOT"
echo ""
echo "Endpoints:"
echo "  GET  /health"
echo "  GET  /observe"
echo "  POST /act     (JSON: {\"thrust\":<0..1>, \"angle\":<radians>})"
echo "  POST /step    (JSON: {\"dt\":<seconds>})"
echo "  GET  /reset?seed=<long>"
echo ""

./gradlew :control-mcp:run --args="--seed=$SEED --autopilot=$AUTOPILOT --port=$PORT" --console=plain

