# Basic Actor Usage Example

The following example demonstrates creating a small actor and connecting it in a workflow.

```kotlin
class EchoActor : Actor() {
    val input = InputPort<String>("input", String::class)
    val output = OutputPort<String>("output", String::class)

    override suspend fun process() {
        val msg = input.receive()
        output.send(msg)
    }
}

val workflow = WorkflowBuilder()
    .addActor("echo", EchoActor())
    .build()
workflow.start()
```
