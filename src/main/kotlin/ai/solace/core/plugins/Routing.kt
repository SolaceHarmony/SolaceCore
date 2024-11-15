package ai.solace.core.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to Solace Core!")
        }

        get("/hello") {
            call.respondText("Hello, World from the script!", status = HttpStatusCode.OK)
        }

        get("/health") {
            call.respondText("Solace Core is healthy.", status = HttpStatusCode.OK)
        }
    }
}
