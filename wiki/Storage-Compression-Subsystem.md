<!-- topic: Runtime -->
<!-- title: Storage Compression Subsystem -->

#### 3.1.8. Storage Compression Subsystem (JVM-Specific)
The `io.github.solaceharmony.core.storage.compression` package in `jvmMain` provides a mechanism to transparently compress and decompress data being persisted through the `Storage` interface.

##### 3.1.8.1. `CompressionStrategy` Interface
This interface defines the contract for various compression and serialization algorithms.

*   **Purpose:** To allow pluggable strategies for data compression and the necessary serialization/deserialization steps before/after compression.
*   **Key Methods:**
    *   `fun compress(data: ByteArray): ByteArray`: Compresses the input byte array.
    *   `fun decompress(data: ByteArray): ByteArray`: Decompresses the input byte array.
    *   `fun serialize(value: Any): ByteArray`: Converts an arbitrary object into a byte array suitable for compression.
    *   `fun <T> deserialize(data: ByteArray, clazz: Class<T>): T`: Converts a byte array (typically after decompression) back into an object of type `T`, requiring the `Class<T>` due to JVM type erasure.

##### 3.1.8.2. `GZIPCompressionStrategy` Class
A concrete implementation of `CompressionStrategy` using the GZIP algorithm.

*   **Compression/Decompression:** Uses `java.util.zip.GZIPOutputStream` and `java.util.zip.GZIPInputStream`. The `compress` method only returns compressed data if it's smaller than the original.
*   **Serialization/Deserialization:**
    *   Uses `kotlinx.serialization.json.Json` (configured with `ignoreUnknownKeys = true`, `isLenient = true`).
    *   `serialize(value: Any)`: Handles `ByteArray`, `String`, and primitive types directly. For `Map<*, *>` it builds a `JsonObject`. Other types are attempted to be JSON serialized directly; on failure, it falls back to serializing `value.toString()` (potentially wrapped).
    *   `deserialize<T>(data: ByteArray, clazz: Class<T>)`: Handles `ByteArray`, `String`, and primitives. For `Map`, it manually parses the JSON. For other types, it attempts to deserialize a wrapped string or, as a last resort, returns the raw string if `clazz` is `String`.

##### 3.1.8.3. `CompressedStorage<K, V>` Class
A decorator class that wraps an existing `Storage<K, V>` implementation to add compression capabilities.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `CompressedStorage<K, V>(storage: Storage<K, V>, compressionStrategy: CompressionStrategy = GZIPCompressionStrategy(), compressionThreshold: Int = 1024, valueClass: Class<V>)`
    *   `storage`: The underlying storage instance.
    *   `compressionStrategy`: The strategy for compression/decompression and serialization/deserialization (defaults to `GZIPCompressionStrategy`).
    *   `compressionThreshold`: Values (in bytes, after serialization) smaller than this threshold will not be compressed (default 1KB).
    *   `valueClass: Class<V>`: Required for type-safe deserialization by the `CompressionStrategy`.
*   **Operation:**
    *   **`store()`:**
        1.  Serializes the value using `compressionStrategy.serialize()`.
        2.  If the serialized size meets the `compressionThreshold`, it compresses the data using `compressionStrategy.compress()`.
        3.  Stores special metadata keys: `COMPRESSED_KEY: Boolean` and `ORIGINAL_SIZE_KEY: Int`.
        4.  Delegates to the underlying `storage.store()` with the (potentially compressed) value and augmented metadata.
    *   **`retrieve()`:**
        1.  Retrieves data and metadata from the underlying `storage`.
        2.  Checks the `COMPRESSED_KEY` in metadata.
        3.  If compressed, it decompresses using `compressionStrategy.decompress()` and then deserializes using `compressionStrategy.deserialize(decompressedData, valueClass)`.
    *   Other `Storage` methods (`listKeys`, `delete`, `exists`) largely delegate to the underlying storage, with `updateMetadata` taking care to preserve compression-related metadata.
*   **Additional Functionality:** Provides methods like `getCompressionRatio(key)` to inspect compression effectiveness.
*   **Thread Safety:** Uses a `Mutex` for compression-related operations and `Dispatchers.IO` for underlying storage calls.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V>" { <<Interface>> }
    }

    package "io.github.solaceharmony.core.storage.compression" {
        interface CompressionStrategy {
            <<Interface>>
            +compress(data: ByteArray): ByteArray
            +decompress(data: ByteArray): ByteArray
            +serialize(value: Any): ByteArray
            +deserialize(data: ByteArray, clazz: Class<T>): T
        }

        class GZIPCompressionStrategy {
            +compress(data: ByteArray): ByteArray
            +decompress(data: ByteArray): ByteArray
            +serialize(value: Any): ByteArray
            +deserialize(data: ByteArray, clazz: Class<T>): T
        }
        CompressionStrategy <|-- GZIPCompressionStrategy

        class "CompressedStorage<K, V>" {
            -storage: Storage<K, V>
            -compressionStrategy: CompressionStrategy
            -compressionThreshold: Int
            -valueClass: Class<V>
            +store(key: K, value: V, metadata: Map): Boolean
            +retrieve(key: K): Pair<V, Map>?
        }
        "Storage<K, V>" <|-- "CompressedStorage<K, V>"
        "CompressedStorage" o-- "Storage" : decorates
        "CompressedStorage" o-- CompressionStrategy : uses
    }
```


[Back to Storage Module Architecture](Storage-Module-Architecture)
