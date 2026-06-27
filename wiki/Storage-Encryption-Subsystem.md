<!-- topic: Runtime -->
<!-- title: Storage Encryption Subsystem -->

#### 3.1.9. Storage Encryption Subsystem (JVM-Specific)
SolaceCore provides a robust mechanism for encrypting data at rest within its storage module. This subsystem, located in the `io.github.solaceharmony.core.storage.encryption` package in `jvmMain`, ensures the confidentiality and integrity of stored values and their metadata.

##### 3.1.9.1. `EncryptionStrategy` Interface
This interface defines the fundamental contract for encryption and decryption operations, allowing for different cryptographic algorithms to be used.

*   **Purpose:** To abstract the specific encryption algorithm, enabling pluggable encryption strategies.
*   **Key Methods:**
    *   `fun encrypt(data: ByteArray): ByteArray`: Takes raw byte data and returns its encrypted form.
    *   `fun decrypt(data: ByteArray): ByteArray`: Takes encrypted byte data and returns its decrypted (original) form.

##### 3.1.9.2. `AESEncryptionStrategy` Class
A concrete implementation of `EncryptionStrategy` utilizing the Advanced Encryption Standard (AES).

*   **Algorithm:** Employs AES in Galois/Counter Mode (GCM) with no padding (`AES/GCM/NoPadding`). GCM provides both encryption and authentication (AEAD - Authenticated Encryption with Associated Data).
*   **Key Management:**
    *   Constructor: `AESEncryptionStrategy(private val key: SecretKey = generateKey())`. It can accept a `javax.crypto.SecretKey` or generate a 256-bit AES key by default.
    *   Companion object provides utilities: `generateKey()`, `createKeyFromBytes(keyBytes: ByteArray)`, and `createKeyFromBase64(keyBase64: String)`.
*   **Encryption Process (`encrypt()`):**
    1.  Generates a random 12-byte Initialization Vector (IV) required for GCM mode.
    2.  Initializes an AES cipher for encryption using the provided key, the generated IV, and a GCM tag length of 128 bits.
    3.  Encrypts the input data.
    4.  Returns a byte array containing the IV prepended to the ciphertext (`IV + Ciphertext`).
*   **Decryption Process (`decrypt()`):**
    1.  Extracts the 12-byte IV from the beginning of the input data.
    2.  Initializes the AES cipher for decryption using the key, the extracted IV, and the 128-bit GCM tag length.
    3.  Decrypts the remaining portion of the input data (the ciphertext).
    4.  GCM mode inherently verifies the authenticity tag during decryption, throwing an exception if the data has been tampered with or the key/IV is incorrect.

##### 3.1.9.3. `EncryptedStorage<K, V>` Class
A decorator class that wraps an existing `Storage` implementation to provide transparent encryption and decryption of stored data.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `EncryptedStorage(storage: Storage<K, ByteArray>, encryptionStrategy: EncryptionStrategy, valueSerializer: (V) -> String, valueDeserializer: (String) -> V)`
    *   `storage: Storage<K, ByteArray>`: The crucial point here is that the underlying storage **must** be capable of storing `ByteArray` values, as the encrypted content is a byte array.
    *   `encryptionStrategy: EncryptionStrategy`: The strategy used for cryptographic operations (e.g., an instance of `AESEncryptionStrategy`).
    *   `valueSerializer: (V) -> String`: A lambda function to serialize the original value of type `V` into a JSON string before it's encrypted. Defaults to a generic JSON serialization.
    *   `valueDeserializer: (String) -> V`: A lambda function to deserialize a JSON string (obtained after decryption) back into an object of type `V`. Defaults to a generic JSON deserialization.
*   **Operation:**
    *   **`store(key, value, metadata)`:**
        1.  The `value` (type `V`) is serialized to a JSON string using `valueSerializer`.
        2.  The `metadata` (type `Map<String, Any>`) is serialized to a JSON string.
        3.  These two JSON strings are combined into a single JSON object structure (e.g., `{"value": "...", "metadata": "..."}`).
        4.  This combined JSON string is converted to a `ByteArray`.
        5.  The byte array is encrypted using `encryptionStrategy.encrypt()`.
        6.  The resulting encrypted `ByteArray` is stored in the underlying `storage` instance (which is of type `Storage<K, ByteArray>`).
    *   **`retrieve(key)`:**
        1.  Retrieves the encrypted `ByteArray` from the underlying `storage`.
        2.  Decrypts it using `encryptionStrategy.decrypt()`.
        3.  Converts the decrypted byte array back to the combined JSON string.
        4.  Parses this JSON to extract the original value's JSON string and the metadata's JSON string.
        5.  Deserializes the value's JSON string back to type `V` using `valueDeserializer`.
        6.  Deserializes the metadata's JSON string back to `Map<String, Any>`.
        7.  Returns the deserialized `value` and `metadata`.
    *   Other methods like `listKeys()`, `delete()`, and `exists()` primarily delegate to the underlying storage, as keys themselves are not encrypted by this wrapper. `updateMetadata` involves a decrypt-update-encrypt cycle.
*   **Thread Safety:** Uses separate `Mutex` instances for `store`, `retrieve`, and `updateMetadata` operations to manage concurrent access, though cryptographic operations and JSON serialization/deserialization are often performed outside these specific storage locks.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V_OUT>" { <<Interface>> }
    }

    package "io.github.solaceharmony.core.storage.encryption" {
        interface EncryptionStrategy {
            <<Interface>>
            +encrypt(data: ByteArray): ByteArray
            +decrypt(data: ByteArray): ByteArray
        }

        class AESEncryptionStrategy {
            -key: SecretKey
            +AESEncryptionStrategy(key: SecretKey)
            +encrypt(data: ByteArray): ByteArray
            +decrypt(data: ByteArray): ByteArray
        }
        EncryptionStrategy <|-- AESEncryptionStrategy

        class "EncryptedStorage<K, V_APP>" {
            -storage: Storage<K, ByteArray>  // Underlying storage takes byte arrays
            -encryptionStrategy: EncryptionStrategy
            -valueSerializer: (V_APP) -> String
            -valueDeserializer: (String) -> V_APP
            +store(key: K, value: V_APP, metadata: Map): Boolean
            +retrieve(key: K): Pair<V_APP, Map>?
        }
        "Storage<K, V_APP>" <|-- "EncryptedStorage<K, V_APP>"
        "EncryptedStorage" o-- "Storage" : decorates (specifically Storage<K, ByteArray>)
        "EncryptedStorage" o-- EncryptionStrategy : uses
    }
    note for "EncryptedStorage" "V_APP is the application-level value type.\nInternally, it's serialized to JSON String, then to ByteArray, then encrypted."
```
This encryption layer provides a robust mechanism for securing sensitive data within the storage system, ensuring that both values and their metadata are protected.


[Back to Storage Module Architecture](Storage-Module-Architecture)
