# Rook-in-Termux: Operator Field Kit

## Purpose
This field kit translates Rook's philosophy into actionable steps for Termux operators who need file vigilance, rollback, and anomaly detection while coding on mobile hardware. It focuses on discipline and workflows rather than bespoke binaries.

## Operating Principles
- **Preserve before you risk.** Create a snapshot or commit before destructive or high-impact work.
- **Watch the line, not the noise.** Monitor only the directories that matter and log with intent.
- **Own the recovery path.** Practice restoring from snapshots so rollback is muscle memory.
- **Stay visible to yourself.** Keep logs and breadcrumbs where you can audit them fast.

## Baseline Loadout
1. Update Termux packages and install essentials:
   ```sh
   pkg update
   pkg install inotify-tools git rsync termux-api
   ```
2. Choose or create a project directory. Example:
   ```sh
   mkdir -p ~/projects/felix
   cd ~/projects/felix
   ```
3. Initialize version control for structured breadcrumbs:
   ```sh
   git init
   git add .
   git commit -m "Baseline snapshot"
   ```

## Core Routines
### 1. Breadcrumb Before Change
Run before patches or risky edits.
```sh
STAMP=$(date +"%Y%m%d%H%M%S")
rsync -a --delete ./ ~/breadcrumbs/${STAMP}/
```
Restoring a breadcrumb:
```sh
rsync -a ~/breadcrumbs/${STAMP}/ ./
```

### 2. Live Watch with Evidence Log
```sh
inotifywait -m -r ./ \
  --format '%T %w%f %e' \
  --timefmt '%Y-%m-%d %H:%M:%S' \
  >> ~/logs/rook_watch.log
```
Tail the log when you need to inspect activity:
```sh
tail -f ~/logs/rook_watch.log
```

### 3. Whisper Check for Ghost Files
```sh
find ./ -type f -printf '%p\n' | sort > /tmp/rook_scan_current
comm -13 /tmp/rook_scan_baseline /tmp/rook_scan_current > /tmp/rook_new_items
```
- Generate `/tmp/rook_scan_baseline` from a known-good snapshot.
- Review `/tmp/rook_new_items` to spot untracked additions.

### 4. Safe Purge with Grace Window
```sh
TARGET=~/projects/felix/tmp
STAMP=$(date +"%Y%m%d%H%M%S")
rsync -a "$TARGET/" "~/purge_buffer/${STAMP}/"
find "$TARGET" -mindepth 1 -delete
```
- Restore by syncing the matching buffer directory back onto `"$TARGET"` within the grace window.

## Alert Pathways
- Use `termux-notification` for high-severity triggers:
  ```sh
  termux-notification --title "ROOK ALERT" --content "Unexpected delete in ./configs"
  ```
- For remote heads-up, pair with `termux-toast`, `termux-vibrate`, or forward logs via SSH when online.

## Discipline Checklist
- [ ] Snapshot before deploys or destructive edits.
- [ ] Keep `rook_watch.log` trimmed and reviewed daily.
- [ ] Rotate breadcrumb directories to manage storage.
- [ ] Regularly test `rsync` restores and git checkouts.
- [ ] Document policy triggers (paths, hours, actors) in a local `ROOK_POLICY.md`.

## Expansion Hooks
- Script the routines above into `~/bin/rook_*` helpers for one-command execution.
- Pair `inotifywait` with policy filters (time-of-day, path tags) to emulate Rook's tiered notifications.
- Encrypt high-value breadcrumbs before offloading to remote storage.
- Combine with Termux:API sensors (battery, network) to adjust watch posture based on operating conditions.

## Final Reminder
Rook-in-Termux is a practice. Automate where it saves time, but keep the operator in the loop. Every snapshot, watch, and alert should reinforce trust: you know what changed, when, and how to recover without panic.
