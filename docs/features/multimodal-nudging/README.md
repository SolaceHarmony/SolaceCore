# Multimodal Cross-Perspective Nudging — How the Senses Talk

A creature with eyes and ears doesn't reason about what it sees and
what it hears as separate problems. The information arrives in
parallel streams — visual cortex doing its work while auditory
cortex does its own — and the streams *converge*, contributing to a
single integrated impression of what just happened. By the time the
creature decides to respond, the response is informed by everything
the senses noticed, not just the modality it consciously focused
on.

The Cross-Perspective Bus is SolaceCore's architectural form of that
convergence. Each modality actor — Vision, Audio, Text, and any
others a deployment adds — runs in parallel. Each one perceives in
its own domain and emits short structured "nudges" to a shared bus.
The Supervisor reads the bus continuously and merges high-confidence
nudges into the prompt prime that feeds the next reasoning step.
The Mouth Tool, when it produces output, can answer modality-aware
questions in the same turn the question was asked, because the
modality answer was already on the bus by the time the question
arrived.

The result is what Addendum A calls *zero-latency answers* to
queries like *"Is the shirt blue?"* or *"Did she sound angry?"* —
not zero in the literal sense, but zero in the sense that the user
doesn't wait for the system to decide to perceive. Perception is
already happening, in parallel, all the time.

## The topology

```
   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
   │ Vision Actor │    │ Audio Actor  │    │  Text Actor  │
   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
          │ VisionCue         │ AudioCue          │ TextCue
          │                   │                   │
          ▼                   ▼                   ▼
   ┌─────────────────────────────────────────────────────┐
   │           Cross-Perspective Bus                     │
   │           (lock-free pub/sub, TTL-aware)            │
   └────┬────────────────┬────────────────┬─────────────┘
        │                │                │
        ▼                ▼                ▼
   Supervisor       Mouth Tool       Other modalities
   (reads as        (active         (mutual priming
   prompt prime)    narrator)       across senses)
```

The bus is not a one-way feed into the Supervisor. Every modality
actor subscribes to the bus too, which means each sense can be
informed by what the other senses noticed. A vision actor that
sees a crowd can be primed by the audio actor's "loud
environment" cue, sharpening its frame analysis for the noisy
context. The mutual priming is what makes the senses *work
together* rather than each producing isolated reports.

## The nudge protocol

Every cue on the bus has the same shape:

```
[NUDGE]::<ORIGIN>::<TTL>::<TEXT>
```

with `ORIGIN ∈ {VISION, AUDIO, TEXT, ...}`, `TTL` in milliseconds,
and `TEXT` a short canonical fact.

In code form:

```kotlin
data class Nudge(
    val origin: Modality,
    val text: String,           // canonical fact
    val confidence: Float,      // 0..1
    val ttlMs: Long,
    val timestamp: Instant
)

interface CrossPerspectiveBus {
    fun publish(nudge: Nudge)
    fun subscribe(filter: NudgeFilter): Flow<Nudge>
    fun snapshot(): List<Nudge>          // active (unexpired) nudges
}
```

Five fields, each load-bearing.

**`origin`** distinguishes which sense produced the cue. The
Supervisor's prompt-prime formatting reads origin to label the
context: *"Context-Prime (VISION): The subject's shirt is blue."*
Origin labels keep modalities legible to the language model so it
doesn't conflate what was seen with what was heard.

**`text`** is the canonical fact. Short. Declarative. *"shirt blue"*
or *"angry tone"*. The text is what the language model reads; it
should fit a single phrase comfortably, because nudges accumulate
and a verbose nudge crowds the prompt.

**`confidence`** is the modality actor's certainty. Vision returning
0.94 for a clear blue shirt is high confidence. Audio returning
0.55 for an ambiguous tone is borderline. The Supervisor's fusion
logic filters by confidence threshold (default 0.7), so low-
confidence nudges don't pollute the prompt.

**`ttlMs`** is when the nudge expires. A vision cue describing the
current frame should live milliseconds. A vision cue describing a
*scene* (the room is a kitchen) should live longer — the scene
doesn't change every frame. TTLs prevent stale modality facts from
polluting later turns.

**`timestamp`** is when the cue was generated. Combined with TTL,
it lets the bus garbage-collect expired nudges without per-nudge
timer threads.

