@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package io.github.solaceharmony.core.mood

import kotlinx.datetime.Clock
import kotlin.uuid.Uuid

/**
 * A structured affective cue produced by an [EmotionalAdvisor] and consumed
 * by the executive supervisor (the LLM).
 *
 * `MoodCue` is the *typed* handle on emotion. It is what the supervisor sees
 * when it reasons about how the user is feeling, what tone to take, whether
 * to escalate empathy, whether to slow down. The cue is advisory — the
 * supervisor decides whether to weigh it, ignore it, or override it.
 *
 * Cues are written to Reflection Memory tagged with `Origin.ADVISOR` (the
 * Reflection Memory abstraction itself is designed but not yet shipped; see
 * [`docs/components/memory/MemoryToolDesign.md`](../../../../../../../../../docs/components/memory/MemoryToolDesign.md)).
 * They are *internal* signals; user-facing output goes through the Mouth Tool,
 * not directly through the supervisor's interpretation of cues.
 *
 * @property correlationId Identifier linking this cue back to the input that
 *   triggered it (typically the [io.github.solaceharmony.core.actor.ActorMessage.correlationId]
 *   of the user's message). Lets the supervisor tie cues to their cause.
 * @property timestamp When the cue was produced, in epoch milliseconds.
 * @property source The advisor that produced this cue (e.g. `"lexical"`,
 *   `"spiking"`, `"audio-tone"`). Multiple advisors may emit cues for the
 *   same input; the supervisor compares them.
 * @property emotion The discrete category. See [Emotion].
 * @property intensity How strong the affect is, in `[0, 1]`. A faint
 *   irritation might be `0.2`; a clearly angry message might be `0.9`.
 * @property confidence How sure the advisor is about the classification, in
 *   `[0, 1]`. The supervisor should weight cues by `confidence`; a high-
 *   intensity, low-confidence cue is *less* reliable than a moderate-
 *   intensity, high-confidence one.
 * @property promptSuggestion Optional natural-language hint the supervisor
 *   may inline into its next prompt context (e.g. `"prioritise empathy"`).
 *   The supervisor is free to ignore this field.
 * @property evidence Optional text fragments that supported the
 *   classification. Useful for debugging; deployments may redact this
 *   field for production.
 */
data class MoodCue(
    val correlationId: String = Uuid.random().toString(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val source: String,
    val emotion: Emotion,
    val intensity: Float,
    val confidence: Float,
    val promptSuggestion: String? = null,
    val evidence: List<String> = emptyList(),
) {
    init {
        require(intensity in 0f..1f) { "intensity must be in [0, 1], got $intensity" }
        require(confidence in 0f..1f) { "confidence must be in [0, 1], got $confidence" }
        require(source.isNotBlank()) { "source must be a non-blank advisor name" }
    }
}
