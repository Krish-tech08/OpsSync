package com.example.opssync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.data.*
import com.example.opssync.ui.components.*
import com.example.opssync.ui.state.UiState
import com.example.opssync.ui.theme.*
import com.example.opssync.viewmodel.AppViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onPipelineClick: (String) -> Unit,
    onIncidentsClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    // ── FIX: pull pipelines from ViewModel, not a bare reference ──
    val pipelinesState = viewModel.pipelinesState
    val pipelines = viewModel.pipelines          // <-- was missing; add this val to AppViewModel
    val stats = viewModel.dashboardStats
    val unreadCount = viewModel.unreadCount

    Scaffold(
        // ── Top App Bar ─────────────────────────────────────
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "OpsSync",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "OpsSync",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White)
                    }
                    // Notification bell with badge
                    Box {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ColorFailed)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // Profile avatar
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        // ── Bottom Navigation ────────────────────────────────
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "dashboard",
                onDashboard = {},
                // FIX: was crashing if pipelines was empty — safe firstOrNull with empty string fallback
                onPipelines = { onPipelineClick(pipelines.firstOrNull()?.id ?: "") },
                onIncidents = onIncidentsClick,
                onAlerts = onNotificationsClick
            )
        },
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "New Pipeline")
            }
        }
    ) { innerPadding ->

        val pullState = rememberPullToRefreshState()
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        PullToRefreshBox(
            state = pullState,
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.loadDashboardData()
                    // Wait for both states to finish loading
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ── Section 1: Global Infrastructure Card ────────
                item {
                    InfrastructureCard(stats = stats)
                }

                // ── Section 2: Stats Row ─────────────────────────
                item {
                    Spacer(Modifier.height(16.dp))
                    StatsRow(stats = stats)
                    Spacer(Modifier.height(16.dp))
                }

                // ── Section 3: Active Pipelines Header ───────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Pipelines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Primary
                        )
                        TextButton(onClick = onIncidentsClick) {
                            Text(
                                text = "VIEW ALL",
                                color = ColorFailed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Section 4: Loading / Empty / Error state ──────
                when (val state = viewModel.pipelinesState) {
                    is UiState.Loading, is UiState.Idle -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Loading pipelines...",
                                        color = Secondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.message, color = ColorFailed, fontSize = 13.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.loadDashboardData() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) { Text("Retry") }
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (pipelines.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No pipelines found for this repo",
                                        color = Secondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }

                // ── Section 5: Pipeline Cards ─────────────────────
                items(pipelines) { pipeline ->
                    PipelineCard(
                        pipeline = pipeline,
                        onClick = { onPipelineClick(pipeline.id) },
                        onRerun = {
                            viewModel.rerunPipeline(pipeline.id)
                            // fetchPipelines is called inside rerunPipeline already
                            // but we also set refresh indicator
                            scope.launch {
                                isRefreshing = true
                                delay(3000) // give GitHub time to queue the run
                                isRefreshing = false
                            }
                        },
                        onCancel = {
                            viewModel.cancelPipeline(pipeline.id)
                            scope.launch {
                                isRefreshing = true
                                delay(2000)
                                isRefreshing = false
                            }
                        },
                        onViewLogs = { onPipelineClick(pipeline.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── Infrastructure Header Card ───────────────────────────────
@Composable
fun InfrastructureCard(stats: Map<String, String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text          = "GLOBAL INFRASTRUCTURE",
                color         = Accent,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = stats["uptime"] ?: "99.98%",
                color      = Color.White,
                fontSize   = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = Color(0xFF4CAF50), size = 10)
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "All systems operational",
                    color    = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfraStatChip(label = "ACTIVE NODES", value = stats["activeNodes"] ?: "1,204")
                InfraStatChip(label = "LATENCY",      value = stats["latency"]     ?: "12ms")
            }
        }
    }
}

@Composable
fun InfraStatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text          = label,
            color         = Accent,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text       = value,
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Stats Row ────────────────────────────────────────────────
@Composable
fun StatsRow(stats: Map<String, String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Default.CheckCircle,
            label     = "Success",
            value     = stats["successCount"] ?: "0",
            iconColor = ColorSuccess,
            highlight = false
        )
        StatCard(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Default.Sync,
            label     = "Running",
            value     = stats["runningCount"] ?: "0",
            iconColor = ColorRunning,
            highlight = false
        )
        StatCard(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Default.Error,
            label     = "Failed",
            value     = stats["failedCount"] ?: "0",
            iconColor = ColorFailed,
            highlight = true
        )
    }
}

@Composable
fun StatCard(
    modifier:  Modifier = Modifier,
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    label:     String,
    value:     String,
    iconColor: Color,
    highlight: Boolean
) {
    val bgColor = if (highlight) FailedBg else SurfaceCard

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = iconColor,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = label,
                    fontSize   = 11.sp,
                    color      = if (highlight) ColorFailed else Secondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = if (highlight) ColorFailed else Primary
            )
        }
    }
}

