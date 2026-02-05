# Workflow Management Design

## Overview
WorkflowManager orchestrates a network of actors: it adds/removes actors, connects their ports, and manages lifecycle transitions. The manager validates type compatibility for connections and starts routing between ports when the workflow starts.

## Start/Stop Ordering
- `start()`
  1. Starts all actors (transition to Running).
  2. Establishes all configured connections by resolving ports (by name and type) and creating `Port.PortConnection` objects.
  3. Starts routing for each connection (a coroutine relays messages from source to target with handlers/adapters/rules as configured).

- `stop()`
  1. Stops-and-joins all active port connections (cancels routing jobs and waits for completion) to avoid sending into closed channels.
  2. Stops all actors (cancels input-processing jobs; ports are preserved).

This ordering minimizes race conditions and prevents `ClosedSendChannelException` during shutdown.

## Pause/Resume Status
- Actor-level pause/resume is supported (`Actor.pause(reason)`, `Actor.resume()`), suspending input processing on a per-actor basis.
- Workflow-level pause/resume is minimal; future enhancements may provide coordinated pause/resume semantics across all actors and connections.

## Connections
- Connections are validated (`validateConnection()`), then started (`start(scope)`).
- `stopAndJoin()` is used during workflow shutdown to ensure a clean termination of routing jobs before stopping actors.

## Failure Handling
- Route-time failures (handler/adapter/conversion) raise `PortException.Validation` and stop routing.
- Start-time validation failures surface as `PortConnectionException` with source/target identifiers and descriptive messages.

