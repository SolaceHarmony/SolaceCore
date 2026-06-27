<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Development Workflow -->

## Development Workflow

### Building the Project

```bash
# Full build
./gradlew build

# Run tests
./gradlew test

# Run specific platform
./gradlew desktopRun
./gradlew jsBrowserRun
./gradlew androidApp
```

### Project Structure

```
SolaceCore/
├── lib/                          # Shared library
│   └── src/commonMain/kotlin/com/solacecore/
│       ├── actor/               # Actor system
│       ├── mood/                # Emotional intelligence
│       ├── mcp/                 # Tool system
│       ├── neutral/             # History system
│       ├── pipeline/            # Pipeline engine
│       ├── providers/           # AI providers
│       └── safety/              # Safety controls
├── composeApp/                   # UI application
│   ├── src/commonMain/          # Shared UI logic
│   ├── src/desktopMain/         # Desktop UI
│   ├── src/webMain/             # Web UI
│   └── src/androidMain/         # Android UI
├── docs/                        # Documentation
│   ├── components/              # Component docs
│   └── kotlin-plans/            # Kotlin implementations
└── build.gradle.kts             # Build configuration
```

### Key Kotlin Features Used

- **Coroutines**: For async operations and concurrency
- **Flow**: For reactive data streams
- **Sealed Classes**: For type-safe message passing
- **Inline Functions**: For performance optimization
- **DSL Builders**: For configuration APIs
- **Operator Overloading**: For domain-specific operations
- **Context Receivers**: For scoped operations
- **Contracts**: For optimization hints

This architecture provides a solid foundation for building sophisticated AI-powered applications with genuine emotional intelligence, all built with Kotlin's modern features and best practices.


[Back to Kotlin-Aligned Architecture Overview](Kotlin-Aligned-Architecture-Overview)
