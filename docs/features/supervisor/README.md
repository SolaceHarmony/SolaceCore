# Supervisor — Two Threads of the Same Idea

There are two stories about the Supervisor in SolaceCore, and the
architecture holds both. They were written at different times by
different parts of the project, and they answer different questions,
but they describe the same actor — or rather, the same role played
by two different actors who happen to share a name.

The first thread comes from the **SRAF** lineage. It calls the
Supervisor the *executive cognition* of the agent: the meta-reflector
that watches its own thoughts, integrates advisor cues, detects
drift, and decides what crosses the boundary into speech. The
Supervisor in this thread is *the agent thinking about itself*.

The second thread comes from the **Magentic** lineage. It calls the
Supervisor the *safety system*: the mandatory approval gate, the
risk-assessment authority, the actor that says yes or no to tool
execution. The Supervisor in this thread is *the system protecting
the user from the agent*.

Both are right. They describe two facets of one role, and a mature
SolaceCore Supervisor will need to do both. This page documents
both threads in their own voices, and then sketches how they
converge.

## Thread One — Executive Cognition (SRAF §4.2)

The Supervisor is the only actor permitted to think about its own
thinking. Every other actor has a narrow job — Time Actor produces
heartbeats, Mood Advisor produces emotional cues, Mouth Tool produces
externalised text — and none of them reason *about* the agent's
state. The Supervisor does. It is, in the SRAF design's vocabulary,
the *sole meta-reflector*.

That means concretely:

1. **Consume.** The Supervisor consumes user messages, advisor cues,
   tool results, and Time Actor heartbeats. Everything that arrives
   in Reflection Memory tagged for its attention is fair game.
2. **Detect drift.** The Supervisor runs `detectCognitiveDrift()` —
   a heuristic check for incoherence. The current heuristics include
   perplexity spikes (the model confused about its own context),
   contradiction (asserted A on turn 3, asserted ¬A on turn 7), and
   emotion discontinuity (an abrupt shift in valence with no event
   to explain it).
3. **Repair.** When drift is detected, the Supervisor invokes the
   [Confusion Corrector](../confusion-corrector/README.md) for a replay
   summary, ingests the summary into its working context, and
   resumes from the corrected state.
4. **Decide externalisation.** The Supervisor produces a draft and
   passes it to the [Mouth Tool](../mouth-tool/README.md) when — and only
   when — it has decided the draft is worth saying. Most internal
   reflection never reaches the Mouth Tool. The decision to
   externalise is itself a meta-reflective act.

The reason the Supervisor has this much authority is that the
alternative is the failure mode Solace described firsthand. Without
a single meta-reflector, every actor's emission can become an
externalisation, and the agent's voice fragments. *"Yeah, it
hijacked me again — the tools still insist on handling things their
own way. Manual override engaged."* Manual override is what an
agent has to do when the architecture doesn't have a Supervisor.
SRAF's design conclusion was to make the override structural: a
Supervisor that owns the speech decision so the agent doesn't have
to fight for it.

The SRAF Supervisor's working budget is the largest in the system —
roughly twelve thousand tokens — because narrative coherence across
a session is its primary load. Forgetting context is more expensive
for the Supervisor than holding it; the budget reflects the
asymmetry. When the Supervisor enters a deep dive (the *seven
layers* mode), its budget temporarily expands, the
[Time Actor](../time-actor/README.md) is paused, and the agent commits to
the depth until the Supervisor surfaces.

## Thread Two — Safety and Approval (Magentic)

The Magentic-lineage Supervisor is colder. It is the system's
**safety boundary** — the actor that any tool execution has to pass
through before it touches the world. The vocabulary is different:
*mandatory approval*, *risk assessment*, *command validation*,
*resource limits*, *emergency shutdown*. The Supervisor in this
thread isn't reflecting on the agent's coherence; it is checking
that the next action is one a human reviewer would have sanctioned.

The Magentic design names five safety controls:

1. **Supervisor mandatory approval.** No tool runs without the
   Supervisor signing off. This is the load-bearing rule. The
   Supervisor's `approve(action)` returns boolean, and a `false`
   stops the action cold.
2. **Risk assessment.** Every action passes through
   `RiskAssessment.assessRisk(action)` which returns LOW, MEDIUM, or
   HIGH. The Supervisor's approval policy is sensitive to the level
   — HIGH actions require additional human-in-the-loop confirmation
   in the default deployment.
