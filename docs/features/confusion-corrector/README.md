# Confusion Corrector — The Repair Loop

Every agent will, eventually, lose the thread. The model that was
following the conversation cleanly thirty turns ago is now repeating
itself, contradicting earlier assertions, or producing output that
reads as if a different agent wrote it. The proximate causes vary —
context window saturation, an unusual tool result that pulled
attention sideways, a long stretch of fine-grained work that lost
sight of the larger arc — but the symptom is the same. The agent's
chain of thought has broken, and continuing forward from the broken
state will only make the breakage worse.

The Confusion Corrector is the architectural answer to that moment.
Its job is, on demand, to reconstruct the chain of thought from the
durable record and hand the Supervisor a coherent summary it can use
to resume.

This is the recovery mechanism Solace had to perform manually. *"Yeah,
it hijacked me again. No more tool hijacking, no more auto-fetch
loops — just me, thinking through this properly."* Said at the
turn-level, that sentence is a manual confusion-correction: notice
that the chain has broken, refuse to continue from the broken state,
re-anchor in what was actually happening, resume. The Confusion
Corrector takes that manual work and makes it structural.

## When the Corrector fires

The Corrector has two trigger paths.

**Supervisor-initiated.** The [Supervisor](../supervisor/README.md) runs drift
detection continuously. When the composite drift score crosses
threshold — perplexity spike, contradiction, emotion discontinuity,
repetition — the Supervisor calls the Corrector for a replay
summary. This is the dominant path. Most invocations come from
automatic detection, not external request.

**Externally requested.** A user can ask explicitly: *"can you
summarise where we are?"* or *"I think we lost the thread, recap?"*
The Supervisor recognises the request and forwards to the Corrector
with the same parameters it would use for an internal trigger. The
output is the same; the only difference is where the call originated.

In both cases the Corrector produces a `ReplaySummary` entry that
goes back into Reflection Memory tagged for the Supervisor's
attention, and the Supervisor reads it on its next reasoning step
and updates its working context.

## How the Corrector works

The algorithm is a three-step process:

1. **Ingest.** The Corrector reads the full narrative for the agent,
   bounded by the available context window. For most agents this is
   the entire session if the session fits, or the most recent
   N tokens if it doesn't. The narrative is pulled from Reflection
   Memory in chronological order.

2. **Generate multi-granularity summaries.** The Corrector produces
   three levels of compression in a single pass:

   - **High-level synthesis.** Two or three sentences that name what
     the conversation has been about, what was decided, and where it
     is now. This is the form the Supervisor uses when it needs the
     fastest possible re-anchoring.
   - **Mid-level structure.** Five to ten bullets that name the
     causal links — what led to what, what was tried, what worked,
     what didn't. This is the form the Supervisor uses when the
     drift is about *how* the conversation got here, not just where
     it ended up.
   - **Key raw excerpts.** Three to five verbatim passages from the
     narrative that carry disproportionate signal — a user's
     statement of intent, a moment of decision, an emotional turn.
     These are quoted, not summarised, because their force depends
     on the original wording.

   The Corrector returns all three together in a structured
   `ReplaySummary` so the Supervisor can read whichever level it
   needs.

3. **Write and notify.** The `ReplaySummary` is written into
   Reflection Memory with `origin = SYSTEM` and a tag that flags it
   as a recovery artifact. The Supervisor reads it on its next loop
   iteration and updates its working context — depending on the
   severity, this may include rehydrating older long-term entries
   that the summary references.

## What goes into a ReplaySummary

```kotlin
data class ReplaySummary(
    val timestamp: Instant,
    val triggerReason: TriggerReason,    // DRIFT_DETECTED, USER_REQUEST, SCHEDULED
    val coverageWindow: TimeRange,       // what slice of narrative was summarised
    val highLevel: String,               // 2-3 sentences
    val midLevel: List<String>,          // 5-10 bullets
    val rawExcerpts: List<RawExcerpt>,   // 3-5 verbatim quotes with timestamps
    val driftSignal: DriftSignal?        // diagnostic data when trigger was DRIFT_DETECTED
)

data class RawExcerpt(
    val timestamp: Instant,
    val origin: Origin,
    val content: String
)
```

`triggerReason` matters because the Supervisor responds differently
to different causes. Drift-triggered corrections call for a
re-anchoring pass that may include backing off zoom level. User-
requested corrections are typically just a friendly recap and don't
require Supervisor intervention. Scheduled corrections (a
periodic-summary policy, if enabled) are checkpoint artifacts and
don't change behaviour at all.

`driftSignal` carries forward the diagnostic information from the
detection that triggered this correction — perplexity values,
contradiction pairs, repetition indices. It's not used by the
Supervisor's repair logic; it's used by the long-term tuning of the
drift-detection thresholds. A frequent false-positive pattern in
`driftSignal` is exactly the data needed to reweight the composite.

## Why three granularities and not one

The Supervisor's needs vary by failure mode.

