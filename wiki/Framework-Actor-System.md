<!-- topic: Orientation -->
<!-- title: Framework Actor System -->

## 3. Actor System

### 3.1 Actor Model Implementation

The actor model forms the backbone of the Solace Core Framework, providing a concurrency model based on message passing rather than shared state. Each actor is an independent unit of computation with the following characteristics:

- **Encapsulated State**: Actors maintain their own state, which is not directly accessible by other actors
- **Message-Driven**: Actors communicate exclusively through asynchronous message passing
- **Reactive Processing**: Actors respond to incoming messages, process them, and potentially send messages to other actors
- **Lifecycle Management**: Actors follow a defined lifecycle (initialization, running, paused, stopped, error)

### 3.2 Actor Structure

The base `Actor` class implements the following interfaces and components:

- **Lifecycle Interface**: Defines methods for starting, stopping, and checking the active state of an actor
- **Disposable Interface**: Ensures proper resource cleanup when an actor is no longer needed
- **Coroutine Scope**: Each actor operates within its own coroutine scope for asynchronous processing
- **Ports**: Actors expose typed ports for input and output communication
- **Metrics Collection**: Built-in mechanisms for gathering performance metrics

### 3.3 Actor Lifecycle

Actors in the framework follow a well-defined lifecycle:

1. **Initialization**: Actor is created and resources are allocated
2. **Starting**: Actor begins processing messages and transitions to the Running state
3. **Running**: Actor actively processes messages
4. **Paused**: Actor temporarily suspends processing but maintains state
5. **Stopped**: Actor ceases processing and prepares for disposal
6. **Error**: Actor enters an error state when exceptions occur
7. **Disposal**: Actor releases all resources and terminates

### 3.4 Actor Communication

Actors communicate through a port system that provides type-safe message passing:

- **Typed Ports**: Each port is associated with a specific message type
- **Channels**: Kotlin coroutine channels provide the underlying message passing mechanism
- **Message Handlers**: Custom processing logic can be applied to messages during transmission
- **Conversion Rules**: Enable type transformations between compatible message types



[Back to Solace Core Framework Architecture](Solace-Core-Framework-Architecture)
