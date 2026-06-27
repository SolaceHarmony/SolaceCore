<!-- topic: Runtime -->

# Workflow Management Design

## Overview
WorkflowManager orchestrates a network of actors: it adds/removes actors, connects their ports, and manages lifecycle transitions. The manager validates type compatibility for connections and starts routing between ports when the workflow starts.

## Related Topics

- [Actor System](Actor-System): workflows compose actor instances.
- [Kernel & Ports](Kernel-and-Ports): workflow connections are port connections with routing jobs.
- [Lifecycle & Resources](Lifecycle-and-Resources): workflow startup and shutdown depend on lifecycle order.
- [Scripting Engine](Scripting-Engine): dynamic actor behavior can be driven by scripts inside workflows.
- [Advanced Workflow Example](Advanced-Workflow-Example): compact example of chained actor orchestration.

## Start/Stop Ordering
- `start()`
  1. Starts all actors (transition to Running).
  2. Establishes all configured connections by resolving ports (by name and type) and creating `Port.PortConnection` objects.
  3. Starts routing for each connection (a coroutine relays messages from source to target with handlers/adapters/rules as configured).

- `stop()`
  1. Stops-and-joins all active port connections (cancels routing jobs and waits for completion) to avoid sending into closed channels.
  2. Stops all actors (cancels input-processing jobs; ports are preserved).

This ordering minimizes race conditions and prevents `ClosedSendChannelException` during shutdown.

## Pause/Resume Status
- Actor-level pause/resume is supported (`Actor.pause(reason)`, `Actor.resume()`), suspending input processing on a per-actor basis.
- Workflow-level pause/resume is minimal; future enhancements may provide coordinated pause/resume semantics across all actors and connections.

## Connections
- Connections are validated (`validateConnection()`), then started (`start(scope)`).
- `stopAndJoin()` is used during workflow shutdown to ensure a clean termination of routing jobs before stopping actors.

## Failure Handling
- Route-time failures (handler/adapter/conversion) raise `PortException.Validation` and stop routing.
- Start-time validation failures surface as `PortConnectionException` with source/target identifiers and descriptive messages.



# Workflow Management Architecture

## Overview
The workflow management system orchestrates the execution of actors within the Solace Core Framework. It enables the dynamic composition of actors into workflows, allowing for flexible and scalable processing pipelines.

> **Note**: This component is currently in the planning and early implementation phase. The documentation will be updated as the implementation progresses.

## Design Principles
- **Flexibility**: Support for dynamic composition of actors into workflows
- **Type Safety**: Ensure type compatibility between connected actors
- **State Management**: Maintain and manage workflow state throughout execution
- **Error Handling**: Provide robust error handling and recovery mechanisms
- **Persistence**: Support for persisting workflow state for recovery and continuity

## Planned Architecture

### Core Components

#### Workflow Manager
The `WorkflowManager` will be responsible for:
- Managing the lifecycle of workflows
- Orchestrating the execution of actors within workflows
- Handling errors and state transitions
- Providing monitoring and control capabilities

#### Workflow Builder
The `WorkflowBuilder` will provide:
- A fluent API for constructing workflows
- Methods for adding actors to workflows
- Methods for connecting actors within workflows
- Validation of workflow structure and connections

### Workflow States
Workflows will have the following states:
- **Initialized**: The workflow has been created but not started
- **Running**: The workflow is actively executing
- **Paused**: The workflow has been temporarily suspended
- **Stopped**: The workflow has been stopped
- **Error**: The workflow has encountered an error

### Execution Flow
1. A workflow is constructed using the `WorkflowBuilder`
2. The workflow is started using the `WorkflowManager`
3. Actors within the workflow are executed according to their connections
4. The workflow can be paused, resumed, or stopped as needed
5. When the workflow completes or is stopped, resources are properly disposed

## Current Implementation Status
- ⚠️ Basic workflow management (partially implemented)
- ⚠️ Actor composition (partially implemented)
- ❌ Advanced state management (planned)
- ❌ Workflow pause and resume (planned)
- ❌ State persistence (planned)

