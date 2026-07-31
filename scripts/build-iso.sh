#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISO_DIR="$ROOT_DIR/iso"
OUT_DIR="$ROOT_DIR/out"
INCLUDES_DIR="$ISO_DIR/config/includes.chroot"
SYSTEMD_WANTS="$INCLUDES_DIR/etc/systemd/system/multi-user.target.wants"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR" "$SYSTEMD_WANTS"

chmod 0755 \
  "$INCLUDES_DIR/usr/local/bin/felix-hallway-launch" \
  "$INCLUDES_DIR/usr/local/bin/felix-door"
ln -sfn ../felix-hallway.service "$SYSTEMD_WANTS/felix-hallway.service"

cd "$ISO_DIR"
sudo lb clean --purge || true

sudo lb config \
  --mode debian \
  --distribution trixie \
  --architectures amd64 \
  --binary-images iso-hybrid \
  --bootloaders "syslinux grub-efi" \
  --archive-areas "main contrib non-free-firmware" \
  --firmware-binary true \
  --firmware-chroot true \
  --apt-recommends true \
  --security true \
  --updates true \
  --checksums sha256 \
  --compression xz \
  --iso-application "FELIX Platform Hallway OS" \
  --iso-volume "FELIX_HALLWAY" \
  --iso-publisher "CRISPYcreations" \
  --bootappend-live "boot=live components username=crispy hostname=felix-hallway locales=en_US.UTF-8 keyboard-layouts=us quiet splash"

sudo lb build

ISO_PATH="$(find . -maxdepth 1 -type f -name '*.iso' -print -quit)"
if [[ -z "$ISO_PATH" ]]; then
  echo "error: live-build completed without producing an ISO" >&2
  exit 1
fi

sudo mv "$ISO_PATH" "$OUT_DIR/felix-hallway-os-amd64.iso"
sudo chown "$(id -u):$(id -g)" "$OUT_DIR/felix-hallway-os-amd64.iso"

(
  cd "$OUT_DIR"
  sha256sum felix-hallway-os-amd64.iso > SHA256SUMS
)

echo "Built $OUT_DIR/felix-hallway-os-amd64.iso"
cat "$OUT_DIR/SHA256SUMS"
