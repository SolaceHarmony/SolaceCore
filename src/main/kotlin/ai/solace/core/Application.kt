package ai.solace.core
import ai.solace.core.actors.ScriptingEngineActor
import ai.solace.core.actors.SupervisorActor
import ai.solace.core.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    val scriptingEngineActor = ScriptingEngineActor()
    val supervisorActor = SupervisorActor(scriptingEngineActor)
    supervisorActor.deployActor("HelloWorldActor.kts")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureObservability()
    configureHTTP()
    configureRouting()
    configureSecurity()
}


