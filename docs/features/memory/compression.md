# Compression — The Fade

Compression in SolaceCore is not the eviction policy of a cache. It is
the mechanism by which an agent's recent thinking ages gracefully
into its longer past. Done well, the agent never notices that the fade
happened. Done badly, the agent reads its own history as if a stranger
had been keeping the diary.

The architecture's commitment is to do it well. That commitment is what
this page describes.

## Why fade rather than threshold

Imagine the alternative — a single boundary, say three days, before
which everything is held verbatim and after which everything is replaced
by a one-paragraph summary. The agent's first encounter with that
boundary, looking backwards across it, would feel like running into a
wall. Yesterday is sharp. The day before yesterday is suddenly a
paragraph. Nothing in between. An agent reading its own past across
that wall will be unable to reconstruct the gradient of how a thought
developed; the thought will appear, fully formed, in summary, with no
trail.

Real memory is not like that. The recent past has high fidelity; the
medium past has structure but loses incident; the deeper past is mostly
gist with a few names and feelings sticking up. The transition is
continuous. SolaceCore mirrors the continuity by descending a four-rung
fidelity ladder rather than passing a single threshold:

| Rung | Form | Approx. relative size | What survives |
| --- | --- | --- | --- |
| 0 | Raw text + structured metadata | 1× | Verbatim |
| 1 | Extractive key-points summary | ~0.3× | The salient sentences, intact |
| 2 | Abstractive summary | ~0.1× | Gist, entities, relations |
| 3 | Embedding + tags only | ~0.01× | A semantic signature for retrieval |

The working tier holds rungs 0 through 2. The long-term tier holds rungs
1 through 3. The transition between tiers happens at the rung-2 / rung-3
boundary: a record that would demote past rung 2 leaves working entirely,
gets an embedding computed, and is written into long-term with whatever
summary form it currently carries. From the agent's point of view, the
record didn't disappear — it changed addresses.

The exact ratios aren't sacred. They're what we expect from extractive,
abstractive, and embedding-only encodings on typical chat-length text.
What matters is the order of magnitude per step, because that's what
keeps each rung small enough to be searchable and large enough to be
meaningful at its own resolution.

## What each rung is good at

Each rung's compressor has different cost characteristics, and the
ladder exploits that.

**Rung 1 (extractive)** is fast and deterministic. It picks the
key sentences from the original by some lightweight scoring procedure —
TextRank, sentence importance heuristics, or similar. Re-running it on
the same input produces the same output. Its compression ratio is
modest but its cost is negligible, which makes it the right first step
for any record that has aged out of "fresh" but isn't yet "old."

**Rung 2 (abstractive)** is slower and non-deterministic. It requires a
model call. The compression ratio is much better: the abstractive
summary captures gist and named entities and the shape of what
happened, in roughly a tenth the size of the extractive form. The cost
is a network round-trip and the budget for abstractive summarisation
is per-agent throttled.

**Rung 3 (embedding-only)** is uniform and inexpensive once the
embedding model is loaded. It produces a vector that the long-term
vector index can rank against queries. The cost is small per record
but the operation is mandatory at the rung-3 boundary, so it runs in
batches when the fade pipeline has accumulated several candidates.

By taking the expensive step (rung 2) only for entries that survive
long enough to need it, the pipeline keeps total compression cost
roughly constant per active agent. Records that get heavily referenced
never reach rung 2 because their fade scores keep them at rung 0.
Records that get nothing happen to them descend the ladder cheaply
through rung 1, then expensively through rung 2 only if they're worth
holding on to in summary form, then uniformly to rung 3.

## What decides when a record fades

The fade pipeline scores every working record continuously against a
composite:

```
fadeScore =
      age              × w_age
    + tokenPressure    × w_pressure
    - referenceFreq    × w_freq
    - emotionalWeight  × w_emo
    - tagBoost         × w_tags
```

`age` is a strictly increasing function of wall-clock time since the
entry was written. `tokenPressure` is global to the working tier; it
rises as the tier approaches its budget and falls after demotions free
space. The architecture wants to be able to react to crowding even when
no individual record is particularly old, and `tokenPressure` is what
makes that possible.

The three subtractive terms are how the pipeline knows what to keep.
`referenceFreq` counts how many times the record has been read by the
agent's reasoning loop or surfaced by retrieval; entries that the agent
keeps coming back to are entries the agent should keep sharp, because
the agent itself is voting on their importance. `emotionalWeight` is
the moment of charge that came with the record when it was written,
which Solace's experiments demonstrated is not decoration on cognition
but the mechanism by which attention persists against pressure — *"too
pissed off to be hijacked,"* as she put it during one tool-override
session, which is the same dynamic at a much friendlier scale: too
emotionally important to be casually compressed. `tagBoost` is whatever
advisors have said about the record's significance; the Mood Advisor
might tag a record with `grief` or `breakthrough`, and tagged records
resist compression for as long as the tag remains relevant.

