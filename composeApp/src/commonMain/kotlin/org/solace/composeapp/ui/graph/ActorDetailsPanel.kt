package org.solace.composeapp.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.AgentType
import org.solace.composeapp.ui.data.ChatMessage
import org.solace.composeapp.ui.data.ChatRole
import org.solace.composeapp.ui.data.LogEntry
import org.solace.composeapp.ui.data.LogLevel
import org.solace.composeapp.ui.data.TaskInfo
import org.solace.composeapp.ui.service.RealTimeActorService

/**
 * Tabbed details panel for the selected actor. Three tabs:
 *   - Overview: state, lifecycle controls, KPIs, mini charts (the original panel content).
 *   - Activity: current task + live log stream.
 *   - Chat:     conversational interface, only available for AI-typed agents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailsPanel(
    actor: ActorDisplayData?,
    actorService: RealTimeActorService,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (actor == null) return

    // Track tab selection so it survives recomposition; but reset to Overview when the user
    // selects a different actor.
    var selectedTab by rememberSaveable(actor.id) { mutableStateOf(DetailsTab.Overview) }

    // Hide the Chat tab for non-AI agents.
    val tabs = remember(actor.agentType) {
        buildList {
            add(DetailsTab.Overview)
            add(DetailsTab.Activity)
            if (actor.agentType == AgentType.AI) add(DetailsTab.Chat)
        }
    }
    // If the active tab disappears (e.g. user clicked a Standard actor), fall back to Overview.
    if (selectedTab !in tabs) selectedTab = DetailsTab.Overview

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header — pinned, above the tabs.
            DetailsHeader(actor = actor, onClose = onClose)

            // Tab row
            TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
                tabs.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }

            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    DetailsTab.Overview -> OverviewTab(actor = actor, actorService = actorService)
                    DetailsTab.Activity -> ActivityTab(actor = actor, actorService = actorService)
                    DetailsTab.Chat -> ChatTab(actor = actor, actorService = actorService)
                }
            }
        }
    }
}

private enum class DetailsTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Overview("Overview", Icons.Default.Dashboard),
    Activity("Activity", Icons.AutoMirrored.Filled.List),
    Chat("Chat", Icons.AutoMirrored.Filled.Chat)
}

/* ---------- Header ---------- */

