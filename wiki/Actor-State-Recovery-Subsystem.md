<!-- topic: Runtime -->
<!-- title: Actor State Recovery Subsystem -->

#### 3.1.5. Actor State Recovery Subsystem (`io.github.solaceharmony.core.storage.recovery`)
The `storage` module includes a dedicated subsystem for managing the snapshotting and recovery of actor states, ensuring data resilience. This is located in the `io.github.solaceharmony.core.storage.recovery` package.

##### 3.1.5.1. `ActorStateSnapshot` Data Class
This data class represents an immutable snapshot of an actor's complete state at a particular point in time.

*   **Purpose:** To encapsulate all necessary information for restoring an actor to a previous state.
*   **Key Properties:**
    *   `actorId: String`: The unique identifier of the actor.
    *   `actorName: String`: The human-readable name of the actor.
    *   `state: ActorState`: The core state of the actor (e.g., Initialized, Running, Stopped, Error, Paused), referencing the `ActorState` sealed class from `io.github.solaceharmony.core.actor`.
    *   `ports: Map<String, Map<String, Any>>`: Configuration of the actor's communication ports.
    *   `metrics: Map<String, Any>`: Metrics associated with the actor.
    *   `customState: Map<String, Any>`: Any additional custom state data for the actor.
    *   `version: Int`: A version number for the snapshot, typically incrementing.
    *   `timestamp: Long`: The epoch milliseconds timestamp when the snapshot was created.
*   **Builder Pattern:**
    *   A companion object `ActorStateSnapshot.builder(actorId: String)` provides an `ActorStateSnapshotBuilder` instance.
    *   The `ActorStateSnapshotBuilder` class offers a fluent API (`withName()`, `withState()`, etc.) to construct `ActorStateSnapshot` objects.

##### 3.1.5.2. `RecoverableActorStateStorage` Interface
This interface extends `ActorStateStorage` to add functionalities specifically for managing actor state snapshots.

*   **Purpose:** To define a contract for storage backends that can persist and retrieve actor state snapshots.
*   **Inheritance:** Extends `io.github.solaceharmony.core.storage.ActorStateStorage`.
*   **Key Snapshot-Specific Methods:**
    *   `suspend fun createSnapshot(actorId: String): ActorStateSnapshot?`: Implementations are expected to capture the current state of the actor (identified by `actorId`) from the storage and persist it as a new snapshot.
    *   `suspend fun restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean`: Restores the actor's state in the persistent storage to match the provided `snapshot`. This means the underlying `ActorStateStorage` will reflect the data within the snapshot.
    *   `suspend fun listSnapshots(actorId: String): List<ActorStateSnapshot>`: Retrieves all stored snapshots for a given actor, typically sorted by timestamp.
    *   `suspend fun getLatestSnapshot(actorId: String): ActorStateSnapshot?`: (Default implementation provided) Retrieves the most recent snapshot for an actor.
    *   `suspend fun getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot?`: (Default implementation provided) Retrieves a specific snapshot by its version number.
    *   `suspend fun deleteSnapshot(actorId: String, version: Int): Boolean`: Deletes a specific version of an actor's snapshot.
    *   `suspend fun deleteAllSnapshots(actorId: String): Boolean`: Deletes all snapshots associated with a particular actor.

##### 3.1.5.3. `ActorRecoveryManager` Class
This class provides a higher-level API to orchestrate actor snapshotting and recovery processes.

*   **Constructor:** `ActorRecoveryManager(private val storage: RecoverableActorStateStorage)`
*   **Key Functionalities:**
    *   **Snapshot Creation (`createSnapshot(actor: Actor)`):**
        1.  Gathers current data for the live `actor` (ID, name, state) and from the `storage` (ports, metrics, custom state).
        2.  Determines the next snapshot `version` by checking the latest existing snapshot for that actor.
        3.  Builds an `ActorStateSnapshot` object using the builder.
        4.  Persists the new snapshot by calling `storage.restoreFromSnapshot(snapshot)`. *Note: The act of "restoring" a newly created snapshot is the mechanism used here to save it.*
    *   **Actor Restoration (`restoreActor(snapshot, actorFactory)`):**
        1.  Calls `storage.restoreFromSnapshot(snapshot)` to ensure the persistent state reflects the snapshot.
        2.  Uses the provided `actorFactory: (String, String) -> Actor` lambda to instantiate a new live `Actor` object.
        3.  Applies the `snapshot.state` to the live actor instance (e.g., by calling `actor.start()`, `actor.pause(reason)`).
    *   **Bulk Recovery (`recoverAllActors(actorFactory)`):**
        1.  Retrieves all actor IDs from `storage.listKeys()`.
        2.  For each actor ID, fetches the `storage.getLatestSnapshot()`.
        3.  Uses its `restoreActor()` method to recreate and restore each actor from its latest snapshot.
    *   **Snapshot Management:** Provides convenience methods that delegate to the underlying `storage` for listing, retrieving by version, and deleting snapshots.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.actor" {
        class Actor {
            +id: String
            +name: String
            +state: ActorState
            +start()
            +stop()
            +pause(reason: String)
        }
        class ActorState {
            <<Sealed>>
        }
    }

    package "io.github.solaceharmony.core.storage" {
        interface ActorStateStorage {
            <<Interface>>
            +getActorPorts(actorId: String): Map?
            +getActorMetrics(actorId: String): Map?
            +getActorCustomState(actorId: String): Map?
        }
    }

    package "io.github.solaceharmony.core.storage.recovery" {
        class ActorStateSnapshot {
            +actorId: String
            +actorName: String
            +state: ActorState
            +ports: Map
            +metrics: Map
            +customState: Map
            +version: Int
            +timestamp: Long
            +static builder(actorId: String): ActorStateSnapshotBuilder
        }

        class ActorStateSnapshotBuilder {
            +withName(name: String): Self
            +withState(state: ActorState): Self
            +build(): ActorStateSnapshot
        }
        ActorStateSnapshotBuilder ..> ActorStateSnapshot : creates

        interface RecoverableActorStateStorage {
            <<Interface>>
            +createSnapshot(actorId: String): ActorStateSnapshot?
            +restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean
            +listSnapshots(actorId: String): List<ActorStateSnapshot>
            +getLatestSnapshot(actorId: String): ActorStateSnapshot?
            +getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot?
            +deleteSnapshot(actorId: String, version: Int): Boolean
            +deleteAllSnapshots(actorId: String): Boolean
        }
        ActorStateStorage <|-- RecoverableActorStateStorage

        class ActorRecoveryManager {
            -storage: RecoverableActorStateStorage
            +ActorRecoveryManager(storage)
            +createSnapshot(actor: Actor): ActorStateSnapshot?
            +restoreActor(snapshot: ActorStateSnapshot, actorFactory): Actor?
            +recoverAllActors(actorFactory): Map<String, Actor>
            +getLatestSnapshot(actorId: String): ActorStateSnapshot?
        }
        ActorRecoveryManager o-- RecoverableActorStateStorage : uses
        ActorRecoveryManager ..> Actor : uses actorFactory to create
        ActorRecoveryManager ..> ActorStateSnapshot : creates & uses
        RecoverableActorStateStorage ..> ActorStateSnapshot : manages
    }


[Back to Storage Module Architecture](Storage-Module-Architecture)
