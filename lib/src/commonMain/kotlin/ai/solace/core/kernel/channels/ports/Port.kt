@file:OptIn(ExperimentalUuidApi::class)
package ai.solace.core.kernel.channels.ports

import ai.solace.core.lifecycle.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a communication port that can send and receive messages of type T.
 */
interface Port<T : Any> : Disposable {
    /**
     * Unique identifier for the port.
     *
     * The `id` is used to uniquely identify and differentiate ports within the system.
     */
    val id: String
    /**
     * The name of the port.
     */
    val name: String
    /**
     * Represents the Kotlin class type of messages this port handles.
     *
     * This property is used to ensure type-safety in message handling,
     * conversion, and validation processes across ports.
     */
    val type: KClass<out T>

    /**
     * Returns the underlying channel for this port.
     *
     * @return The channel associated with this port.
     */
    fun asChannel(): Channel<T>
    /**
     * Sends a message through the port.
     *
     * @param message The message to send through the port.
     * @throws PortException.Validation if message processing fails.
     */
    suspend fun send(message: T)

    /**
     * Interface defining a handler for processing messages.
     *
     * The `MessageHandler` interface provides a contract for handling messages
     * of type `IN` and producing results of type `OUT`. Implementations of this
     * interface must define the behavior for the `handle` function, which processes
     * the given message and returns the result.
     *
     * @param IN The type of input messages this handler can process.
     * @param OUT The type of output this handler produces.
     */
    interface MessageHandler<in IN : Any, out OUT : Any> {
        /**
         * Handles the provided message and returns a result of type OUT.
         *
         * @param message The message to be handled.
         * @return The result of handling the message.
         */
        suspend fun handle(message: IN): OUT
    }

    /**
     * A ProtocolAdapter facilitates the conversion between different types of protocols.
     * It can encode a source type to a target type and decode a target type back to the source type.
     * Additionally, it provides a method to verify if the adapter can handle specific source and target types.
     *
     * @param SOURCE The source type for the protocol conversion.
     * @param TARGET The target type for the protocol conversion.
     */
    interface ProtocolAdapter<SOURCE : Any, TARGET : Any> {
        /**
         * Encodes a source object of type SOURCE into a target object of type TARGET.
         *
         * This function is typically used in scenarios where data needs to be transformed
         * between different formats or protocols. The exact encoding logic is determined
         * by the implementing class.
         *
         * @param source The source object to be encoded.
         * @return The encoded object of type TARGET.
         */
        suspend fun encode(source: SOURCE): TARGET
        /**
         * Decodes the given target of type TARGET into a source object of type SOURCE.
         *
         * @param target The target object to be decoded.
         * @return The decoded source object.
         */
        suspend fun decode(target: TARGET): SOURCE
        /**
         * Determines if the specified protocol adapter can handle conversion
         * between the given source type and target type.
         *
         * @param sourceType The Kotlin class of the source type.
         * @param targetType The Kotlin class of the target type.
         * @return `true` if the protocol adapter can handle conversion between
         *         the source and target types, `false` otherwise.
         */
        fun canHandle(sourceType: KClass<*>, targetType: KClass<*>): Boolean
    }

    /**
     * Represents an abstract rule for converting input of type IN to output of type OUT.
     */
    abstract class ConversionRule<in IN : Any, out OUT : Any> {
        /**
         * Converts an input of type `IN` to an output of type `OUT`.
         * The conversion process is potentially time-consuming and should be run in a coroutine context.
         *
         * @param input The input value of type `IN` to be converted.
         * @return The converted value of type `OUT`.
         * @throws PortException.Validation if the conversion fails.
         */
        abstract suspend fun convert(input: IN): OUT
        /**
         * Determines if the current rule can handle conversion between the specified input and output types.
         *
         * @param inputType The type of the input data.
         * @param outputType The type of the output data.
         * @return `true` if the rule can handle conversion between the specified types, otherwise `false`.
         */
        abstract fun canHandle(inputType: KClass<*>, outputType: KClass<*>): Boolean
        /**
         * Provides a description of the conversion rule.
         *
         * @return A string describing the conversion rule.
         */
        abstract fun describe(): String

