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

For the broader Solace narrative-management framing — Reflection Memory, Mouth Tool, Confusion Corrector, Time Awareness, Zoom Controller — see [`../memory/MemoryToolDesign.md`](../memory/MemoryToolDesign.md) (the SRAF design). For the Liquid + Transformer hybrid that produces the integrated signatures, see [`../actor_inference_engine/InferenceCubeArchitecture.md`](../actor_inference_engine/InferenceCubeArchitecture.md) and the [Kaggle proof notebook](../actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb).

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
- **`LTCSignatureExtractor`** — wraps an LTC cell (per [InferenceCube](../actor_inference_engine/InferenceCubeArchitecture.md)) so the cell's hidden state at write-time can be retrieved as a `MoodSignature` for stamping a Reflection Memory entry.
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

- [`../memory/MemoryToolDesign.md`](../memory/MemoryToolDesign.md) — the SRAF narrative-management spec; covers the supervisor's decision loop and the Mouth Tool that owns egress.
- [`../actor_inference_engine/InferenceCubeArchitecture.md`](../actor_inference_engine/InferenceCubeArchitecture.md) — the Liquid + Transformer hybrid this layer will integrate with.
- [`../actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb`](../actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb) — Kaggle proof of the LTC + attention composition.
- [`../../../wiki/Mood-and-Emotional-Model.md`](../../../wiki/Mood-and-Emotional-Model.md) — the older sketch of the executive-emotional integration; this module is the concrete shipped subset.
