#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
S="${FELIX_SESSION:-felix_daemon}"
D="$HOME/FELIX_DRIVE/daemon/FELIX_DAEMON.sh"
case "${1:-status}" in
  start)  tmux has-session -t "$S" 2>/dev/null || tmux new -d -s "$S" "bash \"$D\"";;
  stop)   tmux kill-session -t "$S" 2>/dev/null || true;;
  status) tmux has-session -t "$S" 2>/dev/null && echo running || echo stopped;;
esac
