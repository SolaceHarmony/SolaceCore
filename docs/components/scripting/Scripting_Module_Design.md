# Scripting Module Design

## Overview and Design Goals

The scripting module in SolaceCore provides a robust framework for integrating and executing dynamic Kotlin scripts (`.kts` files). This allows for flexible and updatable logic within the system, particularly for actor behaviors. The design emphasizes Kotlin script integration, hot-reloading, validation, versioning, and persistent storage.

The scripting engine is designed with the following key requirements and features in mind:

*   **Kotlin Script Integration:**
    *   Leverage Kotlin's native scripting capabilities.
    *   Support compilation and execution of `.kts` script files.
    *   Provide a sandboxed environment for safe script execution.
*   **Hot-Reloading:**
    *   Enable dynamic updates to actor logic by reloading scripts without system downtime.
    *   Support automatic detection of script changes and subsequent reloading.
    *   Ensure actor state is preserved across script reloads.
*   **Script Validation:**
    *   Perform syntax and semantic validation of scripts before execution.
    *   Verify that scripts adhere to any required interfaces or contracts.
    *   Offer clear error reporting for invalid scripts.
*   **Versioning and Rollback:**
    *   Track different versions of scripts.
    *   Allow rollback to previously known good versions if issues arise with new script versions.
*   **Script Storage:**
    *   Persistently store scripts and their metadata (version, creation date, etc.).
    *   Support loading scripts from various sources (e.g., file system, database).

## Core Scripting Interfaces

The foundational interfaces for the scripting engine are defined as follows:

### `CompiledScript` Interface

Represents a script that has been successfully compiled by the `ScriptEngine` and is ready for execution.

*   **Purpose:** To serve as a handle or representation of a validated and prepared script.
*   **Key Properties:**
    *   `val name: String`: The unique name assigned to the script.
    *   `val compilationTimestamp: Long`: The epoch milliseconds timestamp indicating when the script was compiled.

### `ScriptEngine` Interface

Defines the contract for the core component responsible for compiling and executing Kotlin scripts.

*   **Purpose:** To manage the lifecycle of scripts, including their compilation and execution.
*   **Key Methods:**
    *   `suspend fun compile(scriptSource: String, scriptName: String): CompiledScript`: Takes Kotlin script source code as a string and a name, then compiles it, returning a `CompiledScript` instance.
    *   `suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any?`: Executes a previously compiled `CompiledScript`, passing in a map of parameters. It returns an optional `Any?` result from the script execution.
    *   `suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?> = emptyMap()): Any?`: A convenience method that combines compilation and execution in a single step.

### `ScriptStorage` Interface

Handles the persistent storage of scripts, providing a mechanism to save, load, list, and delete scripts.

*   **Purpose:** To provide a standardized way of storing and retrieving scripts from various storage backends.
*   **Key Methods:**
    *   `suspend fun saveScript(scriptName: String, scriptSource: String, metadata: Map<String, Any> = emptyMap())`: Saves a script with its metadata to storage.
    *   `suspend fun loadScript(scriptName: String): Pair<String, Map<String, Any>>?`: Loads a script and its metadata from storage, returning null if the script doesn't exist.
    *   `suspend fun listScripts(): List<String>`: Lists all available scripts in storage.
    *   `suspend fun deleteScript(scriptName: String): Boolean`: Deletes a script from storage, returning true if successful.

### Core Interface Relationships

The SolaceCore scripting module's interfaces work together to provide a complete script management solution:

1. **Compilation**: The `ScriptEngine` compiles script source code into a `CompiledScript` object.
2. **Execution**: The `ScriptEngine` executes a `CompiledScript`, potentially with parameters, to produce a result.
3. **Storage**: The `ScriptStorage` interface provides persistence for scripts, allowing them to be saved, loaded, listed, and deleted.

Implementations of these interfaces, such as the `FileScriptStorage` class, provide concrete functionality for script storage using specific backend technologies (e.g., file system).