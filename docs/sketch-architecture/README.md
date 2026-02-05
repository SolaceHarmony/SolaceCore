# Kotlin Plans - Codex Vendored Documents Translation

This folder contains Kotlin translations of the codex-vendored documents, converted into executable Kotlin code plans and interfaces.

## Overview

The original codex-vendored documents described the architecture and implementation of Magentic Codex, a TypeScript-based AI agent system. These have been translated into Kotlin code structures that could be used to implement similar functionality in the SolaceCore Kotlin project.

## Files

### Core Architecture
- **ArchitectureOverview.kt** - High-level system architecture with interfaces for agent loop, emotional intelligence, pipeline system, provider layer, tool system, and safety controls
- **ArchitectureClarificationMCP.kt** - Clarification of MCP protocol vs tool format negotiation vs neutral history
- **NeutralHistoryCompletionStatus.kt** - Neutral History XML event system with multi-lane support and event types
- **ImplementationStatus.kt** - Current implementation status with component metrics and known issues

### Components
- **ComponentsOverview.kt** - Modular component architecture with dependency management and health monitoring
- **ProviderSpecs.kt** - Provider specifications for Ollama, OpenAI, etc. with detection and negotiation logic
- **ConfigTemplates.kt** - Configuration templates for different provider setups with DSL builder

### Advanced Features
- **QuickStartDevelopers.kt** - Quick start guide translated to Kotlin interfaces for startup sequence, tool execution flow, and multi-actor system
- **ArchitectureReview.kt** - Architecture review findings with compliance checking and security assessment
- **CoroutineSharedMemory.kt** - Shared-memory coroutine scheduler with lock-free queues and atomic operations
- **MoodTransparencyExperiment.kt** - Mood system transparency with operation visibility, episode storage, and trust/safety models

### Work Plan
- **WORK_PLAN.md** - Phase-by-phase delivery plan with DoD and test entry points

## Key Concepts Translated

### 1. Dual-Model Cognition
- Technical Brain (reasoning, coding, tool use)
- Emotional Core (spiking neural network for authentic emotions)
- Separate memory streams with emotional modulation

### 2. Neutral History System
- Provider-agnostic XML event storage
- Multi-lane architecture (emotional, technical, unified)
- Perfect replay capability and provenance tracking

### 3. Tool System Architecture
- MCP (Model Context Protocol) for server-client communication
- Tool format negotiation for model capabilities
- Structured tools with safety controls and approval workflows

### 4. Pipeline Architecture
- FlowLang DSL for composable request shaping
- Block-based processing with runtime selection
- Model family adaptation and protocol conversion

### 5. Safety-First Design
- Multi-layered approval workflows
- Supervisor mandatory for all tool execution
- Risk assessment and intelligent controls

## Usage

These Kotlin files provide:
- Interface definitions for implementing the described systems
- Data classes for configuration and state management
- Example implementations and usage patterns
- Compliance checking and validation logic

They can serve as a blueprint for implementing similar AI agent capabilities in Kotlin, adapted for the SolaceCore actor-based architecture.

## Integration with SolaceCore

The existing SolaceCore components (actor_system, memory, workflow, etc.) can be extended with these interfaces to add:
- Emotional intelligence through mood systems
- Tool execution with MCP integration
- Neutral event logging and replay
- Pipeline-based request processing
- Safety supervision and approval workflows

## Planning

See **WORK_PLAN.md** for the ordered execution plan (safety → history → pipeline/providers → mood/memory → UI → observability → performance → release). Each phase lists a Definition of Done and tests to run.

## How to Use These Plans

1. **Pick a module**: Choose the Kotlin plan that matches the feature you are implementing (e.g., `NeutralHistoryCompletionStatus.kt` for storage/replay).
2. **Wire interfaces**: Place implementations under `lib/src/commonMain/kotlin/com/solacecore/...` following the package hints inside each plan.
3. **Add tests**: For each implementation, add JVM tests in `lib/src/jvmTest/` and, when relevant, JS tests in `lib/src/jsTest/`.
4. **Document the result**: Update the matching doc in `docs/kotlin-aligned-docs/` so code and docs stay synchronized.
5. **Verify contracts**: Run smoke tests (actor ping, MCP negotiation) before committing.

## Recommended Implementation Order

1. **Safety-first**: Implement `ArchitectureClarificationMCP.kt` and `ArchitectureOverview.kt` interfaces so MCP, negotiation, and approvals are in place.
2. **History layer**: Build `NeutralHistoryCompletionStatus.kt` to guarantee provenance and replay.
3. **Pipeline + providers**: Implement `PipelineEngine` pieces from `ComponentsOverview.kt` and provider specs in `ProviderSpecs.kt`.
4. **UI and actors**: Integrate actor system flows and expose state to Compose UI.
5. **Transparency and monitoring**: Add `MoodTransparencyExperiment.kt` hooks and log emission.

## Validation Checklist

- Interfaces implemented with expect/actual where platform-specific IO is needed.
- Neutral History persists and replays events across JVM and JS targets.
- MCP calls validated against JSON-RPC 2.0 and XML tool schemas.
- Supervisor approval is enforced on every tool path (happy and failure cases).
- Pipelines selectable by provider/family with fallback behavior covered.
- Observability: logs with correlation IDs; metrics for mailbox depth and lane latency.

## Cross-References

- `docs/kotlin-aligned-docs/ARCHITECTURE_OVERVIEW.md` — conceptual architecture aligned to Kotlin.
- `docs/kotlin-aligned-docs/IMPLEMENTATION_STATUS.md` — current state and priorities.
- `docs/kotlin-aligned-docs/QUICK_START_DEVELOPERS.md` — commands, setup, and examples.