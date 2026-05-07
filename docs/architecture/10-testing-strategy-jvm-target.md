[← Back to index](./README.md) · §10 of 15

---

## 10. Testing Strategy (JVM Target)
SolaceCore employs a structured approach to testing its components on the JVM, with dedicated test files located within the `lib/src/jvmTest/kotlin/ai/solace/core/` directory, mirroring the package structure of the main codebase. The primary testing framework appears to be JUnit Jupiter, used in conjunction with Kotlin Test utilities and `kotlinx-coroutines-test` for asynchronous code.

### 10.1. Module-Specific Tests:
*   **Actor Module (`actor/`):**
    *   `ActorTest.kt`: Focuses on unit testing the base `Actor` class, covering its lifecycle, state management, port operations, and error handling.
    *   `supervisor/SupervisorActorTest.kt`: Contains tests for the `SupervisorActor`, verifying its capabilities in managing actor registration, unregistration, hot-swapping, and collective lifecycle control.
    *   Functionality of `ActorMessage`, `ActorState`, `ActorBuilder`, and `ActorMetrics` is likely tested implicitly through `ActorTest.kt` and `SupervisorActorTest.kt`.

*   **Kernel Module (`kernel/`):**
    *   The test directory `kernel/channels/ports/` was found to be empty. This suggests that the core port functionalities defined in `commonMain` might be tested indirectly through higher-level components that utilize them (e.g., `ActorTest.kt`), or dedicated tests might be planned for the future.

*   **Lifecycle Module (`lifecycle/`):**
    *   The test directory for `lifecycle` was found to be empty. The `Disposable` and `Lifecycle` interfaces are fundamental contracts, and their correct implementation is likely verified within the tests of classes that implement them (e.g., `Actor`, `StorageManager`, `WorkflowManager`).

*   **Scripting Module (`scripting/`):**
    *   Demonstrates comprehensive test coverage for its JVM-specific implementations:
        *   `JvmScriptEngineTest.kt`: Tests the (currently simulated) JVM script engine.
        *   `FileScriptStorageTest.kt`: Tests file-based script persistence.
        *   `FileScriptVersionManagerTest.kt`: Tests file-based script versioning.
        *   `SimpleScriptValidatorTest.kt`: Tests the basic script validator.
        *   `ScriptManagerTest.kt`: Tests the orchestrating `ScriptManager`.

*   **Storage Module (`storage/`):**
    *   Exhibits extensive testing across its various facets:
        *   **Core Implementations:** Dedicated tests for both in-memory and file-based versions of `Storage`, `ActorStateStorage`, `ConfigurationStorage`, `TransactionalStorage`, and their respective `StorageManager` classes (e.g., `InMemoryStorageTest.kt`, `FileStorageTest.kt`, `TransactionalFileStorageTest.kt`).
        *   **Concurrency:** Specific concurrency tests for both `FileStorageManager` and `InMemoryStorageManager` (e.g., `FileStorageManagerConcurrencyTest.kt`).
        *   **Decorators & Strategies:** Each decorator and its strategies have dedicated tests:
            *   `cache/`: `CachedStorageTest.kt`, `LRUCachePolicyTest.kt`, `TTLCachePolicyTest.kt`.
            *   `compression/`: `CompressedStorageTest.kt`, `GZIPCompressionStrategyTest.kt`.
            *   `encryption/`: `EncryptedStorageTest.kt`, `AESEncryptionStrategyTest.kt`.
        *   **Recovery:** `recovery/ActorRecoveryManagerTest.kt` tests the actor state snapshot and recovery mechanisms.
        *   **Serialization:**
            *   `StorageSerializerRegistryTest.kt` (for the commonMain registry).
            *   `serialization/DelegatingSerializableActorStateStorageTest.kt` (for the JVM-specific actor state serialization).

*   **Workflow Module (`workflow/`):**
    *   `WorkflowManagerTest.kt`: Contains unit tests for the `WorkflowManager`, focusing on its lifecycle, actor and connection management.
    *   `WorkflowExample.kt`: Likely serves as an integration test or a runnable example showcasing the setup and execution of a complete actor workflow.

### 10.2. General Observations on Testing:
*   The project follows a convention of placing test classes in packages that mirror the source code they are testing.
*   There is a clear emphasis on testing concrete implementations, especially within the storage and scripting modules.
*   Special considerations like concurrency for storage managers are explicitly tested.
*   Fundamental interfaces like `Lifecycle` and `Port` appear to be tested via their implementing classes rather than through direct, isolated tests for the interfaces themselves.

This testing structure suggests a commitment to ensuring the reliability and correctness of SolaceCore's components, particularly for the critical storage and scripting functionalities on the JVM.

---

← [§9 JVM-Specific Utilities (`io.github.solaceharmony.core.util`)](./09-jvm-specific-utilities-io-github-solaceharmony-core-util.md)  ·  [Index](./README.md)  ·  [§11 Architectural Vision](./11-architectural-vision.md) →
