# MCP & Tool Format — Three Things, Not One

There are three things that look similar enough to get conflated,
and have been, repeatedly, in earlier sketches of this code. They
are different. The architecture's commitment is to keep them
separate, because conflating them produces the kind of bug that
"works" in test conditions and silently produces wrong behaviour
in production.

The three things:

1. **MCP Protocol.** The JSON-RPC 2.0 server-client protocol that
   the framework uses to talk to *tool servers*. MCP is between
   *us and the server*.
2. **Tool Format Negotiation.** The matter of what JSON shape a
   *model* natively expects when emitting tool calls — OpenAI's
   function-calling format versus Anthropic's tool-use format
   versus a free-text fallback. Tool format is between *us and
   the model*.
3. **Neutral History.** The framework's *internal* universal
   storage format for events. Neutral History is between *us and
   ourselves*, across time and across providers.

The earlier code conflated all three. It sent a model a message
like *"emit this JSON if you support MCP"*, observed the model
repeat the pattern, and concluded *"MCP protocol confirmed!"* —
which is not MCP protocol detection. It's not anything. It's the
model doing what models do, namely echoing patterns from context.
The conflation produced a system that looked like it negotiated
protocols but actually did nothing of the kind.

This page documents the three concerns separately, in the
vocabulary the architecture commits to, with the boundaries
between them explicit.

## Concern 1: The MCP Protocol

The Model Context Protocol is a JSON-RPC 2.0 spec for talking to
tool servers. The framework is the client; the tool servers are
external processes (or in-process libraries with an MCP shim).
The protocol defines two operations the framework cares about:

```kotlin
interface MCPProtocol {
    fun callTool(request: MCPToolCallRequest): MCPToolCallResponse
    fun listTools(): MCPListToolsResponse
}
```

A `tools/call` request:

```kotlin
data class MCPToolCallRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String = "tools/call",
    val params: MCPToolParams
)

data class MCPToolParams(
    val name: String,
    val arguments: Map<String, Any>
)
```

The response is structured content — text, image references,
binary blobs:

```kotlin
data class MCPContent(
    val type: String,             // "text", "image", etc.
    val text: String? = null,
    val data: String? = null
)
```

The protocol doesn't care what model the framework is using. The
protocol doesn't care how the framework decided to call this
tool. The protocol cares that the request is well-formed
JSON-RPC and that the response is well-formed JSON-RPC. It is a
boundary, not a behaviour.

The framework's MCP client is what implements this side of the
boundary. It opens connections to tool servers (typically over
stdio or websockets), serialises requests, deserialises responses,
and surfaces errors as Kotlin exceptions or sealed result types.
What the framework does *with* a tool result is none of the
protocol's business.

## Concern 2: Tool Format Negotiation

A model's tool-calling format is the JSON shape it natively
emits when asked to use a tool. Different model families have
different formats:

```kotlin
enum class ToolFormat {
    // Native model formats
    FUNCTION_CALLING,      // OpenAI-style
    TOOL_USE,              // Anthropic-style
    JSON_SCHEMA,           // JSON Schema based
    XML_TOOLS,             // XML-based tool calls
    TEXT_BASED,            // Plain text instructions

    // Protocol formats (NOT model formats; included for clarity)
    MCP_PROTOCOL,          // JSON-RPC 2.0 server-client
    OPENAI_PROTOCOL,       // REST API protocol
    ANTHROPIC_PROTOCOL     // Anthropic API protocol
}
```

Format negotiation is the act of deciding, for a given model,
which native format the framework will use to talk to that model.
The negotiation typically happens once at startup based on what
the model documentation says, what the API capabilities expose,
and what fallback the framework supports.

```kotlin
interface ToolFormatNegotiator {
    fun detectSupportedFormats(model: Model): Set<ToolFormat>
    fun negotiateFormat(model: Model, requestedFormat: ToolFormat): ToolFormat
}
```

A negotiation result is a *contract* between the framework and
the model: *for this session, when you want to call a tool,
emit it in this shape; I'll parse it and dispatch.* The contract
is necessary because the framework needs deterministic parsing
of model output, and the model's training only produces one
format reliably.

The native format the model emits is *not* MCP. It might be
*function-calling JSON*, which the framework then translates into
an MCP request before sending it across the wire to a tool
server. Format on the model side; protocol on the wire side.
Translation between them happens in the framework.

## Concern 3: Neutral History

Underneath both of the above lives the framework's universal
event storage format. Every tool call, every tool result, every
model response, every user message, every system event lands in
Neutral History as a typed event:

```kotlin
data class NeutralEvent(
    val id: String,
    val timestamp: Long,
    val type: EventType,
    val content: NeutralContent,
    val metadata: Map<String, Any>
)

enum class EventType {
    TOOL_CALL, TOOL_RESULT, MODEL_RESPONSE, USER_MESSAGE, SYSTEM_EVENT
}
```

Neutral History is provider-agnostic and format-agnostic by
design. A function-calling tool invocation and a tool-use tool
invocation produce the *same* `NeutralEvent` shape; the only
difference is the metadata field that records which native format
was used. The framework reads Neutral History when it needs
context, when it replays for the [Confusion Corrector]
(../confusion-corrector/), when it migrates between providers.

Conversion in the other direction — from Neutral History back to
a model-specific format — is also explicit:

```kotlin
fun convertToModelFormat(
    event: NeutralEvent,
    targetFormat: ToolFormat
): ModelSpecificEvent
```

This is what makes the framework cross-provider in a way that
preserves history. A session that started with one provider's
function-calling format can be replayed against another
provider's tool-use format because the history is stored
neutrally and converted on demand.

