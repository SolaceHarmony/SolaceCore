package ai.solace.core.actor.interfaces

import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

class ActorInterface {
    private val inputs = mutableMapOf<String, Port.Input<*>>()
    private val outputs = mutableMapOf<String, Port.Output<*>>()
    private val tools = mutableMapOf<String, Port.Tool<*>>()

    fun <T : Any> input(name: String, type: KClass<T>): Port.Input<T> {
        return Port.Input(name, type).also { inputs[name] = it }
    }

    fun <T : Any> output(name: String, type: KClass<T>): Port.Output<T> {
        return Port.Output(name, type).also { outputs[name] = it }
    }

    fun <T : Any> tool(name: String, type: KClass<T>): Port.Tool<T> {
        return Port.Tool(name, type).also { tools[name] = it }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> connect(
        outputPort: Port.Output<T>,
        inputPort: Port.Input<T>,
        bufferSize: Int = Channel.BUFFERED
    ) {
        require(outputPort.type == inputPort.type) {
            "Type mismatch: ${outputPort.type} cannot be connected to ${inputPort.type}"
        }
        
        val channel = Channel<T>(bufferSize)
        outputPort.connect(channel)
        inputPort.channel = channel
    }

    fun getInput(name: String): Port.Input<*>? = inputs[name]
    fun getOutput(name: String): Port.Output<*>? = outputs[name]
    fun getTool(name: String): Port.Tool<*>? = tools[name]

    fun getAllPorts(): Map<String, List<Port<*>>> = mapOf(
        "inputs" to inputs.values.toList(),
        "outputs" to outputs.values.toList(),
        "tools" to tools.values.toList()
    )
}