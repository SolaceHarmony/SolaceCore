# Reflection Memory

**Status:** designed (now scoped as the substrate beneath per-agent memory)
**Origin:** SRAF §4.1 (1524574, Jun 2025); recent rename Neutral History XML in codex-vendored material

> Append-only unified narrative log. Every internal/external event lands here in one chronologically-ordered store. Prevents split-brain across actors.
>
> **Role in the current design:** the system-wide *substrate* beneath per-agent dual-context [memory](../memory/). Working and long-term tiers are agent-local *views*; their underlying raw events all persist here so any compressed-down record can be rehydrated to verbatim form on demand.

---

## Design

Reflection Memory is the single source of truth for *what happened in the
system*. Every actor — Supervisor, Mouth Tool, advisors, tool executors —
writes events here. The store is:

- **Append-only.** Once written, an entry is never edited or deleted. New
  facts about an old entry are written as new entries that reference it.
- **Chronologically ordered.** Timestamps are the primary key; ties broken
  by per-source sequence number.
- **Origin-tagged.** Every entry knows whether it was `INTERNAL`,
  `USER`, `ADVISOR`, or `SYSTEM` in origin (per SRAF §4.1).
- **System-wide, not per-agent.** Multiple agents can witness the same event
  and write their own perspectives; all records land in the same log.

Per-agent [memory](../memory/) tiers index *into* this log. Working memory
holds raw text in process; long-term holds embedding-and-summary records that
reference the underlying log entry by id. When a query through long-term
needs the raw form, the substrate reads it back from here.

## Implementation

_(Substrate hasn't landed in `lib/` yet. The closest thing today is
`actorRegistry` + per-actor mailboxes; those are control-plane state, not the
event log this describes. The `core.mood` module landing today is a downstream
consumer.)_

## Open questions

- **Storage backend.** In-memory ring + periodic flush to a durable file/DB
  (per SRAF §8: "lock-free queue; periodic batch flush"). What's the file
  format — JSONL, append-only binary, embedded SQLite? Tradeoff between
  ease of inspection and write throughput.
- **Retention policy.** Reflection Memory keeps everything by default —
  raw text never disappears even after long-term has compressed it. Is
  this true forever, or do we eventually rotate cold partitions out to
  a colder tier?
- **Cross-process visibility.** When the framework runs distributed (per
  the original Nov 2024 vision), is Reflection Memory replicated, sharded,
  or eventually-consistent? The original design assumed single-node; the
  distributed case is open.
- **Privacy + access control.** SRAF §7 calls out that reflections may
  contain sensitive user data; encryption at rest is required. Per-agent
  read access to others' entries is governed by the cross-agent retrieval
  question over in [memory/retrieval.md](../memory/retrieval.md).

## Cross-references

- **[memory](../memory/)** — the per-agent dual-context layer that sits
  on top of this substrate.
- [confusion-corrector](../confusion-corrector/) — writes `ReplaySummary`
  entries here when it compresses a span of context.
- [time-actor](../time-actor/) — writes temporal cues into this log.
- [mouth-tool](../mouth-tool/) — its outputs are also recorded here, with
  origin tag `EXTERNAL` (or `SYSTEM`, depending on framing).

---

[← Features index](../README.md)
