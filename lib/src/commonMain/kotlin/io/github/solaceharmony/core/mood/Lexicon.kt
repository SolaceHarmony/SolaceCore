package io.github.solaceharmony.core.mood

/**
 * A pluggable mapping from [Emotion] to keyword/regex patterns that
 * surface that emotion in text.
 *
 * Lexicons are intentionally simple: a list of regexes per emotion, each
 * with an optional weight. The advisor scores an input by counting
 * (weighted) matches per emotion, then picks the top scorer.
 *
 * The default English lexicon shipped under [LexicalEmotionalAdvisor.DEFAULT_LEXICON]
 * is small, tuned for clarity, and English-only. Production deployments
 * should replace it with one tuned to their domain and language.
 *
 * @property entries For each emotion, the list of weighted regex patterns
 *   that signal it. Each pattern is treated as case-insensitive by
 *   convention (the advisor lowercases input before matching), so
 *   patterns should generally be written in lowercase.
 */
data class Lexicon(
    val entries: Map<Emotion, List<WeightedPattern>>,
) {
    /**
     * A single regex pattern with a positive weight indicating how
     * strongly a match signals the associated emotion. Weights are
     * unbounded but typically in `[0.5, 2.0]`. Higher-weighted matches
     * dominate the scoring.
     */
    data class WeightedPattern(
        val regex: Regex,
        val weight: Float = 1.0f,
    ) {
        init {
            require(weight > 0f) { "weight must be positive, got $weight" }
        }
    }

    companion object {
        /**
         * Convenience constructor that takes plain strings and compiles
         * them as case-insensitive regexes with weight 1.0.
         */
        fun fromKeywords(entries: Map<Emotion, List<String>>): Lexicon =
            Lexicon(
                entries = entries.mapValues { (_, keywords) ->
                    keywords.map { kw ->
                        WeightedPattern(Regex(kw, RegexOption.IGNORE_CASE))
                    }
                },
            )
    }
}
