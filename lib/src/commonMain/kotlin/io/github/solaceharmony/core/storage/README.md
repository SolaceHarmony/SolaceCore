# Storage module — `io.github.solaceharmony.core.storage`

Persistence substrate for SolaceCore. Generic typed key/value storage, transactions, caching, recovery, serialization, and (on JVM) file-backed implementations with optional compression and encryption.

This module is the foundation for the **Reflection Memory** that the Solace cognition layer will need (append-only narrative log, dual-keyed by timestamp and signature). The primitives are shipped; the higher-level Reflection Memory abstraction is designed but not yet built — see [`docs/components/memory/MemoryToolDesign.md`](../../../../../../../../docs/components/memory/MemoryToolDesign.md).

## Layered design

```
                     ┌───────────────────────────┐
                     │  TransactionalStorage<K,V>│
                     │  (begin / commit / rollback)
                     └────────────▲──────────────┘
                                  │ (optional)
   ┌──────────────────────────────┴───────────────────────────────┐
   │                   Storage<K, V>                              │
   │   suspend store / retrieve / delete / exists / listKeys      │
   │   suspend updateMetadata                                     │
   └─▲─────────────────────────▲─────────────────────────▲────────┘
     │                         │                         │
     │                         │                         │
  ┌──┴────────────┐    ┌───────┴──────────┐    ┌─────────┴─────────┐
  │ InMemory…     │    │ Cached…          │    │ Recoverable…      │
  │ (volatile)    │    │ (LRU / TTL)      │    │ (snapshots)       │
  └───────────────┘    └──────────────────┘    └───────────────────┘

  JVM-only adds File…, Compressed…, Encrypted… wrappers
  and the FileStorageManager.
```

`StorageManager` registers per-(K,V,name) `Storage<K, V>` instances under a name and serves `getStorage(...)`/`registerStorage(...)`/`unregisterStorage(...)` with mutex protection. Use `KClass<K>`/`KClass<V>` (the API is KMP-friendly).

## Public surface

### Common (`commonMain`)

| Type | Role |
|---|---|
| `Storage<K, V>` | Suspending CRUD over typed keys/values with associated metadata. |
| `TransactionalStorage<K, V>` | Adds `beginTransaction`, `commit`, `rollback` over a `Storage`. |
| `Transaction<K, V>` | The active transaction handle. |
| `InMemoryStorage<K, V>` | Volatile reference implementation. |
| `TransactionalInMemoryStorage<K, V>` | In-memory + transactions. |
| `StorageManager` | Per-(KClass, KClass, name) storage registry, mutex-locked. |
| `InMemoryStorageManager` | Default in-memory `StorageManager`. |
| `StorageSerializer<T>` | The serializer contract for `T`. |
| `ActorStateStorage`, `InMemoryActorStateStorage` | Specialized storage for `ActorState`. |
| `ConfigurationStorage`, `InMemoryConfigurationStorage` | Specialized storage for actor/system configuration. |
| `cache.CachePolicy` | Eviction strategy contract. |
| `cache.LRUCachePolicy`, `cache.TTLCachePolicy` | Concrete eviction strategies. |
| `cache.CachedStorage<K, V>` | Wraps any `Storage<K, V>` with a `CachePolicy`. |
| `recovery.ActorStateSnapshot` | Point-in-time snapshot record. |
| `recovery.RecoverableActorStateStorage` | Storage with snapshot/restore on top of `ActorStateStorage`. |
| `recovery.ActorRecoveryManager` | Coordinates recovery operations across actors. |
| `serialization.SerializableActorStateStorage` | `ActorStateStorage` with explicit serialization. |
| `serialization.SerializableInMemoryActorStateStorage` | In-memory variant of the above. |
| `serialization.ActorStateSerializer`, `ActorStateEnumSerializer`, `ActorStateSerializerRegistry` | Serializer infrastructure for `ActorState`. |

### JVM (`jvmMain`)

| Type | Role |
|---|---|
| `FileStorage<K, V>` | Disk-backed `Storage` using JSON serialization. Provides `createJsonValue` / `parseJsonValue` helpers for nested maps/lists. |
| `TransactionalFileStorage<K, V>` | File-backed transactional storage. |
| `FileStorageManager` | Disk-backed `StorageManager`. |
| `FileActorStateStorage`, `FileConfigurationStorage` | File-backed specialized stores. |
| `compression.CompressionStrategy`, `GZIPCompressionStrategy`, `compression.CompressedStorage` | Optional compression wrapper. |
| `encryption.EncryptionStrategy`, `AESEncryptionStrategy`, `encryption.EncryptedStorage` | Optional AES-encrypted wrapper. |
| `serialization.SerializationWrapper` | JVM-side serialization helper. |

## Composing storages

Wrappers nest in any order. Common stacks:

```kotlin
val raw          = InMemoryStorage<String, ActorState>()
val transactional = TransactionalInMemoryStorage<String, ActorState>()
val cached       = CachedStorage(transactional, LRUCachePolicy(maxEntries = 1024))

// JVM-only:
val onDisk       = FileStorage<String, ActorState>(directory = file("./state"))
val compressed   = CompressedStorage(onDisk, GZIPCompressionStrategy)
val encrypted    = EncryptedStorage(compressed, AESEncryptionStrategy(key))
```

The `TransactionalStorage` interface is orthogonal to caching/compression/encryption — wrap in whatever order matches your durability and confidentiality needs.

## Recovery

`recovery.RecoverableActorStateStorage` adds snapshotting on top of `ActorStateStorage`. The intended workflow:

1. Actor enters a known-good state. Take a snapshot via the recovery manager.
2. Actor processes messages and updates its state through the underlying storage.
3. On crash or unrecoverable error, restore from the most recent snapshot.

`ActorRecoveryManager` coordinates this across multiple actors so the supervisor can restart a subset cleanly.

## See also

- [`../actor/`](../actor/) — the actor runtime whose state this module persists.
- [`../scripting/`](../scripting/) — uses `Storage` for compiled-script artifacts and version metadata.
- [`docs/STORAGE_DOCUMENTATION.md`](../../../../../../../../docs/STORAGE_DOCUMENTATION.md) — long-form guidance and JSON coercion rules.
- [`docs/STORAGE_CHECKLIST.md`](../../../../../../../../docs/STORAGE_CHECKLIST.md) — backlog and known gaps.
- [`docs/components/memory/MemoryToolDesign.md`](../../../../../../../../docs/components/memory/MemoryToolDesign.md) — the Reflection Memory design that will be built on top of this substrate.