## How the Supervisor fuses nudges

The Supervisor reads the bus at the start of every reasoning step
and assembles the active nudges into a *Context-Prime* section of
the prompt:

```kotlin
fun integrateNudges(nudges: List<Nudge>): ContextPrime {
    return nudges
        .filter { it.confidence > 0.7 && !it.expired() }
        .sortedByDescending { it.confidence }
        .joinToString("\n") { "Context-Prime (${it.origin}): ${it.text}" }
}
```

The fused prime is appended *below* the conversation synopsis but
*above* the response scaffold. Position matters. Below the
synopsis means the model sees the conversational context first,
which is what governs *what* the user is asking. Above the
response scaffold means the modality facts are present when the
model is shaping its response, which is what governs whether
those facts get incorporated.

The user never sees the raw nudge. The prompt prime is internal
context for the LM, and what the user reads is the Mouth Tool's
emission, which is grounded in the modality facts but framed as
natural language.

## How the Mouth Tool reads the bus

The Mouth Tool's [v2 active narrator](../mouth-tool/README.md) subscribes to
the same bus the Supervisor reads. Inside the framing engine, the
modality cues become *candidate facts* — things the response could
mention. The CandidateBuilder collects them, the ContextRanker
scores them by relevance × utility × politeness, and the Framing &
Style Engine renders the top-K into natural language at the
detail level the active zoom calls for.

The tight integration is what makes the multimodal answer happen
in a single turn. The same bus the Supervisor reads to prime its
reasoning is the bus the Mouth Tool reads to ground its
articulation. Vision, audio, supervisor draft, and final emission
all share the same modality-fact substrate.

## Latency budget

Each modality actor runs on its own coroutine context with bounded
parallelism (`Dispatchers.Default.limitedParallelism(n)`). The
extraction pipeline budget is **≤ 50 ms per modality**. The bus
itself is a lock-free queue; publication is sub-millisecond. The
Supervisor reads the latest snapshot per decode loop, which adds
no measurable latency. The Mouth Tool polls the bus every 5 ms,
synced to its framing tick.

End-to-end, the canonical example trace from Addendum B:

| T (ms) | Actor | Event |
| --- | --- | --- |
| +0 | User | *"Is the shirt blue and does she sound angry?"* |
| +8 | VisionActor | Cue: shirt blue (0.94) |
| +15 | AudioActor | Cue: tone angry (0.83) |
| +18 | Bus | Nudges published |
| +20 | Supervisor | Drafts answer skeleton |
| +25 | Mouth Tool | Polls bus, builds candidates |
| +32 | Mouth Tool | Ranks and frames |
| +35 | Output | *"Yes — the shirt is a rich cobalt blue, and her tone suggests she's rather angry."* |

Total < 40 ms after the last cue. The user perceives an immediate,
modality-aware answer.

## Mutual priming across senses

The Cross-Perspective Bus's most subtle benefit is that the senses
can prime each other. A vision actor running in a noisy environment
can subscribe to audio nudges describing the soundscape and
sharpen its frame analysis accordingly. An audio actor can
subscribe to vision nudges describing the visible speaker and
tighten its emotion classification using face cues that aren't on
its own input stream.

The architectural form is just the bus's pub/sub topology. Each
modality actor subscribes to the modalities it can usefully be
primed by, and the priming happens through the same nudge protocol
the Supervisor reads. There's no special "mutual priming" code
path; it's the bus doing its job.

This matters because it's what prevents the senses from being
isolated experts. A vision system that doesn't know there's audio
context, and an audio system that doesn't know there's a face on
camera, will each make worse decisions than the same systems
talking to each other through a bus that lets them notice what the
other one noticed.

## Privacy and security

Three things matter here, named explicitly in the SRAF design:

**Raw frames and PCM are erased after cue extraction.** The
modality actors hold raw input in in-memory ring buffers only long
enough to extract semantic cues. Once a `VisionCue` or `AudioCue`
is on the bus, the raw frame or audio chunk is gone. This bounds
the privacy exposure to the time of the extraction, not the
duration of the conversation.

**Nudges contain no PII — only derived semantic tags.** *"shirt
blue"* and *"angry tone"* are facts about state, not facts about
identity. Names, faces, voiceprints, and other identifying signals
should not appear on the bus. The Mouth Tool's last-mile PII scrub
is a defence-in-depth check, but the primary guarantee is that the
nudge protocol itself doesn't carry PII.

