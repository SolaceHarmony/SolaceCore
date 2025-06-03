# Solace Core Framework Documentation

## Overview
The `docs` directory contains comprehensive documentation for the Solace Core Framework, a versatile, resilient, and extendable platform built using Kotlin and Ktor. The framework is designed around an actor-based architecture with hot-pluggable components, flexible input/output nodes, and a robust communication system.

## Structure
- **components/**: Detailed documentation for each major component of the framework
  - **actor_system/**: Documentation for the core actor-based architecture
  - **kernel/**: Documentation for the foundational communication and resource management infrastructure
  - **lifecycle/**: Documentation for the standardized approach to component lifecycle management
  - **workflow/**: Documentation for the workflow management system (planned)
- **diagrams/**: Visual representations of the framework's architecture
  - **system_architecture.md**: High-level overview of the system architecture
  - **actor_system_class_diagram.md**: Class structure of the actor system
  - **actor_communication_sequence.md**: Sequence diagram showing actor interaction
  - **kernel_class_diagram.md**: Class structure of the kernel component
  - **lifecycle_class_diagram.md**: Class structure of the lifecycle management system
- **archive/**: Older documentation and planning documents that have been superseded by the current documentation structure
- **Design Documents**: Detailed design documents for core components and systems
  - [SolaceCoreFramework.md](SolaceCoreFramework.md): Vision and design overview
  - [Actor_System_Design.md](Actor_System_Design.md): Design of the actor system
  - [Interface_and_Port_System_Design.md](Interface_and_Port_System_Design.md): Design of the interface and port system
  - [Hot-Pluggable_Actor_System.md](Hot-Pluggable_Actor_System.md): Design of the hot-pluggable actor system
  - [Workflow_Management_Design.md](Workflow_Management_Design.md): Design of the workflow management system
- **Checklists**: Task lists for tracking progress and planning future work
  - [CHECKLIST.md](CHECKLIST.md): General project checklist
  - [COMPONENT_CHECKLIST.md](COMPONENT_CHECKLIST.md): Component-specific checklist
  - [FRAMEWORK_CHECKLIST.md](FRAMEWORK_CHECKLIST.md): Framework-specific checklist

## Current Implementation Status

The framework is under active development. Here's the current status of major components:

### Actor System
- ✅ Basic actor structure and lifecycle management
- ✅ Port system with type-safe message passing
- ✅ Error handling and timeout mechanisms
- ✅ Performance metrics collection
- ⚠️ Dynamic actor registration (partially implemented)
- ⚠️ Hot-swapping capabilities (in progress)

### Kernel
- ✅ Port interface and BidirectionalPort implementation
- ✅ Port connections with validation
- ✅ Message handlers and conversion rules
- ✅ Protocol adapters for type conversion
- ⚠️ Dynamic port creation and disconnection (partially implemented)

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
2. Add architectural diagrams to the `diagrams/` directory
3. Use Markdown for all documentation
4. Include code examples where appropriate
5. Keep documentation up-to-date with code changes
6. Cross-reference related documentation
