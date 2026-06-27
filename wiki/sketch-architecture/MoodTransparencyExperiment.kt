// Kotlin Plan for Mood System Transparency Experiment
// Translated from docs/codex-vendored/mood-transparency-experiment/
// This file outlines the transparency and memory systems for the mood system

package com.solacecore.codexplans.mood.transparency

/**
 * Mood System Transparency Experiment
 * Making all operations visible to the user
 */

// Operation Types
enum class OperationType {
    TOOL_CALL,
    MOOD_CHANGE,
    DELEGATION,
    MEMORY_RETRIEVAL,
    SUPERVISION_DECISION
}

// Display Types with Emojis
enum class DisplayType(val emoji: String) {
    TOOL("🔧"),
    SUCCESS("✅"),
    ERROR("❌"),
    MOOD("🎭"),
    MEMORY("🧠"),
    SUPERVISION("👁️")
}

// Operation Visibility Contract
interface TransparencyContract {
    fun makeOperationVisible(operation: Operation)
    fun trackCoverage(operation: Operation): CoverageResult
    fun detectViolations(): List<Violation>
}

data class Operation(
    val id: String,
    val type: OperationType,
    val timestamp: Long,
    val description: String,
    val details: Map<String, Any>,
    val displayType: DisplayType
)

data class CoverageResult(
    val operationId: String,
    val visible: Boolean,
    val coverage: Double, // 0.0 to 1.0
    val missingElements: List<String>
)

data class Violation(
    val operationId: String,
    val violationType: String,
    val description: String,
    val severity: ViolationSeverity
)

enum class ViolationSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Display Formatters
interface DisplayFormatter {
    fun formatOperation(operation: Operation): String
    fun formatCoverage(coverage: CoverageResult): String
    fun formatViolation(violation: Violation): String
}

class EmojiDisplayFormatter : DisplayFormatter {

    override fun formatOperation(operation: Operation): String {
        return "${operation.displayType.emoji} ${operation.description}"
    }

    override fun formatCoverage(coverage: CoverageResult): String {
        val percentage = (coverage.coverage * 100).toInt()
        val status = when {
            coverage.coverage >= 0.9 -> "✅"
            coverage.coverage >= 0.7 -> "⚠️"
            else -> "❌"
        }
        return "$status Coverage: ${percentage}%"
    }

    override fun formatViolation(violation: Violation): String {
        val severityEmoji = when (violation.severity) {
            ViolationSeverity.CRITICAL -> "🚨"
            ViolationSeverity.HIGH -> "⚠️"
            ViolationSeverity.MEDIUM -> "ℹ️"
            ViolationSeverity.LOW -> "📝"
        }
        return "$severityEmoji ${violation.description}"
    }
}

// Transparency Monitor
interface TransparencyMonitor {
    fun trackOperation(operation: Operation)
    fun getCoverageReport(): CoverageReport
    fun getViolationReport(): ViolationReport
    fun resetTracking()
}

data class CoverageReport(
    val totalOperations: Int,
    val visibleOperations: Int,
    val averageCoverage: Double,
    val coverageByType: Map<OperationType, Double>
)

data class ViolationReport(
    val totalViolations: Int,
    val violationsByType: Map<String, Int>,
    val violationsBySeverity: Map<ViolationSeverity, Int>
)

class DefaultTransparencyMonitor : TransparencyMonitor {

    private val operations = mutableListOf<Operation>()
    private val violations = mutableListOf<Violation>()

    override fun trackOperation(operation: Operation) {
        operations.add(operation)

        // Check for transparency violations
        if (!isOperationTransparent(operation)) {
            violations.add(Violation(
                operationId = operation.id,
                violationType = "TRANSPARENCY_VIOLATION",
                description = "Operation ${operation.id} lacks required transparency",
                severity = ViolationSeverity.MEDIUM
            ))
        }
    }

    override fun getCoverageReport(): CoverageReport {
        val visibleCount = operations.count { isOperationTransparent(it) }
        val coverageByType = operations.groupBy { it.type }
            .mapValues { (_, ops) ->
                ops.count { isOperationTransparent(it) }.toDouble() / ops.size
            }

        return CoverageReport(
            totalOperations = operations.size,
            visibleOperations = visibleCount,
            averageCoverage = if (operations.isNotEmpty()) visibleCount.toDouble() / operations.size else 0.0,
            coverageByType = coverageByType
        )
    }

    override fun getViolationReport(): ViolationReport {
        val byType = violations.groupBy { it.violationType }.mapValues { it.value.size }
        val bySeverity = violations.groupBy { it.severity }.mapValues { it.value.size }

        return ViolationReport(
            totalViolations = violations.size,
            violationsByType = byType,
            violationsBySeverity = bySeverity
        )
    }

    override fun resetTracking() {
        operations.clear()
        violations.clear()
    }

    private fun isOperationTransparent(operation: Operation): Boolean {
        // Implementation would check if operation meets transparency requirements
        return operation.details.containsKey("user_visible") &&
               operation.details["user_visible"] as? Boolean == true
    }
}

