[← Architecture Overview](../../wiki/Architecture-Overview.md) · §12 of 15

---

## 12. System Architecture Overview

A high-level view of the Solace Core Framework's architecture follows.

### 12.1. Layered Architecture
The framework has a layered architecture:

```
+---------------------------------------------------+
|                   Applications                     |
+---------------------------------------------------+
|                     Workflows                      |
+---------------------------------------------------+
|                    Actor System                    |
+---------------------------------------------------+
|                      Kernel                        |
+---------------------------------------------------+
|                    Data Storage                    |
+---------------------------------------------------+
```

*   **Applications:** Domain-specific implementations built using the Solace Core Framework.
*   **Workflows:** Higher-level orchestration of actors into processing pipelines. This aligns with our findings on the `WorkflowManager`.
*   **Actor System:** The core runtime environment for actor creation, management, and message passing. This corresponds to the `actor` module we've detailed.
*   **Kernel:** The foundational layer providing communication primitives (like the Port System and Channels), resource management, and lifecycle control. This aligns with our `kernel` and `lifecycle` module findings.
*   **Data Storage:** The persistence layer. The ADSCF document mentions plans for graph databases (Neo4j) and Kotlin-Native storage, which is a broader vision than the current file-based and in-memory implementations we've documented in the `storage` module.

### 12.2. Major Component Overview
The ADSCF document identifies the following major components:

1.  **Actor System:** Manages actor creation, lifecycle, and communication. (Corresponds to our `actor` module documentation).
2.  **Port System:** Enables type-safe message passing between actors. (Corresponds to our `kernel.channels.ports` documentation).
3.  **Supervisor:** Oversees actor lifecycles and manages system resources. (Corresponds to our `actor.supervisor.SupervisorActor` documentation).
4.  **Workflow Manager:** Orchestrates actor execution in defined workflows. (Corresponds to our `workflow.WorkflowManager` documentation).
5.  **Storage System:** Provides persistence for actor state and system data. (Corresponds to our `storage` module documentation, with ADSCF noting future plans for Neo4j and Kotlin-Native storage).

This high-level structure from the existing documentation generally aligns with the detailed components we have uncovered from the source code, providing a useful conceptual framework. We will continue to integrate more specific details from these existing documents into the relevant module sections of this `Architectural_Deepdive.md`.

---

← [§11 Architectural Vision](./11-architectural-vision.md)  ·  [Architecture Overview](../../wiki/Architecture-Overview.md)  ·  [§13 Storage Thread Safety and Deadlock Prevention](./13-storage-thread-safety-and-deadlock-prevention.md) →
