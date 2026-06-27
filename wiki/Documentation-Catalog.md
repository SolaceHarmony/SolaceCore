<!-- topic: Reference -->

# Documentation Catalog

This catalog tracks the migration from the old `docs/` tree into topic-based wiki pages. It is not a file dump. Each source document is either being consolidated into a topic page, split across several topic pages, or retained temporarily until its line ranges have been curated.

The working ledger is [curation-tracker.csv](curation-tracker.csv). Use it to see the article, source file, line range, and current status for each migration chunk.

## Topic Spine

| Topic | Role |
|---|---|
| [Vision & Solace AI](Vision-and-Solace-AI) | Why the project exists and how Solace AI maps onto the runtime. |
| [Architecture Overview](Architecture-Overview) | The system map and reading path. |
| [Kernel & Ports](Kernel-and-Ports) | Communication substrate. |
| [Lifecycle & Resources](Lifecycle-and-Resources) | Startup, pause, stop, dispose, and cleanup discipline. |
| [Actor System](Actor-System) | Actor runtime, state isolation, messaging, metrics. |
| [Supervisor & Hot-Swap](Supervisor-and-Hot-Swap) | Dynamic registration, hot-plugging, and hot-swapping. |
| [Workflow Orchestration](Workflow-Orchestration) | Composing actors into executable flows. |
| [Scripting Engine](Scripting-Engine) | Runtime Kotlin scripting and script actor behavior. |
| [Storage & Persistence](Storage-and-Persistence) | Storage abstractions, recovery, caching, file/in-memory backends. |
| [Memory & Reflection](Memory-and-Reflection) | Companion memory, reflection, and continuity. |
| [Mood & Emotional Model](Mood-and-Emotional-Model) | Emotional advisor and mood-signature work. |
| [Voice & Mouth Tool](Voice-and-Mouth-Tool) | Spoken interaction and thought-to-speech filtering. |
| [Inference Cube](Inference-Cube) | Actor inference engine and hybrid reasoning model. |
| [Project Status](Project-Status) | Generated implementation/status snapshot. |
| [Roadmap](Roadmap) | Generated implementation roadmap. |

## Curated So Far

| Source | Lines | Destination | Status |
|---|---:|---|---|
| `docs/ARCHITECTURE_READING_GUIDE.md` | 1-76 | [Architecture Overview](Architecture-Overview) | Consolidated, source removed |
| `docs/architecture/README.md` | 1-45 | [Architecture Overview](Architecture-Overview) | Consolidated, source removed |
| `docs/architecture/12-system-architecture-overview.md` | 1-45 | [Architecture Overview](Architecture-Overview) | Covered, source retained until architecture section migration |
| `docs/architecture/00-solace-project-context.md` | 1-52, 141-169 | [Vision & Solace AI](Vision-and-Solace-AI) | Split by topic, source removed |
| `docs/architecture/00-solace-project-context.md` | 54-140 | [Solace AI Overview](Solace-AI-Overview) | Split by topic, source removed |
| `docs/architecture/00-solace-project-context.md` | 172-240 | [Architecture Overview](Architecture-Overview) | Split by topic, source removed |
| `docs/components/kernel/README.md` | 1-102 | [Kernel & Ports](Kernel-and-Ports) | Consolidated, source removed |

## Current Rule

Move content when it has a topic home. If a source document spans multiple topics, split it by source line range in `curation-tracker.csv` and update each destination page before removing the old document.
