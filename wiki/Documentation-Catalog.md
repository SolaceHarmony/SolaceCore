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
| [Zoom Levels](Zoom-Levels) | Cognitive focus and output granularity control. |
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
| `docs/components/kernel/channel_system.md` | 1-261 | [Kernel & Ports](Kernel-and-Ports) | Consolidated, source removed |
| `docs/components/kernel/system_architecture.md` | 1-77 | [Architecture Overview](Architecture-Overview) | Consolidated, source removed |
| `docs/components/actor_system/README.md` | 1-69, 83-89 | [Actor System](Actor-System) | Split by topic, source removed |
| `docs/components/actor_system/README.md` | 70-82 | [Supervisor & Hot-Swap](Supervisor-and-Hot-Swap) | Split by topic, source removed |
| `docs/components/actor_system/actor_communication_sequence.md` | 1-63 | [Actor Communication Sequence](Actor-Communication-Sequence) | Moved as its own diagram page, source removed |
| `docs/components/actor_system/actor_system_class_diagram.md` | 1-132 | [Actor System Class Diagram](Actor-System-Class-Diagram) | Moved as its own diagram page, source removed |
| `docs/components/actor_system/SupervisorActor.md` | 1-128 | [SupervisorActor](SupervisorActor) | Moved as its own design page, source removed |
| `docs/components/actor_system/Workflow_Management_Design.md` | 1-29 | [Workflow Orchestration](Workflow-Orchestration) | Moved, source removed |
| `docs/components/actor_system/SupervisorAI_EmotionalModel.md` | 1-114 | [Mood & Emotional Model](Mood-and-Emotional-Model) | Moved, source removed |
| `docs/components/lifecycle/README.md` | 1-107 | [Lifecycle & Resources](Lifecycle-and-Resources) | Moved, source removed |
| `docs/components/lifecycle/lifecycle_class_diagram.md` | 1-65 | [Lifecycle Class Diagram](Lifecycle-Class-Diagram) | Moved as its own diagram page, source removed |
| `docs/components/workflow/README.md` | 1-60 | [Workflow Orchestration](Workflow-Orchestration) | Moved, source removed |
| `docs/components/workflow/Workflow_Management_Design.md` | 1-56 | [Workflow Orchestration](Workflow-Orchestration) | Moved, source removed |
| `docs/components/scripting/Scripting_Module_Design.md` | 1-96 | [Scripting Engine](Scripting-Engine) | Moved, source removed |
| `docs/components/mood/README.md` | 1-110 | [Mood & Emotional Model](Mood-and-Emotional-Model) | Moved, source removed |
| `docs/components/memory/MemoryToolDesign.md` | 1-272 | [Memory & Reflection](Memory-and-Reflection) | Split by topic, source removed |
| `docs/components/memory/MemoryToolDesign.md` | 273-424 | [Perception Actors](Perception-Actors) | Split by topic, source removed |
| `docs/components/memory/MemoryToolDesign.md` | 425-562 | [Voice & Mouth Tool](Voice-and-Mouth-Tool) | Split by topic, source removed |
| `docs/components/memory/MemoryToolDesign.md` | 563-752 | [Zoom Levels](Zoom-Levels) | Split by topic, source removed |
| `docs/components/actor_inference_engine/InferenceCubeArchitecture.md` | 1-138 | [Inference Cube](Inference-Cube) | Moved, source removed |
| `docs/components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb` | 1-1017 | [Hybrid notebook](notebooks/liquid-neural-networks-hybrid-transformer.ipynb) | Moved as wiki notebook asset, source removed |
| `docs/features/memory/README.md` | 1-256 | [Memory Feature Overview](Memory-Feature-Overview) | Moved, source removed |
| `docs/features/memory/working-memory.md` | 1-173 | [Working Memory](Working-Memory) | Moved, source removed |
| `docs/features/memory/long-term-memory.md` | 1-198 | [Long-Term Memory](Long-Term-Memory) | Moved, source removed |
| `docs/features/memory/retrieval.md` | 1-215 | [Memory Retrieval](Memory-Retrieval) | Moved, source removed |
| `docs/features/memory/compression.md` | 1-237 | [Memory Compression](Memory-Compression) | Moved, source removed |
| `docs/features/reflection-memory/README.md` | 1-288 | [Reflection Memory](Reflection-Memory) | Moved, source removed |
| `docs/features/shared-memory/README.md` | 1-332 | [Shared Memory](Shared-Memory) | Moved, source removed |
| `docs/features/confusion-corrector/README.md` | 1-255 | [Confusion Corrector](Confusion-Corrector) | Moved, source removed |
| `docs/features/multimodal-nudging/README.md` | 1-329 | [Multimodal Nudging](Multimodal-Nudging) | Moved, source removed |
| `docs/features/time-actor/README.md` | 1-273 | [Time Actor](Time-Actor) | Moved, source removed |
| `docs/features/zoom-levels/README.md` | 1-333 | [Zoom Levels](Zoom-Levels) | Moved, source removed |
| `docs/features/mood/README.md` | 1-327 | [Mood & Emotional Model](Mood-and-Emotional-Model) | Moved, source removed |
| `docs/features/mouth-tool/README.md` | 1-299 | [Voice & Mouth Tool](Voice-and-Mouth-Tool) | Moved, source removed |
| `docs/features/inference-cube/README.md` | 1-376 | [Inference Cube](Inference-Cube) | Moved, source removed |
| `docs/features/supervisor/README.md` | 1-310 | [Supervisor AI](Supervisor-AI) | Moved, source removed |
| `docs/features/README.md` | 1-75 | [Feature Index](Feature-Index) | Moved, source removed |
| `docs/features/providers/README.md` | 1-398 | [Providers & MCP Tools](Providers-and-MCP-Tools) | Moved, source removed |
| `docs/features/mcp-tools/README.md` | 1-336 | [Providers & MCP Tools](Providers-and-MCP-Tools) | Moved, source removed |
| `docs/features/pipeline/README.md` | 1-345 | [Pipeline DSL](Pipeline-DSL) | Moved, source removed |
| `docs/status/README.md` | 1-184 | [Status Documentation](Status-Documentation) | Moved, source removed |
| `docs/status/QUICK_STATUS.md` | 1-124 | [Quick Status](Quick-Status) | Moved, source removed |
| `docs/status/PROJECT_STATUS.md` | 1-418 | [Project Status Report](Project-Status-Report) | Moved, source removed |
| `docs/status/DESIGN_VS_IMPLEMENTATION.md` | 1-278 | [Design vs Implementation](Design-vs-Implementation) | Moved, source removed |
| `docs/status/IMPLEMENTATION_ROADMAP.md` | 1-696 | [Roadmap](Roadmap) | Moved, source removed |
| `docs/status/ROADMAP_ISSUES_README.md` | 1-219 | [Roadmap Issues](Roadmap-Issues) | Moved, source removed |
| `docs/status/roadmap_issues.json` | 1-248 | [roadmap_issues.json](roadmap_issues.json) | Moved unchanged, source removed |
| `docs/tasks/README.md` | 1-39 | [Task Documentation](Task-Documentation) | Moved, source removed |
| `docs/tasks/task1.md` | 1-9 | [Task 1 Core Tests](Task-1-Core-Tests) | Moved, source removed |
| `docs/tasks/task2.md` | 1-10 | [Task 2 Connection Wiring](Task-2-Connection-Wiring) | Moved, source removed |
| `docs/tasks/task3.md` | 1-11 | [Task 3 Concurrency Issues](Task-3-Concurrency-Issues) | Moved, source removed |
| `docs/tasks/task4.md` | 1-10 | [Task 4 Dynamic Registration](Task-4-Dynamic-Registration) | Moved, source removed |
| `docs/tasks/task5.md` | 1-23 | [Task 5 Integration Tests](Task-5-Integration-Tests) | Moved, source removed |
| `docs/tasks/task6.md` | 1-29 | [Task 6 Deadlock Detection](Task-6-Deadlock-Detection) | Moved, source removed |
| `docs/examples/Basic_Actor_Usage.md` | 1-20 | [Basic Actor Usage](Basic-Actor-Usage) | Moved, source removed |
| `docs/examples/advanced_workflow_example.md` | 1-30 | [Advanced Workflow Example](Advanced-Workflow-Example) | Moved, source removed |
| `docs/COMPOSE_APP_FEATURES.md` | 1-129 | [Compose App Features](Compose-App-Features) | Moved, source removed |
| `docs/REAL_TIME_UI_IMPLEMENTATION.md` | 1-132 | [Real-Time UI Implementation](Real-Time-UI-Implementation) | Moved, source removed |
| `docs/ACTOR_GRAPH_VIEW.md` | 1-144 | [Actor Graph View](Actor-Graph-View) | Moved, source removed |
| `docs/STORAGE_DOCUMENTATION.md` | 1-753 | [Storage & Persistence](Storage-and-Persistence) | Moved into topic page, source removed |
| `docs/STORAGE_CHECKLIST.md` | 1-151 | [Storage & Persistence](Storage-and-Persistence) | Moved into topic page, source removed |
| `docs/SETUP.md` | 1-8 | [Setup Instructions](Setup-Instructions) | Moved, source removed |
| `docs/MASTER_CHECKLIST.md` | 1-595 | [Master Checklist](Master-Checklist) | Moved, source removed |
| `docs/Test_Coverage_Checklist.md` | 1-163 | [Test Coverage Checklist](Test-Coverage-Checklist) | Moved, source removed |
| `docs/CODE_CHANGES.md` | 1-376 | [LangChain Code Changes](LangChain-Code-Changes) | Renamed by topic, source removed |
| `docs/langchain-patterns.md` | 1-341 | [LangChain Patterns](LangChain-Patterns) | Renamed by topic, source removed |
| `docs/chain-implementation.md` | 1-230 | [LangChain Chain Implementation](LangChain-Chain-Implementation) | Renamed by topic, source removed |
| `docs/RECOMMENDATIONS_LANGCHAIN.md` | 1-380 | [LangChain Recommendations](LangChain-Recommendations) | Renamed by topic, source removed |
| `docs/notes/langchain/BE_MORE_LIKE_LANGCHAIN_HERE.md` | 1-370 | [LangChain Type-Safe Dynamic Wiring](LangChain-Type-Safe-Dynamic-Wiring) | Renamed by topic, source removed |
| `docs/notes/langchain/BUGS.md` | 1-100 | [LangChain Bugs](LangChain-Bugs) | Renamed by topic, source removed |
| `docs/notes/langchain/FIX_PROPOSAL.md` | 1-314 | [LangChain Fix Proposal](LangChain-Fix-Proposal) | Renamed by topic, source removed |
| `docs/notes/langchain/USAGE_DESIGN_IMPROVEMENTS.md` | 1-395 | [LangChain Usage Design Improvements](LangChain-Usage-Design-Improvements) | Renamed by topic, source removed |

## Current Rule

Move content when it has a topic home. If a source document spans multiple topics, split it by source line range in `curation-tracker.csv` and update each destination page before removing the old document.
