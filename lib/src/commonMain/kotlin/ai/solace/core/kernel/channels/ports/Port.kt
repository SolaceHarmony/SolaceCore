package ai.solace.core.kernel.channels.ports

import ai.solace.core.lifecycle.Disposable
import kotlinx.coroutines.channels.Channel
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Represents a bidirectional communication port that can handle messages of type T.
 * @param T The type of messages this port handles
 */
interface Port<T : Any> : Disposable {
    val id: String
    val name: String
    val type: KClass<T>

    /**
     * Access to the underlying channel for direct operations
     */
    fun asChannel(): Channel<T>

    /**
     * Sends a message through the port, applying handlers and conversion rules.
     * @throws SendMessageException if sending fails
     */
    suspend fun send(message: T)

    /**
     * Generic message handler interface with type safety
     * @param IN The input message type
     * @param OUT The output message type
     */
    interface MessageHandler<in IN : Any, out OUT : Any> {
        suspend fun handle(message: IN): OUT
    }

    /**
     * Protocol adapter interface with enhanced error handling and type safety
     * @param SOURCE The source message type
     * @param TARGET The target message type
     */
    interface ProtocolAdapter<SOURCE : Any, TARGET : Any> {
        /**
         * Encodes the source message to target format
         * @throws PortException if encoding fails
         */
        suspend fun encode(source: SOURCE): TARGET

        /**
         * Decodes the target message back to source format
         * @throws PortException if decoding fails
         */
        suspend fun decode(target: TARGET): SOURCE

        /**
         * Verifies if this adapter can handle the given type combination
         */
        fun canHandle(sourceType: KClass<*>, targetType: KClass<*>): Boolean
    }

    companion object {
        fun generateId(): String = buildString {
            append("port-")
            append(Random.nextBytes(8).joinToString("") { "%02x".format(it) })
        }

        /**
         * Creates a connection between two ports with optional protocol adaptation
         * @throws PortConnectionException if connection validation fails
         */
        fun <IN : Any, OUT : Any, MID : Any> connect(
            source: Port<IN>,
            target: Port<OUT>,
            handlers: List<MessageHandler<IN, MID>> = emptyList(),
            protocolAdapter: ProtocolAdapter<MID, OUT>? = null,
            rules: List<ConversionRule<IN, OUT>> = emptyList()
        ): PortConnection<IN, OUT> = PortConnection(
            source,
            target,
            handlers,
            protocolAdapter,
            rules
        ).also { it.validateConnection() }
    }

    /**
     * Base class for conversion rules with enhanced error handling and type safety
     * @param IN The input type for conversion
     * @param OUT The output type after conversion
     */
    abstract class ConversionRule<in IN : Any, out OUT : Any> {
        /**
         * Converts input to output format
         * @throws PortException if conversion fails
         */
        abstract suspend fun convert(input: IN): OUT

        /**
         * Verifies if this rule can handle the given type combination
         */
        abstract fun canHandle(inputType: KClass<*>, outputType: KClass<*>): Boolean

        /**
         * Provides a description of the conversion rule
         */
        abstract fun describe(): String

        companion object {
            /**
             * Creates a test conversion rule with proper error handling
             */
            internal inline fun <reified IN : Any, reified OUT : Any> create(
                crossinline converter: suspend (IN) -> OUT,
                crossinline validator: (KClass<*>, KClass<*>) -> Boolean = { _, _ -> true },
                description: String = "Test conversion rule"
            ): ConversionRule<IN, OUT> = object : ConversionRule<IN, OUT>() {
                override suspend fun convert(input: IN): OUT = try {
                    converter(input)
                } catch (e: Exception) {
                    throw PortException.Validation("Conversion failed: ${e.message}")
                }

                override fun canHandle(inputType: KClass<*>, outputType: KClass<*>): Boolean =
                    validator(inputType, outputType)

                override fun describe(): String = description
            }
        }
    }

    /**
     * Connection between ports with enhanced validation and type safety
     */
    data class PortConnection<IN : Any, OUT : Any>(
        val sourcePort: Port<IN>,
        val targetPort: Port<OUT>,
        val handlers: List<MessageHandler<IN, *>>,
        val protocolAdapter: ProtocolAdapter<*, OUT>?,
        val rules: List<ConversionRule<IN, OUT>>
    ) {
        /**
         * Validates the connection configuration
         * @throws PortConnectionException if validation fails
         */
        fun validateConnection() {
            if (!canConnect()) {
                throw PortConnectionException(
                    sourceId = sourcePort.id,
                    targetId = targetPort.id,
                    message = buildConnectionErrorMessage()
                )
            }
        }

        private fun canConnect(): Boolean = when {
            sourcePort.type == targetPort.type -> true
            protocolAdapter?.canHandle(sourcePort.type, targetPort.type) == true -> true
            rules.isNotEmpty() -> validateConversionChain()
            else -> false
        }

        private fun validateConversionChain(): Boolean {
            var currentType: KClass<*> = sourcePort.type
            for (rule in rules) {
                if (!rule.canHandle(currentType, targetPort.type)) {
                    return false
                }
                currentType = targetPort.type as KClass<*>
            }
            return true
        }

        private fun buildConnectionErrorMessage(): String = buildString {
            append("Incompatible port types: ${sourcePort.type.simpleName} -> ${targetPort.type.simpleName}")
            if (protocolAdapter != null) {
                append(", protocol adapter validation failed")
            }
            if (rules.isNotEmpty()) {
                append(", invalid conversion chain: ")
                append(rules.joinToString(", ") { it.describe() })
            }
        }
    }
}