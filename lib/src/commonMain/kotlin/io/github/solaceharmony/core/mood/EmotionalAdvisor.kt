@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package io.github.solaceharmony.core.mood

import io.github.solaceharmony.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.uuid.Uuid

/**
 * Abstract base for actors that consume text input and emit [MoodCue]s.
 *
 * The contract is intentionally narrow: `analyze(text)` returns a single
 * cue (or `null` for "nothing to say"). Subclasses can vary wildly in how
 * they classify — a keyword lexicon, a remote sentiment API, a small
 * on-device model, an integrate-and-fire spike net — but every advisor
 * speaks the same protocol back to the supervisor.
 *
 * The actor owns two ports:
 *
 * | Port name | Direction | Type |
 * |-----------|-----------|------|
 * | [INPUT_PORT] | input  | `String` (the user text or other advisor cue text) |
 * | [OUTPUT_PORT] | output | `MoodCue` (analysis result, when not null) |
 *
 * Multiple advisors typically run in parallel as siblings under the
 * supervisor's coroutine scope. When the supervisor cancels its scope,
 * every advisor's input-handler coroutine is cancelled cleanly; the
 * structured-concurrency invariant the SolaceCore actor system enforces
 * is what makes this safe.
 *
 * @see io.github.solaceharmony.core.actor.Actor for the underlying actor
 *   contract this base extends.
 */
abstract class EmotionalAdvisor(
    id: String = Uuid.random().toString(),
    name: String = "EmotionalAdvisor",
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : Actor(id, name, scope) {

    companion object {
        /** Name of the input port that consumes user/advisor text. */
        const val INPUT_PORT: String = "userText"

        /** Name of the output port that emits classified cues. */
        const val OUTPUT_PORT: String = "cues"
    }

    /**
     * Analyse the given input and produce a [MoodCue], or `null` if there
     * is nothing meaningful to emit (e.g. the input fell below the
     * advisor's confidence threshold and the advisor prefers silence over
     * emitting a `Neutral` cue).
     *
     * Implementations are free to be slow or asynchronous; the actor
     * runtime serializes calls per port and applies a per-message
     * processing timeout configured at port-creation time.
     */
    abstract suspend fun analyze(text: String): MoodCue?

    /**
     * Initializes the advisor's input and output ports. Idempotent —
     * safe to call multiple times. Subclasses that override [start] to
     * add their own bring-up should still call this (or `super.start()`)
     * before using the ports.
     */
    suspend fun initialize() {
        if (getPort(INPUT_PORT, String::class) == null) {
            createPort(
                name = INPUT_PORT,
                messageClass = String::class,
                handler = { text -> handleInput(text) },
                bufferSize = 16,
            )
        }
        if (getPort(OUTPUT_PORT, MoodCue::class) == null) {
            createOutputPort(
                name = OUTPUT_PORT,
                messageClass = MoodCue::class,
                bufferSize = 16,
            )
        }
    }

    /**
     * Default consumer for the input port: invoke [analyze] and forward a
     * non-null cue to the output port. Subclasses can override to add
     * pre/post-processing (e.g., trimming evidence, applying a deployment-
     * specific redaction policy) but should typically delegate the actual
     * classification to [analyze].
     */
    protected open suspend fun handleInput(text: String) {
        val cue = analyze(text) ?: return
        getPort(OUTPUT_PORT, MoodCue::class)?.send(cue)
    }

    override suspend fun start() {
        initialize()
        super.start()
    }
}
