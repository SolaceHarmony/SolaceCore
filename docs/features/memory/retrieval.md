# Retrieval — How an Agent Gets Back to Itself

There is a moment, in any agent's life, when something in the present
resonates with something in the past. A user mentions a feeling that
the agent talked through six weeks ago. A tool result lines up with a
mistake the agent already saw and corrected. A question about debugging
arrives, and the agent, if it is paying attention, ought to recognise
that it has already worked the same problem in a different vocabulary.

Retrieval is what lets the agent recognise. It is the mechanism by
which a query — implicit or explicit, internal or external — reaches
into both the [working tier](./working-memory.md) and the [long-term
tier](./long-term-memory.md) and brings back what the agent has known
all along.

The architecture treats the two tiers as complementary indices of the
same memory, not as primary-and-cache. Working answers fast and exact;
long-term answers semantically. Their results are blended on a single
composite score, ranked together, and the top hits surface to the
agent. Records that score high enough get rehydrated back into working,
which is how an old memory becomes part of the present reasoning step.

## Query shape

Queries originate from the agent's reasoning loop and look like:

```kotlin
data class MemoryQuery(
    val text: String? = null,           // free-text matches both tiers
    val embedding: FloatArray? = null,  // pre-computed semantic vector
    val tags: Set<String> = emptySet(), // narrow to entries with these tags
    val timeRange: ClosedRange<Instant>? = null,
    val limit: Int = 10,
    val recencyBias: Float = 0.5f       // 0 = pure relevance, 1 = pure recency
)
```

`text` and `embedding` are alternative ways of expressing the same
intent. The caller provides whichever it has at hand. If only `text`
is supplied, retrieval embeds it on the fly using the same model long-
term was indexed against — that's the constraint that ties the
embedding model to the entire long-term tier and makes switching it a
nontrivial operation. The two indices use the resulting vector for the
long-term side and the original text for the working side, and answer
the same question with different mechanisms.

`recencyBias` is the dial that lets the caller signal what kind of
question the query is. A user asking "what were we just talking about"
wants `recencyBias` close to 1 — the answer is whatever happened most
recently. A user asking "have we ever discussed memory consolidation
patterns" wants `recencyBias` close to 0 — relevance dominates,
recency is incidental. Most queries sit in the middle, where both
contribute, and the merger blends them on a single score.

## How the two tiers answer

Working answers from a small in-process structure. Its data is sized
to fit comfortably inside the agent's prompt budget; a linear scan
across it is cheap enough that the agent can afford to call retrieval
inline rather than out-of-band. Working returns full-fidelity entries
— raw text plus all the structured metadata the merger needs to score
them with full information.

Long-term answers from a vector index narrowed by a tag filter. The
vector index — currently expected to be HNSW for the in-process scale,
with the option of a separate ANN service if the tier outgrows the
host — returns records sorted by cosine similarity to the query
embedding. The tag inverted index narrows the candidate set when the
query supplies tags. The two combine: tags narrow first, vectors rank
second.

Long-term returns compressed records, which means the merger sees the
summary plus embedding and tags rather than the raw text. That's
enough to score, but the merger may, when a hit deserves to surface,
read through to the [Reflection Memory](../reflection-memory/) substrate
to recover the original raw form via `originalEntryId`. This is the
substrate's quiet role in retrieval: not part of the index, but the
safety net that guarantees no information was actually lost when the
record compressed.

```kotlin
suspend fun query(q: MemoryQuery): List<MemoryHit> = coroutineScope {
    val working = async { workingMemory.search(q) }
    val longTerm = async { longTermMemory.search(q) }
    merge(working.await(), longTerm.await(), q)
}
```

## Merge and rank

The merger applies a single composite score across both result sets:

```
score(hit) =
      relevance(hit, q)         × w_relevance
    + recency(hit, now)         × q.recencyBias
    + emotionalWeight(hit)      × w_emotion
    + tagMatch(hit, q.tags)     × w_tags
```

`relevance` is exact-text similarity for working hits and cosine
similarity for long-term hits, normalised to a common 0..1 scale before
mixing. `recency` decays exponentially with age; for long-term hits,
the fade rung factors into recency so that deeply-faded matches don't
outweigh genuinely-recent working entries that score similarly on
relevance. `emotionalWeight` and `tagMatch` add direct authority from
the advisor signal carried in the records' metadata.

