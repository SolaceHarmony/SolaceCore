package ai.solace.core.storage.compression

import ai.solace.core.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


/**
 * A wrapper class that adds compression capabilities to any Storage implementation.
 *
 * This class implements the Storage interface and delegates to the underlying storage
 * while compressing values before storing them and decompressing them after retrieval.
 * It uses GZIP compression by default, but can be configured to use other compression
 * algorithms through the CompressionStrategy interface.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 * @param storage The underlying storage implementation.
 * @param compressionStrategy The compression strategy to use.
 * @param compressionThreshold The minimum size (in bytes) for a value to be compressed.
 */
class CompressedStorage<K, V>(
    private val storage: Storage<K, V>,
    private val compressionStrategy: CompressionStrategy = GZIPCompressionStrategy(),
    private val compressionThreshold: Int = 1024, // Default to 1KB
    private val valueClass: Class<V> // Class object for V to handle type erasure
) : Storage<K, V> {
    /**
     * Mutex for thread-safe access to the compression operations.
     */
    private val mutex = Mutex()

    /**
     * Metadata key for storing whether a value is compressed.
     */
    private val compressedKey = "compressed"

    /**
     * Metadata key for storing the original size of a compressed value.
     */
    private val originalSizeKey = "originalSize"

    /**
     * Stores a value with the given key.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Create a mutable copy of the metadata
                val mutableMetadata = metadata.toMutableMap()

                // Check if the value should be compressed
                val shouldCompress = shouldCompress(value)

                if (shouldCompress) {
                    // Serialize and compress the value
                    @Suppress("UNCHECKED_CAST")
                    val serialized = compressionStrategy.serialize(value as Any)
                    val originalSize = serialized.size
                    
                    val compressed = compressionStrategy.compress(serialized)
                    
                    // Check if compression actually occurred (data got smaller)
                    val actuallyCompressed = compressed.size < serialized.size
                    mutableMetadata[compressedKey] = actuallyCompressed
                    mutableMetadata[originalSizeKey] = originalSize

                    // Store the compressed value (which might be the original if compression didn't help)
                    @Suppress("UNCHECKED_CAST")
                    storage.store(key, compressed as V, mutableMetadata)
                } else {
                    // Store the value without compression
                    mutableMetadata[compressedKey] = false
                    storage.store(key, value, mutableMetadata)
                }
            }
        }
    }

    /**
     * Retrieves a value with the given key.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Retrieve the value and metadata from the underlying storage
                val result = storage.retrieve(key) ?: return@withContext null
                val (storedValue, metadata) = result

                // Check if the value is compressed
                val isCompressed = metadata[compressedKey] as? Boolean ?: false

                if (isCompressed) {
                    // Decompress the value
                    @Suppress("UNCHECKED_CAST")
                    val compressed = storedValue as ByteArray
                    val decompressed = compressionStrategy.decompress(compressed)

                    // Use the valueClass parameter
                    @Suppress("UNCHECKED_CAST")
                    val value = compressionStrategy.deserialize(decompressed, valueClass)

                    // Return the decompressed value with the original metadata
                    Pair(value, metadata)
                } else {
                    // Return the value as is
                    result
                }
            }
        }
    }

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        return storage.listKeys()
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        return storage.delete(key)
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        return storage.exists(key)
    }

    /**
     * Updates the metadata for a value with the given key.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Retrieve the current metadata
                val currentResult = storage.retrieve(key) ?: return@withContext false
                val currentMetadata = currentResult.second

                // Preserve compression-related metadata
                val mutableMetadata = metadata.toMutableMap()
                if (currentMetadata.containsKey(compressedKey)) {
                    mutableMetadata[compressedKey] = currentMetadata[compressedKey]!!
                }
                if (currentMetadata.containsKey(originalSizeKey)) {
                    mutableMetadata[originalSizeKey] = currentMetadata[originalSizeKey]!!
                }

                // Update the metadata
                storage.updateMetadata(key, mutableMetadata)
            }
        }
    }

    /**
     * Checks if a value should be compressed.
     *
     * @param value The value to check.
     * @return True if the value should be compressed, false otherwise.
     */
    private suspend fun shouldCompress(value: V): Boolean {
        // If the value is already a ByteArray, check its size
        if (value is ByteArray) {
            return value.size >= compressionThreshold
        }

        // Otherwise, serialize the value and check its size
        @Suppress("UNCHECKED_CAST")
        val serialized = compressionStrategy.serialize(value as Any)
        return serialized.size >= compressionThreshold
    }

    /**
     * Gets the compression ratio for a value.
     *
     * @param key The key to identify the value.
     * @return The compression ratio (original size / compressed size), or 1.0 if the value is not compressed.
     */
    suspend fun getCompressionRatio(key: K): Double {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Retrieve the metadata
                val result = storage.retrieve(key) ?: return@withContext 1.0
                val metadata = result.second

                // Check if the value is compressed
                val isCompressed = metadata[compressedKey] as? Boolean ?: false
                if (!isCompressed) {
                    return@withContext 1.0
                }

                // Get the original size
                val originalSize = metadata[originalSizeKey] as? Int ?: return@withContext 1.0

                // Get the compressed size
                @Suppress("UNCHECKED_CAST")
                val compressed = result.first as ByteArray
                val compressedSize = compressed.size

                // Calculate the compression ratio
                originalSize.toDouble() / compressedSize.toDouble()
            }
        }
    }

    /**
     * Gets the compression threshold.
     *
     * @return The minimum size (in bytes) for a value to be compressed.
     */
    fun getCompressionThreshold(): Int {
        return compressionThreshold
    }

    /**
     * Gets the compression strategy.
     *
     * @return The compression strategy being used.
     */
    fun getCompressionStrategy(): CompressionStrategy {
        return compressionStrategy
    }
}
