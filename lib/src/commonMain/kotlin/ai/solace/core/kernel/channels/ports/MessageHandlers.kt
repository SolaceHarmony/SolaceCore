package ai.solace.core.kernel.channels.ports

import kotlin.reflect.KClass

/**
 * Type-safe protocol adapter for string-based messages with proper interface implementation
 */
open class StringProtocolAdapter<T : Any> : Port.ProtocolAdapter<T, String> {
    override suspend fun encode(source: T): String =
        source.toString()

    override suspend fun decode(target: String): T =
        throw UnsupportedOperationException("Decode operation must be implemented by concrete classes")

    override fun canHandle(sourceType: KClass<*>, targetType: KClass<*>): Boolean =
        targetType == String::class

    companion object {
        inline fun <reified T : Any> create(
            crossinline decoder: (String) -> T
        ): Port.ProtocolAdapter<T, String> = object : StringProtocolAdapter<T>() {
            override suspend fun decode(target: String): T = decoder(target)

            override fun canHandle(sourceType: KClass<*>, targetType: KClass<*>): Boolean =
                targetType == String::class && sourceType == T::class
        }
    }
}