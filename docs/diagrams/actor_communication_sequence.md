# Actor Communication Sequence Diagram

```mermaid
sequenceDiagram
    participant ActorA
    participant PortA as ActorA.OutputPort
    participant PortB as ActorB.InputPort
    participant ActorB
    
    Note over ActorA, ActorB: Initialization Phase
    ActorA->>PortA: createPort("output", MessageType::class)
    ActorB->>PortB: createPort("input", MessageType::class)
    
    Note over ActorA, ActorB: Connection Phase
    ActorA->>PortA: connect(PortA, PortB)
    
    Note over ActorA, ActorB: Communication Phase
    ActorA->>PortA: send(message)
    PortA->>PortA: Apply handlers
    PortA->>PortA: Apply conversion rules
    PortA->>PortB: Channel.send(processedMessage)
    
    Note over PortB: Message is queued in channel
    
    PortB->>ActorB: startProcessing() job receives message
    ActorB->>ActorB: processMessageWithTimeout(message)
    
    alt Successful processing
        ActorB->>ActorB: Process message
        ActorB->>ActorB: Record metrics
    else Timeout occurs
        ActorB->>ActorB: handleProcessingTimeout(message)
        ActorB->>ActorB: Record error
    else Error occurs
        ActorB->>ActorB: handleMessageProcessingError(error, message)
        ActorB->>ActorB: Record error
    end
    
    Note over ActorA, ActorB: Cleanup Phase
    ActorA->>PortA: dispose()
    ActorB->>PortB: dispose()
```

This sequence diagram illustrates the communication flow between two actors:

1. **Initialization Phase**:
   - Actor A creates an output port
   - Actor B creates an input port

2. **Connection Phase**:
   - A connection is established between the output port of Actor A and the input port of Actor B

3. **Communication Phase**:
   - Actor A sends a message through its output port
   - The message is processed by handlers and conversion rules
   - The processed message is sent through the channel to Actor B's input port
   - Actor B receives and processes the message
   - Different paths are shown for successful processing, timeout, and error scenarios

4. **Cleanup Phase**:
   - Both actors dispose of their ports when they're no longer needed

This diagram demonstrates the asynchronous, message-passing nature of the actor system and how type-safe communication is maintained between actors.