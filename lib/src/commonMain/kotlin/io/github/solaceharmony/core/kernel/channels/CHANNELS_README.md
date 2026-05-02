# Channels module — `io.github.solaceharmony.core.kernel.channels.ports`

Type-safe, resource-managed message passing between actors. Built on `kotlinx.coroutines.channels.Channel` and the [`Disposable`](../../../lifecycle/) contract.

## Public surface

| Type | Role |
|---|---|
| `Port<T : Any>` | The single port abstraction. Implements [`Disposable`](../../../lifecycle/). Carries `id`, `name`, `type: KClass<out T>`, `asChannel(): Channel<T>`, and `suspend fun send(message: T)`. Hosts nested types and a companion `connect(...)` factory. |
| `BidirectionalPort<T>` | The concrete implementation. One Kotlin coroutines `Channel<T>` underneath. |
| `Port.MessageHandler<IN, OUT>` | Per-connection async transformer. `suspend fun handle(message: IN): OUT`. |
| `Port.ProtocolAdapter<SOURCE, TARGET>` | Encodes from a source protocol to a target protocol; `canHandle(KClass, KClass)` for capability checking. |
| `Port.ConversionRule<IN, OUT>` | Abstract per-rule converter with `convert`, `canHandle`, `describe`; a `create { ... }` companion factory for inline rules. |
| `Port.PortConnection<IN, OUT>` | The active routing runtime — owns a coroutine job, a mutex, the rule chain, the protocol adapter. Methods: `start(scope: CoroutineScope): Job`, `stop()`, `suspend stopAndJoin()`, `validateConnection()`. |
| `Port.connect(source, target, ...)` | The factory. Validates the connection and returns a `PortConnection` ready to be `start`-ed. |
| `PortException.Validation` / `PortConnectionException` | The error types thrown when a connection cannot be established or a conversion fails. |
| `MessageHandlers` | Standard handler implementations (e.g., logging). |

There is **one** `Port<T>` interface. Earlier drafts of this module had separate `InputPort` / `OutputPort` / `PortRegistry` / `ConnectionManager` types — those were collapsed. Direction is a *role* an actor assigns at registration time:

- `actor.createPort(name, messageClass, handler, ...)` registers the port and starts a consumer coroutine that drains the channel and feeds the handler. This is the **input** role.
- `actor.createOutputPort(name, messageClass, ...)` registers the port for `send()` use only — no consumer is started. This is the **output** role.

## Connecting two ports

```kotlin
import io.github.solaceharmony.core.kernel.channels.ports.Port

val source: Port<String> = actorA.getPort("output", String::class)!!
val target: Port<String> = actorB.getPort("input", String::class)!!

val connection = Port.connect(source, target)
connection.start(scope = workflowScope)

// Optional during shutdown — cancels the routing coroutine and waits.
connection.stopAndJoin()
```

`Port.connect` validates the type compatibility before returning. If the types differ, you can either supply a `ProtocolAdapter` or a non-empty list of `ConversionRule`s; if neither can bridge the types, `validateConnection()` throws `PortConnectionException` immediately.

## Message handlers, protocol adapters, conversion rules

These are the three points where a connection can transform messages between source and target:

```kotlin
val connection = Port.connect(
    source = a.getPort("rawText", String::class)!!,
    target = b.getPort("annotated", AnnotatedText::class)!!,
    handlers = listOf(LoggingHandler()),                       // observe (or modify) every message
    protocolAdapter = TextToAnnotated(),                       // type-bridge from source to target
    rules = listOf(NormalizeWhitespace, RemoveControlChars),   // ordered conversion chain
)
```

The routing coroutine, started by `connection.start(scope)`, applies them in this order:

1. Read one message from `source.asChannel()`.
2. Pass it through every `MessageHandler` (sequentially).
3. If a `ProtocolAdapter` is set, run `encode(...)` to convert source-protocol to target-protocol.
4. If `rules` is non-empty, apply each `ConversionRule.convert(...)` in order.
5. `target.send(...)`.

If the target's channel is already closed, the routing coroutine returns gracefully — it does not propagate the exception upward.

## Lifecycle and cancellation

`PortConnection` is built around structured concurrency:

- `start(scope)` launches the routing job in the *caller's* `CoroutineScope`. In practice this is the source actor's scope, so cancelling the source actor cancels its connections.
- `stop()` cancels the job under the connection's mutex; non-blocking.
- `stopAndJoin()` cancels and `join()`s the job; use this from workflow shutdown to avoid sending into a target that is closing.

The `Port` itself implements `Disposable`. Disposing a port closes its underlying channel, which causes any `PortConnection` reading from it to exit its routing loop on the next iteration.

## Why one `Port` instead of `InputPort`/`OutputPort`/`PortRegistry`

The earlier hierarchy required callers to commit to direction at port-creation time and added a registry layer for hot-swap. The collapsed design relies instead on:

- One concrete `BidirectionalPort<T>` whose channel can be sent-to and consumed-from.
- The actor itself owns its port registry (the `getPort`/`createPort`/`removePort` API on `Actor`); this is enough for hot-swap because the actor mediates registration.
- Direction is a role, not a type — one symmetric data structure with two usage patterns simplifies the API and removes a class of "you connected an InputPort to an InputPort" errors.

If you need to forbid sending into a port that an actor uses only for output (or vice versa), do that at the actor's policy layer rather than at the port type.

## See also

- [`../../actor/`](../../actor/) — the actor runtime that calls into this module.
- [`docs/components/kernel/`](../../../../../../../../docs/components/kernel/) — design-level docs for the kernel.
- [`docs/components/kernel/channel_system.md`](../../../../../../../../docs/components/kernel/channel_system.md) — channel-system design.
