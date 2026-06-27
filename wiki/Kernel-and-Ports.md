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

## Related Topics

- [Actor System](Actor-System): actors expose behavior through ports.
- [Lifecycle & Resources](Lifecycle-and-Resources): kernel resources follow cleanup discipline.
- [Workflow Orchestration](Workflow-Orchestration): workflows compose actors by wiring ports.
- [Supervisor & Hot-Swap](Supervisor-and-Hot-Swap): hot-swapping depends on stable port contracts.

---
Source coverage: `docs/components/kernel/README.md` lines 1-102.
