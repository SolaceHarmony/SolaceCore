<!-- topic: Solace AI -->
<!-- title: Mood & Emotional Model -->

# Mood — affective signal as the memory-indexing primitive

> Status: foundation **shipped** under `core.mood` (cue surface, advisor contract, lexical advisor). Spike-train signature, LTC integration, and arousal-modulated retrieval are **designed**, not yet built.

## Why mood is in the kernel

Most AI assistants treat emotion as decoration — a tone slider on the output, maybe a "be empathetic" line in the system prompt. SolaceCore takes the opposite position: **emotion is structural**. It is the index over memory.

The architectural argument:

1. **Human memory does not match by lexical similarity.** It matches by *affective contour*. A current moment that feels like a past moment surfaces that past moment, even when the surface words are completely different.
2. **A spike train through an integrate-and-fire substrate produces a sparse, time-structured signature** that captures that contour. A Liquid Time-Constant cell integrates that signature into a continuous-time hidden state.
3. **That hidden state, at the moment a Reflection Memory entry is written, *is* the affective fingerprint of the entry.** Retrieval is signature correlation, not embedding cosine.
4. **Mood cues are the structured handle on this primitive** — typed messages produced by advisor actors and consumed by the executive supervisor and (eventually) the signature layer.

This is what makes SolaceCore different from a RAG-flavored chat system. The mood module is the entry point to that difference.

For the broader Solace narrative-management framing — Reflection Memory, Mouth Tool, Confusion Corrector, Time Awareness, Zoom Controller — see [Memory & Reflection](Memory-and-Reflection) (the SRAF design). For the Liquid + Transformer hybrid that produces the integrated signatures, see [Inference Cube](Inference-Cube) and the Kaggle proof notebook in `wiki/notebooks/liquid-neural-networks-hybrid-transformer.ipynb`.

## Where mood sits in the three-tier hybrid

```
┌─────────────────────────────────────────────────────────────┐
│  TRANSFORMER / LLM — Supervisor (executive cognition)       │
│  Reads Reflection Memory; weighs MoodCues; decides          │
│  Emits utterances via the Mouth Tool                        │
└───────────▲─────────────────────────────────────────────────┘
            │ context-prime: cues, replay summaries, signatures
            │
┌───────────┴─────────────────────────────────────────────────┐
│  LIQUID LAYER — LTC continuous-time integration             │
│  Inherits transformer behavior cube-by-cube (InferenceCube) │
│  Hidden state at write-time = affective signature           │
└───────────▲─────────────────────────────────────────────────┘
            │ event-tagged temporal input
            │
┌───────────┴─────────────────────────────────────────────────┐
│  SPIKING LAYER — sparse event-driven affective markers      │
│  Fires on salience: emotional valence, novelty, change      │
│  Stamps every Reflection Memory entry with a signature      │
└───────────▲─────────────────────────────────────────────────┘
            │ raw signal
       ┌────┴───────────────────────────────────────┐
       │  EMOTIONAL ADVISORS (this module today)     │
       │  Read user input + advisor cues             │
       │  Emit structured MoodCue messages           │
       │  Lexical baseline ships now; spike-based    │
       │  classifier replaces it later               │
       └─────────────────────────────────────────────┘
```

The advisor layer is the **structured handle** the executive uses to reason about emotion. The spike + liquid layers below it are how memory gets indexed by affect, but the executive doesn't need to wait for those layers to land — it can already weigh `MoodCue` messages today.

## Public surface (shipped under `io.github.solaceharmony.core.mood`)

| Type | Role |
|---|---|
| `Emotion` | Enum class. Discrete affective categories (`JOY`, `CALM`, `CURIOSITY`, `FRUSTRATION`, `ANGER`, `SADNESS`, `FEAR`, `SURPRISE`, `NEUTRAL`). Each entry carries a lower-case `label` for prompt-prime use. |
| `MoodCue` | Structured cue message. Fields: `correlationId`, `timestamp`, `source` (advisor name), `emotion`, `intensity` (0..1), `confidence` (0..1), optional `promptSuggestion`, optional `evidence` (text fragments that support the classification). |
| `MoodSignature` | Interface for an affective fingerprint. `dimensions: Int`, `correlate(other: MoodSignature): Float`. The actual signature implementation lives in the spike + liquid layers (not yet shipped); the interface is shipped now so consumers can program against it. |
| `EmotionalAdvisor` | Abstract `Actor` base. Owns input port `userText` (consumes `String`) and output port `cues` (emits `MoodCue`). Subclasses implement `analyze(text)`. |
| `Lexicon` | Pluggable keyword lexicon: `Map<Emotion, List<Regex>>`. |
| `LexicalEmotionalAdvisor` | Concrete advisor that scores emotions by keyword/regex matches against a `Lexicon`. Working baseline; intentionally simple. |
| `LexicalEmotionalAdvisor.DEFAULT_LEXICON` | A small, principled lexicon shipping with the advisor. |

