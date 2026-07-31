# FELIX PLATFORM — WAKE/122

FELIX Platform is a reproducible bootable live operating system built around one principle: **the AI is not a chat program beside the PC; FELIX is the machine's executive control plane.**

The Linux kernel still owns hardware, drivers, scheduling, and memory protection. FELIX boots above that deterministic foundation before the desktop, reads the machine's state, accepts operator intent, references local event memory, selects a bounded capability route, and exposes the result through the visual Hallway.

## Image profile

- Debian 13 `trixie`, amd64
- Hybrid BIOS and UEFI boot
- XFCE desktop with LightDM live-session autologin
- Native static `felixd` cortex started by systemd before the display manager
- Machine-state, intent, memory, door-registry, and event APIs on `127.0.0.1:8080`
- Visual Hallway as FELIX's live thought surface
- Deterministic offline intent cortex
- Optional local or operator-selected OpenAI-compatible model provider
- Append-only event memory at `/var/lib/felix/events.jsonl`
- Door policy registry at `/etc/felix/doors.json`
- Files, Terminal, Browser, Network, Settings, and Logs capability doors
- No cloud key or provider token embedded in the image
- SHA-256 checksums and El Torito boot-catalog validation

## Runtime architecture

```text
Firmware
   ↓
Linux kernel + systemd
   ↓
felix-core.service
   ├─ reads machine state
   ├─ receives operator intent
   ├─ references auditable memory
   ├─ asks a local/selected model when configured
   └─ validates every route against policy
   ↓
XFCE operator session
   ↓
Hallway thought surface
   ↓
allowlisted felix:// capability doors
```

The Hallway is not the AI process. It is the visible face of an already-running system presence.

## Operator commands

```bash
felixctl status
felixctl think "find the project photos"
felixctl remember "copy before modifying operator files"
felixctl events

felix-door list
felix-door open files
felix-door open terminal
felix-door open browser
felix-door open network
felix-door open settings
felix-door open logs
```

## Attach a reasoning model

FELIX works offline with its native bounded intent cortex. For broader reasoning, copy the example provider configuration and point it at an OpenAI-compatible endpoint:

```bash
sudo cp /etc/felix/provider.env.example /etc/felix/provider.env
sudo chmod 600 /etc/felix/provider.env
sudo editor /etc/felix/provider.env
sudo systemctl restart felix-core.service
```

A local endpoint keeps inference on the PC. A remote endpoint is optional. Model output remains a proposal: the native policy layer rejects unknown doors and holds destructive actions for explicit confirmation.

## Build locally

Use a Debian or Ubuntu x86_64 host with Go 1.23+ and `sudo` access.

```bash
sudo apt-get update
sudo apt-get install -y \
  live-build debootstrap squashfs-tools xorriso isolinux syslinux-common \
  grub-pc-bin grub-efi-amd64-bin mtools dosfstools rsync

bash scripts/build-iso.sh
```

Outputs:

```text
out/felix-presence-os-amd64.iso
out/SHA256SUMS
```

## Build with GitHub Actions

The `Build FELIX Operating Presence ISO` workflow:

1. tests and vets the native cortex;
2. compiles a static amd64 `felixd` binary;
3. creates the Debian Live hybrid image;
4. validates the boot catalog and live filesystem;
5. publishes the `felix-presence-os-amd64` artifact.

## Test in QEMU

```bash
qemu-system-x86_64 \
  -m 4096 \
  -enable-kvm \
  -cdrom out/felix-presence-os-amd64.iso
```

## Write to USB

This destroys existing data on the selected destination. Verify `/dev/sdX` first.

```bash
sudo dd if=out/felix-presence-os-amd64.iso of=/dev/sdX bs=4M status=progress oflag=sync
```

## Design boundary

FELIX is the thought behind the system in the architectural sense: first reasoning service, shared state interpreter, memory anchor, and route owner. It is not claimed to be conscious, and it does not replace the kernel's deterministic safety mechanisms.

See `ARCHITECTURE.md` inside the live system at `/usr/share/doc/felix-platform/ARCHITECTURE.md`.
