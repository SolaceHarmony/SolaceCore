<!-- topic: Runtime -->
<!-- title: Actor Supervision Module -->

### 4.4. Actor Supervision (`supervisor` Subdirectory)
The `io.github.solaceharmony.core.actor.supervisor` package provides a mechanism for managing groups of actors.

#### 4.4.1. `SupervisorActor` Class
This class is a concrete implementation of `Actor` designed to oversee and manage a collection of other "child" or "managed" actors. Its focus is on dynamic registration, lifecycle control, and hot-swapping of these actors.

*   **Purpose:** To act as a central point for managing a group of actors, allowing for their addition, removal, and replacement at runtime, as well as collective lifecycle operations.
*   **Inheritance:** Extends `io.github.solaceharmony.core.actor.Actor`.
*   **Key Responsibilities and Features:**
    *   **Actor Registry:**
        *   Maintains an internal registry (`actorRegistry: Map<String, Actor>`) of actors it manages, keyed by actor ID.
        *   Also keeps track of the `KClass` of each registered actor (`actorTypeRegistry`) for type-safe operations like hot-swapping.
        *   Access to these registries is synchronized using a `Mutex`.
    *   **Dynamic Actor Management:**
        *   `suspend fun registerActor(actor: Actor): Boolean`: Adds a new actor to its management pool. Requires the `SupervisorActor` to be running.
        *   `suspend fun unregisterActor(actorId: String): Boolean`: Removes an actor from its management pool. Requires the `SupervisorActor` to be running.
        *   `suspend fun hotSwapActor(oldActorId: String, newActor: Actor): Boolean`: Replaces an existing managed actor with a new instance. The process, as detailed in the wiki [SupervisorActor](SupervisorActor) page, involves:
            1.  Checking if the `oldActorId` exists in the registry.
            2.  Verifying that the `newActor` is of the same `KClass` as the old one, ensuring type safety.
            3.  Noting if the old actor was in a `Running` state.
            4.  Stopping the old actor.
            5.  Replacing the old actor with the new one in the internal `actorRegistry` and `actorTypeRegistry`.
            6.  Starting the `newActor` if the old actor was previously running.
            This operation requires the `SupervisorActor` itself to be in a `Running` state.
*   **Operational Note:** Most dynamic management operations (`registerActor`, `unregisterActor`, `hotSwapActor`) and collective lifecycle controls (`startAllActors`, `stopAllActors`) require the `SupervisorActor` to be in a `Running` state. Attempting these operations when the supervisor is not running will typically result in an `IllegalStateException`. Furthermore, methods like `registerActor`, `unregisterActor`, and `hotSwapActor` return a `Boolean` value: `true` indicates successful completion, while `false` usually signifies a failure due to conditions such as a non-existent actor ID or a type mismatch during hot-swapping, as noted in the wiki [SupervisorActor](SupervisorActor) page.
    *   **Actor Discovery:**
        *   `suspend fun getActor(actorId: String): Actor?`: Retrieves a specific managed actor by its ID.
        *   `suspend fun getAllActors(): List<Actor>`: Returns a list of all actors currently managed by the supervisor.
        *   `suspend fun getActorsByType(actorType: KClass<out Actor>): List<Actor>`: Retrieves all managed actors that are instances of a specific `actorType`.
    *   **Collective Lifecycle Control:**
        *   `suspend fun startAllActors()`: Calls `start()` on all managed actors. Requires the `SupervisorActor` to be running.
        *   `suspend fun stopAllActors()`: Calls `stop()` on all managed actors. Requires the `SupervisorActor` to be running.
    *   **Resource Cleanup (`dispose()`):**
        *   When the `SupervisorActor` itself is disposed, it iterates through all its managed actors in `actorRegistry` and calls `dispose()` on each, then clears its internal registries before calling `super.dispose()`.
##### 4.4.1.1. Best Practices
The wiki [SupervisorActor](SupervisorActor) page outlines several best practices for using the `SupervisorActor` effectively:

*   **Initialization Order:** Always start the `SupervisorActor` (by calling its `start()` method) before attempting to register any child actors.
*   **Unique Actor IDs:** Ensure that all actors registered with a supervisor have unique IDs to prevent registration conflicts and ensure predictable behavior.
*   **Hot-Swap Compatibility:** When hot-swapping actors, verify that the new actor instance is not only of the same `KClass` but also logically compatible with the role and connections of the actor it is replacing.
*   **Resource Management:** Dispose of the `SupervisorActor` using its `dispose()` method when it is no longer needed. This ensures that all managed actors are also properly disposed of, releasing their resources.
*   **Fault Tolerance:**
    *   The `SupervisorActor` as defined in this file primarily focuses on lifecycle management and dynamic updates rather than implementing traditional actor supervision strategies (e.g., "one-for-one," "all-for-one" restart/stop strategies upon child failure). Fault handling for individual actors would typically be the responsibility of the actors themselves or a different supervisory layer if more complex strategies are needed.



[Back to Actor System](Actor-System)
