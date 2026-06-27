<!-- topic: Runtime -->
<!-- title: Kernel Port Usage Example -->

#### 1.1.6. Basic Usage Example
The following example illustrates how ports might be created using the `BidirectionalPort` implementation and connected using `Port.connect`:

```kotlin
// import io.github.solaceharmony.core.kernel.channels.ports.BidirectionalPort
// import io.github.solaceharmony.core.kernel.channels.ports.Port // For Port.connect

suspend fun main() { // Example, typically run in a coroutine scope
    // Create ports using BidirectionalPort concrete implementation
    val outputPort = BidirectionalPort.create<String>("sourceOutputChannel")
    val inputPort = BidirectionalPort.create<String>("targetInputChannel")

    try {
        // Establish a connection using Port.connect
        // Assuming no complex handlers, adapters, or rules for this basic example
        val connection = Port.connect(outputPort, inputPort)
        println("Successfully connected ${connection.sourcePort.name} to ${connection.targetPort.name}")

        // Send a message from outputPort
        val messageToSend = "Hello from ${outputPort.name}!"
        println("Sending: '$messageToSend'")
        outputPort.send(messageToSend)

        // Receive the message on inputPort
        val receivedMessage = inputPort.receive()
        println("Received on ${inputPort.name}: '$receivedMessage'")

    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        // Dispose of ports to release resources
        outputPort.dispose()
        inputPort.dispose()
        println("Ports disposed.")
    }
}
```


[Back to Kernel & Ports](Kernel-and-Ports)
