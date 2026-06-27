<!-- topic: Runtime -->
<!-- title: Storage Serialization Compression Encryption -->

### JSON Serialization/Deserialization Rules

File-based storages and script metadata use kotlinx.serialization JSON with robust handling of nested structures:
- Nested Map/List values are serialized recursively to `JsonObject`/`JsonArray`.
- Number parsing prefers `Int` when possible, then `Long`, then `Double`.
- Booleans parse from case-insensitive "true"/"false"; other primitives remain strings.

### Compression and Encryption Wrappers

#### CompressedStorage<K, V>
Wraps a `Storage<K, V>` and compresses values larger than a configurable threshold. Metadata records:
- `compressed = true` (when threshold met)
- `originalSize = <bytes>` of the serialized (pre-compression) value

On retrieval, values are decompressed and deserialized back to `V`.

#### EncryptedStorage<K, V>
Wraps a `Storage<K, V>` and encrypts values at rest using an `EncryptionStrategy` (e.g., AES). On retrieval, values are decrypted before deserialization.

Note: Key management and rotation are deployment concerns and should be handled by the host application.


[Back to Storage & Persistence](Storage-and-Persistence)
