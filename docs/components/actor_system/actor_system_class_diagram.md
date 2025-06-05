# Actor System Class Diagram

```mermaid
classDiagram
    class Lifecycle {
        <<interface>>
        +start() suspend
        +stop() suspend
        +isActive() boolean
        +dispose() suspend
    }
    
    class Disposable {
        <<interface>>
        +dispose() suspend
    }
    
    class Actor {
        <<abstract>>
        -id: String
        -name: String
        -scope: CoroutineScope
        -state: ActorState
        -metrics: ActorMetrics
        -ports: Map~String, TypedPort~
        -jobs: List~Job~
        +start() suspend
        +stop() suspend
        +isActive() boolean
        +dispose() suspend
        +createPort() suspend
        +getPort() suspend
        +getMetrics() suspend
        +pause(reason: String) suspend
        +resume() suspend
        #handleMessageProcessingError(error: Throwable, message: Any) suspend
        #onError(error: Throwable, message: Any) suspend
    }
    
    class TypedPort~T~ {
        -port: Port~T~
        -messageClass: KClass~T~
        -handler: suspend (T) -> Unit
        -bufferSize: Int
        -processingTimeout: Duration
        +send(message: T) suspend
        +startProcessing() Job
        -processMessageWithTimeout(message: T) suspend
        -processMessage(message: T) suspend
        -handleProcessingTimeout(message: T) suspend
    }
    
    class Port~T~ {
        <<interface>>
        +id: String
        +name: String
        +type: KClass~T~
        +asChannel() Channel~T~
        +send(message: T) suspend
    }
    
    class BidirectionalPort~T~ {
        -channel: Channel~T~
        -handlers: List~MessageHandler~
        -conversionRules: List~ConversionRule~
        +asChannel() Channel~T~
        +addHandler(handler: MessageHandler~T, T~)
        +addConversionRule(rule: ConversionRule~T, T~)
        +send(message: T) suspend
        +receive() T suspend
        +dispose() suspend
    }
    
    class ActorState {
        <<sealed class>>
    }
    
    class Initialized {
        <<object>>
    }
    
    class Running {
        <<object>>
    }
    
    class Stopped {
        <<object>>
    }
    
    class Error {
        -reason: String
    }
    
    class Paused {
        -reason: String
    }
    
    class ActorMetrics {
        -messageReceived: AtomicLong
        -messageProcessed: AtomicLong
        -errors: AtomicLong
        -processingTimes: List~Duration~
        +recordMessageReceived()
        +recordMessageProcessed()
        +recordError()
        +recordProcessingTime(duration: Duration)
        +getMetrics() Map~String, Any~
    }
    
    Lifecycle <|-- Actor
    Disposable <|-- Lifecycle
    Actor *-- TypedPort
    Actor *-- ActorMetrics
    Actor o-- ActorState
    TypedPort o-- Port
    Port <|.. BidirectionalPort
    ActorState <|-- Initialized
    ActorState <|-- Running
    ActorState <|-- Stopped
    ActorState <|-- Error
    ActorState <|-- Paused
```

This class diagram illustrates the key classes in the actor system and their relationships:

1. **Actor** implements the **Lifecycle** interface, which extends **Disposable**
2. **Actor** contains multiple **TypedPort** instances for communication
3. **TypedPort** wraps a **Port** interface, which is implemented by **BidirectionalPort**
4. **Actor** maintains an **ActorState** (which can be Initialized, Running, Stopped, Error, or Paused)
5. **Actor** uses **ActorMetrics** for performance monitoring

The diagram shows the inheritance relationships (solid lines with triangular arrowheads), composition relationships (solid lines with filled diamonds), and aggregation relationships (solid lines with empty diamonds).