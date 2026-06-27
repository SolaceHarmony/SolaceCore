<!-- topic: Orientation -->
<!-- title: Framework Port System -->

## 4. Port System

### 4.1 Port Design

The port system enables type-safe communication between actors through a well-defined interface:

- **Port Interface**: Defines methods for sending messages and accessing the underlying channel
- **BidirectionalPort Implementation**: Concrete implementation supporting bidirectional communication
- **TypedPort Wrapper**: Associates a port with a specific message type and processing handler

### 4.2 Port Connections

Ports are connected to establish communication pathways between actors:

- **Type Compatibility**: Connections are only allowed between ports with compatible message types
- **Validation**: The system validates port connections to ensure type safety
- **Multiple Connections**: Ports can be connected to multiple other ports

### 4.3 Message Processing

Messages flowing through ports undergo several processing stages:

1. **Sending**: Actor sends a message through an output port
2. **Handler Application**: Message handlers process the message
3. **Conversion**: Type conversion rules transform the message if needed
4. **Channel Transmission**: Message is sent through the underlying channel
5. **Reception**: Receiving actor's input port receives the message
6. **Processing**: Message is processed by the receiving actor



[Back to Solace Core Framework Architecture](Solace-Core-Framework-Architecture)
