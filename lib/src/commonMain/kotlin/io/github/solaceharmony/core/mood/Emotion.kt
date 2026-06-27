package io.github.solaceharmony.core.mood

/**
 * A discrete affective category produced by an [EmotionalAdvisor].
 *
 * Solace treats emotion as a structured signal, not a tone modifier. Discrete
 * categories are the *coarse* surface that the executive supervisor reasons
 * over; the underlying signature (spike train + LTC hidden state) is the
 * *fine-grained* index over Reflection Memory. The two layers are deliberate:
 * the discrete categories are what an LLM can talk about, the signature is
 * what biological retrieval rhymes against.
 *
 * The set is intentionally small and principled. Lexicons can map many
 * surface words onto each category; the category itself is the contract
 * with the supervisor.
 *
 * For the design rationale see [`wiki/Mood-and-Emotional-Model.md`](../../../../../../../../../wiki/Mood-and-Emotional-Model.md).
 *
 * @property label Human-readable label for logging and prompt-prime construction.
 */
enum class Emotion(val label: String) {
    /** Positive valence. Lightness, warmth, satisfaction. */
    JOY("joy"),

    /** Low arousal, positive valence. Steadiness; ground state. */
    CALM("calm"),

    /** Engaged attention. Wanting to know. */
    CURIOSITY("curiosity"),

    /** Negative valence with stuck-ness. The "I keep trying and it isn't working" signal. */
    FRUSTRATION("frustration"),

    /** High arousal, negative valence, externally directed. */
    ANGER("anger"),

    /** Low arousal, negative valence. Loss, withdrawal. */
    SADNESS("sadness"),

    /** High arousal, negative valence, anticipatory. */
    FEAR("fear"),

    /** Sudden discrepancy from expectation. Valence-neutral on its own. */
    SURPRISE("surprise"),

    /** No detectable affect or signal below the advisor's confidence threshold. */
    NEUTRAL("neutral");

    companion object {
        /**
         * The non-Neutral emotions, in a stable order suitable for iteration
         * (e.g., for scoring loops in advisors that consider every category).
         */
        val ALL_NON_NEUTRAL: List<Emotion> = listOf(
            JOY, CALM, CURIOSITY, FRUSTRATION, ANGER, SADNESS, FEAR, SURPRISE,
        )
    }
}
