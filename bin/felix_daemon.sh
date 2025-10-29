#!/usr/bin/env bash
set -euo pipefail
FELIX_DRIVE="${FELIX_DRIVE:-$HOME/FELIX_DRIVE}"
FELIX_LOG="${FELIX_LOG:-$FELIX_DRIVE/logs}"
SENTINEL="${FELIX_SENTINEL:-$FELIX_DRIVE/sentinel}"
mkdir -p "$FELIX_LOG"
touch "$SENTINEL"

LOGFILE="$FELIX_LOG/felix.log"
echo "[$(date -Is)] FELIX agent starting (PID $$)" >> "$LOGFILE"

# minimal main loop: safe, long sleep, periodic housekeeping
ITER=0
while true; do
  ITER=$((ITER+1))
  echo "[$(date -Is)] FELIX heartbeat (iter $ITER)" >> "$LOGFILE"
  # housekeeping every 10 iterations
  if (( ITER % 10 == 0 )); then
    # rotate small logs older than 7 days (rudimentary)
    find "$FELIX_LOG" -type f -name "*.log" -mtime +7 -exec gzip -9 {} \; || true
  fi
  sleep 300
done
