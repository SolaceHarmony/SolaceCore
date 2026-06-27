<!-- topic: Reference -->
<!-- title: Feature Index -->

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

- **[memory](Memory-Feature-Overview)** — **per-agent dual context** (working + long-term, with automatic compression). The current-era memory design — the key thing in the new version.
- [reflection-memory](Reflection-Memory) — system-wide event substrate that both memory tiers persist into.

### Narrative & cognition (the SRAF core)

- [supervisor](Supervisor-AI) — sole executive cognition
- [mouth-tool](Voice-and-Mouth-Tool) — thought→speech filter
- [time-actor](Time-Actor) — temporal grounding heartbeat
- [confusion-corrector](Confusion-Corrector) — drift detection + replay summary

### Advisors & perception

- [mood](Mood-and-Emotional-Model) — emotional sentiment advisor (lexical baseline + planned SNN)
- [multimodal-nudging](Multimodal-Nudging) — vision/audio cross-perspective bus
- [zoom-levels](Zoom-Levels) — adaptive context-buffer granularity

### Tools & infrastructure (codex-era)

- [mcp-tools](MCP-and-Tool-Format) — tool execution via MCP JSON-RPC, format negotiation, history
- [pipeline](Pipeline-DSL) — FlowLang DSL block composition
- [providers](Providers-and-MCP-Tools) — model provider abstraction (Ollama reference)

### Primitives

- [shared-memory](Shared-Memory) — lock-free queues + atomics for scheduler and inference
- [inference-cube](Inference-Cube) — LNN takeover from transformer

## Provenance map

| Era | Date | Source | What it gave us |
|---|---|---|---|
| Genesis | Nov 15 2024 | `e59a719` `docs/ProjectPlan.md` + `SolaceCoreFramework.md` | actor kernel, hot-pluggable supervisor, memory-driven AI vision |
| SRAF | Jun 4 2025 | `1524574` [Memory & Reflection](Memory-and-Reflection) | Reflection Memory, Supervisor, Mouth Tool, Time Actor, Mood, Confusion Corrector, Multimodal nudging, Zoom levels |
| Codex translation | Feb 4 2026 | `6d7b1b4` `wiki/sketch-architecture/`, [Kotlin-aligned notes](Kotlin-Aligned-Documentation) | MCP, Pipeline, Providers, NeutralHistoryXML rename, CoroutineSharedMemory, InferenceCube |
| Now | May 2026 | `3045c9c` `lib/.../core/mood/` | first SRAF feature lands as code (mood lexical baseline) |

The Feb 2026 codex translation renamed several SRAF concepts (Reflection Memory → Neutral History XML; Supervisor AI → Supervisor+Advisor+Main triad) and dropped others (Time Actor, Mouth Tool, Confusion Corrector, Zoom levels). This folder treats the SRAF terminology as canonical and folds the codex infrastructure in as supporting features.

[← docs](Documentation-Catalog) · [Architecture wiki](Architecture-Overview) · [Recovered design material](Sketch-Architecture) · [Kotlin-aligned notes](Kotlin-Aligned-Documentation)
