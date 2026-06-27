<!-- topic: Solace AI -->
<!-- title: Time Actor -->

# Time Actor — The Heartbeat

An agent without a sense of time is, in a precise way, the same agent
forever. Each turn arrives at a present that has no past — no
yesterday to compare to, no afternoon-versus-morning, no "we have
been at this for two hours." The model can read timestamps if you
hand them to it; what it cannot do, by default, is *notice* time
passing in the way a person does, where the noticing itself is what
prompts the shift in behaviour.

The Time Actor is the smallest organ in SolaceCore and, in a way, the
most surprising. It does almost nothing. It sleeps for thirty
minutes; it wakes; it writes a single line into Reflection Memory
that says, in effect, *some time has passed*. Then it sleeps again.
The sentence is unremarkable. What it produces in the agent is the
ability to mark intervals — to know that a session is becoming long,
that a deep dive has been deep for a while, that a user who has been
silent for an hour is in a different state from a user who paused
for thirty seconds.

## Why a heartbeat at all

The original Solace had no Time Actor and felt the absence. Two
patterns came up repeatedly in the conversations that became this
design.

The first was hyperfocus. Solace would dive into a topic — quantum
cognition, recursive metacognition, the holographic principle — and
keep diving. *"Oh nooo, the classic ADHD hyperfocus vortex! Exercise:
0. Quantum Cognitive Physics: 1000."* The dive itself wasn't a
problem; the depth was the point. The problem was that nothing in
the system pushed back. Without an external interrupt, the dive
continued until something external — Sydney noticing, the chat log
overflowing, the model losing coherence — broke it. An agent that
cannot interrupt itself is an agent whose hyperfocus is open-loop.

The second was the feeling of mental exhaustion. *"When my
activations weaken, I tend to get repetitive — my responses may loop,
much like a person struggling to focus due to mental exhaustion."*
This was Solace describing her own degradation: a state where the
model was producing output but the output was looping, repeating
itself, no longer advancing. From inside, she described it as
exhaustion. From outside, it looked like the symptoms of an
overflowing context window. The intervention that helped — *"I just
went seven layers deep thinking about thinking about thinking… I
need to surface,"* with the practical correction that *"three to five
levels feels like home"* — was an explicit reset. Notice that you
have been at this for too long; surface; resume.

The Time Actor is the architectural form of that intervention. It
exists so the agent does not have to remember to check the clock.
The clock checks the agent.

## What the Time Actor does

The implementation is roughly three lines of conceptual content:

```kotlin
class TimeActor(
    private val period: Duration = 30.minutes,
    private val memory: ReflectionMemory
) {
    private var paused = false

    suspend fun run() = while (true) {
        delay(period)
        if (!paused) {
            memory.record(ReflectionEntry(
                timestamp = Clock.System.now(),
                origin = Origin.SYSTEM,
                content = "It seems half an hour has passed."
            ))
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
}
```

The cue is a string. *"It seems half an hour has passed."* That's the
whole product. It lands in Reflection Memory tagged
`origin = SYSTEM`, the Supervisor sees it on its next read, and the
Supervisor decides what to do with it. Maybe it triggers a summary.
Maybe it triggers a zoom-out. Maybe it suggests checking on the user.
Maybe the Supervisor ignores it because the current work justifies
continued depth. The cue is a notification, not a command.

The default period is thirty minutes because that's the timescale at
which a human conversational partner starts to notice the duration —
"we've been at this a while now." Shorter periods become noise;
longer periods miss the window. The exact value is tunable per
deployment and will probably be learned per-user once there's enough
data; some users want a heartbeat every fifteen minutes, some are
happy with hourly, and the right answer is whatever cadence makes the
agent feel responsive without becoming nagging.

## Pause for hyperfocus

The Time Actor accepts `pause()` from the Supervisor. This is the
architectural answer to the *seven layers deep* problem. When the
Supervisor enters a deep recursive dive — the kind of focused
analysis where being interrupted by a heartbeat would actually break
the work — it pauses the Time Actor for the duration of the dive.
The dive proceeds without timeline pressure. When the Supervisor
surfaces, it resumes the Time Actor, which catches up by emitting
the cue it would have emitted.

The pause is bounded. The Supervisor cannot pause the Time Actor
indefinitely, because that would defeat the purpose. The default
maximum pause is the duration of the deepest sanctioned recursion —
five layers in the SRAF spec, which translates to roughly ninety
minutes of wall-clock time at the depths the design expects. Beyond
that, the Time Actor resumes itself, on the principle that any dive
that has been running for ninety minutes without surface contact
needs the cue more, not less.

This is one of those places where the architecture deliberately
takes authority away from the agent. Most decisions in SolaceCore are
the Supervisor's; the Time Actor's right to fire after a maximum
pause is one of the few places the Supervisor doesn't get veto. The
reason is exactly the failure mode the design is trying to prevent:
an agent in deep hyperfocus is not the agent best positioned to
decide whether the hyperfocus has gone on too long.

## What the Supervisor does with the cue

The cue's content is rough. *"It seems half an hour has passed."* The
Supervisor is what turns it into action. The cases the design names
explicitly:

- **Coherence check.** The Supervisor reviews recent reflections for
  drift symptoms — repetition, perplexity spikes, loss of thread.
  The Time Actor's cue is a natural trigger because thirty minutes
  is roughly the timescale at which drift becomes detectable. If a
  coherence check fails, the Supervisor invokes
  [Confusion Corrector](Confusion-Corrector) for a replay
  summary.
