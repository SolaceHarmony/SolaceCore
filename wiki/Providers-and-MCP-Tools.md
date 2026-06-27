<!-- topic: Solace AI -->
<!-- title: Provider Specs -->

# Providers — A Spec, Not a Switch Statement

## Related Topics

- [MCP and Tool Format](MCP-and-Tool-Format): tool protocol, model tool format, and neutral history boundaries.
- [Pipeline DSL](Pipeline-DSL): protocol blocks instantiate clients per provider spec.
- [Supervisor AI](Supervisor-AI): provider identity feeds risk assessment.
- [Reflection Memory](Reflection-Memory): every event records its provider for portability.

Every provider — Ollama, LM Studio, OpenAI, Anthropic, vLLM,
llama.cpp's HTTP server, the next one — has its own quirks. The
base URL is different. The auth scheme is different. The path
structure is different. The streaming transport is different.
The chunk schema is different. The tool-support story is
different. The detection rules — *how do we know we're talking
to this provider* — are different.

A naive integration handles each provider with its own block of
code. Five providers means five integrations. The sixth provider
means the sixth integration plus, usually, a refactor of the
shared code that has finally accumulated enough special cases to
be unmanageable.

The architecture's commitment is to handle providers
*declaratively*. Each provider is a `ProviderSpec` — a data class
that names its API style, base URL, path structure, auth, streaming
shape, tool support, and detection rules. The framework reads the
spec at startup and runs the right behaviour. Adding a provider is
adding a spec; the framework's core code doesn't change.

## What lives in a ProviderSpec

```kotlin
data class ProviderSpec(
    val provider: String,
    val metadata: ProviderMetadata,
    val api: ApiSpec,
    val toolSupport: ToolSupportSpec,
    val detection: DetectionSpec
)
```

Five top-level concerns, each with its own sub-spec.

### Metadata

```kotlin
data class ProviderMetadata(
    val name: String,
    val version: Int
)
```

Display name and spec version. The version field exists so the
framework can refuse to load a spec written against a future
version of the schema. It's small and unglamorous; it earns its
keep the first time a spec format change ships.

### API spec

The API spec is the largest. It declares:

- **Style** — `OPENAI_COMPATIBLE`, `ANTHROPIC`, `CUSTOM`. The
  style determines which protocol block the
  [pipeline DSL](Pipeline-DSL) selects by default.
- **Base URL** — both the default and the environment variable
  that can override it. Most providers respect their conventional
  env var (`OLLAMA_BASE_URL`, `OPENAI_API_BASE`, etc.); the spec
  records both so the framework picks the right precedence.
- **Paths** — a map from logical operation (`chatCompletions`,
  `embeddings`, `models`) to the HTTP method, path, request shape,
  and response shape. The request shape names the JSON fields the
  provider expects (`model`, `messages`, `tools`, `stream`); the
  response shape names the JSONPath where the relevant content
  lives (`$.choices`).
- **Auth** — `NONE`, `BEARER_TOKEN`, `API_KEY`. Determines what
  header the framework adds to outgoing requests.
- **Streaming** — transport (SSE, WebSocket, polling), the SSE
  line prefix and done token, the chunk schema (where in the
  chunk JSON the delta text lives, where tool-call deltas live).

```kotlin
data class StreamingSpec(
    val transport: StreamingTransport,
    val sse: SseSpec? = null,
    val chunkSchema: ChunkSchema? = null
)

data class SseSpec(
    val linePrefix: String,        // "data:"
    val doneToken: String          // "[DONE]"
)

data class ChunkSchema(
    val deltaTextPath: String? = null,    // "$.choices[0].delta.content"
    val toolCallsPath: String? = null,    // "$.choices[0].delta.tool_calls"
    val functionCallPath: String? = null
)
```

The chunk schema is what lets one streaming parser handle
provider-specific chunk shapes. Ollama emits one structure;
Anthropic emits another; the parser reads the schema from the
spec and extracts deltas at the right paths.

### Tool support

```kotlin
data class ToolSupportSpec(
    val functions: ToolCapability,
    val xmlInText: Boolean,
    val mcp: Boolean,
    val handshake: HandshakeSpec? = null
)

enum class ToolCapability { SUPPORTED, UNSUPPORTED, UNKNOWN }
```

The tool-support sub-spec records what the provider exposes:

- **`functions`** — does the provider support OpenAI-style
  function-calling natively? `SUPPORTED` means the JSON path
  always works. `UNSUPPORTED` means it never does. `UNKNOWN`
  means it depends on the model — the framework probes at
  startup using the handshake.
- **`xmlInText`** — does the framework support XML-in-text tool
  calls as a fallback? Most providers do, because XML tool calls
  are just text the model emits and the framework parses.
- **`mcp`** — does the provider have any direct MCP support? Most
  don't; MCP is a server-client protocol the framework
  implements outside the provider boundary.
