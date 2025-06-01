package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A simple implementation of the ScriptValidator interface.
 *
 * This implementation performs basic syntax validation of Kotlin scripts.
 * In a real implementation, this would use the Kotlin compiler to perform more thorough validation.
 */
class SimpleScriptValidator : ScriptValidator {
    /**
     * Validates a script.
     *
     * @param scriptSource The source code of the script.
     * @return The validation result.
     */
    override suspend fun validate(scriptSource: String): ValidationResult {
        return withContext(Dispatchers.Default) {
            val errors = mutableListOf<ValidationError>()

            // Check for basic syntax errors
            try {
                // Check for unbalanced braces
                checkUnbalancedBraces(scriptSource, errors)

                // Check for missing semicolons (optional in Kotlin, but good to check)
                checkMissingSemicolons(scriptSource, errors)

                // Check for invalid imports
                checkInvalidImports(scriptSource, errors)

                // Check for empty script
                if (scriptSource.trim().isEmpty()) {
                    errors.add(ValidationError("Script is empty", 1, 1))
                }
            } catch (e: Exception) {
                errors.add(ValidationError("Validation error: ${e.message}", 1, 1))
            }

            ValidationResult(errors.isEmpty(), errors)
        }
    }

    /**
     * Checks for unbalanced braces in the script.
     *
     * @param scriptSource The source code of the script.
     * @param errors The list of errors to add to.
     */
    private fun checkUnbalancedBraces(scriptSource: String, errors: MutableList<ValidationError>) {
        val stack = mutableListOf<Pair<Char, Pair<Int, Int>>>()
        val lines = scriptSource.lines()

        for ((lineIndex, line) in lines.withIndex()) {
            for ((charIndex, char) in line.withIndex()) {
                when (char) {
                    '(', '[', '{' -> stack.add(Pair(char, Pair(lineIndex + 1, charIndex + 1)))
                    ')' -> {
                        if (stack.isEmpty() || stack.last().first != '(') {
                            errors.add(ValidationError("Unbalanced parenthesis", lineIndex + 1, charIndex + 1))
                        } else {
                            stack.removeAt(stack.size - 1)
                        }
                    }
                    ']' -> {
                        if (stack.isEmpty() || stack.last().first != '[') {
                            errors.add(ValidationError("Unbalanced bracket", lineIndex + 1, charIndex + 1))
                        } else {
                            stack.removeAt(stack.size - 1)
                        }
                    }
                    '}' -> {
                        if (stack.isEmpty() || stack.last().first != '{') {
                            errors.add(ValidationError("Unbalanced brace", lineIndex + 1, charIndex + 1))
                        } else {
                            stack.removeAt(stack.size - 1)
                        }
                    }
                }
            }
        }

        // Check if there are any unclosed braces
        for ((brace, position) in stack) {
            val braceType = when (brace) {
                '(' -> "parenthesis"
                '[' -> "bracket"
                '{' -> "brace"
                else -> "unknown"
            }
            errors.add(ValidationError("Unclosed $braceType", position.first, position.second))
        }
    }

    /**
     * Checks for missing semicolons in the script.
     *
     * @param scriptSource The source code of the script.
     * @param errors The list of errors to add to.
     */
    private fun checkMissingSemicolons(scriptSource: String, errors: MutableList<ValidationError>) {
        // Kotlin doesn't require semicolons, but we can check for cases where they might be needed
        val lines = scriptSource.lines()
        for ((lineIndex, line) in lines.withIndex()) {
            // Check for multiple statements on a single line without semicolons
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("//") && !trimmedLine.startsWith("/*")) {
                val statements = trimmedLine.split(";")
                if (statements.size > 1) {
                    // Check if the last statement is empty (i.e., the line ends with a semicolon)
                    if (statements.last().trim().isNotEmpty()) {
                        errors.add(ValidationError("Multiple statements on a single line should be separated by semicolons", lineIndex + 1, trimmedLine.length))
                    }
                }
            }
        }
    }

    /**
     * Checks for invalid imports in the script.
     *
     * @param scriptSource The source code of the script.
     * @param errors The list of errors to add to.
     */
    private fun checkInvalidImports(scriptSource: String, errors: MutableList<ValidationError>) {
        val lines = scriptSource.lines()
        for ((lineIndex, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("import ")) {
                val importStatement = trimmedLine.substring("import ".length).trim()
                if (importStatement.isEmpty()) {
                    errors.add(ValidationError("Empty import statement", lineIndex + 1, "import ".length + 1))
                } else if (importStatement.endsWith(";")) {
                    // Kotlin imports don't need semicolons
                    errors.add(ValidationError("Import statements in Kotlin don't need semicolons", lineIndex + 1, trimmedLine.length))
                }
            }
        }
    }
}