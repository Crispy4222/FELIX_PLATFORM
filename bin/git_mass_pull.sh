#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
echo "[*] scanning repos…"
while IFS= read -r -d '' g; do
  repo="${g%/.git}"
  printf "\n=== %s ===\n" "$repo"
  (cd "$repo" && git remote -v && git fetch --all --prune && git pull --ff-only || echo "[warn] pull failed")
done < <(find "$HOME" -type d -name .git -print0 2>/dev/null)
echo "[*] done."
