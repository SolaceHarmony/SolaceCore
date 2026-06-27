<!-- topic: Runtime -->
<!-- title: Storage & Persistence -->

# SolaceCore Storage System Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Storage Core Interfaces](Storage-Core-Interfaces)
4. [Storage In-Memory Implementations](Storage-In-Memory-Implementations)
5. [Storage File-Based Implementations](Storage-File-Based-Implementations)
6. [Storage Transactions](Storage-Transactions)
7. [Storage Thread Safety Guide](Storage-Thread-Safety-Guide)
8. [Storage Testing](Storage-Testing)
9. [Storage Status and Future Plans](Storage-Status-and-Future-Plans)
10. [Storage Usage Examples](Storage-Usage-Examples)
11. [Storage Serialization Compression Encryption](Storage-Serialization-Compression-Encryption)

## Overview

The SolaceCore Storage System provides a flexible and extensible way to store and retrieve data for the SolaceCore framework. It is designed to handle different types of data, including configuration data and actor state data, and to support different storage backends. The system is built with thread safety, performance, and reliability in mind, with special attention to preventing deadlocks in concurrent environments.

## Related Topics

- [Lifecycle & Resources](Lifecycle-and-Resources): storage managers share the lifecycle/disposal contract.
- [Actor System](Actor-System): actor state and metrics are storage use cases.
- [Workflow Orchestration](Workflow-Orchestration): workflow state persistence is a planned storage use case.
- [Memory & Reflection](Memory-and-Reflection): companion memory sits above the storage substrate.
- [Shared Memory](Shared-Memory): concurrency-oriented memory design related to storage safety.
- [Storage Module Architecture](Storage-Module-Architecture): component-level architecture deep dive for `io.github.solaceharmony.core.storage`.
- [Storage Checklist](Storage-Checklist): implementation checklist split from the storage reference.
- [Storage Thread Safety and Deadlock Prevention](Storage-Thread-Safety-and-Deadlock-Prevention): architecture appendix for storage locking patterns.
- [Storage Core Interfaces](Storage-Core-Interfaces): storage, configuration, actor-state, manager, and transaction interfaces.
- [Storage Transactions](Storage-Transactions): transactional storage contract and usage.
- [Storage Usage Examples](Storage-Usage-Examples): basic, transactional, custom, and file-backed examples.

## Architecture

The storage system is built around a set of interfaces that define the contract for storage operations. These interfaces are implemented by different storage backends, allowing for flexibility in how data is stored and retrieved. The system also includes transaction support for atomic operations, ensuring data consistency even in the face of failures.

The architecture follows these key principles:
- **Separation of concerns**: Each component has a specific responsibility
- **Interface-based design**: Components interact through well-defined interfaces
- **Thread safety**: All operations are thread-safe
- **Extensibility**: New storage backends can be easily added
- **Reliability**: The system is designed to prevent deadlocks and handle errors gracefully

---

## Storage Implementation Checklist

The implementation checklist has moved to [Storage Checklist](Storage-Checklist).

## Storage Module Architecture

The architecture deep dive has moved to [Storage Module Architecture](Storage-Module-Architecture).

---

## Storage Thread Safety and Deadlock Prevention

The architecture appendix has moved to [Storage Thread Safety and Deadlock Prevention](Storage-Thread-Safety-and-Deadlock-Prevention).
