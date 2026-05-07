# Pipeline DSL — Composable Request Shaping

A request to a language model isn't a single thing. It's a stack
of decisions: which provider's API endpoint to hit, which wire
format the provider expects, which tool format the model speaks,
how to advertise the available tools, what family-specific
prompt shape to apply, what sampler parameters to use, where to
write the result, what to do on retry. Each decision is
independent of the others. Most of them have only a handful of
sensible answers. The combinations matter, and they have to be
consistent.

The naive approach — one big function with a long parameter list
and a series of `when (provider)` branches — works briefly and
then collapses under its own complexity the moment a new
provider arrives or a model family ships with a slightly
different format. The Pipeline DSL is the architecture's answer:
each independent decision is a *block*, blocks compose by name,
and a request's pipeline is just a list of which blocks to apply
in which order.

## The block vocabulary

A block is a named piece of behaviour that participates in
shaping a request or response. Thirteen blocks ship with the
current design, organised into four namespaces:

**Protocol blocks** — *how to talk to the provider on the wire.*

| Block | Purpose |
| --- | --- |
| `protocol.ollama` | Ollama's native streaming JSON API |
| `protocol.openai` | OpenAI-compatible REST (also LM Studio, vLLM, llama.cpp) |
| `protocol.anthropic` | Anthropic API |

**Codec blocks** — *how tool calls are encoded inside model output.*

| Block | Purpose |
| --- | --- |
| `codec.mcp_over_xml` | XML-tagged tool calls in text (for models without native function calling) |
| `codec.mcp_over_functions` | Native function-calling JSON unwrapped from MCP envelopes |
| `codec.mcp_over_tools` | Anthropic-style tool-use blocks |

**Tools blocks** — *how the model is told what tools exist.*

| Block | Purpose |
| --- | --- |
| `tools.negotiation_advertise` | Negotiation handshake at session start |
| `tools.always_advertise` | Tools listed in every request |
| `tools.lazy_advertise` | Tools listed only when requested |

**Family blocks** — *family-specific prompt shaping and sampler tuning.*

| Block | Purpose |
| --- | --- |
| `family.qwen3` | Qwen3-specific prompt scaffolding and stop tokens |
| `family.llama3` | Llama-3-specific scaffolding |
| `family.mistral` | Mistral-specific scaffolding |
| `family.claude` | Claude-specific scaffolding |

A request's pipeline is a list of blocks, e.g.:

```
[
    "protocol.ollama",
    "codec.mcp_over_xml",
    "tools.negotiation_advertise",
    "family.qwen3"
]
```

Each block sees the request, applies its concern, and passes the
shaped request to the next block. The order matters because
later blocks may depend on what earlier blocks did — `family.qwen3`
adds Qwen-specific scaffolding around the tool-call format that
`codec.mcp_over_xml` already established.

## Why named blocks rather than a config struct

A flat configuration struct has the same expressive power on
paper, but it loses something important in practice: *only valid
combinations exist*. With a flat config, the framework has to
detect at runtime that *"Ollama provider cannot use OpenAI
protocol block"* and emit an error. With a pipeline, the
relationship between provider and protocol is encoded in the
block list itself; an invalid combination is one that doesn't
parse cleanly into a sequence of compatible blocks, and the
validation happens up front.

The validator does still exist — `ConfigValidator` checks for
incompatibilities and warns on suspicious combinations — but
its job is to catch the cases where someone *names* an invalid
combination, not to reason about every possible parameter
interaction. The pipeline shape is the structure; the validator
is the safety net.

## Templates as canonical pipelines

Most users won't compose blocks by hand. The framework ships
templates that name canonical pipelines for known
provider-model combinations:

```kotlin
val ollamaQwenConfig = CodexConfig(
    pipeline = PipelineConfig(enabled = true),
    negotiation = NegotiationConfig(enabled = true),
    negotiationDebug = true,
    provider = "ollama",
    model = "qwen3-coder:30b",
    modelFamily = "qwen",
    blocks = listOf(
        "protocol.ollama",
        "codec.mcp_over_xml",
        "tools.negotiation_advertise",
        "family.qwen3"
    )
)

val lmStudioQwenConfig = CodexConfig(
    // ...
    provider = "lmstudio",
    model = "qwen3-coder-30b-a3b-instruct-1m",
    modelFamily = "qwen",
    blocks = listOf(
        "protocol.openai",          // LM Studio's OpenAI-compatible API
        "codec.mcp_over_functions",
        "tools.negotiation_advertise",
        "family.qwen3"
    )
)
```

Two templates for the same model family running on different
providers. Notice what's the same and what changes. The model
family is the same (`qwen3`), so the family block is the same.
The wire protocol is different (Ollama vs OpenAI-compatible),
so the protocol block changes. The codec changes too because
LM Studio's OpenAI-compatible endpoint exposes function-calling
natively, while Ollama needs the XML-in-text fallback for
Qwen3 tool calls.

The templates compose what would otherwise be twenty-line
configurations into named entries the user picks from. The
framework's `ConfigTemplateRegistry` ships with a starter set;
deployments register their own.

## Pipeline execution

The pipeline runs as a chain of suspend functions, each of which
takes a request and produces a request:

```kotlin
typealias PipelineBlock = suspend (Request) -> Request

class Pipeline(private val blocks: List<PipelineBlock>) {
    suspend fun execute(initial: Request): Request =
        blocks.fold(initial) { request, block -> block(request) }
}
```

The simplicity is deliberate. Each block is a function; the
pipeline is a fold; there's no shared state between blocks
except what's encoded in the request object. A block can
inspect the request, modify it, or short-circuit by throwing.
Coroutine cancellation propagates through the chain naturally.

