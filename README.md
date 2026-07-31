# FELIX PLATFORM - WAKE/122

FELIX Platform now includes a reproducible bootable live operating system image built around the Hallway model: one visual shared runtime with application doors for Files, Terminal, Browser, Network, Settings, and Logs.

## Image profile

- Debian 13 `trixie`, amd64
- Hybrid BIOS and UEFI boot
- XFCE desktop with LightDM live-session autologin
- Firefox ESR kiosk visual shell
- Local Hallway service on `127.0.0.1:8080`
- NetworkManager, Thunar, XFCE Terminal, Git, curl, jq, htop, SSH client
- Door registry at `/etc/felix/doors.json`
- `felix-door` command router
- SHA-256 checksums and El Torito boot-catalog validation

## Door commands

```bash
felix-door list
felix-door open hallway
felix-door open files
felix-door open terminal
felix-door open browser
felix-door open network
felix-door open settings
felix-door open logs
```

## Build locally

Use a Debian or Ubuntu x86_64 host with `sudo` access.

```bash
sudo apt-get update
sudo apt-get install -y \
  live-build debootstrap squashfs-tools xorriso isolinux syslinux-common \
  grub-pc-bin grub-efi-amd64-bin mtools dosfstools rsync

sudo bash scripts/build-iso.sh
```

Outputs:

```text
out/felix-hallway-os-amd64.iso
out/SHA256SUMS
```

## Build with GitHub Actions

The `Build FELIX Hallway ISO` workflow builds and validates the image, then publishes the `felix-hallway-os-amd64` artifact containing:

- `felix-hallway-os-amd64.iso`
- `SHA256SUMS`
- `file-report.txt`
- `el-torito-report.txt`
- `live-files-report.txt`

## Test in QEMU

```bash
qemu-system-x86_64 \
  -m 4096 \
  -enable-kvm \
  -cdrom out/felix-hallway-os-amd64.iso
```

## Write to USB

The following command destroys existing data on the selected destination. Verify `/dev/sdX` first.

```bash
sudo dd if=out/felix-hallway-os-amd64.iso of=/dev/sdX bs=4M status=progress oflag=sync
```

## Runtime architecture

```text
Debian Live boot
      ↓
XFCE operator session
      ↓
systemd: felix-hallway.service
      ↓
localhost static Hallway shell
      ↓
visual doors + bounded app launcher
```

The runtime avoids open-ended user-space polling. systemd owns service recovery with restart limits, and the visual launcher uses a finite readiness check before opening a recovery terminal.
