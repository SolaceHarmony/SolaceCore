package ai.solace.core.actor.connections

import ai.solace.core.channels.InputPort
import ai.solace.core.channels.OutputPort
import ai.solace.core.common.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectionManager(private val scope: CoroutineScope) : Disposable {
    private val connections = mutableMapOf<String, Connection<*>>()
    private val connectionMutex = Mutex()
    private val cleanupJobs = mutableListOf<Job>()

    suspend fun <T : Any> connect(
        output: OutputPort<T>,
        input: InputPort<T>,
        bufferSize: Int = Channel.Factory.BUFFERED
    ): String {
        return connectionMutex.withLock {
            validateConnection(output, input)

            val connectionId = "${output.id}-${input.id}"
            val channel = Channel<T>(bufferSize)

            val forwardingJob = scope.launch {
                try {
                    for (message in channel) {
                        input.receive()
                        output.send(message)
                    }
                } catch (e: Exception) {
                    handleConnectionError(connectionId, e)
                }
            }

            Connection(output, input, channel, forwardingJob).also {
                connections[connectionId] = it
            }

            connectionId
        }
    }

    private fun <T : Any> validateConnection(
        output: OutputPort<T>,
        input: InputPort<*>
    ) {
        require(output.type == input.type) {
            "Type mismatch: cannot connect ${output.type} to ${input.type}"
        }
    }

    private fun handleConnectionError(connectionId: String, error: Exception) {
        println("Connection error on $connectionId: ${error.message}")
    }

    suspend fun disconnect(connectionId: String) {
        connectionMutex.withLock {
            connections.remove(connectionId)?.dispose()
        }
    }

    override suspend fun dispose() {
        connectionMutex.withLock {
            connections.values.forEach { it.dispose() }
            connections.clear()
            cleanupJobs.forEach { it.cancel() }
            cleanupJobs.clear()
        }
    }

    suspend fun getActiveConnections(): List<String> {
        return connectionMutex.withLock {
            connections.keys.toList()
        }
    }
}