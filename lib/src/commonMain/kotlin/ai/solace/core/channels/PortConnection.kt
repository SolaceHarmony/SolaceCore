package ai.solace.core.channels

import ai.solace.core.common.Disposable
import kotlin.reflect.KClass

/**
 * Represents a connection between ports
 */
data class PortConnection<T : Any>(
    val sourcePortId: String,
    val targetPortId: String,
    val type: KClass<T>,
    val id: String = Port.generateId()
) : Disposable {
    override suspend fun dispose() {
        // Connection cleanup logic
    }

    companion object {
        fun <T : Any> create(
            source: OutputPort<T>,
            target: InputPort<T>
        ): PortConnection<T> {
            require(source.type == target.type) {
                throw PortTypeException(
                    expectedType = source.type.qualifiedName ?: "",
                    actualType = target.type.qualifiedName ?: ""
                )
            }

            return PortConnection(
                sourcePortId = source.id,
                targetPortId = target.id,
                type = source.type
            )
        }
    }
}