// ─── Pipeline Card ─────────────────────────────────────────────
@Composable
fun PipelineCard(
    pipeline:  Pipeline,
    onClick:   () -> Unit,
    onRerun:   () -> Unit,
    onCancel:  () -> Unit,
    onViewLogs: () -> Unit
) {
    val leftBorderColor = when (pipeline.status) {
        PipelineStatus.SUCCESS -> ColorSuccess
        PipelineStatus.FAILED  -> ColorFailed
        PipelineStatus.RUNNING -> ColorRunning
        PipelineStatus.PENDING -> ColorPending
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left colored status border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = leftBorderColor,
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                // ── Header ──────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PipelineStatusIcon(pipeline.status)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = pipeline.name,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = Primary
                        )
                    }
                    StatusBadge(pipeline.status)
                }
                Spacer(Modifier.height(4.dp))

                // ── Sub info ────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(pipeline.environment, fontSize = 12.sp, color = Secondary, fontWeight = FontWeight.SemiBold)
                    Text("•", fontSize = 12.sp, color = Secondary)
                    Text(pipeline.startedAt,   fontSize = 12.sp, color = Secondary)
                }

                // ── Error snippet for failed pipelines ──────
                if (pipeline.status == PipelineStatus.FAILED) {
                    Spacer(Modifier.height(6.dp))
                    val errorLog = pipeline.logs.lastOrNull { it.startsWith("ERROR") } ?: ""
                    if (errorLog.isNotEmpty()) {
                        Text(
                            text     = errorLog.take(60) + "…",
                            fontSize = 11.sp,
                            color    = ColorFailed
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Action buttons based on status ──────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (pipeline.status) {
                        PipelineStatus.RUNNING -> {
                            DangerButton(
                                label    = "Cancel",
                                onClick  = onCancel,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                        PipelineStatus.FAILED,
                        PipelineStatus.SUCCESS -> {
                            PrimaryButton(
                                label    = "Re-run",
                                onClick  = onRerun,
                                modifier = Modifier.height(34.dp)
                            )
                            OutlineButton(
                                label    = "Logs",
                                onClick  = onViewLogs,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                        else -> {
                            PrimaryButton(
                                label    = "Re-run",
                                onClick  = onRerun,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Pipeline Status Icon ──────────────────────────────────────
@Composable
fun PipelineStatusIcon(status: PipelineStatus) {
    val (icon, color, bgColor) = when (status) {
        PipelineStatus.SUCCESS -> Triple(Icons.Default.RocketLaunch, ColorSuccess, SuccessBg)
        PipelineStatus.FAILED  -> Triple(Icons.Default.Error,        ColorFailed,  FailedBg)
        PipelineStatus.RUNNING -> Triple(Icons.Default.Sync,         ColorRunning, RunningBg)
        PipelineStatus.PENDING -> Triple(Icons.Default.Schedule,     ColorPending, PendingBg)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ─── Status Badge ──────────────────────────────────────────────
// FIX: was referenced but not defined in this file — added here
@Composable
fun StatusBadge(status: PipelineStatus) {
    val (label, bgColor, textColor) = when (status) {
        PipelineStatus.SUCCESS -> Triple("SUCCESS", SuccessBg, ColorSuccess)
        PipelineStatus.FAILED  -> Triple("FAILED",  FailedBg,  ColorFailed)
        PipelineStatus.RUNNING -> Triple("RUNNING", RunningBg, ColorRunning)
        PipelineStatus.PENDING -> Triple("PENDING", PendingBg, ColorPending)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text          = label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            color         = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Status Dot ────────────────────────────────────────────────
// FIX: was referenced in InfrastructureCard but not defined
@Composable
fun StatusDot(color: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ─── Bottom Navigation Bar ─────────────────────────────────────
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onDashboard:  () -> Unit,
    onPipelines:  () -> Unit,
    onIncidents:  () -> Unit,
    onAlerts:     () -> Unit
) {
    NavigationBar(
        containerColor = Primary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick  = onDashboard,
            icon     = { Icon(Icons.Default.Dashboard,    "Dashboard") },
            label    = { Text("DASHBOARD", fontSize = 9.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                selectedTextColor   = Color.White,
                unselectedIconColor = Accent,
                unselectedTextColor = Accent,
                indicatorColor      = Secondary
            )
        )
        NavigationBarItem(
            selected = currentRoute == "pipelines",
            onClick  = onPipelines,
            icon     = { Icon(Icons.Default.AccountTree,  "Pipelines") },
            label    = { Text("PIPELINES", fontSize = 9.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                selectedTextColor   = Color.White,
                unselectedIconColor = Accent,
                unselectedTextColor = Accent,
                indicatorColor      = Secondary
            )
        )
        NavigationBarItem(
            selected = currentRoute == "incidents",
            onClick  = onIncidents,
            icon     = { Icon(Icons.Default.Warning,      "Incidents") },
            label    = { Text("INCIDENTS", fontSize = 9.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                selectedTextColor   = Color.White,
                unselectedIconColor = Accent,
                unselectedTextColor = Accent,
                indicatorColor      = Secondary
            )
        )
        NavigationBarItem(
            selected = currentRoute == "alerts",
            onClick  = onAlerts,
            icon     = { Icon(Icons.Default.Notifications,"Alerts") },
            label    = { Text("ALERTS", fontSize = 9.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                selectedTextColor   = Color.White,
                unselectedIconColor = Accent,
                unselectedTextColor = Accent,
                indicatorColor      = Secondary
            )
        )
    }
}
