package ai.solace.core.actor.builder

import ai.solace.core.actor.Actor
import ai.solace.core.actor.types.KernelActor
import ai.solace.core.actor.types.scripted.ScriptedActor
import javax.script.ScriptEngine
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
sealed class ActorBuilder {
    protected var id: String? = null

    fun withId(id: String) = apply {
        this.id = id
    }

    abstract fun build(): Actor

    /**
     * Builder for core framework/kernel actors implemented in Kotlin
     */
    class KernelActorBuilder : ActorBuilder() {
        private var actorClass: Class<out KernelActor>? = null

        fun withActorClass(clazz: Class<out KernelActor>) = apply {
            actorClass = clazz
        }

        override fun build(): Actor {
            requireNotNull(actorClass) { "Actor class must be provided for kernel actors" }
            return actorClass!!.getDeclaredConstructor(String::class.java)
                .newInstance(id ?: Uuid.random().toString())
        }
    }

    /**
     * Builder for dynamically scripted actors (e.g., for flows/chains)
     */
    class ScriptedActorBuilder : ActorBuilder() {
        private var scriptEngine: ScriptEngine? = null
        private var script: String? = null

        fun withScriptEngine(engine: ScriptEngine) = apply {
            scriptEngine = engine
        }

        fun withScript(script: String) = apply {
            this.script = script
        }

        override fun build(): Actor {
            requireNotNull(scriptEngine) { "ScriptEngine must be provided for scripted actors" }
            requireNotNull(script) { "Script must be provided for scripted actors" }

            return ScriptedActor(
                id = id ?: Uuid.random().toString(),
                scriptEngine = scriptEngine!!,
                script = script!!
            )
        }
    }

    companion object {
        fun kernel() = KernelActorBuilder()
        fun scripted() = ScriptedActorBuilder()

        // Factory methods for common kernel actors
        fun router() = kernel().withActorClass(RouterActor::class.java)
        fun supervisor() = kernel().withActorClass(SupervisorActor::class.java)
        fun messageHub() = kernel().withActorClass(MessageHubActor::class.java)
    }
}