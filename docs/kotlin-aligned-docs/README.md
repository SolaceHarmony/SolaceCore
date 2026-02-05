# Kotlin-Aligned Documentation

This folder contains documentation from the original codex-vendored sources, translated and adapted to align with the SolaceCore Kotlin project.

## Overview

The original documentation was written for a TypeScript-based system (Magentic Codex). These documents have been updated to:

- Reference Kotlin code paths and file structures
- Include Kotlin-specific examples and best practices
- Update build commands and development workflows
- Align with the actor-based architecture of SolaceCore
- Include Kotlin multiplatform considerations
- Update configuration examples for JSON-first approach

## Translated Documents

### Core Documentation
- **[README.md](README.md)** - Main documentation index with Kotlin paths
- **[ARCHITECTURE_OVERVIEW.md](ARCHITECTURE_OVERVIEW.md)** - System architecture adapted for Kotlin/actors
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Current implementation status with Kotlin metrics
- **[QUICK_START_DEVELOPERS.md](QUICK_START_DEVELOPERS.md)** - Developer guide with Kotlin examples

## Key Changes Made

### File Paths
- `src/cli.tsx` → `composeApp/src/desktopMain/kotlin/`
- `src/shared/neutral-history.ts` → `lib/src/commonMain/kotlin/com/solacecore/neutral/`
- `src/providers/*` → `lib/src/commonMain/kotlin/com/solacecore/providers/`
- `src/mcp/mcp-bridge.ts` → `lib/src/commonMain/kotlin/com/solacecore/mcp/`

### Technology Updates
- **TypeScript** → **Kotlin**
- **React Ink TUI** → **Compose Multiplatform**
- **Node.js/npm** → **Gradle/Kotlin**
- **CLI commands** → **Gradle tasks**
- **JavaScript objects** → **Kotlin data classes**

### Architecture Alignment
- **Agent loop** → **Actor system**
- **Event emitters** → **Kotlin Flow**
- **Promises/async** → **Coroutines**
- **TypeScript interfaces** → **Kotlin interfaces**
- **Module system** → **Kotlin multiplatform**

### Code Examples
- Added Kotlin DSL examples for pipeline configuration
- Included actor creation and message passing examples
- Added coroutine-based async operation examples
- Included null safety and type safety examples

## Integration with Kotlin Plans

This documentation complements the [kotlin-plans](../kotlin-plans/) folder by providing:

- **Documentation**: Human-readable guides and specifications
- **Code Plans**: Executable Kotlin interfaces and implementations

Together they provide both the "what" (documentation) and the "how" (code plans) for implementing SolaceCore features.

## Usage

These documents serve as:
- **Developer onboarding** for new team members
- **Architecture documentation** for system understanding
- **Implementation guides** for feature development
- **Reference material** for maintenance and debugging

## Maintenance

When updating these documents:
1. Keep them synchronized with the kotlin-plans implementations
2. Update file paths if the project structure changes
3. Include Kotlin-specific examples and best practices
4. Maintain alignment with the actor-based architecture
5. Update metrics and status information regularly

## Related Resources

- **[kotlin-plans](../kotlin-plans/)** - Executable Kotlin code plans
- **[components](../components/)** - Component-specific documentation
- **[examples](../examples/)** - Usage examples and tutorials
- **[CONTRIBUTING.md](../CONTRIBUTING.md)** - Contribution guidelines