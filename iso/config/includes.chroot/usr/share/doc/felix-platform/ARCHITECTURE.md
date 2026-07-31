# FELIX Operating Presence Architecture

FELIX is not presented as a chat application running beside the operating system. It is the machine's executive control plane: the layer that observes state, receives operator intent, references memory, reasons about the goal, chooses a bounded capability route, and explains the evidence behind that route.

The Linux kernel remains responsible for hardware, memory protection, scheduling, and drivers. Replacing those safety-critical mechanisms with a language model would make the machine less reliable. FELIX sits immediately above them and becomes the thought layer that coordinates the system.

## Boot relationship

```text
Firmware
  -> Linux kernel and systemd
  -> felix-model.service
       resident Qwen2.5 reasoning model through llama.cpp
  -> felix-core.service
       machine state + event memory + intent cortex + policy
  -> graphical operator session
  -> Hallway thought surface
  -> allowlisted capability doors
```

The model and core start before the display manager. The visual Hallway is therefore a view into an already-running system presence, not the process that creates that presence.

## Resident reasoning organ

The default ISO contains:

- `/usr/local/libexec/felix-llama-server` — pinned llama.cpp runtime;
- `/opt/felix-models/qwen2.5-1.5b-instruct-q4_k_m.gguf` — bundled offline model;
- `felix-model.service` — localhost-only inference service on port 8081;
- model license, source revision, runtime revision, and SHA-256 documentation.

The resident model gives FELIX open-ended language reasoning without requiring an internet connection or cloud account. It is deliberately treated as one organ inside a larger architecture rather than as unrestricted machine authority.

## Native executive cortex

`/usr/local/sbin/felixd` is a static native Go binary. It owns:

- live system-state observation from `/proc`, filesystems, and network interfaces;
- append-only event and decision memory in `/var/lib/felix/events.jsonl`;
- operator-intent handling through `/api/intent` and `felixctl think`;
- calls to the resident model using an OpenAI-compatible localhost endpoint;
- deterministic native routing when the model is unavailable or times out;
- validation of every suggested route against `/etc/felix/doors.json`;
- localhost delivery of the Hallway and machine-state API.

The cortex is event-driven. It reasons when startup, operator intent, status, or memory events arrive. It does not depend on an uncontrolled `while true` Python process.

## Authority relationship

The model is FELIX's language and planning organ, but model output is a proposal. The native executive layer remains responsible for authority and continuity:

1. read current machine state;
2. retrieve recent auditable memory;
3. give the goal, state, memory, and registered doors to the resident model;
4. parse the proposed thought and route;
5. reject any door outside the hard registry;
6. withhold destructive intent for explicit policy-controlled confirmation;
7. record the full event and return the explanation to the Hallway.

No generated shell command is executed. No API credential is built into the image.

## Hallway and doors

The Hallway is FELIX's visible thought surface. It displays body state, accepts goals in natural language, shows the resulting thought, evidence, provider, and confidence, and exposes the selected capability door.

Doors are allowlisted adapters to applications. A `felix://open/<door>` link is handed to the operator's desktop session and resolved by `/usr/local/bin/felix-door`. This separation lets the system think broadly while acting through narrow, inspectable capabilities.

## Continuity

Every boot, observation, intent, plan, and explicit memory becomes an auditable JSON event. Recent events can be referenced during reasoning without injecting an entire lifetime of logs into every request.

```bash
felixctl status
felixctl think "find the project photos"
felixctl remember "copy before modifying operator files"
felixctl events
```

## Boundary

FELIX is the thought behind the system in the practical architectural sense: resident reasoning organ, first executive service, shared state interpreter, and route owner for the desktop. It is not claimed to be conscious, and it does not replace the kernel's deterministic safety mechanisms.