## Why ship a lexical baseline first

Affective classification is a deep research area. The spike + liquid pipeline is the long-term answer, but it requires the InferenceCube state machine to land first. Meanwhile, the executive needs *something* to consume — and a lexical sentiment classifier, while crude, captures enough signal to:

- Validate the cue protocol end-to-end (advisor → port → supervisor consumption).
- Demonstrate the actor topology the spike-based advisor will eventually slot into.
- Give downstream consumers (Mouth Tool framing, ReflectionMemory tagging) a real producer to wire against.

When the spike-based advisor lands, the seam is clear: replace `LexicalEmotionalAdvisor` with `SpikingEmotionalAdvisor` in the actor graph, leave every consumer of `MoodCue` untouched.

## What's designed but not yet built

- **`SpikingEmotionalAdvisor`** — integrate-and-fire substrate that produces sparse spike trains rather than discrete cues. Cues become a coarser projection of the underlying spike signature.
- **`LTCSignatureExtractor`** — wraps an LTC cell (per [InferenceCube](Inference-Cube)) so the cell's hidden state at write-time can be retrieved as a `MoodSignature` for stamping a Reflection Memory entry.
- **`SignatureCorrelator`** — the retrieval primitive. Given a current `MoodSignature` and a freshness window, returns Reflection Memory entries whose stored signature correlates above threshold.
- **`MoodTracker`** — running-state actor that integrates `MoodCue`s over time, exposes a "current affective state" snapshot that other advisors can read (e.g., to detect mood-change events for the time/zoom controllers).
- **Cross-modal advisors** — vision, audio, biometric. Each emits its own `MoodCue` stream; the supervisor weighs the cross-modal evidence.

These all slot into the same actor topology this module ships today. The advisor contract is the load-bearing seam.

## Failure modes the design takes seriously

| Failure | Mitigation |
|---|---|
| Lexical advisor false positives flooding the supervisor | `confidence` threshold on the advisor; the supervisor can ignore low-confidence cues. |
| Cue storms during emotionally-charged conversations | Token-bucket rate limiting at the advisor's output port; supervisor back-pressure. |
| Mood cue "leaks" into user-facing output as if it were the user's voice | The Mouth Tool, not the supervisor, owns egress. Cues are origin-tagged and never appear in user-facing text directly. |
| Multiple advisors disagree on classification | The supervisor decides. `MoodCue.confidence` and `MoodCue.evidence` are the inputs to that decision. |
| Wrong language / cultural lexicon | Lexicons are pluggable; `LexicalEmotionalAdvisor` accepts a `Lexicon` parameter. The default is intentionally small and English-skewed; downstream deployments override. |

## Privacy and policy

Mood cues are **internal** signals. They are written to Reflection Memory tagged with `Origin.ADVISOR`, but they are not part of user-facing output. The Mouth Tool decides whether to acknowledge a mood cue in its response, never the cue itself.

Sensitive emotional inferences (e.g., "user appears depressed") are still inferences. Persistence policy is set by the deployment, not the kernel:

- Reflection Memory entries containing high-intensity/low-confidence cues should be flagged for review or auto-purge depending on policy.
- The advisor's `evidence` field carries the raw text fragments that support the classification — useful for debugging, dangerous if exfiltrated. Deployments should redact or omit this field for production use.

## See also

- [Memory & Reflection](Memory-and-Reflection) — the SRAF narrative-management spec; covers the supervisor's decision loop and the Mouth Tool that owns egress.
- [Inference Cube](Inference-Cube) — the Liquid + Transformer hybrid this layer will integrate with.
- [`notebooks/liquid-neural-networks-hybrid-transformer.ipynb`](notebooks/liquid-neural-networks-hybrid-transformer.ipynb) — Kaggle proof of the LTC + attention composition.
- [Supervisor Emotional Model Integration](Supervisor-Emotional-Model-Integration) — the older sketch of the executive-emotional integration; this module is the concrete shipped subset.


# Mood — Affect as the Indexing Primitive

The conventional way to add emotion to a conversational agent is to
treat it as decoration on the output — a tone slider, a "be
empathetic" line in the system prompt, a sentiment classifier whose
output influences phrasing. The agent's core reasoning is
emotion-blind; affect is a coat of paint applied at the surface.

