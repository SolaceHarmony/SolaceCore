<!-- topic: Runtime -->
<!-- title: Storage JVM Serialization Utilities -->

#### 3.1.10. JVM-Specific Serialization Utilities (`storage/serialization` Subdirectory)
The `io.github.solaceharmony.core.storage.serialization` package in `jvmMain` contains utility classes for serialization on the JVM.

##### 3.1.10.1. `SerializationWrapper` Data Class
This data class provides a fallback mechanism for serializing objects that may not have explicit support from `kotlinx.serialization` or a custom registered serializer.

*   **Purpose:** To wrap an object's string representation (`toString()`) for serialization, typically when direct serialization of the object itself is not feasible or fails.
*   **Definition:**
    ```kotlin
    package io.github.solaceharmony.core.storage.serialization

    import kotlinx.serialization.Serializable

    @Serializable
    data class SerializationWrapper(val value: String)
    ```
*   **Usage:**
    *   It is used, for example, in `GZIPCompressionStrategy` as a last resort to serialize an object by taking its `toString()` output and storing it in the `value` field of `SerializationWrapper`.
    *   This wrapper can then be easily serialized to JSON (e.g., `{"value": "object_as_string"}`).
    *   Upon deserialization, one would retrieve the original object's string representation from the `value` property. This does not reconstruct the original object instance but provides its string form.

This utility ensures that a string representation can almost always be persisted, even for complex or non-standard objects, albeit with the loss of the original object's type and structure beyond its string form.


[Back to Storage Module Architecture](Storage-Module-Architecture)
