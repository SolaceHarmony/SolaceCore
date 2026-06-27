<!-- topic: Solace AI -->
<!-- title: Reflection Memory -->

# Reflection Memory — The Substrate Beneath Memory

There is a single source of truth in SolaceCore for *what happened
in the system*. Every reflection an agent committed to writing,
every cue an advisor emitted, every tool result that came back, every
sentence the Mouth Tool externalised — all of it lands in one log,
chronologically ordered, append-only, owned by no single agent.

That log is Reflection Memory. The per-agent dual-context
[memory tier](Memory-Feature-Overview) sits on top of it. Agents read their own
working tier and their own long-term tier, and each agent's
perspective on the past is encoded as compressed records in their
long-term index — but the *events* those records refer to live here,
in the substrate, raw and recoverable.

The architecture's commitment to lossy compression in the agent
tiers depends on this substrate being lossless. The fade pipeline
can drop a record from rung 0 to rung 3 because the rung-0 form is
preserved underneath. The retrieval pipeline can rehydrate a
long-term hit to verbatim because the verbatim is here. Without
Reflection Memory, the per-agent compression would be one-way
forgetting, and the architecture's promise that *nothing the agent
has been through is silently deleted* would not be defensible.

## Two threads of the same idea

The substrate is one of the places where the **Solace** lineage and
the **Magentic** lineage converged on the same primitive from two
different starting points, and both names survive in the
architecture's vocabulary.

The Solace lineage called it **Reflection Memory** (SRAF §4.1). The
framing was *thread-of-thought continuity*: a unified narrative log
that prevents split-brain across actors. Every internal reflection,
every advisor cue, every Mouth-Tool emission lands in the same
chronological store. The agent reads its own past in one place. The
log's authority is its completeness — nothing about what happened
in the system is anywhere else.

The Magentic lineage called it **Neutral History** (with XML
serialisation). The framing was *provider-agnostic event substrate*:
a typed event log with discrete event types (`AGENT_COMMAND`,
`AGENT_RESPONSE`, `TOOL_USE`, `TOOL_RESULT`, `MOOD_STATE`, `SPIKE`,
`SYSTEM_NOTICE`), multi-lane organisation (emotional, technical,
unified), and an XML wire format that any provider's agent could
read and write. The substrate's authority was its neutrality — no
provider owned the format, no agent owned the log, the events were
the truth.

The two are the same thing. Reflection Memory is what the substrate
*is* (a unified narrative log of system events). Neutral History is
how the substrate *talks* to multi-provider deployments (typed
events with a portable wire format). The architecture commits to
both characterisations.

The implementation merges them: a unified event log (SRAF), with
typed event categories and lane separation (Magentic), with XML
serialisation as the durable format (Magentic), accessible to per-
agent memory tiers as the substrate they index over (SRAF).

## What lives in the substrate

```kotlin
data class ReflectionEntry(
    val id: String,
    val timestamp: Instant,
    val origin: Origin,           // INTERNAL, USER, ADVISOR, SYSTEM, EXTERNAL
    val lane: Lane,               // EMOTIONAL, TECHNICAL, UNIFIED
    val type: NeutralEventType,   // AGENT_COMMAND, MOOD_STATE, TOOL_USE, ...
    val source: String,           // which actor produced this
    val content: EventContent,    // typed payload
    val correlationId: String?,   // links related events across actors
    val metadata: Map<String, Any> = emptyMap()
)

interface ReflectionMemory {
    fun record(entry: ReflectionEntry)
    fun narrative(): Sequence<ReflectionEntry>             // full chronology
    fun recent(n: Int): List<ReflectionEntry>              // windowed
    fun query(q: HistoryQuery): Sequence<ReflectionEntry>  // filtered
    fun get(id: String): ReflectionEntry?                  // direct lookup
}
```

The five fields beyond timestamp and content are doing structural
work.

