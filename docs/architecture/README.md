# SolaceCore Architecture — Wiki
> **Status:** Canonical reference (Rosetta). Other docs translate against this one.
> Recovered design material in [`../sketch-architecture/`](../sketch-architecture/) and
> [`../kotlin-aligned-docs/`](../kotlin-aligned-docs/) is incorporated here as it is
> reconciled with current code.

This page is the index. The body of the deep-dive lives in the per-section files
linked below; each is small enough to read in one sitting.

The original monolithic [`Architectural_Deepdive.md`](../Architectural_Deepdive.md) is
preserved as a redirect stub.

## Sections
| # | Section | Source lines |
|---|---------|-------------:|
| 0 | [Solace Project Context](./00-solace-project-context.md) | 1–57 |
| 1 | [Kernel Module](./01-kernel-module.md) | 58–574 |
| 2 | [Lifecycle Module (`io.github.solaceharmony.core.lifecycle`)](./02-lifecycle-module-io-github-solaceharmony-core-lifecycle.md) | 575–706 |
| 3 | [Storage Module (`io.github.solaceharmony.core.storage`)](./03-storage-module-io-github-solaceharmony-core-storage.md) | 707–1647 |
| 4 | [Actor Module (`io.github.solaceharmony.core.actor`)](./04-actor-module-io-github-solaceharmony-core-actor.md) | 1648–2077 |
| 5 | [Workflow Module (`io.github.solaceharmony.core.workflow`)](./05-workflow-module-io-github-solaceharmony-core-workflow.md) | 2078–2080 |
| 6 | [Scripting Module (`io.github.solaceharmony.core.scripting`)](./06-scripting-module-io-github-solaceharmony-core-scripting.md) | 2081–2414 |
| 7 | [Build System and Dependencies](./07-build-system-and-dependencies.md) | 2415–2478 |
| 8 | [Development Tooling and Practices](./08-development-tooling-and-practices.md) | 2479–2499 |
| 9 | [JVM-Specific Utilities (`io.github.solaceharmony.core.util`)](./09-jvm-specific-utilities-io-github-solaceharmony-core-util.md) | 2500–2538 |
| 10 | [Testing Strategy (JVM Target)](./10-testing-strategy-jvm-target.md) | 2539–2588 |
| 11 | [Architectural Vision](./11-architectural-vision.md) | 2589–2611 |
| 12 | [System Architecture Overview](./12-system-architecture-overview.md) | 2612–2650 |
| 13 | [Storage Thread Safety and Deadlock Prevention](./13-storage-thread-safety-and-deadlock-prevention.md) | 2651–2727 |
| 14 | [InferenceCube Architecture](./14-inferencecube-architecture.md) | 2728–2771 |

## How this is organized
- Numbered sections follow the original Rosetta numbering exactly. `§N` references
  in commit messages, code comments, and other docs continue to resolve.
- Each section file ends with prev/next navigation links so you can read straight
  through, or jump back here to skim.
- Recovered design material (mood transparency, neutral history, MCP, etc.) gets
  folded into the matching numbered section over time. New design notes that don't
  fit a numbered section land as a new `§NN-<topic>.md` and get a row added above.

## Cross-references
- Vision and project context: [§0 Solace Project Context](./00-solace-project-context.md)
- High-level architecture: [§12 System Architecture Overview](./12-system-architecture-overview.md)
- Actor system (where Tool / Memory / Mood actor types land): [§4 Actor Module](./04-actor-module-io-github-solaceharmony-core-actor.md)
- Storage thread-safety guarantees: [§13 Storage Thread Safety and Deadlock Prevention](./13-storage-thread-safety-and-deadlock-prevention.md)
