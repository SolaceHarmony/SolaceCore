@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package io.github.solaceharmony.core.mood

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.uuid.Uuid

/**
 * A baseline [EmotionalAdvisor] that scores emotions by keyword/regex
 * matches against a [Lexicon].
 *
 * This is intentionally simple. Lexical sentiment classification is well-
 * known to miss sarcasm, negation, cultural variance, and almost every
 * subtlety humans care about. It ships not because it's the right
 * long-term answer but because:
 *
 * 1. It validates the cue protocol end-to-end (advisor → port →
 *    supervisor).
 * 2. It demonstrates the actor topology a future spike-based advisor will
 *    slot into.
 * 3. It lets downstream consumers (Mouth Tool framing, Reflection Memory
 *    tagging) wire against a real producer today.
 *
 * When a stronger advisor lands (a small on-device classifier, an
 * integrate-and-fire spike net, an LLM-backed classifier), it can replace
 * this advisor in the actor graph with no change to consumers.
 *
 * @param lexicon The keyword/regex lexicon used for scoring. Defaults to
 *   [DEFAULT_LEXICON] (small, English-only, intentionally rough).
 * @param confidenceThreshold Cues are emitted only when the advisor's
 *   computed confidence meets or exceeds this value. Below the threshold
 *   the advisor stays silent rather than emitting a `Neutral` cue. Range
 *   `[0, 1]`. Defaults to `0.4`.
 * @param maxEvidenceFragments Maximum number of matched fragments to
 *   include in [MoodCue.evidence] for debugging. Set to `0` to omit
 *   evidence entirely (recommended for production deployments).
 */
class LexicalEmotionalAdvisor(
    private val lexicon: Lexicon = DEFAULT_LEXICON,
    private val confidenceThreshold: Float = 0.4f,
    private val maxEvidenceFragments: Int = 5,
    id: String = Uuid.random().toString(),
    name: String = "LexicalEmotionalAdvisor",
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : EmotionalAdvisor(id, name, scope) {

    init {
        require(confidenceThreshold in 0f..1f) {
            "confidenceThreshold must be in [0, 1], got $confidenceThreshold"
        }
        require(maxEvidenceFragments >= 0) {
            "maxEvidenceFragments must be non-negative, got $maxEvidenceFragments"
        }
    }

    override suspend fun analyze(text: String): MoodCue? {
        if (text.isBlank()) return null

        // Score every non-Neutral emotion by summing match weights.
        // Track the matched fragments for evidence.
        val scores = mutableMapOf<Emotion, Float>()
        val fragments = mutableMapOf<Emotion, MutableList<String>>()

        for (emotion in Emotion.ALL_NON_NEUTRAL) {
            val patterns = lexicon.entries[emotion] ?: continue
            for (wp in patterns) {
                wp.regex.findAll(text).forEach { match ->
                    scores[emotion] = (scores[emotion] ?: 0f) + wp.weight
                    fragments.getOrPut(emotion) { mutableListOf() }.add(match.value)
                }
            }
        }

        if (scores.isEmpty()) return null

        // Pick the top-scoring emotion. Compute intensity as the
        // top score normalized by total score across categories
        // (so a tied two-way classification gives intensity ~0.5
        // for the winner). Compute confidence as the margin between
        // the top and second-best scores.
        val total = scores.values.sum()
        val ranked = scores.entries.sortedByDescending { it.value }
        val winner = ranked.first()
        val runnerUp = ranked.drop(1).firstOrNull()?.value ?: 0f

        val intensity = (winner.value / total).coerceIn(0f, 1f)
        val confidence = ((winner.value - runnerUp) / total).coerceIn(0f, 1f)

        if (confidence < confidenceThreshold) return null

        val evidence = if (maxEvidenceFragments == 0) {
            emptyList()
        } else {
            fragments[winner.key].orEmpty().take(maxEvidenceFragments)
        }

        return MoodCue(
            source = name,
            emotion = winner.key,
            intensity = intensity,
            confidence = confidence,
            evidence = evidence,
            promptSuggestion = winner.key.suggestion(),
        )
    }

    private fun Emotion.suggestion(): String? = when (this) {
        Emotion.FRUSTRATION -> "user appears frustrated; prioritise empathy and concrete next steps"
        Emotion.ANGER -> "user is expressing anger; acknowledge before proposing solutions"
        Emotion.SADNESS -> "user seems low; soften tone, validate before advising"
        Emotion.FEAR -> "user expresses concern; reassure with specifics, avoid hedging"
        Emotion.JOY -> "user is positive; match the energy briefly, stay focused"
        Emotion.CURIOSITY -> "user is engaged; depth is welcome"
        Emotion.SURPRISE -> "user noticed something unexpected; clarify whether it's a problem"
        Emotion.CALM, Emotion.NEUTRAL -> null
    }

    companion object {
        /**
         * A small, principled English-only baseline lexicon. Tuned for
         * clarity over recall. Production deployments should override.
         */
        val DEFAULT_LEXICON: Lexicon = Lexicon.fromKeywords(
            mapOf(
                Emotion.JOY to listOf(
                    "\\bhappy\\b", "\\bglad\\b", "\\blove(d|s)?\\b", "\\bgreat\\b",
                    "\\bawesome\\b", "\\bperfect\\b", "\\bthank you\\b", "\\bthanks\\b",
                    "\\byay\\b", "🎉",
                ),
                Emotion.CALM to listOf(
                    "\\bcalm\\b", "\\bok(ay)?\\b", "\\bfine\\b", "\\balright\\b",
                    "\\bsteady\\b", "\\bquiet\\b",
                ),
                Emotion.CURIOSITY to listOf(
                    "\\bcurious\\b", "\\bwhy\\b", "\\bhow does\\b", "\\bwondering\\b",
                    "\\bwhat if\\b", "\\binteresting\\b",
                ),
                Emotion.FRUSTRATION to listOf(
                    "\\bfrustrat(ing|ed)\\b", "\\bstuck\\b", "\\bwhy (does|won'?t|isn'?t)\\b",
                    "\\bnot working\\b", "\\bkeeps? failing\\b", "\\bagain\\b",
                    "\\b(dammit|damn it|wtf)\\b", "\\bstupid\\b",
                ),
                Emotion.ANGER to listOf(
                    "\\b(angry|mad|furious)\\b", "\\bhate(d|s)?\\b", "\\bunacceptable\\b",
                    "\\b(this is|that'?s) ridiculous\\b",
                ),
                Emotion.SADNESS to listOf(
                    "\\b(sad|depressed|down|blue|miserable)\\b", "\\bcry(ing)?\\b",
                    "\\bgrief(ing)?\\b", "\\blonely\\b", "\\btired\\b", "\\bexhausted\\b",
                ),
                Emotion.FEAR to listOf(
                    "\\b(afraid|scared|worried|anxious|nervous)\\b",
                    "\\b(panic|panicking)\\b", "\\bdread\\b",
                ),
                Emotion.SURPRISE to listOf(
                    "\\b(wait|huh|what)\\b\\??", "\\b(no way|seriously)\\b",
                    "\\bunexpected\\b", "\\bsuddenly\\b", "\\bweird\\b",
                ),
            )
        )
    }
}