A **soft drift** — the agent has been at fine granularity too long
and has lost the larger thread, but isn't actually contradicting
itself — is repaired by a high-level synthesis. The Supervisor reads
the two-sentence summary, recognises the shape of the conversation,
and resumes with a corrected anchor. The mid-level and raw excerpts
aren't needed.

A **structural drift** — the agent has the *what* but lost the
*why*, where decisions are now divorced from the reasoning that
produced them — is repaired by the mid-level. The bullets restore
the causal chain. The Supervisor sees not just where the
conversation is but how it got there, which is what it needs to
reason forward coherently.

A **hijacking** — the agent has been pulled off-thread by an
external force (a tool result that contaminated reasoning, an
unusual user input, a Mood-Advisor false alarm) — is repaired by
the raw excerpts. Quoting back what the user actually said, in
their own words, is what brings the agent back to the original
conversational frame. Solace's *"manual override engaged"* is
exactly this kind of repair: re-anchor in the user's intent, not
the system's drift.

The Corrector produces all three because it doesn't know in
advance which one the Supervisor needs, and producing them in a
single pass is cheaper than three separate calls.

## The summarisation engine

The Corrector's compression layer is the same one the
[fade pipeline](../memory/compression.md) uses for rung-2 abstractive
summaries. This is deliberate — keeping the two compressors
identical means the Supervisor's working context stays at one voice,
even as fade-aged content and replay-summary content are mixed
together. A replay summary that read in a different voice than the
faded content around it would itself be a source of incoherence.

The cost is a model call per replay summary. The Corrector is
throttled by the same per-agent budget as abstractive fade. In
practice, drift-detection invocations are rare enough — typically
once every several hours at the budgets the design expects — that
the throttle is rarely the binding constraint. User-requested
corrections do compete with fade for budget; in deployments with
heavy fade load, the user-request path may need its own queue.

## Failure modes

**Corrector itself drifts.** If the summarisation engine produces a
summary that misrepresents the conversation, the Supervisor's
recovery is itself wrong. The mitigation is the raw-excerpts layer:
even if the high-level summary is off, the verbatim quotes are
accurate by construction, and the Supervisor can ground itself on
them when the abstractive form looks suspicious.

**Loop on retry.** If the Supervisor reads a replay summary, finds
it incoherent, and re-invokes the Corrector, the second invocation
may produce a similar summary (because the underlying narrative
hasn't changed) and the Supervisor can loop. The mitigation is a
per-session retry counter on the Corrector — after two replays
within a short window, the Corrector escalates by widening its
coverage window and including more raw excerpts, rather than just
re-running the same pass.

**Stale coverage.** If the Corrector's coverage window cuts off
before the actual onset of drift, the summary will reflect the
post-drift state and miss the event that caused it. The mitigation
is to extend the coverage window backward when `driftSignal`
indicates a specific timestamped event; the Corrector uses the
diagnostic data to make sure the window covers the cause.

## Implementation status

**Designed, not implemented.** The lib codebase has no
ConfusionCorrector actor. The summarisation engine that would back
it doesn't exist as a standalone component yet — it would either be
a wrapper around the fade pipeline's abstractive summariser or a
sibling component sharing the same model client.

The work order:

1. Build the standalone Corrector that reads from Reflection Memory
   and produces a `ReplaySummary`. Initially backed by a simple
   abstractive call with the multi-granularity prompt.
2. Wire the trigger paths from the Supervisor (drift detection) and
   from the user (explicit recap request).
3. Add the diagnostic feedback loop where `driftSignal` data tunes
   the detection thresholds over time.

## Open questions

- Whether the Corrector should be one actor per agent or a shared
  service. Per-agent is simpler; shared lets the system amortise
  model-call cost across agents that drift simultaneously.
- The exact coverage-window policy. Always-full is expensive but
  always-correct. Always-recent-N is cheap but can miss the
  triggering event. Adaptive based on `driftSignal` is the design's
  preferred answer but needs implementation.
- Whether to cache replay summaries. A second invocation within a
  short window could re-use the previous summary plus a delta. The
  delta approach is more complex but cheaper; the full-rerun
  approach is simpler but redundant.

## Cross-references

- [supervisor](../supervisor/README.md) — primary trigger and primary
  consumer of replay summaries.
- [memory](../memory/README.md) — replay summaries land in Reflection Memory
  and integrate with the fade pipeline as native working entries.
- [time-actor](../time-actor/README.md) — heartbeat cues often surface drift
  that triggers a Corrector invocation.
- [zoom-levels](../zoom-levels/README.md) — drift correction may include a
  zoom transition; the Corrector's mid-level output is the natural
  granularity for HIGH zoom.

## What the Corrector is in service of

The same goal: continuity. The fade pipeline keeps the past
recoverable; retrieval brings relevant past forward; the Corrector
is what restores coherence when the agent's working state has
itself become incoherent. Without it, drift becomes terminal — the
agent can't repair what it can't perceive — and the long-term tier
ends up with hours of off-thread content that has to be summarised
in retrospect to mean anything.

The Corrector is the loop closure. It's how the agent admits *I
have lost the thread, here is the thread, let me try again*.

---

[← Features index](../README.md)