// Display Hooks
interface DisplayHooks {
    fun onOperation(operation: Operation)
    fun onCoverageUpdate(coverage: CoverageResult)
    fun onViolation(violation: Violation)
}

class IntegrationDisplayHooks(
    private val formatter: DisplayFormatter = EmojiDisplayFormatter(),
    private val onItemCallback: ((String) -> Unit)? = null
) : DisplayHooks {

    override fun onOperation(operation: Operation) {
        val formatted = formatter.formatOperation(operation)
        onItemCallback?.invoke(formatted)
    }

    override fun onCoverageUpdate(coverage: CoverageResult) {
        val formatted = formatter.formatCoverage(coverage)
        onItemCallback?.invoke(formatted)
    }

    override fun onViolation(violation: Violation) {
        val formatted = formatter.formatViolation(violation)
        onItemCallback?.invoke(formatted)
    }
}

// Memory System - Episode Storage
interface EpisodeStorage {
    suspend fun storeEpisode(episode: Episode)
    suspend fun retrieveEpisodes(query: EpisodeQuery): List<Episode>
    suspend fun getEpisode(id: String): Episode?
    suspend fun deleteEpisode(id: String): Boolean
}

sealed class Episode {
    abstract val id: String
    abstract val timestamp: Long
    abstract val context: EpisodeContext

    data class EmotionalEpisode(
        override val id: String,
        override val timestamp: Long,
        override val context: EpisodeContext,
        val emotion: Emotion,
        val valence: Double,
        val arousal: Double,
        val trigger: String,
        val response: String
    ) : Episode()

    data class TechnicalEpisode(
        override val id: String,
        override val timestamp: Long,
        override val context: EpisodeContext,
        val task: String,
        val solution: String,
        val tools: List<String>,
        val success: Boolean
    ) : Episode()
}

data class EpisodeContext(
    val userId: String,
    val sessionId: String,
    val tags: Set<String>
)

data class Emotion(
    val type: String,
    val intensity: Double,
    val description: String
)

data class EpisodeQuery(
    val context: EpisodeContext? = null,
    val timeRange: LongRange? = null,
    val emotionTypes: Set<String>? = null,
    val tags: Set<String>? = null,
    val limit: Int? = null
)

// Dual-Context Memory Architecture
interface DualContextMemory {
    suspend fun storeEmotionalEpisode(episode: Episode.EmotionalEpisode)
    suspend fun storeTechnicalEpisode(episode: Episode.TechnicalEpisode)
    suspend fun retrieveEmotionalContext(query: EmotionalQuery): EmotionalContext
    suspend fun retrieveTechnicalContext(query: TechnicalQuery): TechnicalContext
    suspend fun crossIndexEpisodes(): List<CrossIndexedEpisode>
}

data class EmotionalQuery(
    val valenceRange: ClosedRange<Double>? = null,
    val arousalRange: ClosedRange<Double>? = null,
    val emotionTypes: Set<String>? = null
)

data class TechnicalQuery(
    val taskTypes: Set<String>? = null,
    val tools: Set<String>? = null,
    val success: Boolean? = null
)

data class EmotionalContext(
    val episodes: List<Episode.EmotionalEpisode>,
    val dominantEmotion: Emotion,
    val averageValence: Double,
    val averageArousal: Double
)

data class TechnicalContext(
    val episodes: List<Episode.TechnicalEpisode>,
    val commonTools: List<String>,
    val successRate: Double,
    val patterns: List<String>
)

data class CrossIndexedEpisode(
    val emotional: Episode.EmotionalEpisode,
    val technical: Episode.TechnicalEpisode,
    val correlation: Double,
    val insights: List<String>
)

// Arousal-Based Retrieval
interface ArousalBasedRetrieval {
    suspend fun retrieveByArousal(query: ArousalQuery): List<Episode>
    suspend fun getArousalModes(): List<ArousalMode>
    suspend fun switchMode(mode: ArousalMode)
}

enum class ArousalMode {
    HIGH_AROUSAL,    // Excited, urgent retrieval
    LOW_AROUSAL,     // Calm, comprehensive retrieval
    BALANCED        // Moderate arousal retrieval
}

data class ArousalQuery(
    val arousalLevel: Double,
    val mode: ArousalMode = ArousalMode.BALANCED,
    val context: EpisodeContext? = null
)

// Trust and Safety Model
interface TrustAndSafetyModel {
    fun assessTrust(operation: Operation): TrustAssessment
    fun checkSafety(operation: Operation): SafetyCheck
    fun superviseExecution(operation: Operation): SupervisionResult
}

data class TrustAssessment(
    val operationId: String,
    val trustLevel: Double, // 0.0 to 1.0
    val factors: List<TrustFactor>,
    val recommendation: TrustRecommendation
)

data class TrustFactor(
    val factor: String,
    val weight: Double,
    val value: Double
)

enum class TrustRecommendation {
    ALLOW, REVIEW, DENY
}

data class SafetyCheck(
    val operationId: String,
    val safe: Boolean,
    val risks: List<String>,
    val mitigations: List<String>
)

data class SupervisionResult(
    val operationId: String,
    val approved: Boolean,
    val supervisorNotes: String,
    val conditions: List<String>
)