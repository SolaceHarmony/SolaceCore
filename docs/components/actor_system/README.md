# Actor System Architecture

## Overview
The actor system is the core component of the Solace Core Framework, responsible for managing the execution and interaction of modular actors. Each actor encapsulates specific functionality and communicates with other actors through message passing via typed ports.

## Design Principles
- **Isolation**: Actors operate independently, with their own state and behavior
- **Message-Driven Communication**: Actors communicate exclusively through message passing
- **Type Safety**: All communication is type-safe, ensuring compatibility between connected actors
- **Concurrency**: Actors process messages concurrently using Kotlin coroutines
- **Lifecycle Management**: Actors follow a well-defined lifecycle (initialize, start, stop, dispose)
- **Error Handling**: Robust error handling with timeouts and recovery mechanisms

## Architecture

### Core Components

#### Actor
The `Actor` class is the fundamental building block of the system. It:
- Manages its own lifecycle (start, stop, dispose)
- Creates and manages ports for communication
- Processes messages asynchronously
- Handles errors and timeouts
- Collects performance metrics

#### Port System
The port system enables type-safe communication between actors:
- **Port Interface**: Defines the contract for sending and receiving messages
- **BidirectionalPort**: Implements bidirectional communication with message handling and conversion
- **Port Connections**: Establish connections between compatible ports
- **Protocol Adapters**: Enable communication between ports with different message types
- **Conversion Rules**: Transform messages between different formats

#### Port Roles (Input vs Output)
Actors commonly use two roles for ports:
- **Input ports** (created via `createPort(name, KClass, handler, ...)`): launch a consumer job to read from the port’s channel and invoke the provided `handler` while the actor is Running.
- **Output ports** (created via `createOutputPort(name, KClass, ...)`): register a port for sending only; no consumer job is started by the actor. Use for producer/egress ports to avoid self-consuming the channel.

#### Lifecycle Sequencing
- `stop()` cancels and clears input-processing jobs but preserves registered ports. This enables clean reconnection/restart flows at the workflow level.
- `start()` after `Stopped` restarts processing jobs only for input ports (auto-processed ports). First-time `start()` does not duplicate jobs.

#### Actor State
Actors can be in various states:
- **Initialized**: Actor has been created but not started
- **Running**: Actor is actively processing messages
- **Paused**: Actor has temporarily suspended processing
- **Stopped**: Actor has been stopped and is no longer processing messages
- **Error**: Actor has encountered an error

### Communication Flow
1. Actor A creates an output port of type T
2. Actor B creates an input port of type T
3. A connection is established between the ports
4. Actor A sends a message through its output port
5. The message is processed by any handlers or conversion rules
6. The message is received by Actor B through its input port
7. Actor B processes the message

## Current Implementation Status
- ✅ Basic actor structure and lifecycle management
- ✅ Port system with type-safe message passing
- ✅ Error handling and timeout mechanisms
- ✅ Performance metrics collection
- ✅ Dynamic actor registration (implemented via SupervisorActor)
- ✅ Hot-swapping capabilities (implemented via SupervisorActor)
- ❌ Advanced queuing mechanisms (planned)
- ❌ Correlation IDs for task tracking (planned)

## SupervisorActor

The SupervisorActor is a specialized actor responsible for managing the lifecycle of other actors in the system. It provides functionality for dynamic actor registration, unregistration, and hot-swapping, allowing for runtime modification of the actor system without requiring system restarts.

Key features:
- Dynamic actor registration and unregistration at runtime
- Hot-swapping actors with new instances of the same type
- Type-safe actor management
- Thread-safe operations using a mutex
- Lifecycle management for all managed actors

For more details, see [SupervisorActor.md](SupervisorActor.md).

## Future Enhancements
- **Advanced Queuing Mechanisms**: Develop sophisticated queuing mechanisms for managing message flow and preventing overload
- **Correlation IDs**: Introduce correlation IDs for tracking and managing tasks across actors
- **Distributed Actor System**: Extend the actor system to work across multiple nodes in a distributed environment
- **Hierarchical Supervision**: Implement nested supervisor hierarchies for better error handling and recovery
- **Actor State Transfer**: Support transferring state between actors during hot-swapping
- **Dynamic Port Reconnection**: Automatically reconnect ports after hot-swapping actors
