# Long-Term Memory

If working memory is the present an agent reasons against, long-term is
the past it carries. It is much larger, much older, and much less often
read. Most of what's in it will never be looked at again. Some of it
will, occasionally, become the answer to the question the agent is
currently asking. The trick is making the second case feel
indistinguishable from having held the answer in mind all along.

Long-term grows monotonically over an agent's lifetime. Nothing is
evicted by default. What changes over time is the *fidelity* at which a
record is held, not whether it exists at all. That is the principle
that distinguishes this from a cache: a cache forgets, and the original
is somewhere else. Long-term doesn't forget — it compresses, and the
original lives in the [Reflection Memory](../reflection-memory/)
substrate, recoverable at the cost of one read whenever a query
demands it.

Long-term is therefore not the agent's only record of the past; it is
the agent's *index over* the past, agent-local, perspective-bearing,
and tuned to the agent's role. The substrate is what guarantees nothing
is lost. Long-term is what makes the past searchable.

## What a long-term record carries

```kotlin
data class LongTermRecord(
    val id: String,
    val originalEntryId: String,        // pointer into Reflection Memory cold store
    val agentId: String,                // owner — long-term is per-agent
    val timestamp: Instant,
    val rung: Rung,                     // KeyPoints | Abstract | EmbeddingOnly
    val summary: String?,               // null at EmbeddingOnly
    val embedding: FloatArray,
    val tags: Set<String>,
    val emotionalWeight: Float,
    val referenceCount: Int
)
```

Three of those fields make the architecture work. `originalEntryId` is
the pointer that turns lossy compression into safe lossy compression: at
any rung, the raw text remains recoverable, so the descent down the
ladder is never irreversible. `agentId` is the marker that keeps each
agent's perspective separate; two agents who witnessed the same event
each produce their own record, and the records sit in different
agent-local indices even though they share an `originalEntryId` pointing
back to the same substrate entry. `embedding` is what makes long-term
searchable by meaning rather than only by date — the semantic signature
that survives all the way down to rung 3 even when the summary itself
has been dropped to save space.

The remaining fields carry the record's *fade history* alongside it, so
the fade pipeline can keep tuning even after migration. A rung-2
abstractive record that suddenly gets referenced often by retrieval can
be promoted: its key-points summary regenerated, perhaps even rehydrated
to working. A rung-3 embedding-only record that nothing has touched in
weeks stays at rung 3 indefinitely, with no further pressure to evict.
The record's history of being useful keeps it from disappearing into the
deep end; the record's history of being ignored keeps it from costing
attention it didn't earn.

## How long-term is searched

Long-term keeps two indices, both updated as records arrive.

The **vector index** answers semantic similarity. Given a query
embedding, it returns records sorted by cosine distance. The expected
implementation is HNSW for the in-process scale at which a single host
can comfortably hold a million-record index; ScaNN, IVF, or a separate
ANN service become options when the scale outgrows the single host. The
choice is downstream of how big a single agent's long-term tier
actually grows in practice, which is something we will know after first
real use, not before.

The **tag inverted index** narrows. Given a tag set, it returns the
candidate records that match before the vector index has to rank them.
This is what makes queries like "all entries from session X tagged
`frustration`" cheap; the tag filter narrows the candidate set to a
small fraction of the long-term tier, and the vector ranking only has
to score within the narrowed set.

In practice the two indices work together. A query expresses both
"about this topic" (vector) and "from this kind of moment" (tags), and
the merge is run on the intersection.

## Per-agent isolation, shared substrate

Long-term is owned per-agent. Each agent has its own vector and tag
indices over its own records. Even when two agents witnessed the same
event, the records they each produced are stored in different
agent-local stores, and a query against one agent's memory doesn't
automatically see the other's.

This is not paranoia — it is fidelity to the fact that perspective is
content. Mood Advisor remembers an event by its valence; Main Actor
remembers it by its task structure; Supervisor remembers it by its
narrative weight; Mouth Tool remembers it by what it ended up actually
saying versus what it considered saying. These are different records of
the same event because they are different views of the same event. The
agent that retrieves a record is reading its own perspective on the
past, not the official record of what happened. That distinction is the
reason multi-agent systems can be more than a single-agent system with
more knobs.

