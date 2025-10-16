# Interactive Actor Graph View

The Actor Graph View provides a visual, interactive representation of your actor system, displaying actors as nodes and communication channels as edges in a force-directed graph layout.

## Features

### Visual Layout
- **Force-Directed Layout**: Uses the Fruchterman-Reingold algorithm to automatically position actors based on their connections
- **State Visualization**: Actors are color-coded by their current state
  - 🟢 Green: Running
  - ⚫ Gray: Stopped  
  - 🔴 Red: Error
  - 🟠 Orange: Paused
  - 🔵 Blue: Initialized

### Interactive Controls

#### Pan & Zoom
- **Pan**: Click and drag to move the graph
- **Zoom**: Pinch to zoom in/out (or use scroll wheel on desktop)
- Scale range: 0.5x to 3x

#### Node Selection
- **Click** any node to select it and view details
- **Click again** to deselect
- Selected nodes are highlighted with:
  - Larger center indicator
  - Primary color text
  - Highlighted incident edges

### Details Panel

When an actor is selected, a details panel appears showing:

#### State & Controls
- Current state badge with description
- Lifecycle control buttons:
  - **Start**: Start a stopped/initialized actor
  - **Pause**: Pause a running actor
  - **Resume**: Resume a paused actor
  - **Stop**: Stop a running/paused actor
  - **Delete**: Remove the actor from the system

#### Key Performance Indicators (KPIs)
- **Received**: Total messages received
- **Processed**: Total messages processed successfully
- **Failed**: Total failed messages
- **Success Rate**: Percentage of successful message processing
- **Avg Time**: Average processing time in milliseconds

#### Performance Charts
- **Throughput**: Visual bar showing processed vs received messages
- **Latency**: Average processing time indicator
- **Error Rate**: Failed messages visualization

## Usage

The Actor Graph View is automatically included in the main monitoring interface when you start the application. It appears above the traditional list views.

### Getting Started

1. **Start Monitoring**: Click the "Start" button in the monitoring controls
2. **View the Graph**: The graph automatically populates with current actors
3. **Explore**: Use pan/zoom to navigate
4. **Inspect**: Click any node to see detailed metrics and controls

### Tips

- 💡 The layout recalculates automatically when actors are added or removed
- 💡 Edge colors indicate channel health (green=connected, red=error, gray=disconnected)
- 💡 Use the graph for quick visual identification of problem areas
- 💡 Selection highlights help trace message flow through connected actors

## Accessibility

The Actor Graph View includes accessibility features:
- **Semantic Roles**: Nodes are marked as buttons for screen readers
- **Content Descriptions**: Each node includes actor name and state information
- **Keyboard Navigation**: Nodes can be focused and activated via keyboard
- **Color Contrast**: All colors meet WCAG contrast guidelines

## Architecture

### Components

- **ForceDirectedLayout**: Implements the graph layout algorithm
- **ActorGraphCanvas**: Main interactive canvas with pan/zoom
- **ActorDetailsPanel**: Shows detailed information and controls
- **ActorGraphView**: Top-level component that integrates everything

### Performance

- Layout calculations are cached and only recompute on topology changes
- Edges are drawn with Canvas API for efficient rendering
- Node composables are lightweight and minimal

### Platform Support

- ✅ Desktop (JVM): Full support
- ✅ JavaScript (Wasm): Full support
- 🔜 iOS/Android: Planned for future release

## Development

### Running Tests

```bash
./gradlew :composeApp:allTests
```

### Building

```bash
# Desktop
./gradlew :composeApp:desktopJar

# JavaScript
./gradlew :composeApp:jsBrowserProductionWebpack
```

## Future Enhancements

Potential improvements for future releases:

1. **Advanced Charts**: Integration with KoalaPlot for time-series visualizations
2. **Layout Persistence**: Save/restore graph layout across sessions
3. **Large Graph Support**: Optimizations for graphs with >200 nodes
4. **Custom Layouts**: Alternative layout algorithms (hierarchical, circular)
5. **Filtering**: Hide/show specific actors based on state or metrics
6. **Export**: Save graph as image or data export

## Contributing

When contributing to the Actor Graph View:
- Maintain multiplatform compatibility (commonMain)
- Add tests for new layout algorithms
- Follow existing accessibility patterns
- Update documentation for new features

## References

- Fruchterman, T. M. J., & Reingold, E. M. (1991). "Graph drawing by force-directed placement"
- Compose Multiplatform: https://www.jetbrains.com/compose-multiplatform/
- Material Design 3: https://m3.material.io/
