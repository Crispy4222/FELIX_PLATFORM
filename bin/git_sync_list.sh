#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
LIST="${1:-$HOME/FELIX_DRIVE/config/repos.txt}"
DEST="${2:-$HOME/repos}"
LOG="$HOME/FELIX_DRIVE/logs/git_sync_list.log"
mkdir -p "$DEST"
while IFS= read -r url; do
  [ -z "$url" ] && continue
  name="$(basename "$url" .git)"
  dir="$DEST/$name"
  if [ -d "$dir/.git" ]; then
    echo "[UPD] $name" | tee -a "$LOG"
    (cd "$dir" && git fetch --all --prune && git pull --ff-only) 2>&1 | tee -a "$LOG" || true
  else
    echo "[CLN] $name <- $url" | tee -a "$LOG"
    git clone "$url" "$dir" 2>&1 | tee -a "$LOG" || true
  fi
done < "$LIST"
