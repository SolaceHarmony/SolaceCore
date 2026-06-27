<!-- topic: Orientation -->
<!-- title: Framework System Architecture -->

## 2. System Architecture

### 2.1 High-Level Architecture

The Solace Core Framework consists of several key components organized in a layered architecture:

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

### 2.2 Component Overview

The framework comprises the following major components:

1. **Actor System**: Manages the creation, lifecycle, and communication of actors
2. **Port System**: Enables type-safe message passing between actors
3. **Supervisor**: Oversees actor lifecycles and manages system resources
4. **Workflow Manager**: Orchestrates the execution of actors in defined workflows
5. **Storage System**: Provides persistence capabilities for actor state and system data



[Back to Solace Core Framework Architecture](Solace-Core-Framework-Architecture)
