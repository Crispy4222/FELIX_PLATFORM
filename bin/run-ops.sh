#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
LOG="${FELIX_LOG:-$HOME/FELIX_DRIVE/logs/session.log}"
mkdir -p "$(dirname "$LOG")"
i=0
for s in "$HOME/FELIX_DRIVE/ops/"*.sh; do
  i=$((i+1))
  nohup bash "$s" >>"$LOG" 2>&1 &
  printf "[felix][ops][%s] started %02d:%s pid=%d\n" "$(date -Is)" "$i" "$(basename "$s")" "$!" >>"$LOG"
done
echo "OK: started $i ops"