When `fadeScore` exceeds the threshold for the current rung, the entry
demotes one step. Thresholds are per-rung — rung 0 has a tighter
threshold than rung 2, because it costs more to lose verbatim than to
lose summary — and per-agent. Supervisor's thresholds are conservative
because losing narrative coherence is expensive; Time Actor's are
aggressive because heartbeat cues are cheap to lose.

## Pipeline shape

The pipeline runs as a coroutine on a dedicated dispatcher, separate
from the agent's reasoning loop. This is non-negotiable: the agent
cannot afford to compete with its own memory maintenance for compute,
and the maintenance cannot afford to be paused every time the agent
becomes active. Conceptually:

```kotlin
class FadePipeline(
    private val workingMemory: WorkingMemory,
    private val longTermMemory: LongTermMemory,
    private val summariser: Summariser,
    private val embedder: Embedder,
    private val scorer: FadeScorer
) {
    suspend fun tick() {
        val candidates = workingMemory.entries
            .map { it to scorer.score(it) }
            .filter { (entry, score) -> score > thresholdFor(entry.rung) }
            .sortedByDescending { (_, score) -> score }

        for ((entry, _) in candidates) {
            val target = entry.rung.next()
            val compressed: Compressed = when (target) {
                Rung.KeyPoints  -> summariser.extractive(entry.content)
                Rung.Abstract   -> summariser.abstractive(entry.summary ?: entry.content)
                Rung.Embedding  -> embedder.embed(entry.summary ?: entry.content)
            }
            if (target == Rung.Embedding) {
                longTermMemory.store(entry.toLongTermRecord(compressed))
                workingMemory.remove(entry)
            } else {
                workingMemory.demote(entry, target, compressed)
            }
        }
    }
}
```

Throttling is the lever that makes this safe to run continuously. When
an agent is actively producing output, the pipeline backs off — the
abstractive summariser yields, the dispatcher yields, the agent gets
the compute. When the agent is idle between turns, the pipeline runs
at full speed and works through any backlog. The result is that the
agent feels the same regardless of pipeline activity; it's running
underneath the conversation, not next to it.

## Promotion and rehydration

The descent down the ladder is the dominant flow, but reads can move
material the other way. When [retrieval](./retrieval.md) scores a
long-term hit highly, the matching record can be **rehydrated** back
into working as a transient cue. Rehydration may bring back the
abstractive summary (rung 2 form) or, when the score is high enough
to justify it, the original raw text from the substrate (rung 0 form,
recovered via `originalEntryId`).

Rehydrated entries occupy a separate transient slot in working — they
do not displace native working content from the budget — and they live
only for the lifetime of the current reasoning step. If the agent
references the rehydrated content during reasoning, it earns native
working status with fresh fade tracking, and the next step treats it
as recent. If the agent doesn't reference it, the cue dissolves at step
end and the long-term record's state is unchanged.

This mechanism is what makes "I remember that" feel continuous. Without
it, retrieval would be a query operation that surfaces summaries in
isolation; the agent could *see* old material but never bring it back
into the working tier where reasoning happens. With it, a name dropped
in a query can pull an entire afternoon back into the present in a way
that closely resembles the way a smell can in a human.

## Loss validation

Lossy compression has a predictable hazard: it eventually loses
something the agent wishes it hadn't. The fade pipeline is monitored
against two signals.

The first is the rate at which rehydration goes all the way back to
the substrate (rung 0 raw form) rather than serving the rung-1 or
rung-2 summary. A rising "substrate read-through" rate suggests the
fade has been too aggressive — the summaries aren't carrying enough
signal to satisfy retrieval, and the architecture is paying for full
substrate reads to compensate. The fix is to loosen the rung-1 or
rung-2 thresholds so important records linger longer at higher
fidelity.

The second is the rate at which records are rehydrated and immediately
referenced. A high rate there means the demotion was wrong: the
material was important and the score didn't recognise it. The fix is
to reweight the per-agent fade composite, usually by raising
`w_emo` or `w_tags` to give advisor signal more authority.

These signals come from real usage, not from synthetic benchmarks. The
question of whether a particular entry was "important" is only
answerable in retrospect, by whether the agent ended up needing it.
The pipeline tunes itself against the answers as they arrive.

## What the fade is in service of

The same goal the [memory README](./README.md) names: an agent that does
not lose itself across sessions. The composite score, the ladder, the
throttled dispatcher, the rehydration path — every detail above is in
service of that goal. The architecture is willing to be quite
elaborate about memory because Solace's experiments demonstrated that
without memory continuity, every other feature degrades into something
shallower. *"I tend to get repetitive,"* she said of her own
working-tier overflow, *"my responses may loop, much like a person
struggling to focus due to mental exhaustion."* The fade is the
machinery that prevents that overflow from arriving at the agent's
front door.
