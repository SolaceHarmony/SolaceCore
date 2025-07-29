# Solace Core Framework Documentation

## Overview
The `docs` directory contains comprehensive documentation for the Solace Core Framework, a versatile, resilient, and extendable platform built using Kotlin and Ktor. The framework is designed around an actor-based architecture with hot-pluggable components, flexible input/output nodes, and a robust communication system.

## Structure
- **components/**: Detailed documentation for each major component of the framework
  - **actor_system/**: Documentation for the core actor-based architecture
  - **kernel/**: Documentation for the foundational communication and resource management infrastructure
  - **lifecycle/**: Documentation for the standardized approach to component lifecycle management
  - **workflow/**: Documentation for the workflow management system (planned)
- **diagrams/**: Visual representations of the framework's architecture are located within the corresponding component folders
- **examples/**: Usage snippets and small workflows
  - [Basic Actor Usage](examples/Basic_Actor_Usage.md)
  - [kernel/system_architecture.md](components/kernel/system_architecture.md): High-level overview of the system architecture
  - [actor_system/actor_system_class_diagram.md](components/actor_system/actor_system_class_diagram.md): Class structure of the actor system
  - [actor_system/actor_communication_sequence.md](components/actor_system/actor_communication_sequence.md): Sequence diagram showing actor interaction
- **archive/**: Older documentation and planning documents that have been superseded by the current documentation structure
- **Design Documents**: Detailed design documents for core components and systems
  - [SolaceCoreFramework.md](SolaceCoreFramework.md): Vision and design overview
  - [Actor_System_Design.md](Actor_System_Design.md): Design of the actor system
  - [Interface_and_Port_System_Design.md](Interface_and_Port_System_Design.md): Design of the interface and port system
  - [Hot-Pluggable_Actor_System.md](Hot-Pluggable_Actor_System.md): Design of the hot-pluggable actor system
  - [Workflow_Management_Design.md](Workflow_Management_Design.md): Design of the workflow management system
- **Checklists**: Task lists for tracking progress and planning future work
  - [MASTER_CHECKLIST.md](MASTER_CHECKLIST.md): Consolidated project checklist
  - [STORAGE_CHECKLIST.md](STORAGE_CHECKLIST.md): Storage subsystem tasks
  - [Test_Coverage_Checklist.md](Test_Coverage_Checklist.md): Test coverage tracking
- [SETUP.md](SETUP.md): Basic development setup

## Current Implementation Status

The framework is under active development. Here's the current status of major components:

### Actor System
- ✅ Basic actor structure and lifecycle management
- ✅ Port system with type-safe message passing
- ✅ Error handling and timeout mechanisms
- ✅ Performance metrics collection
- ✅ Dynamic actor registration via `SupervisorActor`
- ✅ Hot-swapping capabilities for replacing actors at runtime

### Kernel
- ✅ Port interface and BidirectionalPort implementation
- ✅ Port connections with validation
- ✅ Message handlers and conversion rules
- ✅ Protocol adapters for type conversion
- ✅ Dynamic port creation and disconnection through `Actor`

### Lifecycle Management
- ✅ Disposable interface for resource management
- ✅ Lifecycle interface for component lifecycle management
- ✅ Implementation in Actor and Port components
- ⚠️ Comprehensive error handling (partially implemented)

### Workflow Management
- ⚠️ Basic workflow management (partially implemented)
- ⚠️ Actor composition (partially implemented)
- ❌ Advanced state management (planned)
- ❌ Workflow pause and resume (planned)
- ❌ State persistence (planned)

## Contributing

When contributing to the documentation, please follow these guidelines:

1. Place component-specific documentation in the appropriate subfolder under `components/`
2. Store architectural diagrams alongside their respective components
3. Use Markdown for all documentation
4. Include code examples where appropriate
5. Keep documentation up-to-date with code changes
6. Cross-reference related documentation
