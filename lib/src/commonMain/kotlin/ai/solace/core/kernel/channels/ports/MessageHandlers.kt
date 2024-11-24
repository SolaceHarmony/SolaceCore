package ai.solace.core.kernel.channels.ports

/**
 * Type-safe message handler for String messages.
 * Adds timestamp to string messages.
 */
class StringMessageHandler : Port.MessageHandler<String, String> {
    override suspend fun handle(message: String): String {
        return "[${System.currentTimeMillis()}] $message"
    }
}

/**
 * Generic logging message handler that can work with any type.
 */
class LoggingHandler<T : Any> : Port.MessageHandler<T, T> {
    override suspend fun handle(message: T): T {
        println("[${System.currentTimeMillis()}] Processing message: $message")
        return message
    }
}

/**
 * Null-safe message handler that filters out null values from nullable types.
 */
class NullSafeHandler<in T : Any> : Port.MessageHandler<T, @UnsafeVariance T> {
    override suspend fun handle(message: T): @UnsafeVariance T {
        return requireNotNull(message) { "Message cannot be null" }
    }
}

/**
 * Type-safe conversion rule for String transformations.
 */
class StringConversionRule : Port.ConversionRule<String, String>() {
    override suspend fun convert(input: String): String = input.uppercase()

    override fun canHandle(inputType: Any, outputType: Any): Boolean =
        inputType == String::class && outputType == String::class

    override fun describe(): String = "Converts string to uppercase"
}

/**
 * Chain handler that combines multiple handlers into a single processing chain.
 */
class ChainHandler<T : Any>(
    private val handlers: List<Port.MessageHandler<T, T>>
) : Port.MessageHandler<T, T> {
    override suspend fun handle(message: T): T {
        return handlers.fold(message) { acc, handler ->
            handler.handle(acc)
        }
    }

    companion object {
        fun <T : Any> of(vararg handlers: Port.MessageHandler<T, T>): ChainHandler<T> =
            ChainHandler(handlers.toList())
    }
}