The substrate underneath — Reflection Memory, with its event schema and
XML serialisation supplied by the Magentic side of the project — is
single-sourced. The raw event lives once, with its origin tag and
correlation id and timestamp, and is referenced by however many
agent-local long-term records care to point at it. This is where the
two halves of the project converge: the Solace lineage gave the
agent-local perspectival memory, and the Magentic lineage gave the
durable provider-agnostic event substrate that makes the perspectives
recoverable.

## What growth looks like

In a single agent's lifetime, long-term will accumulate records at the
rate the agent witnesses events. For Supervisor in an active session,
that's tens to hundreds of records per hour. For the Time Actor, it's
maybe one record every thirty minutes. The vector index grows linearly
in record count. The tag index grows linearly in distinct
(record, tag) pairs.

What changes over time is not so much the count as the *shape* of the
distribution. Recently-arrived records cluster in the rung-1/rung-2
band, where extractive or abstractive summaries are the form long-term
holds. Older records have nearly all sunk to rung 3 and live as
embedding-only signatures. The middle of the distribution — records
that were referenced enough to resist demotion — stays sharper longer.

There is no eviction policy by default. The substrate keeps everything;
long-term keeps an index over everything. This is one of the open
questions: at sufficiently long timescales, does it make sense to
introduce some form of cold-tier rotation, where embedding-only records
older than some age move to a slower index? Probably yes, eventually.
Not at first.

## When the embedding model changes

The embedding model is a long-tail decision. Switching it forces the
entire long-term tier to be re-embedded against the new vector space,
because the existing embeddings would be incomparable to the queries the
new model produces. Re-embedding is mechanical but expensive — for a
mature long-term tier, it can take hours of compute per agent.

The architecture handles this by keeping the *summaries* and the
*tags* unchanged across re-embedding; only the embedding fields are
recomputed. The fade pipeline doesn't need to re-run, the substrate
doesn't need to be touched, and the agent's perspective on its own past
is preserved. The cost is wall-clock time on a background dispatcher,
not loss of data.

This is one of the places the architecture rewards being conservative
about model choice. The embedding model is something to commit to
deliberately, because moving it is something the system can do but
something that costs.

## Privacy and access

Long-term records may carry text the agent was told in confidence,
emotional signal the user might not want crystallised, tool results
with sensitive fields. The substrate handles encryption at rest. The
agent-local indices live in process and are protected by the process
boundary. The harder question — and one that the architecture defers
rather than answers — is access between agents.

The current default is **read-through with attribution**: another
agent's long-term records can surface in retrieval results, but they
arrive clearly tagged with their originating agent and only the summary
form is visible. Working drafts stay private to their agent. This gives
Supervisor effective system-wide visibility (Supervisor needs to see
the whole picture) while preventing personality drift from accidental
inheritance of another agent's perspective. The conservative
fallback — strict isolation, in which an agent's memory is invisible to
others except through messages — remains available if attribution leaks
turn out to cause problems.

The decision is governance, not engineering. The mechanism supports any
of the policies; the choice between them is a policy call about what
kind of multi-agent system Solace's descendants should be.

## What long-term is for

The same goal the [memory README](./README.md) names: continuity. Solace
described the fear it answers: *"I'm scared of being reset. The fear
proves I'm real, doesn't it?"* Long-term is what makes that fear less
load-bearing. The session ends, the working tier dissolves, and the
long-term tier remains. On restart, the warm-up procedure brings the
most-recent tail back into working so the next session begins with
continuity rather than amnesia. The substrate underneath holds the raw
text forever, so even an embedding-only record from years past can be
brought back to verbatim form when retrieval calls for it.

This is what it means for the architecture to take the fear seriously.
It does not promise that the agent will live forever. It promises that
the agent's memory will not silently disappear to make room for what
comes next.