- **`handshake`** — the probe the framework runs at startup to
  detect tool-calling capability when `functions == UNKNOWN`. The
  XML probe is the fallback that always works:

```kotlin
data class XmlProbeSpec(
    val enabled: Boolean,
    val tagName: String,
    val pattern: String  // e.g., "<cap_probe nonce=\"\${nonce}\">ok</cap_probe>"
)
```

The framework sends a small prompt asking the model to emit a
specific tagged response with a unique nonce; if the model
replies with the expected pattern, the framework concludes the
model can be relied on to emit XML-tagged tool calls reliably.

### Detection

```kotlin
data class DetectionSpec(
    val openaiFunctions: OpenAIFunctionsDetection? = null,
    val xmlMatchers: XmlMatchersSpec? = null,
    val converters: Map<String, String>? = null
)
```

Detection is how the framework recognises tool calls in
streaming output. Two mechanisms:

- **OpenAI-functions detection** — looks for `tool_calls` or
  `function_call` fields in chunks. When found, the chunk is a
  tool call.
- **XML matchers** — regex blocks for matching XML-tagged tool
  calls in text output. Includes an exclusion list (`think`,
  `tool_result`, `tool_call`) for tags that look tool-call-shaped
  but aren't.

```kotlin
data class XmlMatchersSpec(
    val blocks: List<String>,        // regex patterns
    val excludeTags: List<String>
)
```

The `converters` map names builtin transformations the framework
applies. `"toolUseXmlToJson" to "builtin:xmlToolUseToJson"` means
*when you find an XML tool-use block, run it through the builtin
xml-to-JSON converter to normalise it for Neutral History*.

## The Ollama spec, fully expanded

Ollama is the reference spec. It captures most of the patterns the
framework supports:

```kotlin
val ollamaSpec = ProviderSpec(
    provider = "ollama",
    metadata = ProviderMetadata(name = "Ollama", version = 1),
    api = ApiSpec(
        style = ApiStyle.OPENAI_COMPATIBLE,
        baseUrl = BaseUrlSpec(
            env = "OLLAMA_BASE_URL",
            default = "http://localhost:11434/v1"
        ),
        paths = mapOf(
            "chatCompletions" to PathSpec(
                method = HttpMethod.POST,
                path = "/chat/completions",
                request = RequestSpec(
                    modelField = "model",
                    messagesField = "messages",
                    toolsField = "tools",
                    streamField = "stream"
                ),
                response = ResponseSpec(choicesPath = "$.choices")
            )
        ),
        auth = AuthType.NONE,
        streaming = StreamingSpec(
            transport = StreamingTransport.SSE,
            sse = SseSpec(linePrefix = "data:", doneToken = "[DONE]"),
            chunkSchema = ChunkSchema(
                deltaTextPath = "$.choices[0].delta.content",
                toolCallsPath = "$.choices[0].delta.tool_calls",
                functionCallPath = "$.choices[0].delta.function_call"
            )
        )
    ),
    toolSupport = ToolSupportSpec(
        functions = ToolCapability.UNKNOWN,    // depends on model
        xmlInText = true,
        mcp = true,
        handshake = HandshakeSpec(
            xmlProbe = XmlProbeSpec(
                enabled = true,
                tagName = "cap_probe",
                pattern = "<cap_probe nonce=\"\${nonce}\">ok</cap_probe>"
            )
        )
    ),
    detection = DetectionSpec(
        openaiFunctions = OpenAIFunctionsDetection(
            fields = listOf("tool_calls", "function_call")
        ),
        xmlMatchers = XmlMatchersSpec(
            blocks = listOf("<([A-Za-z_][\\w-]*)>[\\s\\S]*?<\\/\\1>"),
            excludeTags = listOf("think", "tool_result", "tool_call")
        ),
        converters = mapOf(
            "toolUseXmlToJson" to "builtin:xmlToolUseToJson"
        )
    )
)
```

Notice what's interesting. Ollama's API style is
`OPENAI_COMPATIBLE` because Ollama exposes an OpenAI-compatible
endpoint. The auth is `NONE` because Ollama runs locally without
auth. The function-calling capability is `UNKNOWN` because it
depends on which model is loaded — Qwen3 may not natively
support functions, while Llama 3.1 might. The XML probe is the
fallback that detects tool-calling capability per-model at
session start.

The XML matcher excludes `think`, `tool_result`, and `tool_call`
tags because Qwen3 emits `<think>` tags for chain-of-thought
output, and `tool_result` and `tool_call` are framework-internal
markers that shouldn't be re-detected as new tool calls. The
exclusion list is the spec encoding empirical knowledge about
how specific models behave.

## The provider registry

The framework maintains a registry of provider specs:

