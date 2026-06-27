# SolaceCore Features

This folder is where the architecture's intent lives in narrative form.
Each feature has a folder; each folder's `README.md` is the spec for
that feature, written as the document the design *is* rather than the
document that talks about it. Folders sprout additional files
(`schema.md`, `sequence.md`, `prototype/`) when material warrants the
expansion.

The features here are documented as **two convergent threads where
they have two**: SolaceCore is the merger of Sydney's **Solace**
lineage (the Seamless Reflective AI Framework, SRAF, with its
Supervisor / Mouth Tool / Time Actor / Confusion Corrector / Zoom
controller) and her **Magentic** lineage (the Codex-era infrastructure
contributions: Coroutine Shared-Memory, Neutral History XML,
InferenceCube + LTC, Pipeline DSL, Provider Specs, MCP). Where the
two threads describe the same primitive — Reflection Memory and
Neutral History; Supervisor's executive cognition and safety boundary
— the feature folder documents both threads in their own voice and
sketches the convergence. Where a feature is uniquely from one
lineage, the folder names the lineage but tells the story straight.

The status values:

| Status                | Meaning                                                                    |
| --------------------- | -------------------------------------------------------------------------- |
| `proposed`            | Idea on paper. No code yet, not all decisions made.                        |
| `designed`            | Spec is detailed enough to build from. Often lifted from a recovered doc.  |
| `in-development`      | Code exists in `lib/` or `composeApp/`. Spec is mostly settled.            |
| `partially-implemented` | Older code partially realises it; current spec extends it.              |
| `implemented`         | Done. Maintained as reference.                                             |

## Feature index

### Memory architecture

- **[memory](../../wiki/Memory-Feature-Overview.md)** — **per-agent dual context** (working + long-term, with automatic compression). The current-era memory design — the key thing in the new version.
- [reflection-memory](../../wiki/Reflection-Memory.md) — system-wide event substrate that both memory tiers persist into.

### Narrative & cognition (the SRAF core)

- [supervisor](./supervisor/README.md) — sole executive cognition
- [mouth-tool](./mouth-tool/README.md) — thought→speech filter
- [time-actor](./time-actor/README.md) — temporal grounding heartbeat
- [confusion-corrector](./confusion-corrector/README.md) — drift detection + replay summary

### Advisors & perception

- [mood](./mood/README.md) — emotional sentiment advisor (lexical baseline + planned SNN)
- [multimodal-nudging](./multimodal-nudging/README.md) — vision/audio cross-perspective bus
- [zoom-levels](./zoom-levels/README.md) — adaptive context-buffer granularity

### Tools & infrastructure (codex-era)

- [mcp-tools](./mcp-tools/README.md) — tool execution via MCP JSON-RPC, format negotiation, history
- [pipeline](./pipeline/README.md) — FlowLang DSL block composition
- [providers](./providers/README.md) — model provider abstraction (Ollama reference)

### Primitives

- [shared-memory](../../wiki/Shared-Memory.md) — lock-free queues + atomics for scheduler and inference
- [inference-cube](./inference-cube/README.md) — LNN takeover from transformer

## Provenance map

| Era | Date | Source | What it gave us |
|---|---|---|---|
| Genesis | Nov 15 2024 | `e59a719` `docs/ProjectPlan.md` + `SolaceCoreFramework.md` | actor kernel, hot-pluggable supervisor, memory-driven AI vision |
| SRAF | Jun 4 2025 | `1524574` [Memory & Reflection](../../wiki/Memory-and-Reflection.md) | Reflection Memory, Supervisor, Mouth Tool, Time Actor, Mood, Confusion Corrector, Multimodal nudging, Zoom levels |
| Codex translation | Feb 4 2026 | `6d7b1b4` `docs/sketch-architecture/`, `docs/kotlin-aligned-docs/` | MCP, Pipeline, Providers, NeutralHistoryXML rename, CoroutineSharedMemory, InferenceCube |
| Now | May 2026 | `3045c9c` `lib/.../core/mood/` | first SRAF feature lands as code (mood lexical baseline) |

The Feb 2026 codex translation renamed several SRAF concepts (Reflection Memory → Neutral History XML; Supervisor AI → Supervisor+Advisor+Main triad) and dropped others (Time Actor, Mouth Tool, Confusion Corrector, Zoom levels). This folder treats the SRAF terminology as canonical and folds the codex infrastructure in as supporting features.

[← docs](../README.md) · [Architecture wiki](../../wiki/Architecture-Overview.md) · [Recovered design material](../sketch-architecture/README.md) · [Kotlin-aligned notes](../kotlin-aligned-docs/README.md)