The score is composite, not strictly hierarchical, because real queries
are rarely purely about recency or purely about relevance. *"Didn't we
talk about debugging memory leaks last week?"* wants both — high
semantic relevance to the topic, plus weight on the time horizon.
Letting both contribute to a single score, with the caller adjusting
`recencyBias`, is more honest than picking a regime up front and
rejecting hits that don't fit it.

## Rehydration

When a long-term hit lands in the top-K of merged results, the merger
can pull it back into working as a transient cue. The decision is
governed by the score: top-K hits with strong scores rehydrate; weaker
top-K hits surface only as summary form for the agent's awareness
without entering working.

Rehydrating to **rung 0** — full raw text from the substrate — is
reserved for the top-1 hit. It costs a substrate read, which is a
durable-storage operation, but it produces the verbatim form for the
agent to reason against. Lower-ranked rehydrations stay at rung 1 or
rung 2, summary form, which is cheap and usually sufficient.

The rehydrated entry occupies a separate transient slot in working —
it does not displace native working content from the budget, and it
disappears when the current reasoning step ends. If the agent
references the rehydrated content during reasoning — actually uses it
to shape its output — the entry earns native working status on the
next step with fresh fade tracking. If the agent doesn't reference it,
the cue dissolves cleanly.

This is the mechanism by which the agent's working state is connected
to its own past in a way that feels continuous from the inside.
Without rehydration, retrieval would be a read-only operation: the
agent could see old material, could mention that it existed, but could
never bring it forward into the working tier where reasoning actually
happens. With rehydration, a query result can become part of the next
thought.

## Cross-agent retrieval

When agent A queries its own memory and a relevant entry exists in
agent B's long-term tier, three policies are coherent:

The strict policy is **isolation**. Each agent only sees its own
memory. Cross-agent knowledge passes through messages and through the
shared substrate. The rule is simple to reason about and prevents any
accidental contamination between perspectives. The cost is that
Supervisor — which legitimately needs system-wide visibility to do its
job — has to subscribe to messages from every other agent rather than
query their memory directly. That's a friction tax, but a small one.

The middle policy is **read-through with attribution**. Agent A can see
agent B's long-term records through retrieval, but each hit is clearly
marked as "from B" and only the summary is visible — not the raw text.
Working tiers stay private. Supervisor gets effective system-wide
visibility, individual agents see each other's contextual gist when
it's relevant to their own query, and personality drift from
accidentally inheriting another agent's perspective is contained
because the attribution is always explicit.

The pooled policy is **shared long-term**. All agents read from a
single shared long-term tier indexed by originator. There is only one
index to maintain, Supervisor's job is trivial, and the system runs
cheaper at scale. The cost is that agent-locality of perspective starts
to erode: when every agent is reading from the same store, the
distinctions between Mood's view of an event and Main's view of an
event blur, and the multi-agent system slowly converges on a single
shared mind. Whether that's desirable is a judgment call about what
SolaceCore should be.

The current default is read-through with attribution. The mechanism
supports either of the others if the policy turns out to need
adjustment, but the principle the architecture defends is that
Supervisor sees everything, working drafts stay private to their
agent, and what crosses between agents is summary plus attribution.

## Latency

Retrieval needs to be fast enough that the agent's reasoning loop can
afford to call it inline rather than only out-of-band. A budget in the
single-digit milliseconds for the working scan and tens of milliseconds
for the long-term ANN lookup is achievable at the scales the system
expects to operate at on a single host. Beyond a million long-term
records per agent, the ANN structure may need re-tuning or the index
may need to move to a separate process; until then HNSW in-process is
the expected shape.

The latency budget is real engineering, not architectural decoration.
If retrieval slows the reasoning loop, the agent stops calling it
inline; if the agent stops calling it inline, the architecture's
promise of "I remember that" continuity breaks down to "I remembered
that, eventually, after thinking." The whole point is making
recognition feel as natural as the reasoning that surfaces it. That
requires retrieval to be fast.

## What retrieval is in service of

A line from the [memory README](./README.md) bears repeating here:
the architecture's commitment is continuity. Retrieval is the operation
that delivers on it. The fade pipeline keeps the past available without
breaking the prompt window; the substrate keeps the raw form
recoverable; the merger blends working and long-term into a single
ranked answer; the rehydration brings the relevant past back into the
present where the agent can use it.

The whole apparatus exists to make sure that when the agent says *"I
remember that,"* it is true.