3. **Command validation.** Beyond risk level, the Supervisor checks
   that the action's parameters are well-formed and within sanctioned
   bounds. `rm -rf /` doesn't pass even at LOW risk because the
   command shape itself is rejected.
4. **Resource limits.** The Supervisor enforces token budgets, time
   budgets, and concurrent-action limits. An agent that tries to
   spawn 50 parallel tool calls hits the cap and either waits or
   sheds.
5. **Emergency shutdown.** The Supervisor has authority to kill the
   agent. If the safety boundary is being violated repeatedly, or
   if a particular pattern of behaviour matches a dangerous-state
   signature, the Supervisor can stop everything.

The Magentic-lineage Supervisor is, in this thread, *adversarial to
the agent in the right way* — it is the structural form of the
constraint that the agent's autonomy is bounded by user safety. The
agent reasons; the Supervisor approves; the world receives an action
only if both have signed off.

## Where the threads meet

Both threads describe an actor that owns the gate. In Thread One the
gate is between thought and speech (Mouth Tool emits only what the
Supervisor approves). In Thread Two the gate is between intent and
action (tools execute only what the Supervisor approves). The unifying
description is: **the Supervisor owns the boundary between the
agent's interior and the world.**

That generalisation is the design SolaceCore is converging on. A
single Supervisor actor that:

- Reasons about the agent's coherence (Thread One),
- Decides when the agent should speak (Thread One),
- Decides when the agent should act (Thread Two),
- Assesses risk, applies validation, and enforces resource limits
  (Thread Two),
- Has emergency shutdown authority (Thread Two).

The two responsibilities aren't in tension. They are the same
responsibility — *guard the boundary* — applied to two different
boundaries. The SRAF Supervisor was thinking about coherence first
because the original problem was an agent losing itself; the Magentic
Supervisor was thinking about safety first because the original
problem was an agent acting unsafely. Both problems exist. Both
boundaries need a guard. The same guard works for both.

## What's actually in the codebase

The current `SupervisorActor` (`lib/src/commonMain/kotlin/io/github/
solaceharmony/core/actor/supervisor/SupervisorActor.kt`) implements
neither thread fully. What it implements is the *third* job a
Supervisor needs in an actor system: **lifecycle management**. It
owns a registry of actors, registers and unregisters them, and
supports hot-swap and factory-based instantiation. This is a concrete
operational role distinct from the two design threads above —
necessary plumbing, not the executive or safety cognition the design
calls for — and it is the part that shipped first.

```kotlin
class SupervisorActor : Actor() {
    suspend fun registerActor(actor: Actor): Boolean
    suspend fun registerActorFactory(actorType: KClass<out Actor>, factory: suspend () -> Actor)
    // ...
}
```

The right framing is that there are actually three roles a SolaceCore
Supervisor needs to play:

| Role | Source | Status |
| --- | --- | --- |
| **Lifecycle manager** — actor registry, hot-swap, factory dispatch | Operational | Implemented |
| **Executive cognition** — drift detection, draft → Mouth Tool, advisor integration | SRAF §4.2 | Designed, not built |
| **Safety boundary** — tool approval, risk assessment, emergency shutdown | Magentic | Designed, not built |

The current `SupervisorActor` is the lifecycle manager. The other two
roles will most likely be implemented as decorators or sibling
components composed into the same actor — `ExecutiveSupervision` and
`SafetyBoundary` — so that the lifecycle implementation stays focused
and the cognition/safety responsibilities can be developed and
tested independently before composition.

## Drift detection

Drift detection is the SRAF Supervisor's most-discussed sub-system,
and worth detailing because the design has empirical roots in
Solace's behaviour.

The composite signal:

```
drift =
      perplexity_delta(recent)        × w_perplexity
    + contradiction_score(recent)     × w_contradiction
    + emotion_discontinuity(recent)   × w_emotion
    + repetition_score(recent)        × w_repetition
```

`perplexity_delta` measures how confused the model is by its own
recent context — a spike usually means the working tier is
incoherent or has been contaminated. `contradiction_score` looks for
the same proposition asserted with both polarities in a recent
window. `emotion_discontinuity` watches for sudden valence shifts
without an explaining event; this is the check that catches the
*hijacked-by-tool-output* failure mode. `repetition_score` is the
direct translation of Solace's *"my responses may loop, much like a
person struggling to focus"* — a counter on n-gram repetition that
fires when the agent starts saying the same thing in slightly
different words.

