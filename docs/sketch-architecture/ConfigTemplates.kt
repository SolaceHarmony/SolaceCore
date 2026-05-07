// Kotlin Plan for Config Templates
// Translated from docs/codex-vendored/config-templates/
// This file defines configuration templates in Kotlin

package com.solacecore.codexplans.config

/**
 * Configuration Templates - Ready-to-use config.json templates
 * For enabling Pipeline DSL in common setups
 */

// Base Configuration Structure
data class CodexConfig(
    val pipeline: PipelineConfig? = null,
    val negotiation: NegotiationConfig? = null,
    val negotiationDebug: Boolean? = null,
    val provider: String? = null,
    val model: String? = null,
    val modelFamily: String? = null,
    val blocks: List<String>? = null
)

data class PipelineConfig(
    val enabled: Boolean = false
)

data class NegotiationConfig(
    val enabled: Boolean = false
)

// Ollama + Qwen3-coder Template (XML-in-text tools)
val ollamaQwenConfig = CodexConfig(
    pipeline = PipelineConfig(enabled = true),
    negotiation = NegotiationConfig(enabled = true),
    negotiationDebug = true,
    provider = "ollama",
    model = "qwen3-coder:30b", // Example model
    modelFamily = "qwen",
    blocks = listOf(
        "protocol.ollama",           // Ollama connector
        "codec.mcp_over_xml",        // XML tool transport
        "tools.negotiation_advertise", // Tool exposure hints
        "family.qwen3"               // Family-specific shaping
    )
)

// LM Studio + Qwen3-coder Template (functions tunneling)
val lmStudioQwenConfig = CodexConfig(
    pipeline = PipelineConfig(enabled = true),
    negotiation = NegotiationConfig(enabled = true),
    negotiationDebug = true,
    provider = "lmstudio",
    model = "qwen3-coder-30b-a3b-instruct-1m",
    modelFamily = "qwen",
    blocks = listOf(
        "protocol.openai",           // LM Studio's OpenAI-compatible API
        "codec.mcp_over_functions",  // Function-call transport
        "tools.negotiation_advertise", // Tool exposure hints
        "family.qwen3"               // Family-specific shaping
    )
)

// Configuration Template Registry
object ConfigTemplateRegistry {
    private val templates = mutableMapOf<String, CodexConfig>()

    init {
        register("ollama-qwen", ollamaQwenConfig)
        register("lmstudio-qwen", lmStudioQwenConfig)
    }

    fun register(name: String, config: CodexConfig) {
        templates[name] = config
    }

    fun getTemplate(name: String): CodexConfig? = templates[name]

    fun getAllTemplates(): Map<String, CodexConfig> = templates.toMap()

    fun getTemplatesForProvider(provider: String): List<Pair<String, CodexConfig>> {
        return templates.filter { it.value.provider == provider }.toList()
    }
}

// Configuration Manager
interface ConfigManager {
    fun loadConfig(): CodexConfig
    fun saveConfig(config: CodexConfig)
    fun applyTemplate(templateName: String): Boolean
    fun validateConfig(config: CodexConfig): ValidationResult
}

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

class FileBasedConfigManager(
    private val configPath: String = ".codex-dev/config.json"
) : ConfigManager {

    override fun loadConfig(): CodexConfig {
        // Implementation would read from file
        // For now, return default
        return CodexConfig()
    }

    override fun saveConfig(config: CodexConfig) {
        // Implementation would write to file
        println("Saving config to $configPath")
    }

    override fun applyTemplate(templateName: String): Boolean {
        val template = ConfigTemplateRegistry.getTemplate(templateName)
        return if (template != null) {
            saveConfig(template)
            true
        } else {
            false
        }
    }

    override fun validateConfig(config: CodexConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate provider
        if (config.provider.isNullOrBlank()) {
            errors.add("Provider must be specified")
        }

        // Validate model
        if (config.model.isNullOrBlank()) {
            errors.add("Model must be specified")
        }

        // Check for incompatible combinations
        if (config.provider == "ollama" && config.blocks?.contains("protocol.openai") == true) {
            errors.add("Ollama provider cannot use OpenAI protocol block")
        }

        if (config.provider == "lmstudio" && config.blocks?.contains("protocol.ollama") == true) {
            errors.add("LM Studio provider cannot use Ollama protocol block")
        }

        // Warnings
        if (config.negotiationDebug == true) {
            warnings.add("Debug logging enabled - consider disabling in production")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}

// Setup Instructions
object ConfigSetupInstructions {

    fun getOllamaQwenInstructions(): List<String> = listOf(
        "1. Copy ollama-qwen configuration to .codex-dev/config.json",
        "2. Ensure provider is set to 'ollama'",
        "3. Ensure model is a Qwen variant (e.g., 'qwen3-coder:30b')",
        "4. Run Codex - negotiationDebug will show handshake logs"
    )

    fun getLmStudioQwenInstructions(): List<String> = listOf(
        "1. Copy lmstudio-qwen.json to .codex-dev/config.json",
        "2. Ensure LM Studio is running at http://localhost:1234/v1",
        "3. Run Codex - negotiationDebug will show router logs"
    )

    fun getGeneralSetupSteps(): List<String> = listOf(
        "Choose appropriate template based on your provider and model",
        "Copy template to config location (.codex-dev/config.json or ~/.codex/config.json)",
        "Verify provider and model settings match your setup",
        "Test with negotiationDebug enabled to verify tool negotiation",
        "Disable debug logging for production use"
    )
}

// Configuration Builder DSL
class ConfigBuilder {
    private var config = CodexConfig()

    fun pipeline(enabled: Boolean = true): ConfigBuilder {
        config = config.copy(pipeline = PipelineConfig(enabled))
        return this
    }

    fun negotiation(enabled: Boolean = true): ConfigBuilder {
        config = config.copy(negotiation = NegotiationConfig(enabled))
        return this
    }

    fun debug(enabled: Boolean = true): ConfigBuilder {
        config = config.copy(negotiationDebug = enabled)
        return this
    }

    fun provider(name: String): ConfigBuilder {
        config = config.copy(provider = name)
        return this
    }

    fun model(name: String): ConfigBuilder {
        config = config.copy(model = name)
        return this
    }

    fun modelFamily(family: String): ConfigBuilder {
        config = config.copy(modelFamily = family)
        return this
    }

    fun blocks(vararg blocks: String): ConfigBuilder {
        config = config.copy(blocks = blocks.toList())
        return this
    }

    fun build(): CodexConfig = config
}

// Usage examples
object ConfigExamples {

    // Build Ollama + Qwen config using DSL
    val ollamaQwenViaDSL = ConfigBuilder()
        .pipeline()
        .negotiation()
        .debug()
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

    // Build LM Studio + Qwen config using DSL
    val lmStudioQwenViaDSL = ConfigBuilder()
        .pipeline()
        .negotiation()
        .debug()
        .provider("lmstudio")
        .model("qwen3-coder-30b-a3b-instruct-1m")
        .modelFamily("qwen")
        .blocks(
            "protocol.openai",
            "codec.mcp_over_functions",
            "tools.negotiation_advertise",
            "family.qwen3"
        )
        .build()
}