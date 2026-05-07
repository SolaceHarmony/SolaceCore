# Shared-Memory Primitives — The Lock-Free Floor

Underneath all the cognition — the Supervisor reasoning, the Mood
Advisor classifying, the Mouth Tool framing, the Time Actor
heartbeating, the modality actors perceiving — there has to be a
substrate that lets all those coroutines coordinate without
fighting each other for locks. The agent's reasoning loop
shouldn't pause because the fade pipeline holds a mutex. The
Cross-Perspective Bus shouldn't drop nudges because the queue is
contended. The substrate underneath every actor needs to be
*lock-free*: every operation makes progress, no operation waits on
another, throughput scales with cores rather than contention.

That substrate is the shared-memory primitives layer. It's the
plumbing — invisible when it works, catastrophic when it doesn't —
and the architecture takes it seriously enough to give it a
top-level feature folder.

## Two threads of the same idea

The shared-memory layer is one of the convergent points where the
**SRAF** lineage and the **Magentic** lineage independently arrived
at the same answer.

The **SRAF** thread (§8) called for *Reflection Memory implemented
with lock-free queue; periodic batch flush to disk/DB.* The
framing was performance: the agent's reasoning loop runs at
millisecond cadences, and a memory substrate that takes locks will
stall the loop the moment a fade-pipeline batch happens to be
running.

The **Magentic** thread (`CoroutineSharedMemory.kt`, journal entry
2025-11-20) called for a *coroutine scheduler shared-memory
retrofit*: replace JS-object queues with SharedArrayBuffer + Atomics
for lock-free coordination. The framing was correctness across
runtimes: when the framework runs in WASM and JavaScript
environments, the JavaScript runtime's "object queue" abstraction
doesn't compose with concurrent workers, and the only portable
answer is a typed buffer with atomic operations.

