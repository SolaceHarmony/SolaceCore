# Shared-Memory Primitives

**Status:** designed
**Origin:** SRAF §8 ("Reflection Memory implemented with lock-free queue"); codex-vendored `CoroutineSharedMemory.kt`; Inference §4.1 SharedMemoryManager

> Lock-free queues + atomic counters for cross-coroutine coordination. Two layers: (a) scheduler (SharedWorkQueue, SharedDescriptorQueue, SharedSchedulerControlState, SharedParkedStack); (b) inference data (zero-copy SharedMemoryManager slices). Adopt `kotlin.concurrent.atomics.AtomicInt/Long` (stdlib, multiplatform).

---

## Design

_(Detailed design lives here. Lift from the origin doc as you reconcile against
current code. Include data contracts, sequence diagrams, failure modes.)_

## Implementation

_(Pointers to the modules in `lib/` and `composeApp/` that realize this
feature, plus a short note on how far along the implementation is.)_

## Open questions

_(Decisions not yet made. List them as bullets so they're easy to triage.)_

## Cross-references

_(Other features this one depends on or that depend on it.)_

---

[← Features index](../README.md)
