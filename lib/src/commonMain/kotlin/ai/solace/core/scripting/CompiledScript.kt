package ai.solace.core.scripting

/**
 * Represents a compiled script that can be executed.
 */
interface CompiledScript {
    /**
     * The name of the script.
     */
    val name: String

    /**
     * The compilation timestamp.
     */
    val compilationTimestamp: Long
}