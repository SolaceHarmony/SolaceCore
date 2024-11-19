package ai.solace.core.actor.connections

import ai.solace.core.channels.InputPort
import ai.solace.core.channels.OutputPort
import ai.solace.core.common.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

class Connection<T : Any>(
    val outputPort: OutputPort<T>,
    val inputPort: InputPort<T>,
    val channel: Channel<T>,
    val forwardingJob: Job
) : Disposable {
    override suspend fun dispose() {
        channel.cancel()
        forwardingJob.cancel()
    }
}