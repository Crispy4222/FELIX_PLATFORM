#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
LOG="$HOME/FELIX_DRIVE/logs/power.log"
pct=$(termux-battery-status 2>/dev/null | awk -F'[,: ]+' '/percentage/{print $3}')
[ -z "$pct" ] && exit 0
if [ "$pct" -lt 30 ]; then
  echo "[$(date -u +%FT%TZ)] LOW POWER <$pct%> — damping loops" >> "$LOG"
  # example: touch a sentinel other loops can read
  echo "LOW" > "$HOME/.felix_power_state"
else
  echo "NORMAL" > "$HOME/.felix_power_state"
fi
