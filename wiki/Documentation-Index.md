<!-- topic: Reference -->
<!-- title: Documentation Index -->

# SolaceCore documentation

This directory holds the design and reference documentation for SolaceCore. The repo's front door is the [root README](Home); this index is the way into the doc tree itself.

## Where to start

- **The recommended reading path** is now the wiki [Architecture Overview](Architecture-Overview) — fast → deep, anchored on the canonical narrative.
- **One-screen mental model**: [`Architecture-Overview`](Architecture-Overview) — the system diagram and runtime map.
- **Full architecture deep dive**: [`Architectural-Deep-Dive`](Architectural-Deep-Dive).
- **The Solace project framing** (what this kernel exists to support): [`Solace-Core-Framework-Architecture`](Solace-Core-Framework-Architecture).

## Layout

### Wiki component topics

| Module | Folder | Notes |
|---|---|---|
| Actor system | [`Actor-System`](Actor-System) | Actor architecture, supervisor notes, communication sequence, class diagrams, and emotional-model integration now live in the wiki. |
| Kernel | [`Kernel-and-Ports`](Kernel-and-Ports) | Port system, channel system, system-architecture overview. |
| Lifecycle | [`Lifecycle-and-Resources`](Lifecycle-and-Resources) | The `Disposable` / `Lifecycle` contract and its class diagram now live in the wiki. |
| Memory | [`Memory-and-Reflection`](Memory-and-Reflection) | The SRAF / Reflection Memory design now lives in the wiki, with Mouth Tool and Zoom split into their own topic pages. |
| Scripting | [`Scripting-Engine`](Scripting-Engine) | The dynamic scripting module design now lives in the wiki. |
| Workflow | [`Workflow-Orchestration`](Workflow-Orchestration) | Workflow orchestration design and management now lives in the wiki. |
| Actor inference engine | [`Inference-Cube`](Inference-Cube) | The InferenceCube architecture and the [Liquid + Transformer hybrid notebook](notebooks/liquid-neural-networks-hybrid-transformer.ipynb). |

### Wiki examples

- [`Basic-Actor-Usage`](Basic-Actor-Usage)
- [`Advanced-Workflow-Example`](Advanced-Workflow-Example)

### Wiki status topics

When you want a gap view (without rewriting the design to match the code), see:
- [`Design-vs-Implementation`](Design-vs-Implementation)
- [`Quick-Status`](Quick-Status)
- [`Project-Status-Report`](Project-Status-Report)
- [`Roadmap`](Roadmap)

### Storage and operations

- [`Storage-and-Persistence`](Storage-and-Persistence) — the storage subsystem and backlog.
- [`Setup-Instructions`](Setup-Instructions) — basic development setup.

### Compose UI

- [`Compose-App-Features`](Compose-App-Features)
- [`Real-Time-UI-Implementation`](Real-Time-UI-Implementation)
- [`Actor-Graph-View`](Actor-Graph-View)

### Project tracking

- [`Master-Checklist`](Master-Checklist) — consolidated checklist.
- [`Test-Coverage-Checklist`](Test-Coverage-Checklist) — test coverage tracking.
- [`LangChain-Code-Changes`](LangChain-Code-Changes) — LangChain integration changes.

### LangChain notes (downstream pattern, not core)

The chain types live in [`prototypes/`](../prototypes/), not in `lib/`. These docs document the pattern for that downstream use:

- [`LangChain-Patterns`](LangChain-Patterns)
- [`LangChain-Chain-Implementation`](LangChain-Chain-Implementation)
- [`LangChain-Recommendations`](LangChain-Recommendations)
- [`LangChain-Type-Safe-Dynamic-Wiring`](LangChain-Type-Safe-Dynamic-Wiring) and related LangChain notes.

### Tasks

- [`Task-Documentation`](Task-Documentation) — discrete task notes used during development phases.

## Implementation status (high level)

The kernel is real. The cognition layers above the kernel are designed and partly prototyped (see [the hybrid notebook](notebooks/liquid-neural-networks-hybrid-transformer.ipynb)) but not yet ported into `lib/`.

| Subsystem | Status |
|---|---|
| Actor system (`core.actor`) | ✅ Shipped — base actor, supervisor, builder, metrics, examples |
| Kernel / ports (`core.kernel.channels.ports`) | ✅ Shipped — `Port`, `BidirectionalPort`, nested `PortConnection` runtime, `Port.connect(...)` factory |
| Lifecycle (`core.lifecycle`) | ✅ Shipped — `Disposable`, `Lifecycle` |
| Storage (`core.storage`) | ✅ Shipped — in-memory, transactional, cached, recoverable, serialization, JVM file backends |
| Scripting (`core.scripting`) | ✅ Shipped — engine, validator, storage, version manager, JVM `kotlin-main-kts` host |
| Workflow (`core.workflow`) | ✅ Shipped — `WorkflowManager` with start/stop ordering, pause/resume, failure handling |
| Reflection Memory + signature retrieval | 🚧 Designed (see [`Memory-and-Reflection`](Memory-and-Reflection)), not yet built |
| Mouth Tool (thought→speech filter) | 🚧 Designed, not yet built |
| Liquid + Transformer hybrid block (LTC port) | 🚧 [Kaggle proof](notebooks/liquid-neural-networks-hybrid-transformer.ipynb) validated; Kotlin port pending |
| InferenceCube state machine | 🚧 Designed in [`Inference-Cube`](Inference-Cube), not yet built |
| Spiking advisor + emotional cue stream | 🚧 Designed, not yet built |

For a more detailed view, [`Design-vs-Implementation`](Design-vs-Implementation) is the canonical gap report.

## Contributing to docs

- Place component-specific docs in the wiki component topics.
- Keep architectural diagrams alongside their component.
- Cross-link aggressively. The doc tree is large; readers should never get lost.
- Code samples in docs must compile against the current `lib/` API surface. If you find one that doesn't, fix it (or call it out in the commit message and open an issue).
- Status claims should match reality. Mark anything aspirational with 🚧 and link to the design doc, not to source code that doesn't exist.
