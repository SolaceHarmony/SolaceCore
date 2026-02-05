# SolaceCore Architecture: Design-First Reading Guide

This guide is meant to help you *read the design* (not “lint the docs”). It gives a recommended path through the documentation set, anchored on the canonical architecture narrative.

## Canonical architecture narrative

- **Architectural Deep Dive (authoritative):** [Architectural_Deepdive.md](Architectural_Deepdive.md)
  - Treat this as the “spine” document: it tries to carry the full story from first principles → module contracts → rationale.

## Recommended reading order (fast → deep)

### 1) One-screen mental model
- [components/kernel/system_architecture.md](components/kernel/system_architecture.md) (the graph + short component descriptions)

### 2) Core runtime primitives (what makes SolaceCore *SolaceCore*)
- **Kernel / Ports / Channels:**
  - [components/kernel/README.md](components/kernel/README.md)
  - [components/kernel/channel_system.md](components/kernel/channel_system.md)
- **Lifecycle discipline:**
  - [components/lifecycle/README.md](components/lifecycle/README.md)
  - [components/lifecycle/lifecycle_class_diagram.md](components/lifecycle/lifecycle_class_diagram.md)
- **Actor system (isolation + message-driven computation):**
  - [components/actor_system/README.md](components/actor_system/README.md)
  - [components/actor_system/actor_communication_sequence.md](components/actor_system/actor_communication_sequence.md)
  - [components/actor_system/actor_system_class_diagram.md](components/actor_system/actor_system_class_diagram.md)
- **Supervisor (hot-plug + hot-swap as a first-class design goal):**
  - [components/actor_system/SupervisorActor.md](components/actor_system/SupervisorActor.md)

### 3) Orchestration layer
- **Workflow design / orchestration:**
  - [components/workflow/Workflow_Management_Design.md](components/workflow/Workflow_Management_Design.md)
  - [components/actor_system/Workflow_Management_Design.md](components/actor_system/Workflow_Management_Design.md) (if you want the actor-system framing)

### 4) Persistence, resilience, and “memory substrate”
- Storage conceptual + operational guidance:
  - [STORAGE_DOCUMENTATION.md](STORAGE_DOCUMENTATION.md)
  - [STORAGE_CHECKLIST.md](STORAGE_CHECKLIST.md)
- Memory-tool design slice:
  - [components/memory/MemoryToolDesign.md](components/memory/MemoryToolDesign.md)

### 5) Dynamic behavior (scripting)
- [components/scripting/Scripting_Module_Design.md](components/scripting/Scripting_Module_Design.md)

## Solace AI context (how this library fits the larger project)

If you want the “Solace AI” vision/requirements framing, read the Solace AI context sections inside:
- [Architectural_Deepdive.md](Architectural_Deepdive.md)

## Specialized/important design documents (easy to miss)

- **InferenceCube / actor inference engine:**
  - [components/actor_inference_engine/InferenceCubeArchitecture.md](components/actor_inference_engine/InferenceCubeArchitecture.md)
- **Supervisor emotional model design notes:**
  - [components/actor_system/SupervisorAI_EmotionalModel.md](components/actor_system/SupervisorAI_EmotionalModel.md)
- **LangChain alignment and patterns:**
  - [architecture.md](architecture.md)
  - [langchain-patterns.md](langchain-patterns.md)
  - [RECOMMENDATIONS_LANGCHAIN.md](RECOMMENDATIONS_LANGCHAIN.md)

## “What’s real today?” (design vs implementation)

When you explicitly want a gap view (without rewriting the design to match code), use:
- [status/DESIGN_VS_IMPLEMENTATION.md](status/DESIGN_VS_IMPLEMENTATION.md)
- [status/QUICK_STATUS.md](status/QUICK_STATUS.md)
- [kotlin-aligned-docs/IMPLEMENTATION_STATUS.md](kotlin-aligned-docs/IMPLEMENTATION_STATUS.md)

## UI / real-time interaction (Compose app work)

- [COMPOSE_APP_FEATURES.md](COMPOSE_APP_FEATURES.md)
- [REAL_TIME_UI_IMPLEMENTATION.md](REAL_TIME_UI_IMPLEMENTATION.md)

---

### How to use this guide

- If you’re evaluating the *design intent*: start with the canonical deep dive + the kernel/system_architecture diagram.
- If you’re implementing: use the component docs + the gap analysis docs.
- If you’re integrating into “Solace AI”: read the Solace AI context and then pick the modules that become your integration surface (workflow, storage, scripting).
