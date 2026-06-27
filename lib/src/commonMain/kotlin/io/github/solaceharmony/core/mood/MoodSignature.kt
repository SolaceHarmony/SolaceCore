package io.github.solaceharmony.core.mood

/**
 * An affective fingerprint suitable for use as an index over Reflection
 * Memory.
 *
 * In the three-tier hybrid this is the *fine-grained* counterpart to the
 * coarse [MoodCue]. A signature captures the time-integrated affective
 * contour of a moment in a way that supports correlation-based retrieval —
 * "find past entries whose stored signature correlates with the current
 * one within a freshness window." That mechanism is the core of how
 * Solace memory rhymes rather than searches.
 *
 * The interface is shipped now so consumers can program against it.
 * Concrete implementations live in the spike + liquid layers and are not
 * yet shipped:
 *
 * - **Spiking signature** — a sparse vector of (neuron index, spike time,
 *   weight) triples produced by an integrate-and-fire substrate. Captures
 *   the contour of the moment at high temporal resolution.
 * - **LTC hidden-state signature** — the dense hidden state of a Liquid
 *   Time-Constant cell at write-time, integrating recent input under a
 *   continuous-time gate. This is what the
 *   [InferenceCube architecture](../../../../../../../../../wiki/Inference-Cube.md)
 *   produces inside each cube.
 *
 * Both implementations satisfy the same correlation contract, so the
 * Reflection Memory retrieval path can stay the same as the cell layer
 * matures.
 */
interface MoodSignature {
    /**
     * Number of dimensions in this signature. Two signatures are
     * comparable via [correlate] only when their dimensions match.
     */
    val dimensions: Int

    /**
     * Compute the affective correlation between this signature and
     * [other]. Returns a value in `[-1, 1]`, where:
     *
     * - `1` means the two signatures are identical contours (perfect rhyme).
     * - `0` means they are uncorrelated.
     * - `-1` means they are anti-correlated (opposing contours).
     *
     * The retrieval primitive is "find entries whose stored signature has
     * `correlate(current) >= threshold` within a freshness window." A
     * typical threshold for affective resonance is in `[0.6, 0.8]` —
     * tunable per deployment.
     *
     * Implementations should throw [IllegalArgumentException] when
     * [other.dimensions] does not match [dimensions].
     */
    fun correlate(other: MoodSignature): Float
}
