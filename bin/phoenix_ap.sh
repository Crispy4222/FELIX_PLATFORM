#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
HOUSE="$HOME/FELIX_DRIVE"; LOGS="$HOUSE/logs"; WAKE="$HOUSE/wake"; RUNS="$HOUSE/runs"
PANIC="$HOUSE/.panic"; PWR="$HOME/.felix_power_state"
STAMP(){ date -u +"%Y-%m-%dT%H:%M:%SZ"; }
say(){ echo "[$(STAMP)] $*" | tee -a "$LOGS/phoenix_autonomy.log" "$WAKE/wake.log"; }

case "${1:-}" in
  status)
    if [ -f "$PANIC" ]; then say "STATUS: PANIC MODE (ice cage) — loops should pause"; else say "STATUS: AP alive"; fi
    echo "Processes:"; ps -Af | grep -Ei 'felix|phoenix|mini_bus' | grep -v grep || true
    [ -f "$PWR" ] && echo "Power sentinel: $(cat "$PWR")"
    ;;
  mark)
    shift; say "MARK: $*";;
  snap)
    TS="$(date -u +%Y%m%d-%H%M%S)"; BAK="$HOUSE/backups/snap_$TS"; mkdir -p "$BAK"
    for t in "$HOME/FELIX_DAEMON" "$HOUSE/bin" "$HOUSE/scripts" "$HOUSE/config" "$HOUSE/protocols"; do
      [ -e "$t" ] && rsync -a "$t" "$BAK"
    done
    echo "$BAK" > "$RUNS/last_snapshot.path"
    say "SNAP: $BAK"
    ;;
  rollback)
    "$RUNS/restart_latest.sh" || true
    say "ROLLBACK invoked"
    ;;
  tail)
    tail -n 200 -f "$LOGS/phoenix_autonomy.log"
    ;;
  panic)
    echo "ICE" > "$PANIC"; say "ICE CAGE: PANIC sentinel raised";;
  unpanic)
    rm -f "$PANIC"; say "ICE CAGE: PANIC sentinel cleared";;
  *)
    echo "AP usage: status | mark <msg> | snap | rollback | tail | panic | unpanic"
    ;;
esac