**The bus is intra-process; no external egress.** The lock-free
queue lives inside the agent's process boundary. Nudges don't
leave the process by any normal path. If a deployment needs
cross-process modality sharing, that's a separate decision with
its own threat model.

## Failure modes

**Cue conflict.** Vision says one thing, audio says something
incompatible, or the user later contradicts what the senses
reported. (*"Is the shirt blue?"* — *"Yes."* — *"It's actually
red."*) The conservative behaviour is to drop both modalities when
they conflict above confidence floor and let the Supervisor draft
respond from the user's correction. The better long-term answer is
weighted arbitration based on source reliability per modality. The
SRAF design names this as an open issue.

**Confidence calibration.** Heterogeneous models produce confidence
values that aren't directly comparable. A vision model's 0.85 may
be more reliable than an audio model's 0.85. Per-modality
threshold tuning helps; learned calibration is the long-term fix.

**Token budget impact.** The prompt-prime length grows with the
number of active nudges. In a busy multimodal scene, this can
crowd the conversational context. Dynamic pruning — keeping only
the top-K most relevant nudges by recency × confidence × topic
match — is the design's answer; the exact pruning policy is open.

**Hallucination between updates.** If the camera samples discrete
frames, the agent can describe a state that is no longer present.
Continuous scene tracking, with cues marked "stable" versus
"momentary", is one approach. The TTL field already provides a
soft form of this, but tighter scene-aware logic is open.

## Implementation status

**Designed, not built.** The lib codebase has no Vision Actor,
Audio Actor, or Cross-Perspective Bus. The actor framework that
would host them exists; the bus would sit on top of the
[shared-memory](../shared-memory/README.md) primitives.

The work order:

1. Build the bus as a lock-free pub/sub layer over the
   shared-memory primitives. Test with synthetic nudge producers.
2. Wire the Supervisor's prompt-prime fusion. Verify that
   high-confidence nudges appear in the prime in the right
   position.
3. Wire the Mouth Tool v2 to subscribe to the bus.
4. Build the first modality actor — probably Vision, because the
   ViT/CLIP integration is well-trodden. Verify end-to-end timing
   against the < 50 ms budget.
5. Add Audio. Verify mutual priming works (vision and audio
   subscribing to each other's nudges).

## Open questions

- **Cue conflict resolution** when senses or user disagree.
- **Confidence calibration** across heterogeneous modality models.
- **Token budget pruning policy** for prompt-prime growth.
- **Continuous scene** vs discrete frame sampling, and how to
  represent "scene state" versus "moment state" in nudge TTLs.
- **User privacy mode** switch to suppress audio emotion disclosure
  in deployments where that's sensitive.
- Whether modality actors should subscribe to *every* other
  modality by default, or be explicitly wired pairwise for
  mutual priming.

## Cross-references

- [supervisor](../supervisor/README.md) — primary consumer of the bus;
  fuses nudges into the prompt prime.
- [mouth-tool](../mouth-tool/README.md) — v2 active narrator subscribes to
  the bus; modality cues become candidate facts in framing.
- [reflection-memory](../reflection-memory/README.md) — modality cues are
  also recorded here as the durable substrate; the bus is the
  fast path, the substrate is the record.
- [mood](../mood/README.md) — emotional advisors emit on the same bus
  pattern; emotion is one of many modalities.
- [shared-memory](../shared-memory/README.md) — the lock-free pub/sub
  primitives the bus is built on.
- [zoom-levels](../zoom-levels/README.md) — Mouth Tool framing of modality
  cues respects the active zoom (LOW gets detailed, HIGH gets
  summarised).

## What the bus is in service of

The architecture's commitment that the agent's responses are
informed by *everything it has perceived*, not just the modality
the user happened to ask about. Without the bus, modality
reasoning becomes a sequence of explicit calls — *"hold on, let me
look at the image"* — and the latency stops being conversational.
With it, the agent is always perceiving, always informed, and the
response can incorporate what every sense noticed without any
single sense having to be polled.

That parallelism is what makes the multimodal answer feel like an
answer rather than a query. The senses talk among themselves;
the agent listens; the response is grounded in what they all
heard.

---

[← Features index](../README.md)
