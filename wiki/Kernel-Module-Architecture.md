<!-- topic: Runtime -->
<!-- title: Kernel Module Architecture -->

[← Architecture Overview](Architecture-Overview) · §1 of 15

---

## 1. Kernel Module
### 1.0. Kernel Design Principles and Overview
The kernel is a foundational component of the Solace Core Framework, providing the underlying infrastructure for communication and resource management. It includes the channels and ports system, which enables type-safe message passing between actors and other components. The kernel's design is guided by the following principles:

*   **Type Safety:** All communication is strongly typed to ensure compatibility.
*   **Resource Management:** Proper cleanup of resources through the `Disposable` interface is a key consideration.
*   **Flexibility:** The system supports different types of communication patterns.
*   **Extensibility:** It is designed to be easy to extend with new port types and protocol adapters.
*   **Concurrency:** Built for concurrent operations using Kotlin coroutines.

The `kernel` module forms the foundational layer of SolaceCore's common library, providing core abstractions and services primarily focused on its robust channels and ports system.



[Back to Kernel & Ports](Kernel-and-Ports)
