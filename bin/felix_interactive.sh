#!/usr/bin/env bash
set -euo pipefail
echo "Entering FELIX shell. Type 'exit' or Ctrl-D to quit."
while true; do
  read -rp "FELIX> " CMD || break
  case "$CMD" in
    exit|quit) break ;;
    __ghostdoor__ )
      echo "Ghostdoor: maintenance shell (PIN-protected). To run secure-cmd, use 'secure-cmd <command>'." ;;
    * )
      echo "You entered: $CMD"
      # naive: log but don't execute untrusted commands
      echo "[$(date -Is)] interactive: $CMD" >> "${FELIX_LOG:-$HOME/FELIX_DRIVE/logs}/interactive.log"
      ;;
  esac
done
echo "Leaving FELIX shell."
