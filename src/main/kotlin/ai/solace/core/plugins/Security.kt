package ai.solace.core.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*


fun Application.configureSecurity() {
    install(Authentication) {
        basic("auth-basic") {
            realm = "ktor-sample"
            validate { credentials ->
                if (credentials.name == "user" && credentials.password == "password") {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }
}
