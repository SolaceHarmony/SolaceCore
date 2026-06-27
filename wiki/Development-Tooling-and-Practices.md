<!-- topic: Reference -->
<!-- title: Development Tooling and Practices -->

[← Architecture Overview](Architecture-Overview) · §8 of 15

---

## 8. Development Tooling and Practices

The tooling below keeps code quality, consistency, and maintainability tight across the project.

### 8.1. Static Analysis (Qodana)
SolaceCore utilizes Qodana, JetBrains' static analysis engine, for comprehensive code quality checks. The configuration is managed via the [`qodana.yaml`](qodana.yaml) file in the project root.

Key configuration aspects from [`qodana.yaml`](qodana.yaml) include:

*   **Qodana Configuration Version:** `1.0` (as specified by `version: "1.0"`)
*   **Inspection Profile:** The project uses the `qodana.starter` profile (defined under `profile: name: qodana.starter`). This profile includes a baseline set of inspections recommended by JetBrains to get started with Qodana, covering common issues and best practices.
*   **Project JDK:** The analysis is configured to run with JDK version `19` (specified as `projectJDK: 19`). This setting is typically applied in the CI/CD pipeline environment where Qodana executes.
*   **Linter:** The `jetbrains/qodana-jvm:latest` linter is specified (via `linter: jetbrains/qodana-jvm:latest`). This indicates that Qodana is configured for JVM-based projects, aligning with SolaceCore's Kotlin/JVM nature.

The [`qodana.yaml`](qodana.yaml) file also provides commented-out sections for more advanced configurations, such as:
*   Including or excluding specific inspections by their ID (e.g., `#include: - name: <SomeEnabledInspectionId>`).
*   Defining bootstrap commands to be executed before Qodana runs (e.g., `#bootstrap: sh ./prepare-qodana.sh`), useful for environment setup.
*   Installing specific IDE plugins that Qodana can leverage during analysis (e.g., `#plugins: - id: <plugin.id>`).

Currently, these advanced options are not actively used in the provided configuration, relying on the default behavior of the `qodana.starter` profile and the specified JVM linter. Regular execution of Qodana helps identify potential bugs, performance issues, and deviations from coding standards early in the development cycle, contributing to the overall robustness and quality of the SolaceCore framework.

---

← [§7 Build System and Dependencies](Build-System-and-Dependencies)  ·  [Architecture Overview](Architecture-Overview)  ·  [§9 JVM-Specific Utilities (`io.github.solaceharmony.core.util`)](JVM-Utilities) →
