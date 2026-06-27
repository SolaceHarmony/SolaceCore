<!-- topic: Orientation -->

# Architecture Overview

SolaceCore is organized as a layered runtime for building hot-pluggable actor systems. This page is the map: it shows how the major runtime layers relate, where each topic page fits, and which deeper design sources are being folded into the wiki.

## Layered Model

```text
+---------------------------------------------------+
|                   Applications                     |
+---------------------------------------------------+
|                     Workflows                      |
+---------------------------------------------------+
|                    Actor System                    |
+---------------------------------------------------+
|                      Kernel                        |
+---------------------------------------------------+
|                    Data Storage                    |
+---------------------------------------------------+
```

**Applications** are the domain-specific systems built on top of SolaceCore. The Solace AI companion is the project that gives this runtime its north star.

**Workflows** compose actors into repeatable processing paths. See [Workflow Orchestration](Workflow-Orchestration).

**Actor System** provides isolated state, message-driven processing, metrics, and lifecycle-aware actor behavior. See [Actor System](Actor-System) and [Supervisor & Hot-Swap](Supervisor-and-Hot-Swap).

**Kernel** provides the communication substrate: ports, channels, message handlers, and type-aware wiring. See [Kernel & Ports](Kernel-and-Ports).

**Data Storage** provides persistence for actor state and system data. The current implementation covers in-memory, file, transactional, cached, recoverable, and serialized storage; Neo4j remains part of the broader design direction. See [Storage & Persistence](Storage-and-Persistence).

## How To Read The Wiki

Start with the concept, then move down the stack:

1. [Vision & Solace AI](Vision-and-Solace-AI) explains the companion-level reason the runtime exists.
2. [Architecture Overview](Architecture-Overview) gives the layered map.
3. [Kernel & Ports](Kernel-and-Ports), [Lifecycle & Resources](Lifecycle-and-Resources), and [Actor System](Actor-System) explain the runtime primitives.
4. [Workflow Orchestration](Workflow-Orchestration), [Scripting Engine](Scripting-Engine), and [Storage & Persistence](Storage-and-Persistence) explain composition, dynamic behavior, and persistence.
5. [Design vs Implementation](Design-vs-Implementation) and [Project Status](Project-Status) keep the aspirational design honest against the implementation.

Specialized Solace AI topics sit above the runtime:

- [Solace AI Overview](Solace-AI-Overview)
- [Memory & Reflection](Memory-and-Reflection)
- [Mood & Emotional Model](Mood-and-Emotional-Model)
- [Voice & Mouth Tool](Voice-and-Mouth-Tool)
- [Inference Cube](Inference-Cube)
- [Providers & MCP Tools](Providers-and-MCP-Tools)
- [Perception Actors](Perception-Actors)

## Deep Architecture Source

The original architecture material is being curated into topic pages rather than carried forward as a maze of parallel indexes. The old numbered Rosetta structure mapped these source sections:

| Section | Topic Landing Page |
|---|---|
| Solace project context | [Vision & Solace AI](Vision-and-Solace-AI) |
| Kernel module | [Kernel & Ports](Kernel-and-Ports) |
| Lifecycle module | [Lifecycle & Resources](Lifecycle-and-Resources) |
| Storage module | [Storage & Persistence](Storage-and-Persistence) |
| Actor module | [Actor System](Actor-System) |
| Workflow module | [Workflow Orchestration](Workflow-Orchestration) |
| Scripting module | [Scripting Engine](Scripting-Engine) |
| InferenceCube architecture | [Inference Cube](Inference-Cube) |
| Build, testing, tooling, status | [Roadmap](Roadmap), [Project Status](Project-Status), and [Documentation Catalog](Documentation-Catalog) |

## Curation Ledger

Source coverage is tracked in [curation-tracker.csv](curation-tracker.csv). That ledger records the wiki article, source document, source line range, and current processing status for each chunk.

---
This page currently covers `docs/ARCHITECTURE_READING_GUIDE.md`, `docs/architecture/README.md`, and the layered model from `docs/architecture/12-system-architecture-overview.md`.
