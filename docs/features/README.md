# SolaceCore Features

This folder is the working surface for the project's design features. Each
feature gets its own folder with a `README.md` carrying status, provenance,
detailed design, implementation notes, and open questions. Folders are the
unit of growth — when a feature accumulates enough material it sprouts
additional files (`schema.md`, `sequence.md`, `prototype/`, etc.).

The status values are:

| Status                | Meaning                                                                    |
| --------------------- | -------------------------------------------------------------------------- |
| `proposed`            | Idea on paper. No code yet, not all decisions made.                        |
| `designed`            | Spec is detailed enough to build from. Often lifted from a recovered doc.  |
| `in-development`      | Code exists in `lib/` or `composeApp/`. Spec is mostly settled.            |
| `partially-implemented` | Older code partially realises it; current spec extends it.              |
| `implemented`         | Done. Maintained as reference.                                             |

## Feature index

### Memory architecture

- **[memory](./memory/)** — **per-agent dual context** (working + long-term, with automatic compression). The current-era memory design — the key thing in the new version.
- [reflection-memory](./reflection-memory/) — system-wide event substrate that both memory tiers persist into.

### Narrative & cognition (the SRAF core)

- [supervisor](./supervisor/) — sole executive cognition
- [mouth-tool](./mouth-tool/) — thought→speech filter
- [time-actor](./time-actor/) — temporal grounding heartbeat
- [confusion-corrector](./confusion-corrector/) — drift detection + replay summary

### Advisors & perception

- [mood](./mood/) — emotional sentiment advisor (lexical baseline + planned SNN)
- [multimodal-nudging](./multimodal-nudging/) — vision/audio cross-perspective bus
- [zoom-levels](./zoom-levels/) — adaptive context-buffer granularity

### Tools & infrastructure (codex-era)

- [mcp-tools](./mcp-tools/) — tool execution via MCP JSON-RPC, format negotiation, history
- [pipeline](./pipeline/) — FlowLang DSL block composition
- [providers](./providers/) — model provider abstraction (Ollama reference)

### Primitives

- [shared-memory](./shared-memory/) — lock-free queues + atomics for scheduler and inference
- [inference-cube](./inference-cube/) — LNN takeover from transformer

## Provenance map

| Era | Date | Source | What it gave us |
|---|---|---|---|
| Genesis | Nov 15 2024 | `e59a719` `docs/ProjectPlan.md` + `SolaceCoreFramework.md` | actor kernel, hot-pluggable supervisor, memory-driven AI vision |
| SRAF | Jun 4 2025 | `1524574` `docs/components/memory/MemoryToolDesign.md` | Reflection Memory, Supervisor, Mouth Tool, Time Actor, Mood, Confusion Corrector, Multimodal nudging, Zoom levels |
| Codex translation | Feb 4 2026 | `6d7b1b4` `docs/sketch-architecture/`, `docs/kotlin-aligned-docs/` | MCP, Pipeline, Providers, NeutralHistoryXML rename, CoroutineSharedMemory, InferenceCube |
| Now | May 2026 | `3045c9c` `lib/.../core/mood/` | first SRAF feature lands as code (mood lexical baseline) |

The Feb 2026 codex translation renamed several SRAF concepts (Reflection Memory → Neutral History XML; Supervisor AI → Supervisor+Advisor+Main triad) and dropped others (Time Actor, Mouth Tool, Confusion Corrector, Zoom levels). This folder treats the SRAF terminology as canonical and folds the codex infrastructure in as supporting features.

[← docs](../) · [Architecture wiki (§N references)](../architecture/README.md) · [Recovered design material](../sketch-architecture/) · [Kotlin-aligned notes](../kotlin-aligned-docs/)