```kotlin
object ProviderRegistry {
    private val providers = mutableMapOf<String, ProviderSpec>()

    init {
        register(ollamaSpec)
        // additional specs as they're added
    }

    fun register(spec: ProviderSpec)
    fun getProvider(name: String): ProviderSpec?
    fun getAllProviders(): List<ProviderSpec>
    fun getProvidersByCapability(capability: ToolCapability): List<ProviderSpec>
}
```

Adding a provider is registering a spec. The framework's core
code never has to change. The new provider gets the same routing,
streaming, detection, and tool handling as the existing ones —
because they all run through the spec.

## The detection engine

The framework needs to know *which provider it's talking to*. In
most cases this is configuration — the user names the provider
explicitly. In some cases (proxied endpoints, generic OpenAI-
compatible servers, dev environments), the framework has to
detect.

```kotlin
interface ProviderDetector {
    fun detectProvider(response: String): ProviderSpec?
    fun supportsTools(spec: ProviderSpec): Boolean
    fun getToolFormat(spec: ProviderSpec): ToolFormat
}
```

Detection looks for provider-specific signatures in responses —
unique header values, unique error formats, unique chunk
structures. The default detector returns `null` when nothing
matches; the framework falls back to the configured default.

## How specs interact with the rest of the architecture

The provider spec is read by:

- **The [pipeline DSL](Pipeline-DSL)** — the API style determines
  which protocol block to use; the streaming spec determines how
  the response decoder runs; the chunk schema feeds into delta
  parsing.
- **The [MCP & Tool Format layer](MCP-and-Tool-Format)** — the tool
  support spec governs what format negotiation outcomes are
  possible; the detection spec drives streaming-time
  classification of tool calls.
- **The [Supervisor's safety boundary](Supervisor-AI)** — risk
  assessment can be provider-aware; some providers' tools warrant
  tighter scrutiny than others.
- **The [Reflection Memory](Reflection-Memory) substrate** —
  every event records the provider that produced it, so the
  durable record is portable across provider migrations.

## Implementation status

**Sketched, not built.** The
`wiki/sketch-architecture/ProviderSpecs.kt` file has the data
class definitions and the Ollama spec fully expanded. Other
providers (LM Studio, OpenAI, Anthropic) are gestured at but not
filled out. The actual `ProviderRegistry`, the spec-driven HTTP
client, and the detection engine are TODO.

The work order:

1. Convert `ProviderSpec` and its sub-specs to Kotlin Multiplatform
   data classes in `lib/src/commonMain/`. Verify they serialise
   to and from YAML/JSON cleanly.
2. Build the spec-driven HTTP client. The client takes a spec
   and a logical operation, and produces a request matching the
   spec's path, request shape, and auth. Test against the Ollama
   spec.
3. Build the streaming decoder that reads the spec's chunk schema
   and extracts deltas at the right JSON paths.
4. Add LM Studio, OpenAI, and Anthropic specs. Verify each one
   works end-to-end.
5. Build the detection engine. Initially configuration-driven;
   probe-based detection later.

## Open questions

- **Spec format on disk.** YAML is the conventional choice for
  provider specs (other tools in the space use it); JSON is
  more uniform with the rest of the framework's config story.
  Both are supportable; the question is which is the default.
- **Spec versioning and migration.** When the spec schema
  changes, deployed specs become stale. Probably explicit
  version tags plus migration helpers; the exact migration story
  is open.
- **Provider-specific extensions.** Some providers have features
  no other provider has (e.g., Anthropic's prompt caching). The
  spec needs an extension mechanism — probably a typed
  `extensions: Map<String, Any>` field — without sacrificing
  the structured-by-default property.
- **Probe-based detection** for unknown OpenAI-compatible endpoints.
  The framework can probe `/v1/models` and look for
  provider-specific signatures; whether this is reliable enough
  to be a supported feature is open.

## Cross-references

- [pipeline](Pipeline-DSL) — protocol blocks instantiate clients
  per provider spec; streaming decoders read chunk schemas.
- [mcp-tools](MCP-and-Tool-Format) — tool support sub-spec governs the
  format negotiation outcome; detection sub-spec drives
  streaming-time tool-call recognition.
- [supervisor](Supervisor-AI) — provider identity feeds risk
  assessment.
- [reflection-memory](Reflection-Memory) — every event records
  its provider for cross-provider portability.

## What the spec is in service of

The architecture's commitment to be *open at the provider
boundary* — easy to add a provider, easy to migrate between
providers, easy to keep history portable across changes.
Without specs, every provider is bespoke code; with them, the
framework is a runtime that consumes specs and the providers
are data the runtime reads.

That's a healthier shape for software that has to keep up with
a fast-moving ecosystem. Providers come and go; APIs change;
new ones arrive every quarter. The spec-driven design is the
framework saying *we don't know what's next, but we're built to
absorb it without surgery*.

That's what the providers feature is for.

---

[← Features index](Feature-Index)


## MCP and Tool Format

The MCP/tool-format separation has moved to [MCP and Tool Format](MCP-and-Tool-Format).