Both threads converge on the same primitive: **lock-free queues
with atomic counters**, expressed in a way that works on both JVM
and Native and (eventually) JS/WASM. The SRAF lineage gave the
*reason* (don't stall the agent loop); the Magentic lineage gave
the *shape* (typed buffer + atomics, not language-specific queue
objects).

## The atomics question, briefly

Kotlin has had three options for atomics over the years. The
project's commitment is firm enough to be worth saying explicitly:

**Use `kotlin.concurrent.atomics.AtomicInt` / `AtomicLong`.** This
is the stdlib multiplatform API since Kotlin 2.1. It works on JVM,
Native, and JS. Same API, same semantics, no platform-specific
imports.

**Do not use `java.util.concurrent.atomic.*`.** That's the JVM-only
API. Reaching for it in `commonMain` breaks the native-first
commitment of the project. Even when JVM is the active target, the
stdlib API is the one to write against.

**Do not use `kotlinx.atomicfu`.** `atomicfu` was the right answer
for a window of years before stdlib atomics existed; that window is
closed. The stdlib API is preferred now.

The Magentic-vendored `CoroutineSharedMemory.kt` shows
`java.util.concurrent.atomic.AtomicInteger` because it was written
before Kotlin 2.1 stabilised the multiplatform API. The
implementation lifted from it should use the stdlib types instead.

## Two layers, one substrate

The architecture distinguishes two layers of shared-memory use, with
different access patterns and different cost characteristics.

### Layer 1: scheduler primitives

The lower layer holds the runtime's coordination state: which
coroutines are runnable, which workers are parked, how many CPU
permits are available, what's queued for execution. Four
structures, each implementing the same `SharedMemoryStructure`
interface:

| Structure | Purpose |
| --- | --- |
| **`SharedWorkQueue`** | Ring buffer for runnable task envelopes. Lock-free enqueue/dequeue with `compareAndSet`. |
| **`SharedDescriptorQueue`** | Specialisation of work queue for descriptor envelopes (handler dispatch). |
| **`SharedSchedulerControlState`** | Atomic counters for `created`, `blocking`, `cpuPermits`. The scheduler's running state. |
| **`SharedParkedStack`** | Stack of parked worker IDs; `compareAndSet` for push/pop. |

```kotlin
class SharedWorkQueue(override val size: Int) : SharedMemoryStructure {
    override val buffer = ByteArray(size * TASK_ENVELOPE_SIZE)
    private val head = AtomicInt(0)
    private val tail = AtomicInt(0)

    fun enqueue(task: TaskEnvelope): Boolean {
        while (true) {
            val currentTail = tail.load()
            val currentHead = head.load()
            if ((currentTail + 1) % size == currentHead) return false
            if (tail.compareAndSet(currentTail, (currentTail + 1) % size)) {
                writeTaskEnvelope(currentTail, task)
                return true
            }
        }
    }
    // ... dequeue similar shape
}
```

The CAS loop is the canonical lock-free idiom. Every operation
either succeeds (advancing the queue) or yields (a contended slot
gets retried by a different coroutine). No operation blocks; no
operation holds a lock that other operations wait on.

### Layer 2: inference data

The upper layer is the [InferenceCube](../inference-cube/README.md) data
plane: the tensors, hidden states, and signatures that the
LTC + Transformer hybrid passes between cubes. The primitive here is
the **`SharedMemoryManager`** — a manager that hands out *slices*
of a single underlying buffer to multiple coroutines without
copying data.

```kotlin
interface SharedMemoryManager {
    fun allocate(bytes: Int): MemorySlice
    fun release(slice: MemorySlice)
    fun snapshot(slice: MemorySlice): MemorySlice  // copy-on-read view
}

data class MemorySlice(
    val offset: Int,
    val length: Int,
    val buffer: SharedByteArray
)
```

The slice abstraction is what makes the inference path zero-copy.
A cube's output buffer can be the next cube's input buffer
directly — no intermediate copy, no allocation churn — because
both cubes hold slices of the same underlying buffer with
non-overlapping windows.

This matters because LTC inference is bandwidth-bound. The hidden
state is small per-step but updated continuously, and copying it
between cubes on every tick would dominate the CPU budget. The
slice abstraction lets the inference pipeline avoid the copies
entirely.

## Why the bus and the tier sit on top

The [Cross-Perspective Bus](../multimodal-nudging/README.md) is one
direct consumer. Its lock-free pub/sub is exactly a
`SharedDescriptorQueue` with one descriptor envelope per nudge.
Every modality actor publishes by appending to the queue;
subscribers consume by dequeuing. No locks, no contention.

The [Reflection Memory](../reflection-memory/README.md) substrate is
another. Its in-memory ring is a `SharedWorkQueue` with
ReflectionEntry envelopes. Periodic flush to durable storage
reads from the ring and writes to disk; the ring stays available
for new entries throughout the flush.

The [working memory](../memory/working-memory.md) tier is a third.
Its in-process structure is a small set of slices managed by the
`SharedMemoryManager`, sized to the agent's working budget. Reads
are direct buffer accesses; writes use atomic counters for the
fade-related metadata (`referenceCount`, `emotionalWeight`).

The pattern repeats throughout the architecture. Anywhere two
coroutines need to coordinate without locks — and *everywhere* in
SolaceCore qualifies — the shared-memory primitives are the floor
they stand on.

## What lives in the buffer

A `SharedMemoryStructure` is more than a wrapped byte array. The
contract it implements:

```kotlin
interface SharedMemoryStructure {
    val buffer: ByteArray            // SharedArrayBuffer on JS/WASM
    val size: Int
    fun initialize()
}
```

Three things matter.

**The buffer is typed.** Despite the `ByteArray` shape, every slice
of every buffer has a known structure — task envelope, descriptor
envelope, control state, parked worker ID. The shape is fixed at
buffer creation; readers and writers agree on it by convention.

**The buffer survives across runtimes.** On JVM the underlying
storage is a JVM `ByteArray`. On Native it's a pinned native
allocation. On JS/WASM it's a `SharedArrayBuffer`. The atomic
operations work the same way on all three — `AtomicInt.load()`,
`AtomicInt.store()`, `AtomicInt.compareAndSet()` — because the
stdlib API hides the platform difference.

**The buffer is single-allocation.** A `SharedWorkQueue` of size
1024 is one allocation of 1024 × `TASK_ENVELOPE_SIZE` bytes, not
1024 separate allocations. This is what keeps allocation pressure
low in the inference and scheduler hot paths; the structures don't
churn the heap as they fill and drain.

## CAS loop discipline

The lock-free idiom has its own discipline.

**Every CAS loop has a bounded retry.** In practice, contention is
low and retries succeed within one or two attempts. But under
pathological contention — one core hammering a slot every cycle
— a CAS loop can spin indefinitely. The implementations include
retry caps and back-off when the cap is hit, falling through to
a parked-and-wake path that yields to the scheduler.

**`load()` and `compareAndSet` use the right memory order.** On
JVM the stdlib API defaults to sequentially-consistent semantics,
which is what these structures need. On Native and JS/WASM the
same API maps to equivalent fences. The implementations don't
reach for relaxed atomics unless a specific structure has been
analysed and proven to need only weaker ordering.

**Visibility tests run in CI.** Lock-free correctness is hard to
verify by inspection. The test suite includes stress tests that
hammer the structures from many coroutines and check for
duplicates, drops, or stale reads. The tests run on JVM as the
primary target with smaller runs on Native to verify the same
shape.

## Failure modes

**ABA in the parked-stack push/pop.** The classic lock-free
hazard: a worker pushes ID X, another pops X, a third pushes X
again, and the original push's CAS sees X-then-X and assumes
nothing happened. The mitigation in `SharedParkedStack` is a
generation counter packed into the top word; CAS compares both
top and generation, so an ABA cycle changes the generation and
the CAS fails as it should.

**Queue full / queue empty.** Both states are returned cleanly
(`enqueue` returns false, `dequeue` returns null) rather than
blocking. Callers handle the failure: enqueue full means the
producer slows down or drops; dequeue empty means the consumer
parks.

**Cross-thread visibility on weakly-ordered platforms.** On
platforms with weak memory models, naive use of atomic load/store
can permit reordering that breaks the lock-free invariants. The
stdlib `compareAndSet` includes the necessary fences; structures
that use raw load/store for hot-path reads include explicit
fences where ordering matters.

## Implementation status

**Magentic-side scaffold exists.** The
`docs/sketch-architecture/CoroutineSharedMemory.kt` file has the
shape of all four scheduler structures — `SharedWorkQueue`,
`SharedDescriptorQueue`, `SharedSchedulerControlState`,
`SharedParkedStack`. The shape is right; the file uses
`java.util.concurrent.atomic.AtomicInteger` instead of the
multiplatform stdlib API and would need that conversion when
moved into `lib/`.

**SRAF-side reference exists in §8** but no scaffold; SRAF described
the requirement, not the implementation.

**The `SharedMemoryManager` for inference data is sketched** in
the InferenceCube vendor docs but not built.

The work order:

1. Convert the Magentic scaffold to use `kotlin.concurrent.atomics`
   and move into `lib/src/commonMain/`. Stress test on JVM.
2. Verify the same code compiles and passes on Native targets.
3. Build the `SharedMemoryManager` for inference data. Wire to
   the InferenceCube data plane when that lands.
4. Migrate the Cross-Perspective Bus and Reflection Memory ring
   to sit on the converted scaffolding.

## Open questions

- **JS/WASM target ETA.** SharedArrayBuffer requires cross-origin
  isolation, which has deployment implications for the web target.
  Compose Multiplatform's web build supports it but only with
  specific server configuration. The decision affects whether
  shared-memory primitives are uniformly available across all
  runtimes or whether the JS path needs a fallback.
- **CAS retry cap values.** Default values are guesses; real
  values come from observing contention patterns in production.
- **Whether to use `CountedReference` for `MemorySlice` lifecycle.**
  Currently the SharedMemoryManager hands out slices and trusts
  callers to release them; counted references would close the
  loophole.

## Cross-references

- [reflection-memory](../reflection-memory/README.md) — sits on the
  shared-memory ring; the substrate's lock-free queue is one of
  these structures.
- [multimodal-nudging](../multimodal-nudging/README.md) — Cross-Perspective
  Bus is a SharedDescriptorQueue.
- [memory](../memory/README.md) — working tier holds slices; long-term
  vector index uses atomic counters for fade metadata.
- [inference-cube](../inference-cube/README.md) — Layer 2 inference data
  plane uses `SharedMemoryManager` for zero-copy slices.
- [pipeline](../pipeline/README.md) — pipeline DSL backs onto these
  primitives for stage-to-stage data flow.

## What the floor is in service of

Everything else. The architecture commits to running every
component continuously without pausing the agent's reasoning loop,
and that commitment requires the substrate underneath to be
non-blocking. Lock-based designs work; they just stall under
load, and the failure mode of stalling under load is exactly the
failure mode the agent's continuity depends on avoiding. The
shared-memory primitives are the choice to pay the complexity cost
of lock-free design once, in this layer, so that everything above
it can assume non-blocking coordination as a property of the
substrate.

That's what the floor is for.

---

[← Features index](../README.md)
