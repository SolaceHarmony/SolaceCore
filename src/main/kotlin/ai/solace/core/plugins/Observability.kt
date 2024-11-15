package ai.solace.core.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureObservability() {
        install(CallLogging) {
            level = Level.INFO
            filter { call -> !call.request.uri.contains("health") } // Exclude health checks from the logs
        }

        // Install Micrometer Metrics for observability
        install(MicrometerMetrics) {
            // Here you can configure your metrics (e.g., Prometheus, JMX)
        }
}