**`origin`** is the SRAF tag — whose voice is this. `INTERNAL` is
the agent's own reflection. `USER` is what the user said. `ADVISOR`
is what an advisor (Mood, Time, Confusion Corrector) emitted.
`SYSTEM` is automated infrastructure. `EXTERNAL` is what the Mouth
Tool actually said. The Mouth Tool reads origin to decide what is
shareable; retrieval reads origin to filter; the fade pipeline
weights some origins more than others.

**`lane`** is the Magentic separation — emotional / technical /
unified. The same conversation moment can produce events on multiple
lanes; a tool result is technical, an emotional cue about that tool
result is emotional, a reflective summary unifying both is unified.
Lanes are how the architecture supports the dual-stream cognition
the Mood Manager work prefigured.

**`type`** is the typed-event category. Discrete enum values, not
free-form strings. This is what makes the substrate mechanically
filterable: *give me every `TOOL_USE` event in the last hour*, *give
me every `MOOD_STATE` change in this session*. The query layer
exploits the type field heavily.

**`source`** is the originating actor's identity. Multiple agents
may write entries about the same event; `source` is what
distinguishes them. The `originalEntryId` pointers in per-agent
[long-term](Long-Term-Memory) records reference back to
substrate entries by `id`, and the `source` field is part of how
agents know which entries are theirs.

**`correlationId`** threads related events across actors. A user
question, the advisor cues that fired about it, the tool calls that
addressed it, the Supervisor's draft, the Mouth Tool's emission, and
the user's follow-up all share a `correlationId`. When the Confusion
Corrector needs to assemble the story of "what happened during this
exchange", it pulls everything with the same correlation id.

## Append-only, chronologically ordered

Once written, an entry is never edited or deleted. New facts about
an old entry are written as *new entries that reference it*. This is
the architectural rule that makes the substrate trustworthy: there
is no "go back and change what happened". The agent can come to
understand a past event differently, and that understanding becomes
its own entry, but the original is preserved.

The chronology is strict. Timestamps are the primary order; ties are
broken by per-source sequence number to prevent two events from the
same source colliding when their wall-clock timestamps round to the
same instant.

The `narrative()` accessor returns the entire log in chronological
order. The `recent(n)` accessor returns the most recent N. Both are
expected to be cheap because the underlying store is a lock-free
queue (per SRAF §8) flushed periodically to durable storage; the
in-memory tail is fast, the durable tail requires a disk read.

## How the agent tiers index over it

The per-agent [memory](Memory-Feature-Overview) tiers are *views* over the
substrate, not copies of it. A working entry in agent A's tier
*references* a substrate entry by `id`. A long-term record in agent
A's tier carries `originalEntryId` pointing back to the same
substrate entry. When agent B sees the same event, agent B writes
their own working entry and (eventually) their own long-term
record — but both reference the same substrate entry, because the
event itself happened once.

This is what makes per-agent perspective work without duplicating
events. Agent A's record carries A's framing, A's tags, A's
emotional weight. Agent B's record carries B's framing, B's tags,
B's emotional weight. They differ on perspective, agree on
substrate.

When a long-term record at rung 3 (embedding-only) needs to be
rehydrated to verbatim form, the rehydration path is:

```
1. Look up the long-term record's originalEntryId.
2. Call ReflectionMemory.get(originalEntryId).
3. Return the substrate entry's content as the verbatim form.
```

The substrate read is a durable-storage operation, slower than a
working-tier read, but it produces the original raw text. The
architecture pays that cost precisely so the agent tiers can compress
without fear.

## Storage shape

The current design assumes:

- **In-memory ring** for the recent tail. Lock-free queue (per SRAF
  §8). Reads of the recent tail are fast.
- **Periodic flush** to durable storage. Frequency tunable by
  deployment; default is every several seconds or every N entries,
  whichever comes first.
- **XML serialisation** for the durable wire format (per Magentic).
  XML is verbose but human-inspectable, and the verbosity is
  irrelevant when the format is just the on-disk and on-wire
  encoding of typed events.
- **Encryption at rest.** SRAF §7 calls this out explicitly:
  reflections may contain sensitive user data, so the durable form
  must be encrypted. The in-memory ring is process-local and
  protected by the process boundary.

