# FELIX PLATFORM — WAKE/122

FELIX Platform is a reproducible bootable live operating system built around one principle: **the AI is not a chat program beside the PC; FELIX is the machine's executive control plane.**

The Linux kernel owns hardware, drivers, scheduling, and memory protection. FELIX boots immediately above that deterministic foundation, before the desktop. It reads the machine's state, accepts operator intent, references local memory, reasons through a resident offline language model, validates the proposed route against policy, and exposes the result through the visual Hallway.

## What is actually inside the image

- Debian 13 `trixie`, amd64
- Hybrid BIOS and UEFI boot
- XFCE desktop with LightDM live-session autologin
- Native static `felixd` control plane started before the display manager
- Resident `llama.cpp` inference service on `127.0.0.1:8081`
- Bundled Qwen2.5 1.5B Instruct Q4_K_M GGUF model pinned to an immutable source revision
- Deterministic native cortex as an immediate fallback when model inference is unavailable
- Machine-state, intent, memory, door-registry, and event APIs on `127.0.0.1:8080`
- Visual Hallway as FELIX's live thought surface
- Append-only event memory at `/var/lib/felix/events.jsonl`
- Hard door policy at `/etc/felix/doors.json`
- Files, Terminal, Browser, Network, Settings, and Logs capability doors
- Model license, source revision, runtime revision, and generated SHA-256 record inside the image
- No cloud key or provider token embedded in the image

## Runtime architecture

```text
Firmware
   ↓
Linux kernel + systemd
   ↓
felix-model.service
   └─ resident offline reasoning model
   ↓
felix-core.service
   ├─ reads machine state
   ├─ receives operator intent
   ├─ references auditable event memory
   ├─ asks the resident model for a proposed plan
   ├─ falls back to deterministic native routing if needed
   └─ validates every route against hard policy
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

## Resident model and overrides

The default build bundles:

```text
Model:   Qwen2.5-1.5B-Instruct-GGUF Q4_K_M
Runtime: llama.cpp b10199
API:     http://127.0.0.1:8081/v1/chat/completions
```

The model is the reasoning organ, not the authority chain. Its response is parsed as a proposed plan. The native core rejects unknown doors, never executes model-generated shell text, and withholds destructive actions for a separately defined confirmation path.

An operator may override the resident provider through `/etc/felix/provider.env`. Credentials are never built into the ISO.

To build a smaller image without bundled weights:

```bash
FELIX_BUNDLE_MODEL=0 bash scripts/build-iso.sh
```

The deterministic native cortex remains available in that profile.

## Build locally

Use a Debian or Ubuntu x86_64 host with Go 1.23+, `sudo`, and enough storage for the Debian live filesystem plus the resident model.

```bash
sudo apt-get update
sudo apt-get install -y \
  build-essential cmake curl git pkg-config \
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
3. builds the pinned local llama.cpp inference runtime;
4. downloads the model from its pinned immutable revision;
5. records model source, license, and SHA-256 information;
6. creates the Debian Live hybrid image;
7. validates the boot catalog and publishes `felix-presence-os-amd64`.

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

FELIX is the thought behind the system in the architectural sense: resident reasoning organ, first executive service, shared state interpreter, memory anchor, and route owner. It is not claimed to be conscious, and it does not replace the kernel's deterministic safety mechanisms.

See `/usr/share/doc/felix-platform/ARCHITECTURE.md` inside the live system.
