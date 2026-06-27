<!-- topic: Runtime -->
<!-- title: Storage Caching Subsystem -->

#### 3.1.4. Storage Caching Subsystem (`io.github.solaceharmony.core.storage.cache`)
To enhance performance, the `storage` module includes a caching subsystem that can wrap existing `Storage` implementations. This subsystem is located in the `io.github.solaceharmony.core.storage.cache` package and comprises a generic caching storage decorator and various cache eviction policies.

##### 3.1.4.1. `CachePolicy<K, V>` Interface
This interface defines the standard contract for all cache management and eviction strategies.

*   **Purpose:** To allow different caching behaviors (like LRU, TTL) to be plugged into the `CachedStorage`.
*   **Generics:**
    *   `K`: The type of the key.
    *   `V`: The type of the value being cached.
*   **Key Methods:**
    *   `fun add(key: K, value: V): Boolean`: Adds or updates an entry in the cache.
    *   `fun get(key: K): V?`: Retrieves an entry from the cache; may return `null` if not found or expired.
    *   `fun remove(key: K): Boolean`: Removes an entry from the cache.
    *   `fun contains(key: K): Boolean`: Checks if an unexpired entry for the key exists.
    *   `fun clear(): Boolean`: Clears all entries from the cache.
    *   `fun size(): Int`: Returns the current number of entries in the cache.
    *   `fun maxSize(): Int`: Returns the maximum capacity of the cache (-1 for unlimited).
    *   `fun maintenance(): Boolean`: Performs periodic maintenance, like evicting expired entries.

##### 3.1.4.2. Concrete Cache Policies
SolaceCore provides two concrete implementations of `CachePolicy`:

###### 3.1.4.2.A. `LRUCachePolicy<K, V>`
Implements a Least Recently Used (LRU) eviction strategy.
*   **Constructor:** `LRUCachePolicy<K, V>(private val maxSize: Int)`
*   **Mechanism:** Uses a `LinkedHashMap` configured for access-order. When the cache exceeds `maxSize`, the least recently accessed item is removed upon adding a new item.
*   **Thread Safety:** Uses `java.util.concurrent.locks.ReentrantReadWriteLock`.
*   **Maintenance:** The `maintenance()` method is a no-op as eviction is handled during `add`.

###### 3.1.4.2.B. `TTLCachePolicy<K, V>`
Implements a Time-To-Live (TTL) eviction strategy.
*   **Constructor:** `TTLCachePolicy<K, V>(private val ttl: Duration, private val maxSize: Int = -1)`
*   **Mechanism:** Stores entries along with their creation timestamps in `ConcurrentHashMap`s. Entries are considered expired if `currentTime - creationTime > ttl`.
    *   Expired entries are removed during `get()`, `contains()`, or explicitly via `maintenance()`.
    *   If `maxSize` is enforced and the cache is full, `add()` first runs `maintenance()`. If still full, it evicts the oldest (earliest creation time) entry.
*   **Thread Safety:** Uses `java.util.concurrent.locks.ReentrantReadWriteLock`.

##### 3.1.4.3. `CachedStorage<K, V>` Class
This class acts as a decorator, adding caching functionality to an existing `Storage` implementation.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `CachedStorage<K, V>(private val storage: Storage<K, V>, private val cachePolicy: CachePolicy<K, Pair<V, Map<String, Any>>>)`
    *   It wraps an underlying `storage` instance.
    *   It uses a `cachePolicy` that stores `Pair<V, Map<String, Any>>`, meaning both the value and its associated metadata are cached together.
*   **Thread Safety:** Uses a `kotlinx.coroutines.sync.Mutex` to protect access to the `cachePolicy`.
*   **Operational Behavior:**
    *   **`store()`:** Writes to the underlying `storage` first. On success, it updates the `cachePolicy` with the new value and metadata.
    *   **`retrieve()`:** Attempts to fetch from `cachePolicy.get()`. If a valid (non-expired) entry is found, it's returned. Otherwise, it fetches from the underlying `storage`, adds the result (value and metadata) to the `cachePolicy`, and then returns it.
    *   **`delete()`:** Deletes from the underlying `storage`. On success, it removes the corresponding entry from `cachePolicy`.
    *   **`exists()`:** Checks `cachePolicy.contains()` first. If not found (or expired), it checks the underlying `storage`.
    *   **`updateMetadata()`:** Updates metadata in the underlying `storage`. On success, if the key exists in the cache, it updates the metadata part of the cached `Pair`.
    *   **`listKeys()`:** This operation is **not** cached and directly delegates to the underlying `storage.listKeys()`.
*   **Cache-Specific Methods:**
    *   `suspend fun clearCache(): Boolean`: Clears the cache via `cachePolicy.clear()`.
    *   `suspend fun maintenance(): Boolean`: Triggers `cachePolicy.maintenance()`.
    *   `suspend fun cacheSize(): Int`: Returns `cachePolicy.size()`.
    *   `suspend fun cacheMaxSize(): Int`: Returns `cachePolicy.maxSize()`.

A conceptual diagram of the caching subsystem:

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V>" {
            <<Interface>>
        }
    }

    package "io.github.solaceharmony.core.storage.cache" {
        interface "CachePolicy<K, V_CACHE>" {
            <<Interface>>
            +add(key: K, value: V_CACHE): Boolean
            +get(key: K): V_CACHE?
            +remove(key: K): Boolean
            +contains(key: K): Boolean
            +clear(): Boolean
            +size(): Int
            +maxSize(): Int
            +maintenance(): Boolean
        }

        class "LRUCachePolicy<K, V_CACHE>" {
            +LRUCachePolicy(maxSize: Int)
        }
        "CachePolicy" <|-- "LRUCachePolicy"

        class "TTLCachePolicy<K, V_CACHE>" {
            +TTLCachePolicy(ttl: Duration, maxSize: Int)
        }
        "CachePolicy" <|-- "TTLCachePolicy"

        class "CachedStorage<K, V_STORAGE>" {
            -storage: Storage<K, V_STORAGE>
            -cachePolicy: CachePolicy<K, Pair<V_STORAGE, Map<String, Any>>>
            +CachedStorage(storage, cachePolicy)
            +clearCache(): Boolean
            +maintenance(): Boolean
            +cacheSize(): Int
            +cacheMaxSize(): Int
        }
        "Storage<K, V_STORAGE>" <|-- "CachedStorage<K, V_STORAGE>"
        "CachedStorage" o-- "Storage" : decorates
        "CachedStorage" o-- "CachePolicy" : uses
    }
    note for "CachedStorage" "V_STORAGE is the type for the underlying storage,\nV_CACHE for CachePolicy here is Pair<V_STORAGE, Map>"
```
This caching layer provides a significant performance optimization opportunity by reducing load on the primary storage backends, with configurable eviction strategies.


[Back to Storage Module Architecture](Storage-Module-Architecture)
