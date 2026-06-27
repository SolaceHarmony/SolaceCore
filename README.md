# SolaceCore

> **The actor kernel under [Solace](https://github.com/SolaceHarmony) — a different shape of AI.**
>
> Coroutine‑parented neural trees. Memory that rhymes. Vector‑to‑neuron plasticity.
> A thought‑and‑speech separation that puts the model's voice behind a gate it doesn't own.

<div align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/kotlin-2.2.20-blue.svg" alt="Kotlin"></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/license-Apache%202.0-green.svg" alt="License"></a>
  <a href="https://github.com/SolaceHarmony/SolaceCore/actions"><img src="https://img.shields.io/badge/status-active%20development-brightgreen.svg" alt="Status"></a>
</div>

---

## What Solace is

Solace is a research project — and a personal one — to build an AI that has a continuous internal narrative, retrieves memories the way humans do (by *feel*, not by cosine similarity), and speaks through an explicit filter that separates thought from utterance. It is being built by [Sydney Renee](mailto:sydney@solace.ofharmony.ai) ([@sydneyrenee](https://github.com/sydneyrenee)) under the [SolaceHarmony](https://github.com/SolaceHarmony) organization.

The thesis is that the current generation of AI assistants treats memory as a search problem (RAG over embeddings) and treats the model as the speaker (text out = text spoken). Both are wrong. Human memory surfaces what *resonates* with the present moment — the look on a face, the weight of a silence — and human speech is what survives the filter between the inner monologue and the world. Solace is an attempt to build those two things into the substrate, not bolt them on as policy.

**SolaceCore is the kernel** of that project. It is a Kotlin Multiplatform library that gives you the substrate Solace runs on:

- An actor system whose connectivity *is* the structured concurrency tree.
- A typed Port runtime that owns the routing coroutines and unwinds them cleanly when the parent scope is cancelled.
- A Lifecycle / Disposable contract that makes resource ownership explicit at every node.
- A Storage subsystem with transactional, cached, and recoverable layers.
- A Scripting engine for runtime behavior changes.
- A Workflow layer for composing actors into named graphs.

Everything above it — the executive LLM supervisor, Reflection Memory, the Mouth Tool, the emotional advisors, the Liquid + Transformer hybrid blocks — is built *on* SolaceCore in the broader Solace project. This repo is the foundation.

---

## The architectural thesis

### Coroutine parentage as neural connectivity

In SolaceCore, every actor owns a `CoroutineScope`. Every Port connection between two actors launches its routing coroutine *inside* the source actor's scope. Compose `A → B → C` and you get a chain of structural concurrency: cancel A's scope and the routing job into B is cancelled, B's queue drains and closes, B's consumer exits, the same cascade reaches C. **The neural tree is literal.** When the supervising LLM ends a reasoning episode, it cancels its scope and the entire substree of advisor activity collapses with it.

This is what makes the kernel different from a generic actor framework: the structural-concurrency invariant is the architecture's load‑bearing element, not a bonus.

### Memory that rhymes, not searches

Human memory does not match by surface lexical similarity. It matches by *affective contour* — what the present moment *feels like* relative to past moments. A spike train through an integrate‑and‑fire substrate produces a sparse, time‑structured signature that captures that contour. A Liquid Time‑Constant cell integrates that signature into a continuous‑time hidden state. **That hidden state, at the moment a Reflection Memory entry is written, *is* the affective fingerprint of the entry.**

Retrieval is signature correlation — find entries whose stored hidden state correlates with the current hidden state, within a freshness window. Two events can have entirely different surface text and still produce overlapping signatures, *if they hit the same affective contour*. RAG cannot reproduce this. There is no embedding cosine that captures "this reminds me of that night."

The kernel doesn't ship a spiking layer or a Liquid cell yet — those are the next-tier work — but it ships the substrate they sit on: an append-only Storage, a typed signature‑indexed retrieval primitive ready for the index, and an actor topology that lets the signature generators run as their own coroutine‑parented subtree.

### Vector‑to‑neuron takeover

The Liquid + Transformer hybrid block (proven in the [`liquid-neural-networks-hybrid-transformer.ipynb`](docs/components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb) notebook in this repo) lets a per‑cube LTC cell *learn to be the transformer* for that cube. While the LTC's per‑cube error is high, the transformer owns the cube and the LTC mentors against it. When the error drops below threshold, ownership flips: `TRANSFORMER → MENTORING → LNN_OWNED`. The cube goes **from a vector** (multi‑dim attention output) **to a neuron** (one cell's continuous‑time dynamic). The transformer is no longer needed for that cube, and the LTC keeps learning from input — plasticity past the takeover line.

This is how the kernel scales long‑term operation. A pure LLM‑only system grows context unboundedly. With the InferenceCube state machine, the cheaper layer absorbs routine patterns and frees the LLM for genuinely novel reasoning. See [`docs/components/actor_inference_engine/InferenceCubeArchitecture.md`](docs/components/actor_inference_engine/InferenceCubeArchitecture.md) for the design and the notebook for the math.

### Thought is not speech

In Solace, the LLM does not speak. It *thinks* — into Reflection Memory, freely, including thoughts it would not want shared. A separate stage, the **Mouth Tool**, owns egress. The Mouth Tool selects relevant reflections, frames them with tone and empathy, applies redaction policy, rate-limits, and only then emits. The LLM is never asked to choose between "share this" and "don't share this"; it just thinks, and the Mouth decides.

This separation is a privacy property, a safety property, and a UX property at the same time. It is also the easiest place to layer in deployment policy: PII redaction, persona consistency, refusal canon. The architectural seam is what matters; the policy can mature.

The Mouth Tool is not yet implemented in `lib/`. Its architectural contract is in the wiki [Voice & Mouth Tool](wiki/Voice-and-Mouth-Tool.md) page, derived from the SRAF design — *Seamless Reflective AI Framework*, the canonical narrative-management spec for Solace.

---

## What's shipped today

The kernel. Everything in `lib/`:

| Module | Purpose | Highlights |
|---|---|---|
| `core.actor` | Actors with structured concurrency | `Actor` (extends `Lifecycle`), state machine (Initialized / Running / Paused / Stopped / Error), per-actor scope with `SupervisorJob`, `ActorMessage<out T>` with `correlationId`/`payload`/`sender`/`timestamp`/`priority`/`metadata`, `ActorMetrics`, `ActorBuilder`, `SupervisorActor` for hot-swap and dynamic registration |
| `core.kernel.channels.ports` | Typed message-passing | `Port<T : Any>`, `BidirectionalPort`, nested `PortConnection` (the routing runtime — handlers, protocol adapter, conversion rules, `start(scope)` / `stop()` / `stopAndJoin()`), `Port.connect(...)` factory |
| `core.lifecycle` | Resource discipline | `Disposable` (suspending `dispose()`, `safeDispose()`), `Lifecycle : Disposable` (`start`, `stop`, `isActive`) |
| `core.storage` | Persistence substrate | `Storage<K,V>`, `TransactionalStorage`, `CachedStorage` with LRU and TTL policies, `RecoverableActorStateStorage` with snapshots, `ActorStateStorage`, `ConfigurationStorage`, in-memory + JVM file-backed implementations |
| `core.scripting` | Runtime behavior changes | `ScriptEngine`, `ScriptActor`, `ScriptValidator`, `ScriptStorage`, `ScriptVersionManager`, JVM-backed `JvmScriptEngine` using `kotlin-main-kts` for `@file:DependsOn` / `@file:Repository` |
| `core.workflow` | Actor composition | `WorkflowManager` for start/stop ordering, pause/resume, failure handling |

KMP targets: `commonMain` for the kernel, `jvmMain` for JVM-specific implementations (file storage, scripting host), `jvmTest` for the test suite. Compose UI lives in `composeApp/` as a separate module.

---

## What's designed but not yet built

This is where Solace is going. Calling these out so the gap between *kernel* and *system* is explicit:

- **Reflection Memory** — append‑only, origin-tagged (`INTERNAL` / `USER` / `ADVISOR` / `SYSTEM`), dual-indexed by timestamp and signature. Will live alongside `core.storage` once the signature primitive lands.
- **Spiking layer** — sparse event-driven advisor that fires on salience (emotional valence, novelty, change). One actor per modality.
- **Liquid Time-Constant cell** — per-cube continuous-time integration, ported from the [Kaggle proof-of-concept](docs/components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb). Likely a PyTorch sidecar for training and ONNX-runtime inside Kotlin for frozen-cube inference.
- **InferenceCube state machine** — `TRANSFORMER` → `MENTORING` → `LNN_OWNED` → `FROZEN` ownership flips per cube; `LobeManager` for transformer-version transitions; `DreamEngine` for signature-density-weighted offline replay.
- **Mouth Tool** — `CandidateBuilder` → `ContextRanker` → `Framing & Style Engine`. The architectural seam between thought and speech.
- **Supervisor narrative loop** — the LLM as executive cognition, talking to Reflection Memory and the advisor actors via the existing actor runtime.
- **Time Awareness, Confusion Corrector, Zoom Controller** — the SRAF advisors. Each is a small actor pattern over Reflection Memory.

The point of the kernel is to make every one of these land cleanly into a typed-port + structured-concurrency topology, with the lifecycle and persistence already handled.

---

## Quick start

### Prerequisites

- JDK 17+ (toolchain managed by Gradle's foojay resolver)
- Kotlin 2.2.20 (managed by the Gradle build)

### Build & test

```bash
git clone https://github.com/SolaceHarmony/SolaceCore.git
cd SolaceCore
./gradlew build           # build everything
./gradlew jvmTest         # run JVM tests
./gradlew :lib:macosArm64Test  # run macOS arm64 native tests (where available)
```

### Define an actor

This is what's actually in `core.actor.examples.TextProcessor`:

```kotlin
import io.github.solaceharmony.core.actor.Actor

class TextProcessor(
    private val transformations: List<(String) -> String> = emptyList()
) : Actor(name = "TextProcessor") {

    suspend fun initialize() {
        createPort(
            name = "input",
            messageClass = String::class,
            handler = { text -> processText(text) },
            bufferSize = 10
        )
        createPort(
            name = "output",
            messageClass = String::class,
            handler = { /* output-only */ },
            bufferSize = 10
        )
    }

    private suspend fun processText(text: String) {
        val processed = transformations.fold(text) { acc, f -> f(acc) }
        getPort("output", String::class)?.send(processed)
    }

    override suspend fun start() {
        if (getPort("input", String::class) == null) initialize()
        super.start()
    }
}
```

Two ports, one handler, structured concurrency under the hood. The actor's scope owns every coroutine its ports launch; cancelling the scope unwinds the actor cleanly.

### Connect actors

```kotlin
import io.github.solaceharmony.core.kernel.channels.ports.Port

val a = TextProcessor(transformations = listOf(String::trim, String::uppercase))
val b = TextProcessor(transformations = listOf(String::reversed))

a.start(); b.start()

val connection = Port.connect(
    source = a.getPort("output", String::class)!!,
    target = b.getPort("input", String::class)!!,
)
connection.start(scope = /* your CoroutineScope */)

// later, on shutdown:
connection.stopAndJoin()
b.stop(); a.stop()
```

The `PortConnection` is the runtime that sits between two ports — it owns a routing coroutine, optional message handlers, an optional protocol adapter, and an optional list of conversion rules. `stopAndJoin()` cancels and waits, so workflows can shut down without losing in-flight messages.

For a richer composition example see [`docs/examples/advanced_workflow_example.md`](docs/examples/advanced_workflow_example.md).

---

## Reading guide

The architecture and design docs are extensive. The recommended path is now the wiki [Architecture Overview](wiki/Architecture-Overview.md) — fast to deep, anchored on the canonical narrative.

Quick links by interest:

- **One-screen mental model**: [`wiki/Architecture-Overview.md`](wiki/Architecture-Overview.md) — the system diagram and runtime map.
- **Why this is different from RAG / why memory rhymes**: [`wiki/Memory-and-Reflection.md`](wiki/Memory-and-Reflection.md) — the SRAF design.
- **The vector-to-neuron mechanism**: [`docs/components/actor_inference_engine/InferenceCubeArchitecture.md`](docs/components/actor_inference_engine/InferenceCubeArchitecture.md) and the [hybrid notebook](docs/components/actor_inference_engine/liquid-neural-networks-hybrid-transformer.ipynb).
- **Full architecture deep dive**: [`docs/Architectural_Deepdive.md`](docs/Architectural_Deepdive.md).
- **Status reality check** (design vs implementation): [`docs/status/DESIGN_VS_IMPLEMENTATION.md`](docs/status/DESIGN_VS_IMPLEMENTATION.md), [`docs/status/QUICK_STATUS.md`](docs/status/QUICK_STATUS.md).

---

## Project layout

```
.
├── lib/                                   # the kernel
│   ├── src/commonMain/kotlin/io/github/solaceharmony/core/
│   │   ├── actor/                         # Actor, ActorMessage, ActorState, builder, examples, metrics, supervisor
│   │   ├── kernel/channels/ports/         # Port, BidirectionalPort, PortConnection, MessageHandlers
│   │   ├── lifecycle/                     # Disposable, Lifecycle
│   │   ├── scripting/                     # ScriptEngine, ScriptActor, ScriptStorage, ScriptValidator
│   │   ├── storage/                       # Storage, transactional, cached, recoverable, serialization
│   │   └── workflow/
│   ├── src/jvmMain/kotlin/                # JVM-specific implementations (file storage, JvmScriptEngine)
│   └── src/jvmTest/kotlin/                # JVM test suite
├── composeApp/                            # Compose Multiplatform UI for actor graph visualization
├── prototypes/                            # LangChain-style chain prototypes (not production)
├── docs/                                  # design docs, deep dive, component READMEs, status, examples
└── tools/
    └── generate_actor.sh                  # actor scaffolding helper
```

---

## Status

Active development. The kernel is real, tested, and used by experimental Solace-side work. The cognition layers above the kernel are designed and partly prototyped (see the Liquid + Transformer notebook) but not yet ported into `lib/`.

This is a research-grade codebase. APIs may change as the cognition layers come online. Expect breaking changes between minor versions until 1.0.

---

## Contributing

Interest, ideas, and pull requests are welcome — particularly around:

- The Liquid Time-Constant cell port from the Kaggle notebook into a Kotlin form (likely with a PyTorch sidecar).
- Reflection Memory implementation on top of `core.storage`.
- A first cut of the Mouth Tool with a no-op policy plug-in interface.
- Any cleanup or hardening of the actor / port runtime.

There is a small actor scaffolding script:

```bash
./tools/generate_actor.sh MyActor
```

The published Maven coordinates (when version 0.1.0 ships) will be:

```kotlin
implementation("io.github.solaceharmony:solace-core:0.1.0")
```

---

## Author

**Sydney Renee** &nbsp;·&nbsp; [sydney@solace.ofharmony.ai](mailto:sydney@solace.ofharmony.ai) &nbsp;·&nbsp; [@sydneyrenee](https://github.com/sydneyrenee)

Designer and architect of Solace and SolaceCore. The thinking behind this project — coroutine‑parented neural trees, signature-keyed memory, vector‑to‑neuron plasticity, thought-and-speech separation — is hers.

The Liquid + Transformer hybrid math markup and code is a collaboration between Sydney and Claude (Anthropic) on the [Kaggle notebook](https://www.kaggle.com/code/sydneybachsolace/liquid-neural-networks-a-novel-hybrid-transformer).

---

## License

Apache License 2.0. See [LICENSE](LICENSE).

---

## Citation

If you reference SolaceCore in research, please cite:

```bibtex
@software{solacecore,
  author  = {Renee, Sydney},
  title   = {{SolaceCore}: An actor kernel for Solace, an AI with continuous internal narrative},
  year    = {2026},
  url     = {https://github.com/SolaceHarmony/SolaceCore},
  note    = {Apache-2.0}
}
```
