package ai.solace.core.actor.config

import ai.solace.core.actor.Actor
import ai.solace.core.actor.message.ActorMessage
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration class for customizing actor behavior
 */
data class ActorConfiguration(
    val channelCapacity: Int = Channel.BUFFERED,
    val metricsInterval: Duration = 5.seconds,
    val errorHandler: (suspend (Exception, ActorMessage<*>, Actor) -> Unit)? = null,
    val messageTimeout: Duration = 30.seconds,
    val enablePrioritization: Boolean = false
) {
    companion object {
        fun default() = ActorConfiguration()

        fun withHighCapacity() = ActorConfiguration(
            channelCapacity = Channel.UNLIMITED,
            messageTimeout = 60.seconds
        )

        fun withPrioritization() = ActorConfiguration(
            enablePrioritization = true,
            channelCapacity = Channel.CONFLATED
        )
    }
}