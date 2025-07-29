# SolaceCore

<div align="center">
  <img src="https://img.shields.io/badge/kotlin-2.0.21-blue.svg" alt="Kotlin 2.0.21">
  <img src="https://img.shields.io/badge/license-Apache%202.0-green.svg" alt="License: Apache 2.0">
  <img src="https://img.shields.io/badge/status-active%20development-brightgreen.svg" alt="Status: Active Development">
</div>

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](#documentation)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Overview

SolaceCore is a powerful, actor-based framework designed to enable the development of flexible, resilient, and dynamically adaptable applications. Built using Kotlin and Ktor, it follows a hot-pluggable component model that allows for runtime modification of system behavior without service interruption.

As the foundation of the broader Solace AI ecosystem, SolaceCore provides the infrastructure for building conversational AI systems with advanced capabilities including contextual memory, emotional intelligence, and sophisticated tool usage.

## Key Features

- **Hot-Pluggable Components**: Add, remove, or modify system components during runtime without requiring system restarts
- **Actor-Based Architecture**: Leverage the actor model for scalable, concurrent processing with isolated state
- **Type-Safe Communication**: Ensure robust message passing between components with strict interface contracts
- **Lifecycle Management**: Standardize component lifecycle handling for consistent resource management
- **Workflow Orchestration**: Build complex processing pipelines by connecting actors in defined workflows
- **Dynamic Scripting**: Modify actor behavior at runtime through scripting capabilities
- **Comprehensive Observability**: Monitor system performance and behavior with integrated metrics

## Architecture

SolaceCore consists of several key components organized in a layered architecture:

```
+---------------------------------------------------+
|                   Applications                     |
+---------------------------------------------------+
|                     Workflows                      |
+---------------------------------------------------+
|                    Actor System                    |
+---------------------------------------------------+
|                      Kernel                        |
+---------------------------------------------------+
|                    Data Storage                    |
+---------------------------------------------------+
```

- **Kernel**: The foundational layer providing communication primitives, resource management, and lifecycle control
- **Actor System**: The core runtime environment for actor creation, management, and message passing
- **Workflows**: Higher-level orchestration of actors into processing pipelines
- **Applications**: Domain-specific implementations built on the framework
- **Data Storage**: Persistence layer using graph and relational databases

For more detailed information about the architecture, see the [Architectural Deep Dive](docs/Architectural_Deepdive.md) and [System Architecture](docs/components/kernel/system_architecture.md) documentation.

## Installation

### Prerequisites

- JDK 11 or higher
- Kotlin 2.0.21 or higher
- Gradle 7.0 or higher

### Gradle

Add the following to your `build.gradle.kts` file:

```kotlin
repositories {
    mavenCentral()
    // Add repository information when published
}

dependencies {
    implementation("ai.solace:solace-core:0.1.0")
}
```

### Building from Source

```bash
git clone https://github.com/yourusername/SolaceCore.git
cd SolaceCore
./gradlew build
```

## Usage

### Creating a Basic Actor

```kotlin
class GreetingActor : Actor() {
    val nameInput = InputPort<String>("nameInput", String::class)
    val greetingOutput = OutputPort<String>("greetingOutput", String::class)

    override suspend fun process() {
        val name = nameInput.receive()
        val greeting = "Hello, $name!"
        greetingOutput.send(greeting)
    }
}
```

### Building a Simple Workflow

```kotlin
val workflow = WorkflowBuilder()
    .addActor("greeter", GreetingActor())
    .addActor("logger", LoggingActor())
    .connect("greeter.greetingOutput", "logger.messageInput")
    .build()

workflow.start()
```

For more examples and detailed usage instructions, see the [Documentation](#documentation) section.

## Documentation

Comprehensive documentation is available in the `docs/` directory:

- [Architectural Document: Solace Core Framework](docs/Architectural_Document_Solace_Core_Framework.md) - Detailed overview of the framework's architecture and design principles
- [Architectural Deep Dive](docs/Architectural_Deepdive.md) - Exhaustive exploration of the project's architecture and implementation
 - [System Architecture](docs/components/kernel/system_architecture.md) - Concise overview of the system architecture and components

## Roadmap

- **Neo4j Integration**: Implement graph database integration for complex data relationships
- **Kotlin-Native Storage**: Develop native storage solutions for structured data
- **Enhanced Actor Management**: Complete dynamic actor management features
- **Workflow Persistence**: Enhance workflow management with state persistence
- **Clustering Support**: Add support for distributed operation across multiple nodes
- **Comprehensive Monitoring**: Implement detailed metrics and monitoring capabilities

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache 2.0 License.

## Acknowledgements

SolaceCore was designed and developed by Sydney Bach, who serves as the project's architect, designer, and primary developer.

Special thanks to all contributors who have helped shape and improve this framework.
