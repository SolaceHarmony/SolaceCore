package ai.solace.core.channels

import ai.solace.core.common.Disposable
import kotlin.reflect.KClass
import kotlin.random.Random

/**
 * Base interface for all port types in the actor system
 */
interface Port<T : Any> : Disposable {
    val id: String
    val name: String
    val type: KClass<T>

    companion object {
        fun generateId(): String = buildString {
            append("port-")
            append(Random.nextBytes(8).joinToString("") { "%02x".format(it) })
        }
    }
}