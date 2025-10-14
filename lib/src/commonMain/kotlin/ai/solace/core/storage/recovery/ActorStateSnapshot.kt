package ai.solace.core.storage.recovery

import ai.solace.core.actor.ActorState
import kotlinx.datetime.Clock

/**
 * Represents a snapshot of an actor's state.
 *
 * This class encapsulates all the state data of an actor at a specific point in time,
 * including its basic state, port configurations, metrics, and custom state data.
 *
 * @property actorId The ID of the actor.
 * @property actorName The name of the actor.
 * @property state The state of the actor.
 * @property ports The port configurations of the actor.
 * @property metrics The metrics of the actor.
 * @property customState The custom state of the actor.
 * @property version The version of the actor state.
 * @property timestamp The timestamp when the snapshot was created.
 */
data class ActorStateSnapshot(
    val actorId: String,
    val actorName: String,
    val state: ActorState,
    val ports: Map<String, Map<String, Any>>,
    val metrics: Map<String, Any>,
    val customState: Map<String, Any>,
    val version: Int,
    val timestamp: Long
) {
    /**
     * Creates a new builder for creating ActorStateSnapshot instances.
     *
     * @return A new ActorStateSnapshotBuilder instance.
     */
    companion object {
        /**
         * Creates a new builder for creating ActorStateSnapshot instances.
         *
         * @param actorId The ID of the actor.
         * @return A new ActorStateSnapshotBuilder instance.
         */
        fun builder(actorId: String): ActorStateSnapshotBuilder {
            return ActorStateSnapshotBuilder(actorId)
        }
    }
}

/**
 * Builder for creating ActorStateSnapshot instances.
 *
 * This class provides a fluent API for creating ActorStateSnapshot instances.
 *
 * @property actorId The ID of the actor.
 */
class ActorStateSnapshotBuilder(private val actorId: String) {
    private var actorName: String = "Unknown"
    private var state: ActorState = ActorState.Initialized
    private var ports: Map<String, Map<String, Any>> = emptyMap()
    private var metrics: Map<String, Any> = emptyMap()
    private var customState: Map<String, Any> = emptyMap()
    private var version: Int = 1
    private var timestamp: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Sets the name of the actor.
     *
     * @param name The name of the actor.
     * @return This builder instance.
     */
    fun withName(name: String): ActorStateSnapshotBuilder {
        this.actorName = name
        return this
    }

    /**
     * Sets the state of the actor.
     *
     * @param state The state of the actor.
     * @return This builder instance.
     */
    fun withState(state: ActorState): ActorStateSnapshotBuilder {
        this.state = state
        return this
    }

    /**
     * Sets the port configurations of the actor.
     *
     * @param ports The port configurations of the actor.
     * @return This builder instance.
     */
    fun withPorts(ports: Map<String, Map<String, Any>>): ActorStateSnapshotBuilder {
        this.ports = ports
        return this
    }

    /**
     * Sets the metrics of the actor.
     *
     * @param metrics The metrics of the actor.
     * @return This builder instance.
     */
    fun withMetrics(metrics: Map<String, Any>): ActorStateSnapshotBuilder {
        this.metrics = metrics
        return this
    }

    /**
     * Sets the custom state of the actor.
     *
     * @param customState The custom state of the actor.
     * @return This builder instance.
     */
    fun withCustomState(customState: Map<String, Any>): ActorStateSnapshotBuilder {
        this.customState = customState
        return this
    }

    /**
     * Sets the version of the actor state.
     *
     * @param version The version of the actor state.
     * @return This builder instance.
     */
    fun withVersion(version: Int): ActorStateSnapshotBuilder {
        this.version = version
        return this
    }

    /**
     * Sets the timestamp when the snapshot was created.
     *
     * @param timestamp The timestamp when the snapshot was created.
     * @return This builder instance.
     */
    fun withTimestamp(timestamp: Long): ActorStateSnapshotBuilder {
        this.timestamp = timestamp
        return this
    }

    /**
     * Builds a new ActorStateSnapshot instance with the configured properties.
     *
     * @return A new ActorStateSnapshot instance.
     */
    fun build(): ActorStateSnapshot {
        return ActorStateSnapshot(
            actorId = actorId,
            actorName = actorName,
            state = state,
            ports = ports,
            metrics = metrics,
            customState = customState,
            version = version,
            timestamp = timestamp
        )
    }
}