Neutral History is also the substrate underneath
[Reflection Memory](../reflection-memory/README.md) — the same one the
agent's per-agent memory tier indexes over. The two are
deliberately the same thing: there is one substrate, and the
tool calls and the cognitive reflections are different
event types in the same log.

## Why the separation matters

Three failure modes that happen when the boundaries blur:

**Pretending to detect MCP support by asking the model.** The
specific bug the earlier code committed: send the model a
prompt asking it to emit MCP-shaped JSON, observe the response,
conclude the model speaks MCP. Models echo patterns. The "test"
passed for any sufficiently capable model regardless of whether
the framework actually had an MCP client wired up. The fix is
to stop conflating model-side format with wire-side protocol;
they're independent concerns.

**Storing events in a model-specific format.** Every time the
provider changes, the history becomes incompatible with the new
provider. The framework either has to migrate the entire
history (expensive) or maintain provider-specific history shards
(complex). Neutral History solves this by being the only
serialisation format that ever touches durable storage; native
formats exist only at the boundary with the model.

**Forwarding model output directly to tool servers.** A model
that emits a tool call in function-calling format cannot be
forwarded as-is to an MCP tool server, because the JSON-RPC
envelope is missing. Code that tries to skip the conversion
either fails noisily (best case) or sends malformed requests
(worse case). The conversion is the boundary; the framework's
job is to not skip it.

## How the three concerns flow together

A typical tool invocation involves all three:

```
1. User asks the agent to do something that requires a tool.
2. Supervisor (the SRAF/Magentic Supervisor) decides to call the tool.
3. Framework asks the model to emit a tool call.
4. Model emits a tool call in its native format (e.g.,
   function-calling JSON).
5. Framework parses the native format and constructs a
   NeutralEvent of type TOOL_CALL.
6. Framework writes the NeutralEvent to Neutral History.
7. Framework converts the NeutralEvent to MCP request format
   (JSON-RPC 2.0).
8. MCP client sends the request to the tool server.
9. Tool server returns a JSON-RPC response.
10. Framework deserialises the response and constructs a
    NeutralEvent of type TOOL_RESULT.
11. Framework writes the NeutralEvent to Neutral History.
12. Framework converts the NeutralEvent back to the model's
    native format and feeds it as context.
13. Model produces its next output, having seen the tool result.
```

Three different format/protocol/storage concerns, each touched
exactly where it belongs. The model never sees MCP; the wire
protocol never sees function-calling; the durable store never
sees provider-specific shapes. The boundaries are the
correctness.

## The Supervisor's role

Tool execution is gated by the [Supervisor's safety boundary]
(../supervisor/). Steps 2 and 7 above pass through the
Supervisor's `approve(action)` check; the MCP request only goes
out the wire if the Supervisor signs off, the risk assessment is
acceptable, and (for HIGH-risk actions) the user has confirmed.

This is where the Magentic-lineage *mandatory approval* rule
lands operationally: every MCP request is an action; every
action passes through the Supervisor; every Supervisor decision
is recorded in Neutral History as a `SYSTEM_EVENT`. The
auditability falls out naturally from the layered design.

## Implementation status

**Sketched, not fully built.** The
`docs/sketch-architecture/ArchitectureClarificationMCP.kt` file
has the type definitions for all three concerns. The actual MCP
client, format negotiator, and Neutral History store are TODO
implementations.

The work order:

1. Build the Neutral History store on top of the
   [Reflection Memory](../reflection-memory/README.md) substrate. Verify
   that all event types serialise and deserialise correctly.
2. Build the MCP client over stdio (the most common transport).
   Test against a known-good MCP tool server.
3. Build the Tool Format Negotiator. Start with hard-coded
   per-provider format tables; the negotiation is mostly a
   lookup, not a probe.
4. Build the conversion functions between native formats and
   Neutral History.
5. Wire the Supervisor approval gate at the right point in the
   flow.

## Open questions

- **Format detection vs declaration.** The current design assumes
  the framework is told what format a model speaks (via
  configuration or provider metadata). Auto-detecting is harder
  and less reliable; should the framework support a probe-based
  detection mode for unknown providers?
- **MCP transport choice.** stdio is the default; websockets is
  the production option. WASM/JS deployments may need a third
  transport.
- **Tool capability discovery.** When the framework calls
  `listTools()` on a tool server, what's the schema for filtering
  and presenting tools to the model? The MCP protocol returns the
  list; the question is how the framework projects the list into
  the model's prompt.
- **Cross-provider session migration.** Moving an in-flight
  session from one provider to another is supported by Neutral
  History's design but requires conversion of the in-flight
  context. The exact conversion policy is open.

## Cross-references

- [supervisor](../supervisor/README.md) — gates MCP requests through the
  approval boundary; tool execution requires Supervisor sign-off.
- [reflection-memory](../reflection-memory/README.md) — Neutral History
  events land in the substrate alongside cognitive reflections.
- [providers](../providers/README.md) — provider abstraction reads which
  ToolFormat each provider's models speak.
- [pipeline](../pipeline/README.md) — the pipeline DSL composes tool
  calls and reasoning steps; MCP is one of its outbound
  destinations.

## What the separation is in service of

The architecture's commitment to being **provider-agnostic in
storage, provider-specific in negotiation, and protocol-clean on
the wire**. Without the three-layer separation, the framework
becomes brittle: a provider change breaks history, a tool-server
change breaks model output, a model change breaks both. With
the separation, each layer has its own boundary and its own
contract, and changes in one don't propagate into the others.

That isolation is what makes the framework portable across
providers, durable across versions, and trustworthy in audits.
The three-thing distinction is the architecture committing to
the discipline.

---

[← Features index](../README.md)
