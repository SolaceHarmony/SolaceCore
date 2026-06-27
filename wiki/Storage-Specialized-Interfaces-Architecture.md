<!-- topic: Runtime -->
<!-- title: Storage Specialized Interfaces Architecture -->

#### 3.1.2. Specialized Storage Interfaces
Building upon the generic `Storage<K, V>` interface, the module defines specialized contracts for common data types like actor state and configuration. Both of these specialized interfaces extend `Storage<String, Map<String, Any>>`, indicating they manage complex data structures (represented as maps) keyed by strings.

##### 3.1.2.1. `ActorStateStorage` Interface
This interface is dedicated to storing and retrieving all pertinent information related to an actor's state.

*   **Purpose:** To provide a tailored API for managing the persistence of actor states, including their core data, port configurations, metrics, and any custom state information.
*   **Inheritance:** Extends `Storage<String, Map<String, Any>>`. The `key` is typically the `actorId`.
*   **Key Specialized Methods:**
    *   `suspend fun getActorState(actorId: String): ActorState?`: Retrieves the primary state object for a given actor. (`ActorState` is defined in `io.github.solaceharmony.core.actor`).
    *   `suspend fun setActorState(actorId: String, state: ActorState): Boolean`: Sets the primary state object for an actor.
    *   `suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>?`: Retrieves the port configurations for an actor.
    *   `suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean`: Sets the port configurations for an actor.
    *   `suspend fun getActorMetrics(actorId: String): Map<String, Any>?`: Retrieves metrics associated with an actor.
    *   `suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean`: Sets metrics for an actor.
    *   `suspend fun getActorCustomState(actorId: String): Map<String, Any>?`: Retrieves any additional custom state data for an actor.
    *   `suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean`: Sets custom state data for an actor.
    *   It also inherits the standard `store`, `retrieve`, `delete`, etc., methods from `Storage<String, Map<String, Any>>` which can be used to manage the actor's entire persisted data map directly if needed.

##### 3.1.2.2. `ConfigurationStorage` Interface
This interface is designed for managing configuration data for the overall system and its individual components.

*   **Purpose:** To provide a structured API for storing and retrieving configuration parameters, supporting hierarchical data access.
*   **Inheritance:** Extends `Storage<String, Map<String, Any>>`. The `key` can represent a component ID or a system-level configuration identifier.
*   **Key Specialized Methods:**
    *   `suspend fun getConfigValue(key: String, path: String): Any?`: Retrieves a specific configuration value from within a configuration map using a dot-separated `path` (e.g., "database.connection.url").
    *   `suspend fun setConfigValue(key: String, path: String, value: Any): Boolean`: Sets a specific configuration value within a configuration map using a dot-separated `path`.
    *   `suspend fun getComponentConfig(componentId: String): Map<String, Any>?`: Retrieves the entire configuration map for a specific component.
    *   `suspend fun setComponentConfig(componentId: String, config: Map<String, Any>): Boolean`: Sets the entire configuration map for a component.
    *   `suspend fun getSystemConfig(): Map<String, Any>`: Retrieves the system-wide configuration map.
    *   `suspend fun setSystemConfig(config: Map<String, Any>): Boolean`: Sets the system-wide configuration map.
    *   Inherited methods from `Storage<String, Map<String, Any>>` allow direct management of entire configuration maps.



[Back to Storage Module Architecture](Storage-Module-Architecture)