The response side is the mirror image:

```kotlin
typealias ResponseBlock = suspend (Response) -> Response

class ResponsePipeline(private val blocks: List<ResponseBlock>) {
    suspend fun process(raw: Response): Response =
        blocks.fold(raw) { response, block -> block(response) }
}
```

For the Qwen3 + Ollama case above, the response pipeline
includes `codec.mcp_over_xml` running in *parse* mode: the raw
text response gets scanned for XML tool-call blocks, those
blocks are extracted into structured tool-call records, and the
records flow into the [Neutral History](../mcp-tools/) substrate
as `TOOL_CALL` events.

## Negotiation

`tools.negotiation_advertise` is the most subtle block. It's the
handshake at session start where the framework declares its
available tools to the model in whatever format the model
supports. The negotiation produces a contract: *for this
session, when you want to call a tool, emit it in this shape;
I'll parse it.*

The negotiation block reads from the
[ToolFormatNegotiator](../mcp-tools/) which knows what each
model family supports natively. For Qwen3 over Ollama, the
negotiation results in *XML-in-text* because Qwen3 doesn't
have native function calling and Ollama doesn't expose a
function-calling endpoint. For Qwen3 over LM Studio, the
negotiation results in *function-calling* because LM Studio's
OpenAI-compatible endpoint does expose it.

The same model family with the same tools produces different
wire shapes depending on the provider, and the negotiation
block is what reconciles the two.

## Validation and warnings

The framework validates pipelines at config-load time. The
`ValidationResult` has three classes:

```kotlin
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
```

Errors stop the framework from starting. The current rules name
specific incompatibilities:

- *"Ollama provider cannot use OpenAI protocol block"*
- *"LM Studio provider cannot use Ollama protocol block"*

These are direct contradictions between named blocks. They
shouldn't happen in practice — templates encode the right
combinations — but a hand-edited config can hit them, and the
framework refuses to run rather than producing confusing
runtime errors.

Warnings are advisory:

- *"Debug logging enabled — consider disabling in production"*

The framework runs through warnings; they're surfaced to the
user but don't block startup.

## ConfigBuilder DSL for programmatic composition

For programmatic users — tests, integrations, deployments that
generate configs from external sources — the framework provides
a small DSL:

```kotlin
val config = ConfigBuilder()
    .pipeline(enabled = true)
    .negotiation(enabled = true)
    .debug(enabled = false)
    .provider("ollama")
    .model("qwen3-coder:30b")
    .modelFamily("qwen")
    .blocks(
        "protocol.ollama",
        "codec.mcp_over_xml",
        "tools.negotiation_advertise",
        "family.qwen3"
    )
    .build()
```

The DSL is a thin wrapper over the data class; its value is
discoverability and refactor safety, not behaviour.

## Implementation status

**Sketched, not built.** The
`docs/sketch-architecture/ConfigTemplates.kt` file has the data
class definitions, the ConfigManager interface, the
ValidationResult shape, and the two starter templates (Ollama +
Qwen3, LM Studio + Qwen3). The actual block implementations and
the pipeline executor are TODO.

The work order:

1. Build the block registry — a typed map from block name to
   `PipelineBlock` function. Verify the fold-based executor
   composes correctly.
2. Implement the four protocol blocks. Each is essentially an
   HTTP client wrapped to match the provider's wire format.
3. Implement the three codec blocks. Each is a pair of
   transformations: request-side serialisation and
   response-side parsing.
4. Implement the three tools blocks. The negotiation handshake
   is the hardest of these because it involves a round-trip
   with the model.
5. Implement family blocks per provider-model combination as
   they come up. The framework should ship with at least
   `qwen3`, `llama3`, and `claude`.

## Open questions

- **Block ordering constraints.** Some block combinations are
  order-sensitive; the framework currently relies on templates
  to encode the right order. Should there be explicit
  precedence metadata so the framework can sort blocks
  automatically?
- **Per-request pipeline override.** A long-running session might
  want different pipelines for different *kinds* of requests
  (a coding tool call vs a casual chat). Currently the pipeline
  is per-session; per-request override is open.
- **Block versioning.** When a provider changes its API shape,
  the protocol block has to update. How are deployed configs
  invalidated? Probably explicit version tags on blocks plus
  validator checks.
- **Async block composition.** The current fold-based executor
  is sequential. For independent blocks (e.g., multiple
  tools-related blocks that don't depend on each other), can
  they run in parallel? Probably not worth the complexity unless
  pipeline latency becomes a bottleneck.

## Cross-references

- [providers](../providers/) — protocol blocks instantiate
  per-provider clients; provider abstractions live there.
- [mcp-tools](../mcp-tools/) — codec blocks and tools blocks
  implement the format/protocol/storage separation; the
  pipeline is what wires the layers together at runtime.
- [supervisor](../supervisor/) — Supervisor's tool-execution
  approval gate runs after the pipeline produces a tool call
  but before the wire request goes out.
- [shared-memory](../shared-memory/) — pipeline stages can
  back onto shared-memory primitives for stage-to-stage
  data flow.

## What the DSL is in service of

The architecture's commitment that adding a new provider, a new
model family, or a new tool format should be an additive change
— a new block, a new template, no surgery on the framework's
core. Without the block decomposition, every provider-specific
or family-specific quirk becomes a special case in the request
shaping logic, and the special cases multiply geometrically with
each new combination. With it, each quirk is a named block, the
combinations are templates, and the framework handles the
unfamiliar combinations as well as it handles the familiar
ones — by following the pipeline.

That extensibility is what the DSL exists for. It's not just
configuration; it's the framework committing to the shape of
its own future.

---

[← Features index](../README.md)