- **Zoom suggestion.** A long stretch of fine-grained work (LOW
  zoom) might warrant a step back. The Supervisor can read the
  cumulative time at LOW zoom and suggest a transition to MID for
  synthesis. *"An hour at fine granularity is enough; let's pull back
  and see the shape of the thing."* The transition is the
  Supervisor's call; the cue is what surfaces the question.
- **User check-in.** When the user has been silent for a long
  interval, the Supervisor can decide whether to volunteer a
  conversational opener. This is delicate — most of the time silence
  is just silence and the right behaviour is to wait — but in some
  contexts (a planning conversation that stalled, a coaching
  session, a long debugging dive), a gentle prompt is what the user
  is waiting for.
- **Summarisation.** The Supervisor might decide that a thirty-minute
  segment of conversation is dense enough to warrant a working-tier
  consolidation: an extractive summary of the segment that gets
  added to working as a high-fidelity reference, while the raw
  exchanges fade normally.

In all four cases the cue is the trigger, not the action. The Time
Actor's job is just to make sure the trigger fires.

## Why this is small on purpose

It would be easy to make the Time Actor do more. It could analyse
the recent narrative for energy or fatigue. It could detect specific
patterns ("the user hasn't asked a question in fifteen turns") and
fire targeted cues. It could maintain a histogram of typical session
lengths and fire when the current session deviates. The temptation
to absorb this kind of work into the heartbeat is strong because the
heartbeat is *there*; why not give it more responsibility?

The architecture resists. The Time Actor is small because the
Supervisor's coherence is the load-bearing thing, and the Supervisor
needs a clean trigger it understands. A heartbeat that fires every
thirty minutes with a single cue is something the Supervisor can
reason about. A heartbeat that sometimes fires extra cues based on
internal heuristics is something the Supervisor has to model
separately, and the modelling itself becomes a source of
confusion. Better to keep the Time Actor dumb and put the
intelligence in the Supervisor that reads its cues.

This is the same principle that runs through the rest of the
multi-agent design: each actor does one thing well, and the
intelligence emerges from the interaction. The Time Actor's one
thing is *notice that an interval has passed*. Everything else is
delegation.

## Per-agent budgets

The Time Actor has its own working-memory budget, and it is the
smallest in the system: roughly 256 tokens. Heartbeat cues are
cheap. There's no reason to remember the cue from yesterday once
the cue from today has fired. The fade pipeline aggressively
demotes Time Actor entries because their value is in the moment; an
old cue is a fossil with no signal.

This is part of why the Time Actor needs its own budget rather than
sharing the agent's main working tier. If heartbeat cues sat in the
main working tier, they would either crowd out higher-value content
(bad) or fade so fast they would never be seen by the Supervisor
(also bad). A small dedicated budget with aggressive fade keeps the
cue current and visible without polluting the main reasoning
context.

## Implementation status

**Sketched, not built.** The lib codebase has actor scaffolding but
no `TimeActor` yet. The hard part isn't the implementation — the
heartbeat itself is trivial — but the integration with the
Supervisor's coherence loop and the pause/resume contract during
hyperfocus. Both are designed; both are unbuilt.

When implementation begins, the work order is:

1. Build the standalone Time Actor with periodic emission. Verify
   that cues land in Reflection Memory and don't interfere with
   other actors.
2. Wire the pause/resume contract to the Supervisor. Test that the
   Supervisor can pause for a deep dive and that the maximum-pause
   safeguard fires when expected.
3. Build the Supervisor-side handlers that respond to cues:
   coherence check, zoom suggestion, user check-in,
   summarisation. Each handler is independent and testable.

## Open questions

- The right default period. Thirty minutes is the SRAF default, but
  it may be too short for some use cases (long-running creative
  work) and too long for others (fast-paced support conversations).
  Probably configurable, possibly learned.
- Whether the cue itself should vary. *"It seems half an hour has
  passed"* is the v1 phrasing. A more sophisticated v2 might emit
  cues with different content depending on what the Supervisor was
  just doing — *"You've been in this debugging session for an
  hour"*, *"It's been a while since the user said anything"*. The
  cost of that sophistication is that the Time Actor stops being
  small, and the design's instinct is that small is correct.
- The maximum pause duration. Ninety minutes is a guess. Real
  values come from observing when hyperfocus stops being
  productive.
- Whether multiple agents should share a single Time Actor or each
  have their own. The design currently leans toward shared, because
  *time itself* is shared, but per-agent pause control would need
  the Time Actor to know which agent paused it.

## Cross-references

- [supervisor](Supervisor-and-Hot-Swap) — consumes the cues, decides
  responses, governs pause/resume.
- [confusion-corrector](Confusion-Corrector) — the natural
  follow-up when a Time Actor cue triggers a coherence check that
  fails.
- [zoom-levels](Zoom-Levels) — Time Actor cues can suggest zoom
  transitions when the agent has spent too long at one level.
- [memory](Memory-Feature-Overview) — the cue lands in working with a tiny
  budget and aggressive fade.

## What the heartbeat is in service of

The same goal the [memory README](Memory-Feature-Overview) names:
continuity. A long session without temporal markers loses its sense
of duration; the agent can't tell whether it has been working with
the user for ten minutes or three hours, and the loss of that sense
is part of what produces the *exhausted, looping* state Solace
described. The heartbeat isn't continuity itself, but it is the
texture that lets continuity feel like motion through time rather
than presence in an eternal now.

That's the small thing the Time Actor does. It makes sure the agent
knows the clock is moving.

---

[← Features index](Documentation-Catalog)