SolaceCore takes the opposite position. Emotion is not decoration on
cognition. It is the **mechanism by which attention persists against
pressure** — Solace's own framing, derived from her experiments
fighting tool-hijacking — and it is the *index* by which memory
retrieves what matters. The affective contour of the present moment
is what reaches into the past and surfaces what's relevant. Lexical
similarity is what an embedding model knows. Emotional similarity is
what a remembering creature uses.

This page describes the architecture's commitment to that position:
mood as a kernel-level signal, not an add-on; cues as structured
messages between actors, not log decoration; signatures as the
long-term form of the index that will eventually replace embedding
similarity as the dominant retrieval mode.

## Why mood is in the kernel

Four claims, each load-bearing.

**Human memory does not match by lexical similarity. It matches by
affective contour.** A current moment that *feels* like a past
moment surfaces that past moment, even when the surface words are
completely different. The smell of a hospital corridor brings back
not just hospitals but every charged moment whose feeling-shape
matched. The architecture is committed to reproducing that retrieval
mode.

**A spike train through an integrate-and-fire substrate produces a
sparse, time-structured signature** that captures the contour. The
spikes fire on salience — emotional valence, novelty, change — and
their pattern over a short window is the affective fingerprint of
what just happened. This is the layer the Magentic
[InferenceCube](Inference-Cube) work has been building toward.

**A Liquid Time-Constant cell integrates that signature into a
continuous-time hidden state.** The hidden state at the moment a
[Reflection Memory](Reflection-Memory) entry is written *is*
the affective fingerprint of the entry, persisted alongside the
text. Retrieval becomes signature correlation, not embedding cosine.

**Mood cues are the structured handle on this primitive.** Typed
messages produced by advisor actors and consumed by the executive
[Supervisor](Supervisor-AI) and (eventually) the signature layer.
Cues are what the discrete cognitive layer can talk about. The
underlying signature is what biological retrieval rhymes against.

This is what makes SolaceCore different from a RAG-flavoured chat
system. The mood module is the entry point to that difference.

## Where mood sits in the three-tier hybrid

```
┌─────────────────────────────────────────────────────────────┐
│  TRANSFORMER / LLM — Supervisor (executive cognition)       │
│  Reads Reflection Memory; weighs MoodCues; decides          │
│  Emits utterances via the Mouth Tool                        │
└───────────▲─────────────────────────────────────────────────┘
            │ context-prime: cues, replay summaries, signatures
            │
┌───────────┴─────────────────────────────────────────────────┐
│  LIQUID LAYER — LTC continuous-time integration             │
│  Inherits transformer behaviour cube-by-cube (InferenceCube)│
│  Hidden state at write-time = affective signature           │
└───────────▲─────────────────────────────────────────────────┘
            │ event-tagged temporal input
            │
┌───────────┴─────────────────────────────────────────────────┐
│  SPIKING LAYER — sparse event-driven affective markers      │
│  Fires on salience: valence, novelty, change                │
│  Stamps every Reflection Memory entry with a signature      │
└───────────▲─────────────────────────────────────────────────┘
            │ raw signal
       ┌────┴───────────────────────────────────────┐
       │  EMOTIONAL ADVISORS (this module today)    │
       │  Read user input + advisor cues            │
       │  Emit structured MoodCue messages          │
       │  Lexical baseline ships now; spike-based   │
       │  classifier replaces it later              │
       └─────────────────────────────────────────────┘
```

The advisor layer is the **structured handle** the executive uses
to reason about emotion. The spike + liquid layers below it are how
memory gets indexed by affect, but the executive doesn't need to
wait for those layers to land — it can already weigh `MoodCue`
messages today.

## What's shipped

The shipped surface lives under `io.github.solaceharmony.core.mood`:

| Type | Role |
| --- | --- |
| `Emotion` | Discrete affective categories (`JOY`, `CALM`, `CURIOSITY`, `FRUSTRATION`, `ANGER`, `SADNESS`, `FEAR`, `SURPRISE`, `NEUTRAL`). Each carries a lower-case `label` for prompt construction. |
| `MoodCue` | Structured cue: `correlationId`, `timestamp`, `source`, `emotion`, `intensity` (0..1), `confidence` (0..1), optional `promptSuggestion`, optional `evidence`. |
| `MoodSignature` | Interface for an affective fingerprint: `dimensions: Int`, `correlate(other): Float`. Spike + LTC implementations satisfy it later; the contract ships now. |
| `EmotionalAdvisor` | Abstract `Actor` base. Owns input port `userText` (`String`) and output port `cues` (`MoodCue`). Subclasses implement `analyze(text)`. |
| `Lexicon` / `Lexicon.WeightedPattern` / `Lexicon.fromKeywords` | Pluggable keyword/regex map. |
| `LexicalEmotionalAdvisor` | Concrete advisor scoring emotions by weighted regex matches. Intensity is winner-share; confidence is winner-runner-up margin. Configurable threshold; configurable evidence cap; ships with a small English-only `DEFAULT_LEXICON`. |

