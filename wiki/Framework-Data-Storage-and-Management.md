<!-- topic: Orientation -->
<!-- title: Framework Data Storage and Management -->

## 7. Data Storage and Management

### 7.1 Storage Architecture

The framework uses a hybrid storage approach:

- **Graph Database (Neo4j)**: For storing relationships between actors, knowledge nodes, and data flow
- **Kotlin-Native Storage**: For structured data needs, supporting tabular or relational-style data

### 7.2 Persistence Mechanisms

Actor state persistence is a key capability:

- **State Serialization**: Converting actor state to a serializable format
- **Storage Integration**: Saving and loading state from the storage system
- **Recovery Mechanisms**: Restoring actor state after system restarts or failures

### 7.3 Implementation Plan

Storage integration is currently in the planning phase:

- **Neo4j Integration**: Planned for representing actor relationships and knowledge graphs
- **Kotlin-Native Storage**: Planned for structured data and actor state persistence
- **Data Synchronization**: Mechanisms for keeping data consistent across the system



[Back to Solace Core Framework Architecture](Solace-Core-Framework-Architecture)
