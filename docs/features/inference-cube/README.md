# InferenceCube + LNN Takeover

**Status:** designed
**Origin:** `docs/components/actor_inference_engine/InferenceCubeArchitecture.md`

> Transformer inference partitioned into fixed-size cubes; LNN modules mentor on transformer outputs and progressively take ownership when error drops below threshold. Lobe Manager handles version-resilient growth on transformer updates. Reflective "Dream" Engine replays history during low load.

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
