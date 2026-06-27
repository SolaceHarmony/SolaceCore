<!-- topic: Runtime -->
<!-- title: Storage Module Architecture -->

## 3. Storage Module (`io.github.solaceharmony.core.storage`)
### 3.0. Strategic Storage Vision
Beyond the currently implemented storage solutions detailed below, the `Storage_Solutions_Design.md` document outlines a strategic vision for future data management capabilities within SolaceCore. This vision centers on a hybrid approach combining graph database technology with Kotlin-native storage solutions.

*   **Overarching Goal:** To provide robust and scalable data management by integrating Neo4j for complex data relationships and a Kotlin-native solution for structured data needs.

*   **Planned Neo4j Integration:**
    *   **Purpose:** To utilize Neo4j for graph-based storage, primarily for representing and querying relationships between actors, knowledge nodes, data flow, and other interconnected entities within the system.
    *   **Intended Use Cases:** Envisioned to support advanced scenarios such as Retrieval-Augmented Generation (RAG) by enabling intelligent and context-aware data retrieval based on these relationships.

*   **Planned Kotlin-Native Storage:**
    *   **Purpose:** To implement a Kotlin-native storage solution tailored for tabular or relational-style data.
    *   **Intended Use Cases:** To ensure actors and other components can persistently store intermediate results, structured states, and other non-graph data efficiently.

*   **Implementation Status (as per design document):**
    *   At the time the design document was authored, both the Neo4j integration and the Kotlin-native storage solution were in the planning stages and had not yet been implemented.

*   **Envisioned Future Enhancements for this Strategic Vision:**
    *   **Data Synchronization:** Mechanisms to synchronize data between the graph database (Neo4j) and the local Kotlin-native storage, ensuring consistency across different data models.
    *   **Advanced Querying:** Development of advanced querying capabilities that can leverage both storage types to support complex data retrieval and manipulation tasks.

This strategic direction suggests a future where SolaceCore can handle a diverse range of data types and relationships with specialized, high-performance storage backends, complementing the existing flexible storage abstractions.
The `storage` module provides a comprehensive framework for data persistence and management within SolaceCore. It defines core abstractions for storage operations, transaction management, serialization, and a centralized manager for accessing different storage implementations.

### 3.1. Subsystem Topics

- [Storage Abstractions Architecture](Storage-Abstractions-Architecture)
- [Storage Specialized Interfaces Architecture](Storage-Specialized-Interfaces-Architecture)
- [Storage In-Memory Architecture](Storage-In-Memory-Architecture)
- [Storage Caching Subsystem](Storage-Caching-Subsystem)
- [Actor State Recovery Subsystem](Actor-State-Recovery-Subsystem)
- [Storage File-Based Architecture](Storage-File-Based-Architecture)
- [Storage Compression Subsystem](Storage-Compression-Subsystem)
- [Storage Encryption Subsystem](Storage-Encryption-Subsystem)
- [Storage JVM Serialization Utilities](Storage-JVM-Serialization-Utilities)
- [Actor State Serialization Subsystem](Actor-State-Serialization-Subsystem)
