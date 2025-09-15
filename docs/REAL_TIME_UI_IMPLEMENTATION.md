# SolaceCore Real-Time UI Implementation

## Overview

This implementation provides a comprehensive real-time UI for the SolaceCore actor system using JetBrains' Compose Multiplatform. The UI follows a declarative approach and enables real-time monitoring and interaction with the actor system across multiple platforms (Desktop and Web).

## Key Features

### 1. Real-Time Actor Monitoring Dashboard

The main dashboard provides a comprehensive view of the entire actor system:

- **System Overview Cards**: Display total actors, running actors, error actors, and system-wide metrics
- **Live Metrics**: Real-time updates of message throughput, average response times, and system uptime
- **Visual Indicators**: Color-coded cards and status indicators for immediate system health assessment

### 2. Individual Actor Monitoring

Each actor is displayed with detailed real-time information:

- **Actor State Badges**: Color-coded status indicators
  - 🟢 **Running** (Green): Actor is actively processing
  - 🔴 **Error** (Red): Actor encountered an error
  - 🟠 **Paused** (Orange): Actor is temporarily paused
  - 🔵 **Initialized** (Blue): Actor is ready but not started
  - ⚫ **Stopped** (Gray): Actor has been stopped

- **Real-Time Metrics**: 
  - Messages received, processed, and failed
  - Success rate percentage
  - Average and last processing times
  - Port-specific and priority-based metrics

### 3. Interactive Controls

- **Start/Stop Monitoring**: Toggle real-time updates on/off
- **Manual Refresh**: Force immediate data update
- **Connection Status**: Visual indicator showing real-time connection state

### 4. Architecture Features

#### Real-Time Data Flow
```kotlin
RealTimeActorService -> StateFlow -> UI Components -> Auto-Updates
```

- Uses Kotlin Coroutines and StateFlow for reactive updates
- 1-second refresh interval for live data
- Efficient state management with minimal re-compositions

#### Cross-Platform Support
- **Desktop (JVM)**: Full Material3 components with native performance
- **Web (JS)**: HTML DOM integration with Compose for Web
- **Shared Logic**: Common business logic in `commonMain` source set

#### Component Architecture
```
SolaceRealTimeUI (Main App)
├── SystemMetricsDashboard (System Overview)
├── MonitoringControlPanel (Interactive Controls)
└── ActorListView (Actor Details)
    └── ActorItemView (Individual Actor Cards)
        ├── ActorStatusBadge (State Indicator)
        └── ActorMetricsView (Live Metrics)
```

## Data Models

### ActorDisplayData
Represents UI-optimized actor information:
- ID, name, and current state
- Last update timestamp
- Comprehensive metrics data

### SystemMetricsData
System-wide statistics:
- Actor counts by state
- Total message throughput
- Average response times
- System uptime

### RealTimeActorService
Core service providing:
- Live data streaming via StateFlow
- Configurable update intervals
- Start/stop monitoring controls
- Future integration points for actual actor system

## Integration Points

The current implementation includes simulation data and is designed for easy integration with the actual SolaceCore actor system:

1. **Supervisor Integration**: `RealTimeActorService` can be extended to connect with `SupervisorActor`
2. **Actor Metrics**: Direct integration with `ActorMetrics` from the core system
3. **State Synchronization**: Real-time state updates from actual actors
4. **Control Interface**: Send commands to actors through the UI

## Running the Application

### Desktop Application
```bash
./gradlew :composeApp:desktopRun
```

### Web Application  
```bash
./gradlew :composeApp:webBrowserRun
```

### Build Verification
```bash
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinWeb
```

## Example UI Flow

1. **Launch Application**: Real-time monitoring starts automatically
2. **System Dashboard**: View system-wide health and metrics
3. **Actor List**: Monitor individual actor states and performance
4. **Interactive Controls**: Start/stop monitoring or force refresh
5. **Real-Time Updates**: All components update every second with live data

## Technical Implementation

- **Framework**: Compose Multiplatform 1.7.0
- **Language**: Kotlin 2.0.21 with coroutines
- **UI Library**: Material3 design system
- **Reactive Updates**: StateFlow and collectAsState
- **Data Simulation**: Realistic actor metrics for demonstration
- **Cross-Platform**: Shared business logic, platform-specific rendering

This implementation fulfills the requirement for a "declarative UI framework for creating shared user interfaces" that "allows developers to write UI code once and deploy it to multiple targets, maintaining a consistent look and feel while enabling real-time UI updates."