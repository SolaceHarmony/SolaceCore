# SolaceCore documentation

This directory holds the design and reference documentation for SolaceCore. The repo's front door is the [root README](../README.md); this index is the way into the doc tree itself.

## Where to start

- **The recommended reading path** is now the wiki [Architecture Overview](../wiki/Architecture-Overview.md) — fast → deep, anchored on the canonical narrative.
- **One-screen mental model**: [`../wiki/Architecture-Overview.md`](../wiki/Architecture-Overview.md) — the system diagram and runtime map.
- **Full architecture deep dive**: [`Architectural_Deepdive.md`](Architectural_Deepdive.md).
- **The Solace project framing** (what this kernel exists to support): [`Architectural_Document_Solace_Core_Framework.md`](Architectural_Document_Solace_Core_Framework.md).

## Layout

### `components/` — per-module design docs

| Module | Folder | Notes |
|---|---|---|
| Actor system | [`../wiki/Actor-System.md`](../wiki/Actor-System.md) | Actor architecture, supervisor notes, communication sequence, class diagrams, and emotional-model integration now live in the wiki. |
| Kernel | [`components/kernel/`](components/kernel/) | Port system, channel system, system-architecture overview. |
| Lifecycle | [`../wiki/Lifecycle-and-Resources.md`](../wiki/Lifecycle-and-Resources.md) | The `Disposable` / `Lifecycle` contract and its class diagram now live in the wiki. |
| Memory | [`components/memory/`](components/memory/) | The SRAF / Reflection Memory / Mouth Tool / Zoom design ([`MemoryToolDesign.md`](components/memory/MemoryToolDesign.md)). |
| Scripting | [`../wiki/Scripting-Engine.md`](../wiki/Scripting-Engine.md) | The dynamic scripting module design now lives in the wiki. |
| Workflow | [`../wiki/Workflow-Orchestration.md`](../wiki/Workflow-Orchestration.md) | Workflow orchestration design and management now lives in the wiki. |
| Actor inference engine | [`components/actor_inference_engine/`](components/actor_inference_engine/) | The InferenceCube architecture and the [Liquid + Transformer hybrid notebook](components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb). |

### `examples/` — small runnable examples

- [`Basic_Actor_Usage.md`](examples/Basic_Actor_Usage.md)
- [`advanced_workflow_example.md`](examples/advanced_workflow_example.md)

### `status/` — design-vs-implementation tracking

When you want a gap view (without rewriting the design to match the code), see:
- [`status/DESIGN_VS_IMPLEMENTATION.md`](status/DESIGN_VS_IMPLEMENTATION.md)
- [`status/QUICK_STATUS.md`](status/QUICK_STATUS.md)
- [`status/PROJECT_STATUS.md`](status/PROJECT_STATUS.md)
- [`status/IMPLEMENTATION_ROADMAP.md`](status/IMPLEMENTATION_ROADMAP.md)

### Storage and operations

- [`STORAGE_DOCUMENTATION.md`](STORAGE_DOCUMENTATION.md) — the storage subsystem.
- [`STORAGE_CHECKLIST.md`](STORAGE_CHECKLIST.md) — storage backlog.
- [`SETUP.md`](SETUP.md) — basic development setup.

### Compose UI

- [`COMPOSE_APP_FEATURES.md`](COMPOSE_APP_FEATURES.md)
- [`REAL_TIME_UI_IMPLEMENTATION.md`](REAL_TIME_UI_IMPLEMENTATION.md)
- [`ACTOR_GRAPH_VIEW.md`](ACTOR_GRAPH_VIEW.md)

### Project tracking

- [`MASTER_CHECKLIST.md`](MASTER_CHECKLIST.md) — consolidated checklist.
- [`Test_Coverage_Checklist.md`](Test_Coverage_Checklist.md) — test coverage tracking.
- [`CODE_CHANGES.md`](CODE_CHANGES.md) — narrative changelog.

### LangChain notes (downstream pattern, not core)

The chain types live in [`prototypes/`](../prototypes/), not in `lib/`. These docs document the pattern for that downstream use:

- [`langchain-patterns.md`](langchain-patterns.md)
- [`chain-implementation.md`](chain-implementation.md)
- [`RECOMMENDATIONS_LANGCHAIN.md`](RECOMMENDATIONS_LANGCHAIN.md)
- [`notes/langchain/`](notes/langchain/) — exploratory notes.

### Tasks

- [`tasks/`](tasks/) — discrete task notes used during development phases.

## Implementation status (high level)

The kernel is real. The cognition layers above the kernel are designed and partly prototyped (see [the hybrid notebook](components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb)) but not yet ported into `lib/`.

| Subsystem | Status |
|---|---|
| Actor system (`core.actor`) | ✅ Shipped — base actor, supervisor, builder, metrics, examples |
| Kernel / ports (`core.kernel.channels.ports`) | ✅ Shipped — `Port`, `BidirectionalPort`, nested `PortConnection` runtime, `Port.connect(...)` factory |
| Lifecycle (`core.lifecycle`) | ✅ Shipped — `Disposable`, `Lifecycle` |
| Storage (`core.storage`) | ✅ Shipped — in-memory, transactional, cached, recoverable, serialization, JVM file backends |
| Scripting (`core.scripting`) | ✅ Shipped — engine, validator, storage, version manager, JVM `kotlin-main-kts` host |
| Workflow (`core.workflow`) | ✅ Shipped — `WorkflowManager` with start/stop ordering, pause/resume, failure handling |
| Reflection Memory + signature retrieval | 🚧 Designed (see [`components/memory/MemoryToolDesign.md`](components/memory/MemoryToolDesign.md)), not yet built |
| Mouth Tool (thought→speech filter) | 🚧 Designed, not yet built |
| Liquid + Transformer hybrid block (LTC port) | 🚧 [Kaggle proof](components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb) validated; Kotlin port pending |
| InferenceCube state machine | 🚧 Designed in [`components/actor_inference_engine/InferenceCubeArchitecture.md`](components/actor_inference_engine/InferenceCubeArchitecture.md), not yet built |
| Spiking advisor + emotional cue stream | 🚧 Designed, not yet built |

For a more detailed view, [`status/DESIGN_VS_IMPLEMENTATION.md`](status/DESIGN_VS_IMPLEMENTATION.md) is the canonical gap report.

## Contributing to docs

- Place component-specific docs in the right `components/<module>/` folder.
- Keep architectural diagrams alongside their component.
- Cross-link aggressively. The doc tree is large; readers should never get lost.
- Code samples in docs must compile against the current `lib/` API surface. If you find one that doesn't, fix it (or call it out in the commit message and open an issue).
- Status claims should match reality. Mark anything aspirational with 🚧 and link to the design doc, not to source code that doesn't exist.
