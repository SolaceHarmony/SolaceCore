# Kernel Class Diagram

```mermaid
classDiagram
    class Disposable {
        <<interface>>
        +dispose() suspend
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
    
    class MessageHandler~IN, OUT~ {
        <<interface>>
        +handle(message: IN) OUT suspend
    }
    
    class ProtocolAdapter~SOURCE, TARGET~ {
        <<interface>>
        +encode(source: SOURCE) TARGET suspend
        +decode(target: TARGET) SOURCE suspend
        +canHandle(sourceType: KClass, targetType: KClass) boolean
    }
    
    class ConversionRule~IN, OUT~ {
        <<abstract>>
        +convert(input: IN) OUT suspend
        +canHandle(inputType: KClass, outputType: KClass) boolean
        +describe() String
    }
    
    class PortConnection~IN, OUT~ {
        +sourcePort: Port~IN~
        +targetPort: Port~OUT~
        +handlers: List~MessageHandler~
        +protocolAdapter: ProtocolAdapter
        +rules: List~ConversionRule~
        +validateConnection()
        -canConnect() boolean
        -validateConversionChain() boolean
        -buildConnectionErrorMessage() String
    }
    
    class PortException {
        <<abstract>>
        +message: String
        +cause: Throwable
    }
    
    class ValidationException {
        +message: String
        +cause: Throwable
    }
    
    class ConnectionException {
        +message: String
        +cause: Throwable
    }
    
    class PortConnectionException {
        +sourceId: String
        +targetId: String
        +message: String
    }
    
    Disposable <|-- Port
    Port <|.. BidirectionalPort
    Port --o PortConnection : source
    Port --o PortConnection : target
    MessageHandler --o PortConnection : uses
    ProtocolAdapter --o PortConnection : uses
    ConversionRule --o PortConnection : uses
    PortException <|-- ValidationException
    PortException <|-- ConnectionException
    ConnectionException <|-- PortConnectionException
```

This class diagram illustrates the key classes in the kernel component and their relationships:

1. **Port** interface extends **Disposable** and defines the contract for communication ports
2. **BidirectionalPort** implements the **Port** interface, providing concrete functionality
3. **PortConnection** represents a connection between two ports, using **MessageHandler**, **ProtocolAdapter**, and **ConversionRule** for message processing
4. **MessageHandler**, **ProtocolAdapter**, and **ConversionRule** define interfaces for message processing and conversion
5. **PortException** and its subclasses represent various error conditions in the port system

The diagram shows the inheritance relationships (solid lines with triangular arrowheads), implementation relationships (dashed lines with triangular arrowheads), and aggregation relationships (solid lines with diamond arrowheads).