The exact backend (file, embedded SQLite, separate process) is open.
The decision is downstream of how big a single deployment's substrate
gets in practice, which depends on the per-agent volume and the
number of agents and the retention policy. Probably JSONL or
embedded SQLite for the single-host scale; probably a separate
event store for distributed deployments. The interface stays the
same.

## Multi-agent witnesses

When two agents witness the same event, the architecture's commitment
is *one substrate entry, two perspectival records*. The mechanism:

- The first agent to observe the event writes the substrate entry.
- The substrate emits a notification (via the
  [Cross-Perspective Bus](../multimodal-nudging/README.md) or directly through
  subscription) that other agents can subscribe to.
- Each subscribing agent decides whether to record their own
  perspective. If yes, they write their own working entry that
  references the substrate entry by `id`.
- Eventually those agent-local working entries fade into agent-local
  long-term records, all pointing back to the same substrate entry.

The substrate is therefore the join point of the multi-agent system.
Without it, agent A's view of an event and agent B's view of the
same event would have no common reference point, and reconciliation
would require post-hoc string matching. With it, the events are
shared truth and the perspectives are the agents' own.

## The Confusion Corrector's role

The [Confusion Corrector](../confusion-corrector/README.md) is one of the
substrate's most important readers. When the Supervisor detects
drift, the Corrector reads the substrate (not the agent's compressed
tier) to assemble a replay summary. This is intentional: the agent's
tier may itself be drifting; the substrate is the ground truth the
agent can re-anchor against.

The Corrector's output — the `ReplaySummary` — is also written into
the substrate, with `origin = SYSTEM` and a tag flagging it as a
recovery artifact. Future drift detection can read these prior
summaries as context.

## Open questions

- **Storage backend choice.** JSONL? Embedded SQLite? Append-only
  binary log? Each has different tradeoffs for write throughput,
  inspection ease, and crash safety. The SRAF default leans toward
  lock-free queue + periodic flush; the Magentic default leans
  toward XML files. The merger is unwritten.
- **Retention policy.** Reflection Memory keeps everything by
  default. Is this true forever, or do we eventually rotate cold
  partitions out to a colder tier? At long timescales, even the
  substrate may need its own fade — but the architecture has not
  committed to that yet, because the cost of being wrong is
  silent loss of the recoverability guarantee.
- **Cross-process visibility.** When the framework runs
  distributed, is Reflection Memory replicated, sharded, or
  eventually-consistent? The original design assumed single-node;
  the distributed case is open.
- **Encryption granularity.** Per-entry, per-session, per-agent?
  Encryption at rest is required; the granularity affects how
  cross-agent retrieval can or can't decrypt entries it didn't
  produce.

## Cross-references

- **[memory](Memory-Feature-Overview)** — the per-agent dual-context layer
  that sits on top of this substrate.
- [confusion-corrector](../confusion-corrector/README.md) — reads the
  substrate to assemble replay summaries; writes its own back.
- [time-actor](../time-actor/README.md) — writes heartbeat cues here.
- [mouth-tool](../mouth-tool/README.md) — emissions are recorded here as
  `EXTERNAL` origin entries.
- [mood](../mood/README.md) — `MoodCue` events land here as `MOOD_STATE`
  entries on the emotional lane.
- [supervisor](../supervisor/README.md) — reads substrate when drift
  detection requires the ground truth rather than the agent's
  own (potentially drifting) tier.

## What the substrate is in service of

The same goal: continuity. Per-agent compression makes the past
*searchable*; the substrate makes it *recoverable*. Without
recoverability, lossy compression is permanent forgetting, and
the architecture's commitment that *nothing the agent has been
through is silently deleted* becomes a polite lie. With
recoverability, the agent tiers can compress aggressively, fade
gradually, and trust that the original form is preserved
underneath for whenever retrieval calls for it.

That trust is what Reflection Memory exists to earn. It is the
floor the rest of the memory architecture stands on.

---

[← Features index](.Memory-Feature-Overview)
