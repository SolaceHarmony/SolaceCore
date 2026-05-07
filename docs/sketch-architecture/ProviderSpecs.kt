// Kotlin Plan for Provider Specifications
// Translated from docs/codex-vendored/provider-specs/
// This file defines provider specifications in Kotlin data classes

package com.solacecore.codexplans.providerspecs

/**
 * Provider Specifications - YAML configurations translated to Kotlin
 */

// Base Provider Specification
data class ProviderSpec(
    val provider: String,
    val metadata: ProviderMetadata,
    val api: ApiSpec,
    val toolSupport: ToolSupportSpec,
    val detection: DetectionSpec
)

data class ProviderMetadata(
    val name: String,
    val version: Int
)

// API Specification
data class ApiSpec(
    val style: ApiStyle,
    val baseUrl: BaseUrlSpec,
    val paths: Map<String, PathSpec>,
    val auth: AuthType,
    val streaming: StreamingSpec
)

enum class ApiStyle {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    CUSTOM
}

data class BaseUrlSpec(
    val env: String? = null,
    val default: String
)

data class PathSpec(
    val method: HttpMethod,
    val path: String,
    val request: RequestSpec? = null,
    val response: ResponseSpec? = null
)

enum class HttpMethod {
    GET, POST, PUT, DELETE
}

data class RequestSpec(
    val modelField: String? = null,
    val messagesField: String? = null,
    val toolsField: String? = null,
    val streamField: String? = null
)

data class ResponseSpec(
    val choicesPath: String? = null
)

enum class AuthType {
    NONE,
    BEARER_TOKEN,
    API_KEY
}

data class StreamingSpec(
    val transport: StreamingTransport,
    val sse: SseSpec? = null,
    val chunkSchema: ChunkSchema? = null
)

enum class StreamingTransport {
    SSE,
    WEBSOCKET,
    POLLING
}

data class SseSpec(
    val linePrefix: String,
    val doneToken: String
)

data class ChunkSchema(
    val deltaTextPath: String? = null,
    val toolCallsPath: String? = null,
    val functionCallPath: String? = null
)

// Tool Support Specification
data class ToolSupportSpec(
    val functions: ToolCapability,
    val xmlInText: Boolean,
    val mcp: Boolean,
    val handshake: HandshakeSpec? = null
)

enum class ToolCapability {
    SUPPORTED,
    UNSUPPORTED,
    UNKNOWN
}

data class HandshakeSpec(
    val xmlProbe: XmlProbeSpec? = null
)

data class XmlProbeSpec(
    val enabled: Boolean,
    val tagName: String,
    val pattern: String
)

// Detection Specification
data class DetectionSpec(
    val openaiFunctions: OpenAIFunctionsDetection? = null,
    val xmlMatchers: XmlMatchersSpec? = null,
    val converters: Map<String, String>? = null
)

data class OpenAIFunctionsDetection(
    val fields: List<String>
)

data class XmlMatchersSpec(
    val blocks: List<String>,
    val excludeTags: List<String>
)

// Specific Provider Implementations

val ollamaSpec = ProviderSpec(
    provider = "ollama",
    metadata = ProviderMetadata(
        name = "Ollama",
        version = 1
    ),
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
                response = ResponseSpec(
                    choicesPath = "$.choices"
                )
            )
        ),
        auth = AuthType.NONE,
        streaming = StreamingSpec(
            transport = StreamingTransport.SSE,
            sse = SseSpec(
                linePrefix = "data:",
                doneToken = "[DONE]"
            ),
            chunkSchema = ChunkSchema(
                deltaTextPath = "$.choices[0].delta.content",
                toolCallsPath = "$.choices[0].delta.tool_calls",
                functionCallPath = "$.choices[0].delta.function_call"
            )
        )
    ),
    toolSupport = ToolSupportSpec(
        functions = ToolCapability.UNKNOWN,
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

// Provider Registry
object ProviderRegistry {
    private val providers = mutableMapOf<String, ProviderSpec>()

    init {
        register(ollamaSpec)
        // Add other providers here
    }

    fun register(spec: ProviderSpec) {
        providers[spec.provider] = spec
    }

    fun getProvider(name: String): ProviderSpec? = providers[name]

    fun getAllProviders(): List<ProviderSpec> = providers.values.toList()

    fun getProvidersByCapability(capability: ToolCapability): List<ProviderSpec> {
        return providers.values.filter { provider ->
            when (capability) {
                ToolCapability.SUPPORTED -> provider.toolSupport.functions == ToolCapability.SUPPORTED
                ToolCapability.UNSUPPORTED -> provider.toolSupport.functions == ToolCapability.UNSUPPORTED
                ToolCapability.UNKNOWN -> provider.toolSupport.functions == ToolCapability.UNKNOWN
            }
        }
    }
}

// Provider Detection Engine
interface ProviderDetector {
    fun detectProvider(response: String): ProviderSpec?
    fun supportsTools(spec: ProviderSpec): Boolean
    fun getToolFormat(spec: ProviderSpec): ToolFormat
}

enum class ToolFormat {
    OPENAI_FUNCTIONS,
    ANTHROPIC_TOOLS,
    XML_TOOLS,
    TEXT_BASED
}

class DefaultProviderDetector : ProviderDetector {

    override fun detectProvider(response: String): ProviderSpec? {
        // Try to detect based on response patterns
        return when {
            response.contains("tool_calls") || response.contains("function_call") ->
                ProviderRegistry.getProvider("openai")
            response.contains("<tool_use>") || response.contains("<function_call>") ->
                ProviderRegistry.getProvider("ollama")
            else -> null
        }
    }

    override fun supportsTools(spec: ProviderSpec): Boolean {
        return spec.toolSupport.functions == ToolCapability.SUPPORTED ||
               spec.toolSupport.xmlInText ||
               spec.toolSupport.mcp
    }

    override fun getToolFormat(spec: ProviderSpec): ToolFormat {
        return when (spec.provider) {
            "openai" -> ToolFormat.OPENAI_FUNCTIONS
            "ollama" -> ToolFormat.XML_TOOLS
            "anthropic" -> ToolFormat.ANTHROPIC_TOOLS
            else -> ToolFormat.TEXT_BASED
        }
    }
}

// Provider Manager
class ProviderManager(
    private val detector: ProviderDetector = DefaultProviderDetector()
) {

    fun initializeProvider(spec: ProviderSpec): ProviderInstance {
        return ProviderInstance(
            spec = spec,
            supportsTools = detector.supportsTools(spec),
            toolFormat = detector.getToolFormat(spec),
            status = ProviderStatus.CONNECTING
        )
    }

    fun detectFromResponse(response: String): ProviderInstance? {
        val spec = detector.detectProvider(response)
        return spec?.let { initializeProvider(it) }
    }
}

data class ProviderInstance(
    val spec: ProviderSpec,
    val supportsTools: Boolean,
    val toolFormat: ToolFormat,
    val status: ProviderStatus
)

enum class ProviderStatus {
    CONNECTING,
    CONNECTED,
    ERROR,
    DISCONNECTED
}