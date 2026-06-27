# Actor module — `io.github.solaceharmony.core.actor`

The actor runtime: actors with structured concurrency, typed message envelopes, lifecycle states, metrics, builders, and a supervisor.

## Public surface

| Type | Role |
|---|---|
| `Actor` | Abstract base. Implements [`Lifecycle`](../lifecycle/). Owns a `CoroutineScope` (defaults to `Dispatchers.Default + SupervisorJob()`). Provides `createPort` / `createOutputPort` / `getPort` / `recreatePort` / `disconnectPort` / `removePort`. Holds an atomic `ActorState`. |
| `ActorMessage<out T : Any>` | Generic, immutable message envelope. Fields: `correlationId`, `payload: T`, `sender: String?`, `timestamp: Long`, `priority: MessagePriority`, `metadata: Map<String, Any>`. |
| `MessagePriority` | `HIGH`, `NORMAL`, `LOW`. |
| `ActorMessageHandler<T>` | Base class for typed handlers; implements `Port.MessageHandler<ActorMessage<T>, ActorMessage<T>>`. |
| `ActorState` | Sealed: `Initialized`, `Running`, `Paused(reason)`, `Stopped`, `Error(exception)`. |
| `builder.ActorBuilder` | Fluent construction with port wiring. |
| `metrics.ActorMetrics` | Per-actor processing-time and error counters. |
| `supervisor.SupervisorActor` | Manages other actors' lifecycles; supports dynamic registration / unregistration / hot-swap. Mutex-protected. |
| `examples.{TextProcessor, Filter}` | Reference actors used in tests and quick-starts. |

## Lifecycle states (what `ActorState` can be, vs what `Lifecycle` exposes)

`ActorState` carries five values; the [`Lifecycle`](../lifecycle/) interface (`start`, `stop`, `isActive`) only exposes transitions for three of them. The other two — `Paused(reason)` and `Error(exception)` — are reached via internal transitions inside the actor (e.g., a thrown exception during message processing flips state to `Error`). Callers observe state via `actor.state`.

```
Initialized ──start──▶ Running ──stop──▶ Stopped ──dispose──▶ (disposed)
                          ▲                  │
                          │                  ▼
                       (resume)            (dispose)
                          │
                       Paused(reason)
                          │
                       Error(exception)  (terminal until restart)
```

## Message handling

Handlers are attached to a port at creation time. The actor's coroutine consumes messages from that port's channel and calls the handler:

```kotlin
class FrustrationDetector : Actor(name = "frustrationDetector") {
    suspend fun initialize() {
        createPort(
            name = "userText",
            messageClass = ActorMessage::class,
            handler = { msg ->
                @Suppress("UNCHECKED_CAST")
                val payload = (msg as ActorMessage<String>).payload
                when {
                    payload.contains(Regex("(?i)never|always")) -> emit(msg, "frustrated")
                    else -> emit(msg, "neutral")
                }
            },
            bufferSize = 16,
        )
        createOutputPort(
            name = "tone",
            messageClass = ActorMessage::class,
            bufferSize = 16,
        )
    }

    private suspend fun emit(original: ActorMessage<String>, tone: String) {
        getPort("tone", ActorMessage::class)?.send(
            ActorMessage(
                correlationId = original.correlationId,
                payload = tone,
                sender = name,
            )
        )
    }

    override suspend fun start() {
        if (getPort("userText", ActorMessage::class) == null) initialize()
        super.start()
    }
}
```

Note: `ActorMessage` is generic. For `when (...)` dispatch, switch on `message.payload` (or its type), not on a non-existent `message.type` field.

## Errors

Exceptions thrown inside a port's handler are caught by the actor's runtime; the actor's state transitions to `ActorState.Error(exception)` and the supervising scope can decide what to do. To bound a message's processing time, pass `processingTimeout` to `createPort`; on timeout the framework records the error in `ActorMetrics` and continues consuming.

## Lifecycle sequencing

- `start()` after first construction creates the consumer jobs for input ports.
- `stop()` cancels the consumer jobs but **preserves the port registry** so the actor can be restarted cleanly (or its connections re-routed).
- `start()` after `Stopped` only restarts consumers for input (auto-processed) ports — output-only ports created via `createOutputPort` are not consumed by the actor.
- `dispose()` (from `Disposable`) tears everything down, including the actor's scope.

## Testing

Patterns used in `lib/src/jvmTest/kotlin/io/github/solaceharmony/core/actor/`:

1. Construct the actor; assert `state == ActorState.Initialized`.
2. `start()`; assert `Running` and that `getPort(...)` returns a non-null port.
3. Send messages through input ports; observe outputs through output ports' `asChannel()`.
4. `stop()`; assert `Stopped` and that ports are still registered.
5. `start()` again to verify clean restart.
6. `dispose()`; assert no leaked coroutines.

## See also

- [`wiki/Actor-System.md`](../../../../../../../../../wiki/Actor-System.md) — design-level docs for actor architecture.
- [`wiki/SupervisorActor.md`](../../../../../../../../../wiki/SupervisorActor.md) — dynamic registration, lifecycle management, and hot-swap behavior.
- [`wiki/Actor-Communication-Sequence.md`](../../../../../../../../../wiki/Actor-Communication-Sequence.md) — actor communication sequence diagram.
- [`wiki/Actor-System-Class-Diagram.md`](../../../../../../../../../wiki/Actor-System-Class-Diagram.md) — actor system class diagram.
- [`../kernel/channels/`](../kernel/channels/) — the port runtime that actors use for message passing.
- [`../lifecycle/`](../lifecycle/) — the `Lifecycle` / `Disposable` contract this module implements.
