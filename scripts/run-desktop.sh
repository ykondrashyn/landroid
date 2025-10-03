#!/usr/bin/env bash
# Run the desktop Swing application with shared :core-sim Universe

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Parse arguments
SEED=42
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
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--seed=<number>]"
      exit 1
      ;;
  esac
done

echo "Running desktop app with seed=$SEED..."
./gradlew :app-desktop:run --args="--seed=$SEED" --console=plain

