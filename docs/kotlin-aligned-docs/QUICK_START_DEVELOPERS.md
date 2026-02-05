---
title: Quick Start for Developers
date: 2026-02-04
---

# Quick Start for Developers

## Understanding the System

Read these documents in order:

1. **[Architecture Overview](ARCHITECTURE_OVERVIEW.md)** - System design and components (20 min)
2. **[Implementation Status](IMPLEMENTATION_STATUS.md)** - Current state and priorities (15 min)
3. **[Actor System Guide](components/actor_system/ACTOR_SYSTEM_GUIDE.md)** - Actor-based architecture (20 min)
4. **[Safety System Spec](components/lifecycle/SAFETY_SYSTEM_SPEC.md)** - Safety and supervision (15 min)
5. **[Neutral History XML Spec](components/memory/NEUTRAL_HISTORY_XML_SPEC.md)** - Context format (20 min)

**Total time**: ~90 minutes to understand the architecture.

## Key Concepts (TL;DR)

### Startup Sequence

```
Actor System Initialization
    ↓
Model Warmup (ping-pong)
    ↓
Negotiation (MCP → Functions → XML → Choice → CLI-only)
    ↓
Initialize Supervisor (mandatory)
    ↓
Ready (accept user input)
```

### Tool Execution

```
User → Main Actor → Advisor Actor → Supervisor Actor → MCP → Execution → Supervisor → Main Actor → User
```

**No shortcuts**: Supervisor is mandatory, cannot be bypassed.

### Multi-Actor System

- **Main Actor**: Handles user requests and coordinates
- **Advisor Actor**: Plans and deliberates using workflow engine
- **Supervisor Actor**: Approves/belays/revises all actions
- **Tool Actors**: Execute via MCP JSON-RPC
- **Memory Actor**: Manages neutral history and context
- **Mood Actor**: Provides emotional intelligence

Each actor gets filtered context (doesn't see others' thoughts).

### Neutral History

ALL conversation stored as XML:

- Timestamp-based ordering (ms since epoch)
- Actor attribution (provider/model:persona)
- MCP JSON-RPC for tools
- Complete provenance with Kotlin Flow streaming

Provider formats (OpenAI JSON) are TRANSPORT only.

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

## Kotlin Development

### Project Structure

```
SolaceCore/
├── lib/                          # Shared library
│   └── src/
│       ├── commonMain/kotlin/    # Platform-agnostic code
│       ├── jvmMain/kotlin/       # JVM-specific code
│       ├── jsMain/kotlin/        # JavaScript-specific code
│       └── nativeMain/kotlin/    # Native-specific code
├── composeApp/                   # UI application
│   └── src/
│       ├── commonMain/kotlin/    # Shared UI logic
│       ├── desktopMain/kotlin/   # Desktop UI
│       └── webMain/kotlin/       # Web UI
└── docs/                        # Documentation
```

### Creating a New Actor

```kotlin
// Define actor messages
sealed class MyActorMessage : ActorMessage() {
    data class ProcessRequest(val input: String) : MyActorMessage()
    data class ProcessingResult(val output: String) : MyActorMessage()
}

// Implement the actor
class MyActor(
    actorId: String,
    supervisor: ActorSupervisor
) : BaseActor(actorId, supervisor) {

    override suspend fun handleMessage(message: ActorMessage) {
        when (message) {
            is MyActorMessage.ProcessRequest -> {
                val result = processInput(message.input)
                sendMessage(message.sender, MyActorMessage.ProcessingResult(result))
            }
            else -> unhandledMessage(message)
        }
    }

    private fun processInput(input: String): String {
        return "Processed: $input"
    }
}
```

### Using the Pipeline DSL

```kotlin
// Create a pipeline using Kotlin DSL
val pipeline = pipeline {
    block("protocol.ollama") {
        "model" to "qwen3-coder:30b"
        "temperature" to 0.7
    }
    block("codec.mcp_over_xml")
    block("tools.negotiation_advertise")
    block("family.qwen3")
}

// Execute the pipeline
val result = pipelineEngine.execute(input, pipeline)
```

### Making a Tool Call via MCP (Example)

```kotlin
val call = McpToolCall(
    name = "list_directory",
    arguments = mapOf("path" to "/tmp"),
    correlationId = UUID.randomUUID().toString()
)

val response = mcpExecutor.execute(call)
if (response.error != null) {
    supervisor.reportError(response.error)
} else {
    neutralHistory.store(
        NeutralEvent(
            id = "tool_${call.correlationId}",
            timestamp = System.currentTimeMillis(),
            type = NeutralEventType.TOOL_RESULT,
            lane = Lane.TECHNICAL,
            source = "main-actor",
            content = NeutralContent(toolName = call.name, result = response.result)
        )
    )
}
```

### Working with Neutral History

```kotlin
// Store an event
val event = NeutralEvent(
    id = "event_${System.currentTimeMillis()}",
    timestamp = System.currentTimeMillis(),
    type = NeutralEventType.MESSAGE,
    lane = Lane.TECHNICAL,
    source = "main-actor",
    content = NeutralContent(text = "User request processed")
)

neutralHistory.store(event)

// Query events
val recentEvents = neutralHistory.retrieve(
    NeutralQuery(
        timeRange = (System.currentTimeMillis() - 3600000)..System.currentTimeMillis(),
        eventTypes = setOf(NeutralEventType.MESSAGE, NeutralEventType.TOOL_USE)
    )
)
```

## Known Issues (Priority Order)

### Critical (Week 1)

1. **Multi-lane initialization incomplete** - Emotional and technical streams not fully parallelized
2. **Supervisor bypass possible** - Some code paths don't enforce mandatory supervision
3. **UI-actor integration weak** - Compose UI doesn't fully reflect actor system state

### High Priority (Week 2-3)

4. **Test coverage low** - Only ~12% unit test coverage
5. **Memory consolidation partial** - Bidirectional linking not fully implemented
6. **Error handling inconsistent** - Some actors don't properly handle failures

### Medium Priority (Month 1)

7. **Performance optimization needed** - Some operations are not optimized for concurrency
8. **Configuration validation weak** - Limited validation of config files
9. **Documentation incomplete** - API documentation needs expansion

## Development Workflow

### Daily Development

```bash
# Start development
./gradlew desktopRun

# In another terminal, run tests continuously
./gradlew test --continuous

# Check code quality
./gradlew detekt
./gradlew ktlintCheck
```

### Before Commit

```bash
# Run full test suite
./gradlew test

# Check code style
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt

# Build all targets
./gradlew build
```

### IDE Setup

- **IntelliJ IDEA**: Use Kotlin plugin, enable annotation processing
- **VS Code**: Install Kotlin extension, configure workspace settings
- **Android Studio**: Full Kotlin support included

### Debugging

```kotlin
// Add debug logging
private val logger = KotlinLogging.logger {}

suspend fun debugActor() {
    logger.debug { "Processing message: $message" }

    try {
        // Your code here
        logger.info { "Processing completed successfully" }
    } catch (e: Exception) {
        logger.error(e) { "Processing failed" }
        throw e
    }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Ensure all tests pass
5. Submit a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed guidelines.