        /**
         * Companion object that provides utility functions for creating ConversionRule instances.
         */
        companion object {
            /**
             * Creates an instance of [ConversionRule] with specified converter and validator functions.
             *
             * @param converter The suspend function used to convert the input from type [IN] to type [OUT].
             * @param validator The function to validate if the conversion can be handled for the given input and output types.
             *        It takes the input type's [KClass] and the output type's [KClass] and returns a boolean.
             *        Default implementation always returns true.
             * @param description A string describing the conversion rule. Defaults to "Test conversion rule".
             * @return A new instance of [ConversionRule] configured with the provided converter, validator, and description.
             */
            internal inline fun <reified IN : Any, reified OUT : Any> create(
                crossinline converter: suspend (IN) -> OUT,
                crossinline validator: (KClass<*>, KClass<*>) -> Boolean = { _, _ -> true },
                description: String = "Test conversion rule"
            ): ConversionRule<IN, OUT> = object : ConversionRule<IN, OUT>() {
                /**
                 * Converts the input of type IN to an output of type OUT.
                 *
                 * This method attempts to convert the provided input using a predefined converter.
                 * If the conversion fails, a PortException.Validation exception is thrown indicating
                 * the failure of the conversion process.
                 *
                 * @param input The input object to be converted.
                 * @return The converted output object.
                 * @throws PortException.Validation if the conversion process fails.
                 */
                override suspend fun convert(input: IN): OUT = try {
                    converter(input)
                } catch (e: Exception) {
                    throw PortException.Validation("Conversion failed: ${e.message}")
                }

                /**
                 * Checks if the current rule can handle the given input and output types.
                 *
                 * @param inputType The type of the input data that the rule will process.
                 * @param outputType The type of the output data that the rule will produce.
                 * @return True if the rule can handle the specified types, otherwise false.
                 */
                override fun canHandle(inputType: KClass<*>, outputType: KClass<*>): Boolean =
                    validator(inputType, outputType)

                /**
                 * Provides a description of the conversion rule.
                 *
                 * @return A string describing the conversion rule.
                 */
                override fun describe(): String = description
            }
        }
    }