## Future Enhancements
- **Workflow Pause and Resume**: Implement functionality to pause and resume workflows
- **State Persistence**: Develop mechanisms for persisting workflow state
- **Distributed Workflows**: Support for workflows that span multiple nodes
- **Workflow Versioning**: Support for versioning and migration of workflow definitions
- **Visual Workflow Designer**: Develop a visual interface for designing workflows

# Workflow Management Design

## Introduction
The workflow management system orchestrates the execution of actors within the Solace Core Framework. It enables the dynamic composition of actors into workflows, allowing for flexible and scalable processing pipelines.

## Core Purpose & Design Principles

The workflow module, centered around the `WorkflowManager` class, is designed to orchestrate the execution of actors within SolaceCore. It enables the dynamic composition of actors into workflows, facilitating the creation of flexible and scalable processing pipelines. This module ties together individual actor capabilities into cohesive, manageable, and higher-level operational units.

### Design Principles
- **Flexibility**: Support for dynamic composition of actors into workflows.
- **Type Safety**: Ensure type compatibility between connected actors.
- **State Management**: Maintain and manage workflow state throughout execution.
- **Error Handling**: Provide robust error handling and recovery mechanisms.
- **Persistence**: Support for persisting workflow state for recovery and continuity.

## Current Implementation

### Workflow Manager
- **Actor Network Management**: Manages a network of actors, including adding actors, connecting them, and managing their lifecycle.
- **State Management**: Maintains the state of the workflow, allowing for start, stop, and error handling.
- **Orchestration**: Orchestrates the execution of actors within workflows.
- **Monitoring and Control**: Provides capabilities for monitoring and controlling workflow execution.

### Workflow States
- **Initialized**: The workflow has been created/defined (actors added, connections specified) but not started.
- **Running**: The workflow is actively executing / its actors are typically running and processing messages.
- **Paused**: The workflow has been temporarily suspended / its actors are temporarily paused.
- **Stopped**: The workflow and its actors have been stopped.
- **Error**: The workflow has encountered an error.

### Conceptual Execution Flow
1. A workflow is constructed (e.g., using the `WorkflowBuilder` concept or direct `WorkflowManager` calls).
2. The workflow is started using the `WorkflowManager`.
3. Actors within the workflow are executed according to their connections.
4. The workflow can be paused, resumed, or stopped as needed.
5. When the workflow completes or is stopped, resources are properly disposed.

## Dynamic Workflow Construction
- **Actor Composition**: Facilitates the dynamic composition of actors into workflows, enabling the creation of complex processing pipelines.
- **Connection Management**: Manages connections between actors, ensuring type compatibility and proper channel setup.
- **WorkflowBuilder Concept**: A fluent API for constructing workflows, with methods for adding actors and connecting them within workflows.

## Current Status
- **Completed Tasks**: Basic workflow management and actor composition are implemented.
- **Partially Completed Tasks**: Advanced state management and error handling are in progress.

## Future Enhancements
- **Workflow Pause and Resume**: Implement functionality to pause and resume workflows, allowing for greater control over execution.
- **State Persistence**: Develop mechanisms for persisting workflow state, enabling recovery and continuity.
- **Distributed Workflows**: Support for workflows that can span multiple nodes or processes.
- **Workflow Versioning**: Implement support for versioning of workflow definitions.
- **Visual Workflow Designer**: Develop a visual interface for designing and monitoring workflows.

## Conclusion
The workflow management system provides a flexible and scalable framework for orchestrating actor-based workflows. Future enhancements will focus on improving control and resilience, enabling advanced state management and persistence capabilities.

---

[← Architecture Overview](Architecture-Overview) · §5 of 15

---

## 5. Workflow Module (`io.github.solaceharmony.core.workflow`)
*Note: The workflow module documentation has been moved to the wiki [Workflow Orchestration](../../wiki/Workflow-Orchestration.md) page.*

---

← [§4 Actor Module (`io.github.solaceharmony.core.actor`)](Actor-System)  ·  [Architecture Overview](Architecture-Overview)  ·  [§6 Scripting Module (`io.github.solaceharmony.core.scripting`)](Scripting-Engine) →
