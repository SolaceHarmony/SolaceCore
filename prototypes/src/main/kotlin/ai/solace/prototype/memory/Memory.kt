package ai.solace.prototype.memory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * Memory interface for storing and retrieving chain state
 */
interface Memory {
    suspend fun get(key: String): Any?
    suspend fun set(key: String, value: Any)
    suspend fun delete(key: String)
    suspend fun clear()
    suspend fun getContext(): Map<String, Any>
}

/**
 * Simple in-memory implementation with conversation history
 */
class ConversationMemory(
    private val maxHistory: Int = 10,
    private val ttl: Duration? = null
) : Memory {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, MemoryEntry>()
    private val history = mutableListOf<HistoryEntry>()
    
    private data class MemoryEntry(
        val value: Any,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private data class HistoryEntry(
        val role: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    override suspend fun get(key: String): Any? = mutex.withLock {
        cleanExpired()
        store[key]?.value
    }
    
    override suspend fun set(key: String, value: Any) = mutex.withLock {
        store[key] = MemoryEntry(value)
        if (key == "conversation") {
            val entry = value as? HistoryEntry ?: return@withLock
            history.add(entry)
            if (history.size > maxHistory) {
                history.removeFirst()
            }
        }
    }
    
    override suspend fun delete(key: String) = mutex.withLock {
        store.remove(key)
    }
    
    override suspend fun clear() = mutex.withLock {
        store.clear()
        history.clear()
    }
    
    override suspend fun getContext(): Map<String, Any> = mutex.withLock {
        cleanExpired()
        mapOf(
            "memory" to store.toMap(),
            "history" to history.toList()
        )
    }
    
    private fun cleanExpired() {
        if (ttl == null) return
        val threshold = System.currentTimeMillis() - ttl.inWholeMilliseconds
        store.entries.removeIf { it.value.timestamp < threshold }
        history.removeIf { it.timestamp < threshold }
    }
    
    suspend fun addToHistory(role: String, content: String) = mutex.withLock {
        val entry = HistoryEntry(role, content)
        history.add(entry)
        if (history.size > maxHistory) {
            history.removeFirst()
        }
    }
}

/**
 * Vector store-backed memory for semantic search
 */
class VectorMemory(
    private val embedder: Embedder,
    private val store: VectorStore
) : Memory {
    private val mutex = Mutex()
    private val metadata = mutableMapOf<String, Any>()
    
    override suspend fun get(key: String): Any? = mutex.withLock {
        metadata[key]
    }
    
    override suspend fun set(key: String, value: Any) = mutex.withLock {
        metadata[key] = value
        if (value is String) {
            val embedding = embedder.embed(value)
            store.add(key, embedding, value)
        }
    }
    
    override suspend fun delete(key: String) = mutex.withLock {
        metadata.remove(key)
        store.delete(key)
    }
    
    override suspend fun clear() = mutex.withLock {
        metadata.clear()
        store.clear()
    }
    
    override suspend fun getContext(): Map<String, Any> = mutex.withLock {
        metadata.toMap()
    }
    
    suspend fun search(query: String, k: Int = 5): List<SearchResult> {
        val embedding = embedder.embed(query)
        return store.search(embedding, k)
    }
}

interface Embedder {
    suspend fun embed(text: String): FloatArray
}

interface VectorStore {
    suspend fun add(id: String, embedding: FloatArray, metadata: Any)
    suspend fun delete(id: String)
    suspend fun search(embedding: FloatArray, k: Int): List<SearchResult>
    suspend fun clear()
}

data class SearchResult(
    val id: String,
    val score: Float,
    val metadata: Any
)