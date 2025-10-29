#!/usr/bin/env bash
set -euo pipefail
OUT_DIR="${HOME}/FELIX_DRIVE"
REPORT="$OUT_DIR/security_report_$(date +%Y%m%d_%H%M%S).txt"
mkdir -p "$OUT_DIR"
echo "FELIX SECURITY CHECK REPORT" > "$REPORT"
echo "Timestamp: $(date -Is)" >> "$REPORT"
echo "Host: $(hostname) / $(uname -a)" >> "$REPORT"
echo "User: $(whoami)" >> "$REPORT"
echo "Shell: ${SHELL:-/bin/sh}" >> "$REPORT"
echo "----" >> "$REPORT"

# Handle /proc in proot: check it, advise mounting if missing
if [ ! -d /proc ] || [ ! -r /proc ]; then
  echo "WARNING: /proc appears missing or not readable. (proot environment?)" >> "$REPORT"
  echo "To mount /proc temporarily (requires root): mount -t proc proc /proc" >> "$REPORT"
else
  # collect process snapshot
  echo "[TOP PROCESSES]" >> "$REPORT"
  ps aux --sort=-%mem | head -n 40 >> "$REPORT" 2>/dev/null || true
fi

echo "----" >> "$REPORT"
# Last logins (if available)
if command -v last >/dev/null 2>&1; then
  echo "[RECENT LOGINS]" >> "$REPORT"
  last -n 30 >> "$REPORT" 2>/dev/null || true
fi

echo "----" >> "$REPORT"
# Network listeners (best-effort)
if command -v ss >/dev/null 2>&1; then
  ss -tunap 2>/dev/null | head -n 300 >> "$REPORT" || true
elif command -v netstat >/dev/null 2>&1; then
  netstat -tunap 2>/dev/null | head -n 300 >> "$REPORT" || true
fi

echo "Report saved to $REPORT"
