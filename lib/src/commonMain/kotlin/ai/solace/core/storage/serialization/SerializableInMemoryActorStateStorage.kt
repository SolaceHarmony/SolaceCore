package ai.solace.core.storage.serialization

import ai.solace.core.actor.ActorState
import ai.solace.core.storage.ActorStateStorage
import ai.solace.core.storage.InMemoryActorStateStorage

/**
 * Delegating implementation of the SerializableActorStateStorage interface.
 *
 * This class implements SerializableActorStateStorage and delegates to an instance of ActorStateStorage
 * for the base functionality, providing serialization capabilities for actor state data.
 *
 * @param delegate The ActorStateStorage instance to delegate to.
 * @param serializerRegistry The registry of serializers to use for serialization/deserialization.
 */
class DelegatingSerializableActorStateStorage(
    private val delegate: ActorStateStorage,
    private val serializerRegistry: ActorStateSerializerRegistry = ActorStateSerializerRegistry()
) : SerializableActorStateStorage {

    /**
     * Gets the serializer registry.
     *
     * @return The serializer registry.
     */
    override fun getSerializerRegistry(): ActorStateSerializerRegistry {
        return serializerRegistry
    }

    /**
     * Initializes the storage with default serializers.
     */
    init {
        // Register default serializers
        serializerRegistry.registerSerializer(ActorStateEnumSerializer())
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun getActorState(actorId: String): ActorState? {
        return delegate.getActorState(actorId)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun setActorState(actorId: String, state: ActorState): Boolean {
        return delegate.setActorState(actorId, state)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>? {
        return delegate.getActorPorts(actorId)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean {
        return delegate.setActorPorts(actorId, ports)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun getActorMetrics(actorId: String): Map<String, Any>? {
        return delegate.getActorMetrics(actorId)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean {
        return delegate.setActorMetrics(actorId, metrics)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun getActorCustomState(actorId: String): Map<String, Any>? {
        return delegate.getActorCustomState(actorId)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean {
        return delegate.setActorCustomState(actorId, customState)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun store(key: String, value: Map<String, Any>, metadata: Map<String, Any>): Boolean {
        return delegate.store(key, value, metadata)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun retrieve(key: String): Pair<Map<String, Any>, Map<String, Any>>? {
        return delegate.retrieve(key)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun listKeys(): List<String> {
        return delegate.listKeys()
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun delete(key: String): Boolean {
        return delegate.delete(key)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun exists(key: String): Boolean {
        return delegate.exists(key)
    }

    /**
     * Delegates to the underlying ActorStateStorage implementation.
     */
    override suspend fun updateMetadata(key: String, metadata: Map<String, Any>): Boolean {
        return delegate.updateMetadata(key, metadata)
    }

    companion object {
        /**
         * Creates a new DelegatingSerializableActorStateStorage with an InMemoryActorStateStorage delegate.
         *
         * @param serializerRegistry The registry of serializers to use for serialization/deserialization.
         * @return A new DelegatingSerializableActorStateStorage instance.
         */
        fun createInMemory(serializerRegistry: ActorStateSerializerRegistry = ActorStateSerializerRegistry()): DelegatingSerializableActorStateStorage {
            return DelegatingSerializableActorStateStorage(InMemoryActorStateStorage(), serializerRegistry)
        }
    }
}
