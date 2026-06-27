<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Running the System -->

## Running the System

### Prerequisites

- JDK 17 or later
- Kotlin 1.9+
- Gradle 8.0+

### One-Time Setup

```bash
# Install Git hooks (lint + ktlint)
./gradlew installGitHooks

# Warm Gradle wrapper and dependencies
./gradlew help

# Generate IDE metadata (optional)
./gradlew idea   # IntelliJ
./gradlew vscode # VS Code (kotlin-language-server users)
```

### Build

```bash
# Full project build
./gradlew build

# Build specific targets
./gradlew compileKotlinJvm
./gradlew compileKotlinJs
./gradlew compileKotlinNative
```

### Run Desktop Application

```bash
# Run desktop UI
./gradlew desktopRun

# Run with debug logging
./gradlew desktopRun -Dkotlin.logging.level=DEBUG
```

### Run Web Application

```bash
# Run web UI in browser
./gradlew jsBrowserRun

# Run web UI in development mode
./gradlew jsBrowserDevelopmentRun
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*ActorSystemTest*"

# Run with coverage
./gradlew test jacocoTestReport
```

### Smoke Tests (fast confidence)

```bash
# Core actor ping
./gradlew :lib:test --tests "*ActorSmoke*"

# MCP tool negotiation happy-path
./gradlew :lib:integrationTest --tests "*McpNegotiationSmoke*"

# Desktop UI boot
./gradlew :composeApp:desktopRun --args="--smoke"
```

### Configuration

Create `.solace-dev/config.json`:

```json
{
  "providers": {
    "ollama": {
      "baseUrl": "http://localhost:11434/v1",
      "models": ["qwen3-coder:30b"]
    }
  },
  "features": {
    "pipeline": true,
    "negotiation": true,
    "supervisor": true
  },
  "actorSystem": {
    "maxActors": 10,
    "messageTimeout": 30000
  }
}
```

### Logging, Tracing, and Flags
- Set `-Dkotlin.logging.level=DEBUG` for verbose logs.
- Set `-Dsolace.traces=true` to emit trace spans for MCP negotiation and tool calls.
- Use `SOLACE_VERBOSE_MCP=1` to log raw MCP JSON-RPC and XML tool envelopes (dev only).
- Use `SOLACE_FEATURE_FLAGS="pipeline,negotiation,supervisor"` to force-enable features.



[Back to Kotlin-Aligned Quick Start](Kotlin-Aligned-Quick-Start)
