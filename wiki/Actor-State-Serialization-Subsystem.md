<!-- topic: Runtime -->
<!-- title: Actor State Serialization Subsystem -->

#### 3.1.6. Actor State Serialization Subsystem (`io.github.solaceharmony.core.storage.serialization`)
To handle the specific needs of persisting complex `ActorState` objects and custom actor data, the `storage` module includes a dedicated serialization subsystem. This is found in the `io.github.solaceharmony.core.storage.serialization` package.

##### 3.1.6.1. `ActorStateSerializer<T : Any>` Interface
This interface defines a contract for serializers specifically designed for actor state components.

*   **Purpose:** To provide a standardized way to convert actor state related objects to and from a `Map<String, Any>` representation, suitable for storage.
*   **Generic:** `T` - The type of object the serializer handles.
*   **Key Methods:**
    *   `fun serialize(obj: T): Map<String, Any>`: Converts the object `obj` into a map.
    *   `fun deserialize(map: Map<String, Any>): T?`: Converts a map back into an object of type `T`, returning `null` on failure.
    *   `fun getType(): KClass<T>`: Returns the `KClass` of the object type `T` this serializer is responsible for.

##### 3.1.6.2. `ActorStateEnumSerializer` Class
A concrete implementation of `ActorStateSerializer<ActorState>` specifically for the `io.github.solaceharmony.core.actor.ActorState` sealed class.

*   **Purpose:** To correctly serialize and deserialize the different states of an actor (e.g., `Initialized`, `Running`, `Stopped`, `Error`, `Paused`), including any associated data like error messages or pause reasons.
*   **Serialization Logic:** Converts `ActorState` instances into a map, typically including a "type" field (e.g., "Running") and other relevant fields (e.g., "exception" for `ActorState.Error`).
*   **Deserialization Logic:** Reconstructs the appropriate `ActorState` instance based on the "type" field and other data in the input map.

##### 3.1.6.3. `ActorStateSerializerRegistry` Class
Manages a collection of `ActorStateSerializer` instances.

*   **Purpose:** To act as a central point for registering and retrieving serializers for different actor state component types.
*   **Internal Structure:** Uses a `mutableMapOf<KClass<*>, ActorStateSerializer<*>>` to store serializers, keyed by the `KClass` they handle.
*   **Key Methods:**
    *   `fun <T : Any> registerSerializer(serializer: ActorStateSerializer<T>)`
    *   `fun <T : Any> getSerializer(clazz: KClass<T>): ActorStateSerializer<T>?`
    *   `fun <T : Any> hasSerializer(clazz: KClass<T>): Boolean`
    *   `fun <T : Any> unregisterSerializer(clazz: KClass<T>): Boolean`
    *   `fun getAllSerializers(): Map<KClass<*>, ActorStateSerializer<*>>`

##### 3.1.6.4. `SerializableActorStateStorage` Interface
Extends `ActorStateStorage` to integrate specialized serialization for custom actor data.

*   **Purpose:** To provide methods for storing and retrieving typed objects within an actor's "customState" map, using registered `ActorStateSerializer`s.
*   **Inheritance:** Extends `io.github.solaceharmony.core.storage.ActorStateStorage`.
*   **Key Added Methods:**
    *   `fun getSerializerRegistry(): ActorStateSerializerRegistry`: Provides access to the associated serializer registry.
    *   `suspend fun <T : Any> serializeAndStore(actorId: String, key: String, obj: T, clazz: KClass<T>): Boolean`:
        1.  Retrieves the appropriate `ActorStateSerializer<T>` for `clazz` from the registry.
        2.  Serializes `obj` into a map.
        3.  Retrieves the actor's current data (or an empty map if new).
        4.  Stores the serialized map under the given `key` within the actor's "customState" field (which is itself a map).
        5.  Persists the updated actor data using the underlying `store` method.
    *   `suspend fun <T : Any> retrieveAndDeserialize(actorId: String, key: String, clazz: KClass<T>): T?`:
        1.  Retrieves the actor's data.
        2.  Accesses the "customState" map, then the serialized map under the given `key`.
        3.  Retrieves the appropriate `ActorStateSerializer<T>` for `clazz`.
        4.  Deserializes the map back into an object of type `T`.