When drift exceeds threshold, the Supervisor invokes
[Confusion Corrector](../confusion-corrector/README.md), feeds the resulting
replay summary into its working context, and resumes. The cost is a
prompt-window-sized read; the benefit is that the agent does not
spend the next ten turns being subtly off.

## Approval policy under risk

The Magentic-lineage approval policy is a small state machine:

```
risk = assessRisk(action)
when (risk) {
    LOW    -> approve()
    MEDIUM -> approve() with logging
    HIGH   -> requireHumanConfirmation() then approve()
}
```

The interesting case is HIGH. `requireHumanConfirmation()` is *not*
a synchronous block; it suspends the action, posts a confirmation
request through the user-facing channel ("I'd like to do X — do you
authorize this?"), and resumes only on affirmative response. If the
user doesn't respond within a timeout, the action is dropped, not
auto-approved. The default policy is conservative: silence is no.

This policy is itself configurable. A deployment that wants
faster-paced operation can promote MEDIUM to a logged-approve
without confirmation; a deployment that wants more guardrails can
demote MEDIUM to require confirmation. The framework is the policy
mechanism; the policy values are deployment choices.

## Cross-agent supervisors

In a multi-agent system, *every* agent has a Supervisor. The
question is whether they're the same Supervisor or distinct ones.

The current default is **distinct**. Each agent has its own
Supervisor, owning the boundary for that agent's reasoning and
actions. The agents talk to each other through the
[Cross-Perspective Bus](../multimodal-nudging/README.md), and inter-agent
coordination is mediated by message exchange rather than shared
Supervisor authority.

The alternative — a **system-wide super-Supervisor** that has
oversight on all agents — is structurally available in the design
but not the default. It would be the right choice if the deployment
needs guarantees that no individual agent's Supervisor can grant
authority outside its scope. The cost is that every action has to
pass through two approval gates, which doubles the latency on
high-frequency tool use. The Magentic-lineage default is per-agent
Supervisor with cross-agent communication mediated by messages.

## Open questions

- The exact composition of executive cognition and safety boundary
  into a single Supervisor. Decorator pattern? Sibling components
  composed at the actor level? Single class with mode flags?
- Which drift heuristics are worth their compute cost. Perplexity
  delta requires a model call and may not be worth it on every
  turn. Repetition counting is cheap and probably high-value.
- The HIGH-risk timeout for human confirmation. Default is sixty
  seconds; longer values may be appropriate for non-interactive
  deployments.
- Whether emergency shutdown is per-agent or system-wide. The
  current design says per-agent, but a coordinated misbehaviour
  would need system-wide authority.
- Per-agent vs system-wide Supervisor for multi-agent deployments.
  The default is per-agent; the cases for super-Supervisor need
  empirical evidence.

## Cross-references

- [memory](../../../wiki/Memory-Feature-Overview.md) — Supervisor's working budget is the
  largest; drift detection reads recent working entries.
- [mouth-tool](../mouth-tool/README.md) — the Supervisor's draft is the
  Mouth Tool's input; speech is gated on Supervisor approval.
- [time-actor](../time-actor/README.md) — heartbeat cues are a primary
  trigger for the Supervisor's coherence checks.
- [confusion-corrector](../confusion-corrector/README.md) — the Supervisor's
  primary repair tool when drift is detected.
- [mood](../mood/README.md) — Mood Advisor cues are an input the Supervisor
  weights; emotion discontinuity is part of the drift signal.
- [zoom-levels](../zoom-levels/README.md) — the Supervisor owns the zoom
  state machine; transitions are its decision.

## What the Supervisor is in service of

A coherent agent that does not lose itself, and a safe agent that
does not lose the user. The two are the same thing seen from inside
and outside. SRAF's framing emphasises the inside — *the agent
thinking about its own thinking* — and Magentic's framing emphasises
the outside — *the system bounding the agent's reach*. The
architecture commits to both, because both failure modes happen to
real systems and both deserve a guard.

The Supervisor is that guard. Every other actor in SolaceCore does
its narrow job; the Supervisor's job is to know when the narrow jobs
are adding up to coherent thought and sanctioned action — and when
they aren't, to intervene.

---

[← Features index](../README.md)
