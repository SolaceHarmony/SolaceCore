package ai.solace.core.actor.interfaces

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

// Serializable state classes
@kotlinx.serialization.Serializable
data class ActorState(
    val inputs: Map<String, PortState>,
    val outputs: Map<String, PortState>,
    val tools: Map<String, PortState>
)

@kotlinx.serialization.Serializable
data class PortState(
    val name: String,
    val type: String // Store the class name as string for easy state handling
)

class ActorInterface {

    private val inputs = mutableMapOf<String, Port.Input<*>>()
    private val outputs = mutableMapOf<String, Port.Output<*>>()
    private val tools = mutableMapOf<String, Port.Tool<*>>()

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Registers an input port with the given name and type.
     *
     * @param T The type of data the port handles.
     * @param name The name of the input port.
     * @param type The Kotlin class of the type.
     * @return The created input port.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> input(name: String, type: KClass<T>): Port.Input<T> {
        val port = Port.Input(name, type)
        inputs[name] = port
        return port
    }

    /**
     * Registers an output port with the given name and type.
     *
     * @param T The type of data the port handles.
     * @param name The name of the output port.
     * @param type The Kotlin class of the type.
     * @return The created output port.
     */
    fun <T : Any> output(name: String, type: KClass<T>): Port.Output<T> {
        val port = Port.Output(name, type)
        outputs[name] = port
        return port
    }

    /**
     * Registers a tool with the given name and type.
     *
     * @param T The type of functionality the tool provides.
     * @param name The name of the tool.
     * @param type The Kotlin class of the type.
     * @return The created tool.
     */
    fun <T : Any> tool(name: String, type: KClass<T>): Port.Tool<T> {
        val port = Port.Tool(name, type)
        tools[name] = port
        return port
    }

    /**
     * Connects an output port to an input port with an optional buffer size for the underlying channel.
     *
     * @param T The type of data the ports handle.
     * @param outputPort The output port.
     * @param inputPort The input port.
     * @param bufferSize The buffer size for the channel. Default is Channel.BUFFERED.
     */
    fun <T : Any> connect(
        outputPort: Port.Output<T>,
        inputPort: Port.Input<T>,
        bufferSize: Int = Channel.BUFFERED
    ) {
        val channel = Channel<T>(bufferSize)
        inputPort.channel = channel
        outputPort.connect(channel)
    }

    /**
     * Gets an input port by its name.
     * @param name The name of the input port.
     * @return The input port or null if not found.
     */
    fun getInput(name: String): Port.Input<*>? = inputs[name]

    /**
     * Gets an output port by its name.
     * @param name The name of the output port.
     * @return The output port or null if not found.
     */
    fun getOutput(name: String): Port.Output<*>? = outputs[name]

    /**
     * Gets a tool by its name.
     * @param name The name of the tool.
     * @return The tool or null if not found.
     */
    fun getTool(name: String): Port.Tool<*>? = tools[name]

    /**
     * Retrieves all ports grouped by their type.
     * @return A map containing lists of all input, output, and tool ports.
     */
    fun getAllPorts(): Map<String, List<Port<*>>> {
        return mapOf(
        "inputs" to inputs.values.toList(),
        "outputs" to outputs.values.toList(),
        "tools" to tools.values.toList()
    )
    }

    /**
     * Saves the current state of the actor interface as a JSON string.
     * @return JSON representation of the current state.
     */
    fun saveState(): String {
        val state = ActorState(
            inputs = inputs.mapValues { PortState(it.key, it.value.type.qualifiedName ?: "unknown") },
            outputs = outputs.mapValues { PortState(it.key, it.value.type.qualifiedName ?: "unknown") },
            tools = tools.mapValues { PortState(it.key, it.value.type.qualifiedName ?: "unknown") }
        )
        return try {
            json.encodeToString(state)
        } catch (e: SerializationException) {
            throw RuntimeException("Failed to serialize actor state", e)
        }
    }

    /**
     * Restores the state from a JSON string.
     * @param stateJson A JSON string representing the state to be restored.
     */
    fun restoreState(stateJson: String) {
        // Implementation to restore state
    }
}