    /**
     * Represents a connection between two ports, defining the data flow from a source port to a target port.
     *
     * @param IN The type of the input data handled by the source port.
     * @param OUT The type of the output data produced by the target port.
     * @property sourcePort The port where the data originates.
     * @property targetPort The port where the data is sent to.
     * @property handlers A list of message handlers to process the data before reaching the target port.
     * @property protocolAdapter An optional adapter for protocol conversion between source and target ports.
     * @property rules A list of conversion rules for transforming the data from the source to the target type.
     */
    data class PortConnection<in IN : Any, out OUT : Any>(
        val sourcePort: Port<@UnsafeVariance IN>,
        val targetPort: Port<@UnsafeVariance OUT>,
        val handlers: List<MessageHandler<IN, Any>>,
        val protocolAdapter: ProtocolAdapter<*, @UnsafeVariance OUT>?,
        val rules: List<ConversionRule<IN, OUT>>
    ) {
        private var routingJob: Job? = null
        private val jobMutex = Mutex()

        /** Starts routing messages from the source port to the target port. */
        fun start(scope: CoroutineScope): Job {
            val job = scope.launch {
                for (msg in sourcePort.asChannel()) {
                    var intermediate: Any = msg
                    @Suppress("UNCHECKED_CAST")
                    for (handler in handlers as List<MessageHandler<Any, Any>>) {
                        intermediate = handler.handle(intermediate)
                    }

                    @Suppress("UNCHECKED_CAST")
                    var out: Any = if (protocolAdapter != null) {
                        (protocolAdapter as ProtocolAdapter<Any, Any>).encode(intermediate)
                    } else {
                        intermediate
                    }

                    if (rules.isNotEmpty()) {
                        for (rule in rules as List<ConversionRule<Any, Any>>) {
                            out = rule.convert(out)
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    try {
                        targetPort.send(out as OUT)
                    } catch (e: Exception) {
                        // Target channel likely closed; stop routing gracefully
                        return@launch
                    }
                }
            }
            // publish job in a synchronized manner
            // (avoid races with stop/stopAndJoin)
            kotlinx.coroutines.runBlocking {
                jobMutex.withLock { routingJob = job }
            }
            return job
        }

        /** Stops routing messages between the ports. */
        fun stop() {
            // cancel under lock; non-blocking
            kotlinx.coroutines.runBlocking {
                jobMutex.withLock {
                    routingJob?.cancel()
                    routingJob = null
                }
            }
        }

        /**
         * Cancels the routing coroutine and waits for it to finish.
         * Use during workflow/actor shutdown to ensure no further sends are attempted
         * into targets that may be closing their channels.
         */
        suspend fun stopAndJoin() {
            // capture and null job under lock, then cancel/join outside
            val job = jobMutex.withLock {
                val j = routingJob
                routingJob = null
                j
            }
            job?.cancel()
            job?.join()
        }

        /** Cancels and waits for the routing job to finish. */
        suspend fun stopAndJoin() {
            val job = routingJob
            routingJob = null
            job?.cancel()
            job?.join()
        }
        /**
         * Validates the connection between the source and target ports.
         *
         * This method checks if a connection can be established between the `sourcePort` and
         * `targetPort` using the `canConnect` method. If a connection cannot be established,
         * it throws a `PortConnectionException` with an appropriate error message generated
         * by the `buildConnectionErrorMessage` method.
         *
         * @throws PortConnectionException if the connection between the ports cannot be established.
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

        /**
         * Determines if a connection can be established between the source and target ports.
         *
         * @return `true` if the source and target ports can be connected, `false` otherwise.
         */
        private fun canConnect(): Boolean = when {
            sourcePort.type == targetPort.type -> true
            protocolAdapter?.canHandle(
                sourcePort.type as KClass<*>,
                targetPort.type as KClass<*>
            ) == true -> true
            rules.isNotEmpty() -> validateConversionChain()
            else -> false
        }

        /**
         * Validates a chain of conversion rules to ensure each rule can handle
         * the current type and transform it into the target type.
         *
         * @return `true` if all conversion rules are valid and can handle the type transformation, otherwise `false`.
         */
        private fun validateConversionChain(): Boolean {
            var currentType: KClass<*> = sourcePort.type
            for (rule in rules) {
                if (!rule.canHandle(currentType, targetPort.type)) {
                    return false
                }
                currentType = targetPort.type
            }
            return true
        }

        /**
         * Builds an error message for connection issues between ports.
         *
         * The error message details the incompatible port types, including further
         * validation issues such as protocol adapter failures and invalid conversion chains.
         *
         * @return A detailed error message describing the connection issues.
         */
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

    /**
     * Companion object for the Port class.
     *
     * Provides utility functions related to ports and connections.
     */
    companion object {
        /**
         * Generates a unique identifier for a port.
         * The ID is formatted as "port-" followed by a UUID to ensure uniqueness.
         *
         * @return A unique identifier string for a port in the form "port-[uuid]"
         */
        fun generateId(): String = "port-${Uuid.random()}"

        /**
         * Establishes a connection between the source port and the target port.
         *
         * @param source The source port to connect from.
         * @param target The target port to connect to.
         * @param handlers A list of message handlers for processing messages, default is an empty list.
         * @param protocolAdapter An optional protocol adapter for handling protocol conversions.
         * @param rules A list of conversion rules for type conversion, default is an empty list.
         * @return A PortConnection object encapsulating the details of the established connection.
         */
        fun <IN : Any, OUT : Any> connect(
            source: Port<IN>,
            target: Port<OUT>,
            handlers: List<MessageHandler<IN, Any>> = emptyList(),
            protocolAdapter: ProtocolAdapter<*, OUT>? = null,
            rules: List<ConversionRule<IN, OUT>> = emptyList()
        ): PortConnection<IN, OUT> = PortConnection(
            source,
            target,
            handlers,
            protocolAdapter,
            rules
        ).also { it.validateConnection() }
    }
}
