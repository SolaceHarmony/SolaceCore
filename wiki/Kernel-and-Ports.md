<!-- topic: Runtime -->

# Kernel & Ports

The kernel is the communication substrate of SolaceCore. It provides type-safe ports, channel-backed message flow, connection validation, message handlers, protocol adapters, conversion rules, and resource cleanup discipline.

Everything above the kernel depends on it: actors communicate through ports, workflows wire actor outputs to actor inputs, and the supervisor can only hot-swap behavior safely when the communication contracts remain explicit.

## Design Principles

- **Type safety:** ports declare the message type they accept and produce.
- **Resource management:** ports participate in cleanup through `Disposable`.
- **Flexible communication:** the model supports different connection and message-processing patterns.
- **Extensibility:** protocol adapters and conversion rules let new message forms enter the system without flattening everything into strings.
- **Concurrency:** the implementation is built around Kotlin coroutines and channels.

## Core Concepts

| Concept | Role |
|---|---|
| `Port<T>` | Communication endpoint with an id, name, type, channel view, and send operation. |
| `BidirectionalPort<T>` | Concrete port implementation for bidirectional send/receive behavior. |
| `PortConnection` | Validated connection between source and target ports, with optional handlers, adapters, and conversion rules. |
| `MessageHandler` | Processes messages during transmission. |
| `ProtocolAdapter` | Encodes/decodes between different protocols or data forms. |
| `ConversionRule` | Transforms one message type into another when a connection requires it. |
| `Disposable` | Resource-cleanup contract shared with lifecycle-aware components. |

## Channel System Abstractions

The channel system is built around `Port.kt` and `kotlinx.coroutines.channels.Channel`. The port API is the entry point for type-safe communication:

- `id`: unique generated identifier, typically `port-...`.
- `name`: human-readable port name.
- `type`: the `KClass` of the messages the port accepts.
- `send(message)`: sends a message through the port and may raise validation failures.
- `asChannel()`: exposes the underlying coroutine channel.
- `connect(...)`: creates and validates a `PortConnection`.

`PortConnection` is not just metadata. When started, it routes messages from source to target through any configured handlers, protocol adapter, and conversion rules. It also owns its routing job and supports `start(scope)`, `stop()`, `stopAndJoin()`, and `validateConnection()`.

## Message Processing Pieces

| Piece | Responsibility |
|---|---|
| `MessageHandler<IN, OUT>` | Handles an input message and returns a processed output. |
| `ProtocolAdapter<SOURCE, TARGET>` | Encodes a source object to a target protocol and decodes back when supported. |
| `ConversionRule<IN, OUT>` | Converts message payloads between compatible types. |
| `StringProtocolAdapter<T>` | Base adapter for `String` protocol conversion, with custom decode supplied by concrete implementations. |

Routing error semantics are intentionally explicit:

- Sending to a closed target channel should exit the router cleanly instead of crashing the system.
- Handler, adapter, and rule failures are validation failures and stop routing.
- Buffers should be chosen deliberately. Non-zero buffers improve throughput; send-only ports should avoid launching unnecessary consumers.

## Communication Flow

1. A component creates a port for a specific message type.
2. A compatible target port is selected.
3. A connection validates the source, target, handlers, adapters, and conversion rules.
4. Messages leave the source port.
5. Handlers and conversion rules process the message.
6. The target port receives the processed message.

## Current Implementation Status

- Port interface and `BidirectionalPort` implementation are present.
- Port connections validate compatibility before routing.
- Message handlers, conversion rules, and protocol adapters are part of the connection model.
- Ports can be created dynamically and disconnected.
- More advanced type-checking and monitoring remain future work.

## Future Work

- First-class fan-out and multi-subscriber routing.
- Pluggable backpressure and overflow policies beyond channel defaults.
- More comprehensive tests for connected ports, handlers, adapters, conversion rules, and distributed scenarios.
- More usage examples covering real handler/adapter/conversion combinations.
- More sophisticated type checking for complex generics and runtime compatibility.
- Performance monitoring for message flow, port activity, and channel health.

## Related Topics

- [Actor System](Actor-System): actors expose behavior through ports.
- [Lifecycle & Resources](Lifecycle-and-Resources): kernel resources follow cleanup discipline.
- [Workflow Orchestration](Workflow-Orchestration): workflows compose actors by wiring ports.
- [Supervisor & Hot-Swap](Supervisor-and-Hot-Swap): hot-swapping depends on stable port contracts.

---
Source coverage: `docs/components/kernel/README.md` lines 1-102 and `docs/components/kernel/channel_system.md` lines 1-261.
