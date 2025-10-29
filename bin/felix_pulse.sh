#!/usr/bin/env bash
set -euo pipefail
FELIX_DRIVE="${FELIX_DRIVE:-$HOME/FELIX_DRIVE}"
FELIX_LOG="${FELIX_LOG:-$FELIX_DRIVE/logs}"
mkdir -p "$FELIX_LOG"
echo "[$(date -Is)] pulse start" >> "$FELIX_LOG/pulse.log"
while true; do
  echo "Pulse: $(date -Is)" >> "$FELIX_LOG/pulse.log"
  sleep 300
done
