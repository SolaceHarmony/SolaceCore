package ai.solace.core.scripting

/**
 * Validates scripts for syntax and semantic errors.
 */
interface ScriptValidator {
    /**
     * Validates a script.
     *
     * @param scriptSource The source code of the script.
     * @return The validation result.
     */
    suspend fun validate(scriptSource: String): ValidationResult
}

/**
 * The result of script validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
)

/**
 * Represents a validation error.
 */
data class ValidationError(
    val message: String,
    val line: Int,
    val column: Int
)