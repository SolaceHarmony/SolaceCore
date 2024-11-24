package ai.solace.core.kernel.channels.ports

import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

/**
 * Implementation of a bidirectional port with type-safe message handling and conversion.
 *
 * @param T The type of messages this port handles
 * @property name The name of the port
 * @property id Unique identifier for the port
 * @property type The Kotlin class representing type T
 * @property channel The underlying message channel
 */
class BidirectionalPort<T : Any>(
    override val name: String,
    override val id: String = Port.generateId(),
    override val type: KClass<T>,
    private val channel: Channel<T> = Channel(Channel.BUFFERED)
) : Port<T> {
    private val handlers = mutableListOf<Port.MessageHandler<T, T>>()
    private val conversionRules = mutableListOf<Port.ConversionRule<T, T>>()

    /**
     * Returns the underlying channel for this port
     */
    override fun asChannel(): Channel<T> = channel

    /**
     * Adds a message handler to the port.
     * Handlers are applied in the order they are added.
     *
     * @param handler The message handler to add
     */
    fun addHandler(handler: Port.MessageHandler<T, T>) {
        handlers.add(handler)
    }

    /**
     * Adds a conversion rule to the port.
     * Rules are applied in the order they are added, after handlers.
     *
     * @param rule The conversion rule to add
     */
    fun addConversionRule(rule: Port.ConversionRule<T, T>) {
        conversionRules.add(rule)
    }

    /**
     * Sends a message through this port, applying handlers and conversion rules in sequence.
     *
     * @param message The message to send
     * @throws PortException if message processing fails
     */
    override suspend fun send(message: T) {
        try {
            var processedMessage = message

            // Apply message handlers
            for (handler in handlers) {
                processedMessage = handler.handle(processedMessage)
            }

            // Apply conversion rules
            for (rule in conversionRules) {
                processedMessage = rule.convert(processedMessage)
            }

            channel.send(processedMessage)
        } catch (e: Exception) {
            throw PortException.Validation("Failed to send message: ${e.message}", e)
        }
    }

    /**
     * Receives a message from this port.
     *
     * @return The received message
     * @throws PortException if receive fails
     */
    suspend fun receive(): T = try {
        channel.receive()
    } catch (e: Exception) {
        throw PortException.Validation("Failed to receive message: ${e.message}", e)
    }

    /**
     * Disposes of the port's resources by closing the underlying channel.
     */
    override suspend fun dispose() {
        channel.close()
    }

    override fun toString(): String = "BidirectionalPort(id=$id, name=$name, type=${type.simpleName})"

    companion object {
        /**
         * Creates a new BidirectionalPort for the specified type.
         */
        inline fun <reified T : Any> create(
            name: String,
            id: String = Port.generateId()
        ): BidirectionalPort<T> = BidirectionalPort(
            name = name,
            id = id,
            type = T::class
        )
    }
}