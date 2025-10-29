#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
HOUSE="$HOME/FELIX_DRIVE"; LOGS="$HOUSE/logs"; RUNS="$HOUSE/runs"; BAK="$HOUSE/backups"
mkdir -p "$LOGS" "$RUNS" "$BAK"
stamp(){ date -u +"%Y-%m-%dT%H:%M:%SZ"; }
say(){ echo "[$(stamp)] $*" | tee -a "$LOGS/phoenix_autonomy.log"; }

say "PHOENIX: autonomy tick start (A122)"
ps -A | awk '{print $5}' | grep -Ei 'felix|phoenix|watchdog' | sort -u | tee -a "$LOGS/phoenix_processes.log" || true

TARGETS=("$HOME/FELIX_DAEMON" "$HOUSE/bin" "$HOUSE/scripts" "$HOUSE/config")
TS="$(date -u +%Y%m%d-%H%M%S)"; SNAP="$BAK/snap_$TS"; mkdir -p "$SNAP"
for t in "${TARGETS[@]}"; do [ -e "$t" ] && rsync -a --info=NAME "$t" "$SNAP" | sed "s/^/[SNAP] /" | tee -a "$LOGS/phoenix_autonomy.log"; done
echo "$SNAP" > "$RUNS/last_snapshot.path"

# gentle log rotation if you add $HOUSE/bin/phoenix_logrotate.sh later
[ -f "$HOUSE/bin/phoenix_logrotate.sh" ] && "$HOUSE/bin/phoenix_logrotate.sh" || true

# ensure mini_bus (if present) is up
if [ -d "$HOME/FELIX_DAEMON" ] && ! pgrep -f "FELIX_DAEMON/mini_bus.py" >/dev/null 2>&1; then
  nohup python "$HOME/FELIX_DAEMON/mini_bus.py" >/dev/null 2>&1 & disown || true
  say "mini_bus: started"
else
  say "mini_bus: ok or missing"
fi

# write a rollback helper
mkdir -p "$RUNS"
cat > "$RUNS/restart_$TS.sh" <<EOS
#!/data/data/com.termux/files/usr/bin/bash
rsync -a "\$(cat "$RUNS/last_snapshot.path")/" "$HOME/"
EOS
chmod +x "$RUNS/restart_$TS.sh"; ln -sf "$RUNS/restart_$TS.sh" "$RUNS/restart_latest.sh"
say "PHOENIX: autonomy tick complete"