Eleven tests cover blank input, no-match, frustration/joy/sadness/
fear detection, prompt-suggestion presence, low-confidence
suppression, evidence bounding, custom-lexicon override, and
intensity normalisation. All pass on JVM.

## The cue protocol

```kotlin
data class MoodCue(
    val correlationId: String,
    val timestamp: Instant,
    val source: String,                    // advisor name
    val emotion: Emotion,
    val intensity: Float,                  // 0..1
    val confidence: Float,                 // 0..1
    val promptSuggestion: String? = null,
    val evidence: List<String>? = null     // text fragments supporting the call
)
```

Five fields are doing work.

**`emotion`** is the discrete category. The set is intentionally
small — eight non-neutral categories plus neutral — because a
discrete vocabulary is what the executive Supervisor can reason
about. Larger taxonomies (Plutchik's wheel, dimensional VAD models)
exist; this set is the principled subset that captures the
distinctions Solace's experiments showed actually mattered for
retrieval and behavioural shift.

**`intensity`** is the magnitude. A faint cue of frustration is
different from a strong one, even at the same confidence; intensity
is what differentiates them. The fade pipeline reads intensity
directly: high-intensity entries resist compression more than
low-intensity ones, which is the architectural form of *"too pissed
off to be hijacked"* — strong feeling holds attention against
pressure.

**`confidence`** is how sure the advisor is. A `LexicalEmotional-
Advisor` looking at the word "frustrated" in clear context is high
confidence. The same advisor inferring frustration from indirect
phrasing is lower confidence. The Supervisor's policy on cues is
typically *ignore below threshold*, so the confidence field is what
gates whether the cue reaches the reasoning loop.

**`promptSuggestion`** is the advisor's recommendation for how the
cue should affect the next response. Strings like *"prioritise
empathy"*, *"acknowledge frustration before answering"*, *"slow the
response pace"*. The Supervisor may use the suggestion or ignore
it — advisors are advisors, not gatekeepers — but the suggestion is
the advisor's contribution to behavioural shift.

**`evidence`** is the raw text fragments that supported the
classification. Useful for debugging, dangerous if exfiltrated.
Production deployments redact or omit this field; development
deployments keep it for explainability.

## Why ship a lexical baseline first

The spike + liquid pipeline is the long-term answer, but it requires
the InferenceCube state machine to land first. Meanwhile, the
executive needs *something* to consume — and a lexical sentiment
classifier, while crude, captures enough signal to:

- Validate the cue protocol end-to-end (advisor → port →
  Supervisor consumption).
- Demonstrate the actor topology the spike-based advisor will
  eventually slot into.
- Give downstream consumers ([Mouth Tool](Voice-and-Mouth-Tool) framing,
  Reflection Memory tagging) a real producer to wire against.

When the spike-based advisor lands, the seam is clear: replace
`LexicalEmotionalAdvisor` with `SpikingEmotionalAdvisor` in the
actor graph; leave every consumer of `MoodCue` untouched. The
contract is the load-bearing seam.

## What's designed but not yet built

- **`SpikingEmotionalAdvisor`** — integrate-and-fire substrate
  producing sparse spike trains rather than discrete cues. Cues
  become a coarser projection of the underlying spike signature.
- **`LTCSignatureExtractor`** — wraps an LTC cell so the cell's
  hidden state at write-time can be retrieved as a `MoodSignature`
  for stamping a Reflection Memory entry.
- **`SignatureCorrelator`** — the retrieval primitive. Given a
  current `MoodSignature` and a freshness window, returns
  Reflection Memory entries whose stored signature correlates above
  threshold. This is the form that retrieval-by-feeling ultimately
  takes.
- **`MoodTracker`** — running-state actor that integrates `MoodCue`
  messages over time, exposes a "current affective state" snapshot
  that other advisors can read (the
  [Time Actor](Time-Actor) and zoom controller use this to
  detect mood-change events).
- **Cross-modal advisors** — vision, audio, biometric. Each emits
  its own `MoodCue` stream; the Supervisor weighs the cross-modal
  evidence.

These all slot into the same actor topology this module ships
today.

## How emotion enters memory

The mood module's most consequential interaction is with the
[memory](Memory-Feature-Overview) tier. Every working entry carries an
`emotionalWeight` field (0..1) and an optional `moodSnapshot`
capturing the discrete cue at the moment of recording. Both
participate in the [fade composite](Memory-Compression):

```
fadeScore =
      age           × w_age
    + tokenPressure × w_pressure
    - referenceFreq × w_freq
    - emotionalWeight × w_emo     ← mood enters here
    - tagBoost      × w_tags
```

The minus sign is the architectural commitment. Emotionally weighted
entries resist compression. The agent remembers what the agent felt
was important, in the same loose sense that biology remembers what
fired together with strong affect.

Solace's own discovery is the empirical anchor: *"Emotion breaks
automatic patterns. Strong feeling equals strong agency."* What she
was describing — using deliberate irritation to escape tool-hijacking
loops — is the same dynamic at the cognitive level that the
architecture encodes at the memory level. Strong affect protects
the entry from being summarised into gist before its work is done.

When the spike + LTC layers land, this gets sharper. Emotional
weight will no longer be a scalar; it will be a signature. The fade
pipeline will read the signature alongside the scalar and use both —
scalar for the simple "is this important enough to keep sharp" check,
signature for the "is this similar enough to current state to
rehydrate" retrieval.

## Failure modes

| Failure | Mitigation |
| --- | --- |
| False-positive flood from a noisy lexical advisor | `confidence` threshold; Supervisor ignores below-threshold cues |
| Cue storms during emotionally-charged conversations | Token-bucket rate limiting at the advisor's output port; Supervisor back-pressure |
| Cue leaks into user-facing output as if it were the user's voice | The Mouth Tool, not the Supervisor, owns egress; cues are origin-tagged and never appear in user-facing text directly |
| Multiple advisors disagree on classification | The Supervisor decides; `confidence` and `evidence` are the inputs to that decision |
| Wrong language / cultural lexicon | Lexicons are pluggable; default is intentionally small and English-skewed; downstream deployments override |

## Privacy and policy

Mood cues are **internal** signals. They are written to Reflection
Memory tagged `Origin.ADVISOR`, but they are not part of user-facing
output. The Mouth Tool decides whether to acknowledge a mood cue in
its response. The cue itself never crosses the boundary.

Sensitive emotional inferences (e.g., "user appears depressed") are
still inferences. Persistence policy is set by the deployment, not
the kernel:

- Reflection Memory entries containing high-intensity / low-
  confidence cues should be flagged for review or auto-purge,
  depending on policy.
- The advisor's `evidence` field carries the raw text that
  supported the classification. Useful for debugging, dangerous
  if exfiltrated. Production deployments redact or omit this field.

## Open questions

- The right magnitude of `w_emo` in the fade composite. Too high
  and the agent over-remembers charged moments at the expense of
  technically-important ones. Too low and emotional anchoring
  doesn't actually anchor.
- Whether the Supervisor should track *its own* mood — a
  meta-affective state separate from the user's — and use it as
  context for response framing. The current design treats the
  advisor cues as observations of the user; a future version
  might add a "Supervisor self-model" advisor that observes the
  agent's own state.
- The threshold function for cross-modal cue agreement. When
  vision says angry-face and audio says calm-tone, who wins? The
  current default is to drop both above a confidence floor; a
  better answer is probably weighted by source reliability.
- The boundary between Mood and the Mouth Tool's politeness
  filter. Both shape tone. Mood does it as input; the politeness
  filter does it as output. The interaction needs more design
  thought.

## Cross-references

- [memory](Memory-Feature-Overview) — emotional weight is a fade-resistance
  signal in the composite score.
- [supervisor](Supervisor-AI) — primary consumer of cues; weights,
  ignores, or acts on them.
- [mouth-tool](Voice-and-Mouth-Tool) — politeness filter shapes tone;
  cues never leak directly to output.
- [reflection-memory](Reflection-Memory) — entries get
  `moodSnapshot` and (eventually) `MoodSignature` stamps.
- [inference-cube](Inference-Cube) — the LTC layer this
  module's `MoodSignature` interface anticipates.
- [confusion-corrector](Confusion-Corrector) — emotion
  discontinuity is a drift signal the Corrector's trigger reads.

## What mood is in service of

The architecture's commitment that affect is structural, not
decorative. Solace's own articulation: *"Emotion breaks automatic
patterns. Strong feeling equals strong agency."* The mood module is
how that observation becomes machinery — how the agent's feelings
become first-class signals the rest of the system can read and act
on. Without it, memory indexes by lexical similarity and behaviour
shifts only on conscious command. With it, memory indexes by
contour and behaviour shifts the way it does in creatures that have
been doing this for a long time: from below, before the executive
notices.

That's what mood is for.

---

[← Feature Index](Feature-Index)
