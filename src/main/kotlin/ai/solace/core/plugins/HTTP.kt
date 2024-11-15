package ai.solace.core.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*

fun Application.configureHTTP() {
    // Content Negotiation with JSON support
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    // CORS Configuration
    install(CORS) {
        anyHost() // For development purposes, allow any host
        allowHeader(HttpHeaders.ContentType)
    }
}


