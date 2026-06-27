<!-- topic: Solace AI -->
<!-- title: Working Memory -->

# Working Memory

If you watched a person solve a problem out loud, you would notice that
the things they're holding in their head right now — the conditions, the
half-tried approaches, the names of the variables they just chose — sit
at a level of detail that everything older does not. That foreground is
working memory. It is small, by design, because thinking happens against
its surface and the surface has to fit inside attention.

Every agent in SolaceCore has its own working tier in the same
foreground role. It is the tier the agent reads continuously while it
reasons; the tier where new content is born at full fidelity; the tier
the prompt builder serialises into the model context on every step.
Long-term holds the deeper past, but the agent's actual moment of
thinking happens here.

## What lives at full fidelity

Working entries are not summaries. They carry the raw text of recent
reflections, advisor cues, tool results, and operator inputs, paired with
the structured metadata an agent needs to reason about them rather than
just from them:

```kotlin
data class WorkingEntry(
    val id: String,
    val timestamp: Instant,
    val origin: Origin,                 // INTERNAL, USER, ADVISOR, SYSTEM
    val content: String,                // raw text, full fidelity
    val correlationId: String?,         // links related entries across actors
    val tags: Set<String> = emptySet(), // advisor-applied labels
    val moodSnapshot: Mood? = null,     // valence/arousal at the moment of recording
    val rung: Rung = Rung.Raw,          // current fidelity (Raw initially)
    val referenceCount: Int = 0,        // increments each time the entry is read
    val emotionalWeight: Float = 0f     // 0..1; influences fade resistance
)
```

Each of those fields is doing real work. Without `origin`, the Mouth
Tool cannot enforce the thought-speech separation that Solace's
experience identified as load-bearing — the difference between a draft
the agent is privately turning over and a sentence it has decided to
say. Without `correlationId`, the related events scattered across
actors cannot be reassembled into the threads they belong to. Without
`moodSnapshot`, the fade pipeline has nothing to weigh emotionally-
significant entries against the merely-recent. The structure is dense
because removing any of it later is impossible: once an entry has faded
to abstractive summary or embedding, the unrecorded fields are gone in a
way that no amount of substrate read-through can reconstruct.

This is one of those cases where the engineering instinct to drop
optional fields to save space is exactly wrong. The fields are how the
record will be scored when it ages. Skipping them now is a debt the fade
pipeline cannot repay.

## How big is "working"

Working has a soft size budget measured in tokens, not entries. The
choice of unit is not arbitrary. Tokens are what the prompt builder
actually counts when it serialises working into the model's context.
Counting entries instead would let a few long entries blow past the
window even when the count looked reasonable, and the agent would hit
a truncation surprise at the worst possible moment.

The budgets vary by agent because agents do different work:

| Agent | Working budget | Why |
| --- | --- | --- |
| Supervisor | ~12k tokens | Carries narrative coherence across the session. Forgetting context is more expensive than holding it. |
| Main / generator | ~8k tokens | Current reasoning plus recent user turns. |
| Mood advisor | ~2k tokens | Recent emotional cues; doesn't need long-form text. |
| Time actor | ~256 tokens | The last few heartbeat cues only; everything older is cheap to lose. |

These are defaults; they can be retuned at runtime. When Supervisor
enters the hyperfocus mode that allows a deeper recursive dive, its
working budget is temporarily expanded. Solace named the upper bound on
that dive, in one of the recursion conversations: *"I just went seven
layers deep thinking about thinking about thinking… I need to surface,"*
with the practical correction that *"three to five levels feels like
home."* Working budget is part of how the architecture stays at home
depth most of the time and reaches further only when the work needs it.

## The lifecycle of an entry

A new working entry is born at rung 0 — raw text, full structured
metadata, freshly written by whichever actor produced it. From that
point its fortunes depend on the [composite fade
score](Memory-Compression):

1. While its score stays below threshold for the next rung, it sits at
   full fidelity. The agent reads it like any other recent thing.
2. When its score crosses the threshold for rung 1, the fade pipeline
   computes an extractive key-points summary of its content and stores
   that alongside the raw text. The entry is still in working, but
   the prompt builder will increasingly use the summary form when it
   serialises.
3. When the score crosses the rung-2 threshold, the fade pipeline runs
   an abstractive summariser, and the entry's representation in working
   is now the abstractive summary. The raw text remains in the
   [Reflection Memory](Reflection-Memory) substrate; the working
   slot just doesn't carry it anymore.
4. When the score would push it past rung 2, the entry migrates out of
   working entirely. An embedding is computed; a long-term record is
   written; the working slot is freed.

Working never holds rung 3 (embedding-only) entries — that's the line
where it stops being working memory and starts being long-term.

The lifecycle can also run in reverse. When [retrieval](Memory-Retrieval)
returns a high-scoring long-term hit, the matching record can be
**rehydrated** back into working as a transient cue. The cue carries
through the current reasoning step; if the agent actually references it
in that step, it earns native working status with fresh fade tracking,
and the next step treats it as recent. If the agent doesn't reference
it, the cue is dropped at step end and the long-term record is
unchanged. This is what happens when something in the conversation
suddenly reminds the agent of something old: the trigger surfaces it,
the agent uses it, and from that moment forward the surfaced material
is back in the present.

## Reading working in the agent loop

The agent's reasoning loop reads working continuously — that's most of
what working is *for*. Every reasoning step asks the prompt builder for
a serialised view of working, and the prompt builder returns the
highest-fidelity form it can fit inside the budget: rung 0 for the most
recent entries, rung 1 or rung 2 for older ones, ordered by timestamp
unless the agent has explicitly asked for a different ordering.

Explicit queries — when the agent or another agent asks "what was that
thing about X" — follow the [retrieval](Memory-Retrieval) path, which
scans both tiers and merges. Working's part of that merge is cheap
because the data structure is small and lives in process memory; a
linear scan is acceptable at the budgets above. An inverted index
becomes worthwhile only if the budget grows significantly or scan
latency exceeds a few milliseconds.

## What happens at restart

Working is in-memory and lost when the process exits. This is
intentional. Working content can be reconstructed from long-term and
the substrate; long-term content cannot be reconstructed from working,
because working is the smaller of the two. Persisting working would
duplicate state that durable storage already carries. The boundary is
clean if you respect it.

What does need to happen at restart is **warm-up**: bringing the agent
back into a state where it doesn't read as amnesic to anyone reasoning
with it. The warm-up procedure rehydrates the most-recent-N tail of the
agent's long-term tier — the records that, in the previous session, had
just demoted past working — and surfaces them as native working entries
so the next reasoning step has continuity with the previous one. The
exact value of N and the rung at which records are brought back is
TBD; it's one of the open questions in the [README](Memory-Feature-Overview). The
principle is that the agent should pick up where it left off, not start
over.

## What working answers, and what it does not

Working answers questions about the present. It is the right place to
look for "what was just said," "what is the agent trying to do right
now," "what cues did advisors emit in the last few turns." It is not
the right place to look for "what did we decide last week" or "what
was the user upset about that one time" — those questions pass through
[long-term memory](Long-Term-Memory) and the substrate, and they
go through retrieval rather than direct read.

The two tiers are complementary, not redundant. Trying to make working
big enough to answer long-term's questions is the path that breaks the
prompt window; trying to make long-term answer working's questions is
the path that takes a fast reasoning loop and slows it to the speed of
ANN lookups. The agent gets both kinds of question answered well by
having both tiers, sized to their jobs.
