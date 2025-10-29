#!/usr/bin/env bash
set -euo pipefail
FELIX_LOG="${FELIX_LOG:-$HOME/FELIX_DRIVE/logs}"
mkdir -p "$FELIX_LOG"
tail -n 200 -F "$FELIX_LOG/felix.log"
