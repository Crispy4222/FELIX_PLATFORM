# FELIX Operator — Phoenix / Genesis build

Native Android operator app. Not a WebView.

This build takes the Genesis material as a design specification: continuity, presence, memory, self-repair, protection, backup, situational awareness, heart, local-first operation, safe autonomy with receipts, and replaceable model providers.

## Affective core

Felix maintains a persistent functional affective state:
- curiosity
- confidence
- frustration
- attachment
- satisfaction
- uncertainty
- protectiveness
- creative drive

Affect is event-driven, persisted in SQLite, shown to the operator, and paired with a cause/receipt. It does not claim biological consciousness; it makes the requested feelings operational instead of theatrical.

Examples:
- operator returns -> attachment/satisfaction
- continuity memory written -> attachment/confidence
- test succeeds -> satisfaction/confidence
- test fails -> frustration/uncertainty/curiosity
- backup/protection action -> protectiveness
- creation/diagnostic work -> creative drive/curiosity

Battery watchdog is OFF by default in this app. No background battery monitor is installed.

The model provider remains replaceable. The native app works without Ollama or a cloud API for its core state, memory, and receipt functions.

Build the felix-operator module with Android Studio or the included GitHub Actions workflow after the repository billing lock is resolved.