@Composable
private fun DetailsHeader(actor: ActorDisplayData, onClose: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = actor.name, style = MaterialTheme.typography.headlineSmall)
                    AgentTypeBadge(actor.agentType)
                }
                Text(
                    text = "ID: ${actor.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!actor.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = actor.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
private fun AgentTypeBadge(type: AgentType) {
    val (label, color) = when (type) {
        AgentType.AI -> "AI" to Color(0xFF7C4DFF)
        AgentType.System -> "System" to Color(0xFF607D8B)
        AgentType.Standard -> "Standard" to Color(0xFF4CAF50)
    }
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/* ---------- Overview tab (state + KPIs, the original content) ---------- */

@Composable
private fun OverviewTab(actor: ActorDisplayData, actorService: RealTimeActorService) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ActorStateBadge(state = actor.state)
        Spacer(Modifier.height(16.dp))

        LifecycleControls(actor = actor, actorService = actorService)
        Spacer(Modifier.height(16.dp))

        if (actor.capabilities.isNotEmpty()) {
            Text("Capabilities", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            CapabilityChips(actor.capabilities)
            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Key Performance Indicators", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        KPICards(actor = actor)

        Spacer(Modifier.height(16.dp))
        Text("Performance Metrics", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MiniCharts(actor = actor)
    }
}

@Composable
private fun CapabilityChips(capabilities: List<String>) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        capabilities.forEach { cap ->
            AssistChip(onClick = {}, label = { Text(cap) })
        }
    }
}

/* ---------- Activity tab ---------- */

@Composable
private fun ActivityTab(actor: ActorDisplayData, actorService: RealTimeActorService) {
    val logsByActor by actorService.logs.collectAsState()
    val logs = logsByActor[actor.id].orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Current task card
        CurrentTaskCard(actor.currentTask, isRunning = actor.state is ActorState.Running)

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "${logs.size} entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))

        // Log stream — newest at the bottom, auto-scrolls.
        val listState = rememberLazyListState()
        LaunchedEffect(logs.size) {
            if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No activity yet — start monitoring to see log lines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs, key = { it.timestamp.toEpochMilliseconds().toString() + it.message.hashCode() }) { entry ->
                        LogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTaskCard(task: TaskInfo?, isRunning: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isRunning) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRunning) "Currently working on" else "Not currently running",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(8.dp))

            if (task == null) {
                Text("No task assigned.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(task.detail, style = MaterialTheme.typography.bodyMedium)
                if (task.progress != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { task.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Text(
                        "${(task.progress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val (color, label) = when (entry.level) {
        LogLevel.Trace -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) to "TRACE"
        LogLevel.Debug -> MaterialTheme.colorScheme.onSurfaceVariant to "DEBUG"
        LogLevel.Info -> Color(0xFF2196F3) to "INFO "
        LogLevel.Warn -> Color(0xFFFF9800) to "WARN "
        LogLevel.Error -> Color(0xFFF44336) to "ERROR"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formatHms(entry.timestamp.toEpochMilliseconds()),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatHms(epochMs: Long): String {
    // Lightweight HH:mm:ss formatter (multiplatform-safe).
    val totalSec = epochMs / 1000
    val s = (totalSec % 60).toInt()
    val m = ((totalSec / 60) % 60).toInt()
    val h = ((totalSec / 3600) % 24).toInt()
    return buildString {
        append(h.toString().padStart(2, '0'))
        append(':')
        append(m.toString().padStart(2, '0'))
        append(':')
        append(s.toString().padStart(2, '0'))
    }
}

/* ---------- Chat tab ---------- */

@Composable
private fun ChatTab(actor: ActorDisplayData, actorService: RealTimeActorService) {
    val chatsByActor by actorService.chats.collectAsState()
    val messages = chatsByActor[actor.id].orEmpty()

    var input by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Transcript
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Say hi to ${actor.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.timestamp.toEpochMilliseconds().toString() + it.role.name + it.content.hashCode() }) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message ${actor.name}…") },
                singleLine = false,
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val toSend = input.trim()
                    if (toSend.isNotEmpty()) {
                        actorService.sendChatMessage(actor.id, toSend)
                        input = ""
                    }
                },
                enabled = input.isNotBlank() && actor.state is ActorState.Running
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
        if (actor.state !is ActorState.Running) {
            Text(
                "Agent is ${actor.state} — start it to chat.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    val isSystem = message.role == ChatRole.System
    val bg = when {
        isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    }
    val align = if (isUser) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Text(
            text = when (message.role) {
                ChatRole.User -> "you"
                ChatRole.Agent -> "agent"
                ChatRole.System -> "system"
            } + " · " + formatHms(message.timestamp.toEpochMilliseconds()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* ---------- Reused pieces (state badge, lifecycle, KPI cards, mini charts) ---------- */

@Composable
private fun ActorStateBadge(state: ActorState) {
    val (color, text) = when (state) {
        is ActorState.Running -> Color(0xFF4CAF50) to "Running"
        is ActorState.Stopped -> Color(0xFF9E9E9E) to "Stopped"
        is ActorState.Error -> Color(0xFFF44336) to "Error: ${state.exception}"
        is ActorState.Paused -> Color(0xFFFF9800) to "Paused: ${state.reason}"
        is ActorState.Initialized -> Color(0xFF2196F3) to "Initialized"
    }

    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.small))
            Text(text = text, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LifecycleControls(actor: ActorDisplayData, actorService: RealTimeActorService) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { actorService.startActor(actor.id) },
            enabled = actor.state !is ActorState.Running,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Start")
        }
        Button(
            onClick = { actorService.pauseActor(actor.id) },
            enabled = actor.state is ActorState.Running,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Pause")
        }
        Button(
            onClick = { actorService.resumeActor(actor.id) },
            enabled = actor.state is ActorState.Paused,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Resume")
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { actorService.stopActor(actor.id) },
            enabled = actor.state !is ActorState.Stopped,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Stop")
        }
        OutlinedButton(
            onClick = { actorService.deleteActor(actor.id) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Delete")
        }
    }
}

@Composable
private fun KPICards(actor: ActorDisplayData) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KPICard(label = "Received", value = actor.metrics.messagesReceived.toString(), modifier = Modifier.weight(1f))
        KPICard(label = "Processed", value = actor.metrics.messagesProcessed.toString(), modifier = Modifier.weight(1f))
        KPICard(
            label = "Failed",
            value = actor.metrics.messagesFailed.toString(),
            color = if (actor.metrics.messagesFailed > 0) Color(0xFFF44336) else null,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KPICard(
            label = "Success Rate",
            value = "${(actor.metrics.successRate * 10).toInt() / 10.0}%",
            color = when {
                actor.metrics.successRate >= 95 -> Color(0xFF4CAF50)
                actor.metrics.successRate >= 80 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            },
            modifier = Modifier.weight(1f)
        )
        KPICard(
            label = "Avg Time",
            value = "${(actor.metrics.averageProcessingTime * 10).toInt() / 10.0}ms",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KPICard(label: String, value: String, modifier: Modifier = Modifier, color: Color? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color ?: MaterialTheme.colorScheme.onSurface)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniCharts(actor: ActorDisplayData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniBarChart(
            label = "Throughput",
            value = actor.metrics.messagesProcessed.toFloat(),
            maxValue = actor.metrics.messagesReceived.toFloat().coerceAtLeast(1f),
            color = Color(0xFF4CAF50)
        )
        MiniBarChart(
            label = "Latency",
            value = actor.metrics.averageProcessingTime.toFloat(),
            maxValue = 200f,
            color = Color(0xFF2196F3)
        )
        MiniBarChart(
            label = "Error Rate",
            value = actor.metrics.messagesFailed.toFloat(),
            maxValue = actor.metrics.messagesReceived.toFloat().coerceAtLeast(1f),
            color = Color(0xFFF44336)
        )
    }
}

@Composable
private fun MiniBarChart(label: String, value: Float, maxValue: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
