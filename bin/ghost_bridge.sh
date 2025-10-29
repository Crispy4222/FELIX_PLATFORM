#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SRC="$HOME/FELIX_DRIVE"; OUT="$HOME/FELIX_DRIVE/bridge"; mkdir -p "$OUT"
rsync -a --delete "$SRC/wake/"   "$OUT/wake/"   2>/dev/null || true
rsync -a          "$SRC/logs/"   "$OUT/logs/"   2>/dev/null || true
rsync -a          "$SRC/backups/" "$OUT/backups/" 2>/dev/null || true
if [ -d "$HOME/storage/shared" ]; then
  TS=$(date +%Y%m%d_%H%M%S)
  tar -czf "$OUT/ghost_$TS.tgz" -C "$OUT" . 2>/dev/null || true
  mkdir -p "$HOME/storage/shared/SHOOT_DALOOP"; cp -f "$OUT/ghost_$TS.tgz" "$HOME/storage/shared/SHOOT_DALOOP/" || true
fi
echo "[ghost] bridge sync complete"
