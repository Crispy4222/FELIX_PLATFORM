# FELIX Operating Presence Architecture

FELIX is not presented as a chat application running beside the operating system. It is the machine's executive control plane: the layer that observes state, receives operator intent, references memory, chooses a bounded capability route, and explains the evidence behind that route.

The Linux kernel remains responsible for hardware, memory protection, scheduling, and drivers. Replacing those safety-critical mechanisms with a language model would make the machine less reliable. FELIX sits immediately above them and becomes the thought layer that coordinates the system.

## Boot relationship

```text
Firmware
  -> Linux kernel and systemd
  -> felix-core.service
  -> machine-state model + event memory + intent cortex
  -> graphical operator session
  -> Hallway thought surface
  -> allowlisted capability doors
```

`felix-core.service` starts before the display manager. The visual Hallway is therefore a view into an already-running system presence, not the process that creates that presence.

## Native cortex

`/usr/local/sbin/felixd` is a static native Go binary. It owns:

- live system-state observation from `/proc`, filesystems, and network interfaces;
- append-only event and decision memory in `/var/lib/felix/events.jsonl`;
- operator-intent handling through `/api/intent` and `felixctl think`;
- a deterministic offline cortex for routing common intents;
- an optional OpenAI-compatible model provider for broader reasoning;
- validation of every suggested route against `/etc/felix/doors.json`;
- localhost delivery of the Hallway and machine-state API.

The cortex is event-driven. It thinks when an operator intent, status request, memory event, or system startup event arrives. It does not depend on an uncontrolled `while true` Python process.

## Model relationship

A model is a reasoning organ, not the whole authority chain. FELIX may attach to a local or operator-selected model through `/etc/felix/provider.env`, but model output is treated as a proposed plan. The native policy layer rejects unknown doors and refuses destructive intent without an explicit, separately defined confirmation path.

No API credential is built into the image.

## Hallway and doors

The Hallway is FELIX's visible thought surface. It displays body state, accepts goals in natural language, shows the resulting thought and evidence, and exposes the selected capability door.

Doors are allowlisted adapters to applications. A `felix://open/<door>` link is handed to the operator's desktop session and resolved by `/usr/local/bin/felix-door`. The core does not execute arbitrary shell text returned by a model.

## Continuity

Every boot, observation, intent, plan, and explicit memory becomes an auditable JSON event. Recent events can be referenced by the reasoning provider without injecting an entire lifetime of logs into every request.

```bash
felixctl status
felixctl think "find the project photos"
felixctl remember "copy before modifying operator files"
felixctl events
```

## Boundary

FELIX is the thought behind the system in the practical architectural sense: it is the first reasoning service, the shared state interpreter, and the route owner for the desktop. It is not claimed to be conscious, and it does not replace the kernel's deterministic safety mechanisms.