##### 3.1.6.5. `DelegatingSerializableActorStateStorage` Class
A concrete implementation of `SerializableActorStateStorage` that uses the decorator pattern.

*   **Purpose:** To provide a ready-to-use `SerializableActorStateStorage` by wrapping an existing `ActorStateStorage` implementation and an `ActorStateSerializerRegistry`.
*   **Constructor:** `DelegatingSerializableActorStateStorage(private val delegate: ActorStateStorage, private val serializerRegistry: ActorStateSerializerRegistry = ActorStateSerializerRegistry())`
*   **Functionality:**
    *   Delegates all standard `ActorStateStorage` methods to the `delegate` instance.
    *   Implements the `serializeAndStore` and `retrieveAndDeserialize` methods using its `serializerRegistry`.
    *   The `init` block automatically registers an `ActorStateEnumSerializer` with its `serializerRegistry`.
*   **Factory Method:** A companion object provides `createInMemory(serializerRegistry: ActorStateSerializerRegistry = ...)` which conveniently creates an instance delegating to a new `InMemoryActorStateStorage`.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.actor" {
        class ActorState { <<Sealed>> }
    }

    package "io.github.solaceharmony.core.storage" {
        interface ActorStateStorage { <<Interface>> }
        class InMemoryActorStateStorage { }
        ActorStateStorage <|-- InMemoryActorStateStorage
    }

    package "io.github.solaceharmony.core.storage.serialization" {
        interface "ActorStateSerializer<T>" {
            <<Interface>>
            +serialize(obj: T): Map
            +deserialize(map: Map): T?
            +getType(): KClass<T>
        }

        class ActorStateEnumSerializer {
            +serialize(obj: ActorState): Map
            +deserialize(map: Map): ActorState?
            +getType(): KClass<ActorState>
        }
        "ActorStateSerializer" <|-- ActorStateEnumSerializer
        ActorStateEnumSerializer ..> ActorState

        class ActorStateSerializerRegistry {
            -serializers: Map<KClass, ActorStateSerializer>
            +registerSerializer(serializer: ActorStateSerializer)
            +getSerializer(clazz: KClass): ActorStateSerializer?
        }
        ActorStateSerializerRegistry o-- "ActorStateSerializer"

        interface SerializableActorStateStorage {
            <<Interface>>
            +getSerializerRegistry(): ActorStateSerializerRegistry
            +serializeAndStore(actorId, key, obj, clazz): Boolean
            +retrieveAndDeserialize(actorId, key, clazz): Any?
        }
        ActorStateStorage <|-- SerializableActorStateStorage

        class DelegatingSerializableActorStateStorage {
            -delegate: ActorStateStorage
            -serializerRegistry: ActorStateSerializerRegistry
            +init() // registers ActorStateEnumSerializer
        }
        SerializableActorStateStorage <|-- DelegatingSerializableActorStateStorage
        DelegatingSerializableActorStateStorage o-- ActorStateStorage : delegates to
        DelegatingSerializableActorStateStorage o-- ActorStateSerializerRegistry : uses

        DelegatingSerializableActorStateStorage ..> InMemoryActorStateStorage : can create (companion)
    }
```
This specialized serialization framework enhances the storage module by providing robust, type-safe handling for actor state data, particularly for custom objects within an actor's state.

---

← [§2 Lifecycle Module (`io.github.solaceharmony.core.lifecycle`)](Lifecycle-and-Resources)  ·  [Architecture Overview](Architecture-Overview)  ·  [§4 Actor Module (`io.github.solaceharmony.core.actor`)](Actor-System) →



[Back to Storage Module Architecture](Storage-Module-Architecture)
