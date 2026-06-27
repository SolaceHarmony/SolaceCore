<!-- topic: Reference -->
<!-- title: LangChain Prompt Management Recommendations -->

## 5. Prompt Management

### 5.1 Add Prompt System

```kotlin
interface PromptTemplate {
    val template: String
    val inputVariables: List<String>

    suspend fun format(variables: Map<String, String>): String

    fun validate(variables: Map<String, String>): Boolean {
        return inputVariables.all { variables.containsKey(it) }
    }
}

class ChatPromptTemplate(
    override val template: String,
    override val inputVariables: List<String>,
    private val systemMessage: String? = null
) : PromptTemplate {
    override suspend fun format(variables: Map<String, String>): String {
        require(validate(variables)) { "Missing required variables" }
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return systemMessage?.let { "$it\n$result" } ?: result
    }
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
