#!/usr/bin/env bash
set -euo pipefail
FELIX_DRIVE="${FELIX_DRIVE:-$HOME/FELIX_DRIVE}"
FELIX_LOG="${FELIX_LOG:-$FELIX_DRIVE/logs}"
BIN_DIR="$FELIX_DRIVE/bin"
DAEMON_SCRIPT="$BIN_DIR/felix_daemon.sh"
LOGFILE="$FELIX_LOG/watchdog.log"
mkdir -p "$(dirname "$LOGFILE")"

# restart guard: if daemon restarts > 4 times within window, trigger recovery
MAX_RESTARTS=4
WINDOW_SECS=300
restart_count=0
last_restart_epoch=0

while true; do
  if ! pgrep -f "$DAEMON_SCRIPT" >/dev/null 2>&1; then
    echo "[$(date -Is)] watchdog: FELIX not running, starting." >> "$LOGFILE"
    nohup bash "$DAEMON_SCRIPT" >> "$FELIX_LOG/felix.log" 2>&1 &
    now=$(date +%s)
    if (( now - last_restart_epoch < WINDOW_SECS )); then
      restart_count=$((restart_count+1))
    else
      restart_count=1
    fi
    last_restart_epoch=$now
    if (( restart_count > MAX_RESTARTS )); then
      echo "[$(date -Is)] watchdog: too many restarts; preserving state and pausing (manual intervention required)" >> "$LOGFILE"
      # create sentinel to signal manual rebuild
      touch "${FELIX_DRIVE:-$HOME/FELIX_DRIVE}/.felix_pause"
      sleep 60
    fi
  fi
  sleep 60
done
