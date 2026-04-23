package com.example.opssync.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.data.*
import com.example.opssync.ui.components.*
import com.example.opssync.ui.state.UiState
import com.example.opssync.ui.theme.*
import com.example.opssync.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineDetailScreen(
    pipelineId: String,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(pipelineId) {
        viewModel.fetchRecentRuns()
    }

    // Reset Teams send state on leave
    DisposableEffect(Unit) {
        onDispose { viewModel.resetTeamsSendState() }
    }

    val pipeline = (viewModel.selectedPipelineState as? UiState.Success)?.data
        ?: viewModel.getPipelineById(pipelineId)

    if (pipeline == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pipeline not found", color = ColorFailed)
        }
        return
    }

    val recentRuns  = viewModel.recentRuns
    val historyRuns = recentRuns.filter { it.id != pipelineId }.take(5)
    val chartRuns   = recentRuns.take(7)
    val chartData   = chartRuns.mapIndexed { index, run ->
        val height = when (run.status) {
            PipelineStatus.SUCCESS -> 0.4f + (index * 0.08f).coerceAtMost(0.5f)
            PipelineStatus.FAILED  -> 0.8f + (index * 0.05f).coerceAtMost(0.2f)
            PipelineStatus.RUNNING -> 0.6f
            PipelineStatus.PENDING -> 0.2f
        }
        Pair(height, run.status)
    }.takeLast(7)

    val teamsSendState        = viewModel.teamsSendState
    var showTeamsLogsDialog   by remember { mutableStateOf(false) }
    var showWebhookSetupDialog by remember { mutableStateOf(false) }
    var teamsWebhookInput     by remember { mutableStateOf("") }
    val teamsStatus           = viewModel.teamsWebhookStatus

    LaunchedEffect(Unit) {
        viewModel.fetchTeamsWebhookStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text          = "PROJECTS › ${pipeline.environment}",
                            fontSize      = 10.sp,
                            color         = Accent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text       = pipeline.name.lowercase().replace(" ", "-"),
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = pipeline.triggeredBy.take(2).uppercase(),
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        containerColor = Background
    ) { padding ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item {
                PipelineStatusBanner(pipeline = pipeline)
                Spacer(Modifier.height(16.dp))
            }

            if (pipeline.steps.isNotEmpty()) {
                item {
                    Text(
                        text       = "Execution Timeline",
                        modifier   = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Primary
                    )
                    Spacer(Modifier.height(12.dp))
                }
                items(pipeline.steps.size) { index ->
                    TimelineStepItem(
                        step   = pipeline.steps[index],
                        isLast = index == pipeline.steps.size - 1
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            if (pipeline.logs.isNotEmpty()) {
                item {
                    LiveLogPanel(logs = pipeline.logs)
                    Spacer(Modifier.height(16.dp))
                }
            }

            item {
                ExecutionDetailsCard(pipeline = pipeline)
                Spacer(Modifier.height(16.dp))
            }

            item {
                PipelineLoadSection(chartData = chartData)
                Spacer(Modifier.height(16.dp))
            }

            item {
                PipelineHistorySection(historyRuns = historyRuns)
                Spacer(Modifier.height(16.dp))
            }

            item {
                val isConnected = (teamsStatus as? UiState.Success)?.data?.isConnected == true
                ActionButtonsRow(
                    pipeline         = pipeline,
                    teamsSendState   = teamsSendState,
                    isTeamsConnected = isConnected,
                    onRerun          = { viewModel.rerunPipeline(pipeline.id) },
                    onCancel         = { viewModel.cancelPipeline(pipeline.id) },
                    onViewLogs       = {
                        val url = "https://github.com/${viewModel.selectedRepo?.owner?.login}" +
                            "/${viewModel.selectedRepo?.name}/actions/runs/${pipeline.id}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onShareToTeams   = {
                        if (isConnected) {
                            showTeamsLogsDialog = true
                        } else {
                            showWebhookSetupDialog = true
                        }
                    }
                )
            }
        }
    }

    // ── Teams Logs Confirmation Dialog ────────────────────────
    if (showTeamsLogsDialog) {
        AlertDialog(
            onDismissRequest = { showTeamsLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6264A7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Share Logs to Teams", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text     = "The following pipeline summary will be sent to your Teams channel:",
                        fontSize = 13.sp,
                        color    = Secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    TeamsPreviewRow("Pipeline",    pipeline.name)
                    TeamsPreviewRow("Status",      pipeline.status.name)
                    TeamsPreviewRow("Git Hash",    pipeline.gitHash)
                    TeamsPreviewRow("Triggered By",pipeline.triggeredBy)
                    TeamsPreviewRow("Log Lines",   "${pipeline.logs.size} lines")
                    val errorCount = pipeline.logs.count {
                        it.startsWith("ERROR") || it.startsWith("WARN")
                    }
                    if (errorCount > 0) {
                        TeamsPreviewRow("Errors/Warnings", "$errorCount found")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendPipelineLogsToTeams(pipeline)
                        showTeamsLogsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6264A7))
                ) { Text("Send Now") }
            },
            dismissButton = {
                TextButton(onClick = { showTeamsLogsDialog = false }) {
                    Text("Cancel", color = Secondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // ── Teams Webhook Setup Dialog ────────────────────────────
    if (showWebhookSetupDialog) {
        AlertDialog(
            onDismissRequest = { showWebhookSetupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6264A7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Connect Microsoft Teams", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text       = "Paste your Teams incoming webhook URL to send pipeline logs to your channel.",
                        fontSize   = 13.sp,
                        color      = Secondary,
                        lineHeight = 18.sp
                    )
                    Card(
                        shape  = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF6264A7).copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text       = "How to get your webhook URL:",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF6264A7)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("1. Open Teams → your channel",            fontSize = 11.sp, color = Secondary)
                            Text("2. Click ··· → Connectors",               fontSize = 11.sp, color = Secondary)
                            Text("3. Find Incoming Webhook → Configure",    fontSize = 11.sp, color = Secondary)
                            Text("4. Copy the URL and paste below",         fontSize = 11.sp, color = Secondary)
                        }
                    }
                    OutlinedTextField(
                        value         = teamsWebhookInput,
                        onValueChange = { teamsWebhookInput = it },
                        placeholder   = {
                            Text(
                                "https://xxx.webhook.office.com/...",
                                fontSize = 11.sp,
                                color    = Secondary
                            )
                        },
                        modifier   = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines   = 3,
                        shape      = RoundedCornerShape(8.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF6264A7),
                            unfocusedBorderColor = DividerColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        if (teamsWebhookInput.isNotBlank()) {
                            viewModel.saveTeamsWebhookUrl(teamsWebhookInput.trim())
                            showWebhookSetupDialog = false
                            viewModel.sendPipelineLogsToTeams(pipeline)
                        }
                    },
                    enabled  = teamsWebhookInput.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6264A7))
                ) { Text("Save & Send") }
            },
            dismissButton = {
                TextButton(onClick = { showWebhookSetupDialog = false }) {
                    Text("Cancel", color = Secondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

// ─── Teams Preview Row ─────────────────────────────────────────
@Composable
private fun TeamsPreviewRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text       = "• $label:",
            fontSize   = 12.sp,
            color      = Secondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text     = value,
            fontSize = 12.sp,
            color    = Primary,
            maxLines = 1
        )
    }
}

// ─── Status Banner ─────────────────────────────────────────────
@Composable
fun PipelineStatusBanner(pipeline: Pipeline) {
    val (bgColor, textColor) = when (pipeline.status) {
        PipelineStatus.RUNNING -> Pair(RunningBg, ColorRunning)
        PipelineStatus.FAILED  -> Pair(FailedBg,  ColorFailed)
        PipelineStatus.SUCCESS -> Pair(SuccessBg, ColorSuccess)
        PipelineStatus.PENDING -> Pair(PendingBg, ColorPending)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(textColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = Color.White, size = 6)
            Spacer(Modifier.width(6.dp))
            Text(
                text          = pipeline.status.name,
                color         = Color.White,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Triggered by: ${pipeline.triggeredBy}", fontSize = 11.sp, color = textColor)
            Text("Started: ${pipeline.startedAt}",        fontSize = 11.sp, color = textColor)
        }
    }
}

// ─── Timeline Step Item ────────────────────────────────────────
@Composable
fun TimelineStepItem(step: PipelineStep, isLast: Boolean) {
    val (dotColor, textColor) = when (step.status) {
        StepStatus.COMPLETED -> Pair(Primary,         Primary)
        StepStatus.RUNNING   -> Pair(ColorRunning,    ColorRunning)
        StepStatus.FAILED    -> Pair(ColorFailed,     ColorFailed)
        StepStatus.PENDING   -> Pair(Color.LightGray, Color.Gray)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (step.status == StepStatus.PENDING) Color(0xFFE0E0E0) else dotColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.COMPLETED -> Icon(Icons.Default.Check,  null, tint = Color.White, modifier = Modifier.size(16.dp))
                    StepStatus.RUNNING   -> Icon(Icons.Default.Sync,   null, tint = Color.White, modifier = Modifier.size(16.dp))
                    StepStatus.FAILED    -> Icon(Icons.Default.Close,  null, tint = Color.White, modifier = Modifier.size(16.dp))
                    StepStatus.PENDING   -> Box(Modifier.size(10.dp).clip(CircleShape).background(Color.Gray))
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(DividerColor))
            }
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 4.dp),
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (step.status == StepStatus.PENDING) PendingBg else SurfaceCard
            ),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = step.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = textColor,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        step.duration.isNotEmpty()            -> Text(step.duration,  fontSize = 12.sp, color = Secondary, fontWeight = FontWeight.Medium)
                        step.status == StepStatus.RUNNING     -> Text("Running...",   fontSize = 12.sp, color = ColorRunning, fontWeight = FontWeight.SemiBold)
                        step.status == StepStatus.PENDING     -> Text("Pending",      fontSize = 12.sp, color = ColorPending)
                    }
                }
                if (step.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = step.description,
                        fontSize = 12.sp,
                        color    = if (step.status == StepStatus.FAILED) ColorFailed else Secondary
                    )
                }
            }
        }
    }
}

// ─── Live Log Panel ────────────────────────────────────────────
@Composable
fun LiveLogPanel(logs: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color(0xFF1E1E2E))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F57)))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF28C840)))
            }
            Text(
                "STDOUT — LIVE_STREAM",
                color         = Color(0xFF6C7086),
                fontSize      = 10.sp,
                letterSpacing = 0.5.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Download, null, tint = Color(0xFF6C7086), modifier = Modifier.size(16.dp))
                Icon(Icons.Default.CopyAll,  null, tint = Color(0xFF6C7086), modifier = Modifier.size(16.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Color(0xFF181825))
                .padding(12.dp)
        ) {
            logs.forEachIndexed { index, log ->
                Row {
                    Text(
                        text       = "${index + 1}  ",
                        fontSize   = 11.sp,
                        color      = Color(0xFF6C7086),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text       = log,
                        fontSize   = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = when {
                            log.startsWith("ERROR") -> Color(0xFFf38ba8)
                            log.startsWith("WARN")  -> Color(0xFFfab387)
                            log.startsWith("==>")   -> Color(0xFF89dceb)
                            else                    -> Color(0xFFcdd6f4)
                        }
                    )
                }
                if (index < logs.size - 1) Spacer(Modifier.height(3.dp))
            }
        }
    }
}

// ─── Execution Details Card ────────────────────────────────────
@Composable
fun ExecutionDetailsCard(pipeline: Pipeline) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Execution Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
            Spacer(Modifier.height(12.dp))
            InfoRow("RUNNER ID",   pipeline.runnerId)
            InfoRow("ENVIRONMENT", pipeline.environment)
            InfoRow("REGION",      pipeline.region)
            InfoRow("GIT HASH",    "#${pipeline.gitHash}", valueColor = Color(0xFF7C3AED))
        }
    }
}

// ─── Pipeline Load Section ─────────────────────────────────────
@Composable
fun PipelineLoadSection(chartData: List<Pair<Float, PipelineStatus>>) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pipeline Load", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
            Spacer(Modifier.height(4.dp))
            Text("${chartData.size} recent runs", fontSize = 11.sp, color = Secondary)
            Spacer(Modifier.height(16.dp))

            if (chartData.isEmpty()) {
                Text("No run history available", color = Secondary, fontSize = 13.sp)
            } else {
                Row(
                    modifier              = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.Bottom
                ) {
                    chartData.forEachIndexed { i, (height, status) ->
                        val barColor = when (status) {
                            PipelineStatus.SUCCESS -> ColorSuccess
                            PipelineStatus.FAILED  -> ColorFailed
                            PipelineStatus.RUNNING -> ColorRunning
                            PipelineStatus.PENDING -> ColorPending
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(height.coerceIn(0.1f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (i == chartData.size - 1) barColor
                                    else barColor.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("OLDEST",  fontSize = 10.sp, color = Secondary, letterSpacing = 0.3.sp)
                    Text("CURRENT", fontSize = 10.sp, color = Primary,   letterSpacing = 0.3.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = ColorSuccess, label = "Success")
                    LegendDot(color = ColorFailed,  label = "Failed")
                    LegendDot(color = ColorRunning, label = "Running")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Secondary)
    }
}

// ─── Pipeline History Section ──────────────────────────────────
@Composable
fun PipelineHistorySection(historyRuns: List<Pipeline>) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
            Spacer(Modifier.height(12.dp))
            if (historyRuns.isEmpty()) {
                Text("No previous runs found", color = Secondary, fontSize = 13.sp)
            } else {
                historyRuns.forEachIndexed { index, run ->
                    HistoryItem(name = run.name, status = run.status, time = run.startedAt)
                    if (index < historyRuns.size - 1) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(name: String, status: PipelineStatus, time: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            StatusDot(
                color = when (status) {
                    PipelineStatus.SUCCESS -> ColorSuccess
                    PipelineStatus.FAILED  -> ColorFailed
                    PipelineStatus.RUNNING -> ColorRunning
                    else                   -> ColorPending
                },
                size = 8
            )
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 13.sp, color = Primary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(time, fontSize = 12.sp, color = Secondary)
    }
}

// ─── Action Buttons Row ────────────────────────────────────────
@Composable
fun ActionButtonsRow(
    pipeline:         Pipeline,
    teamsSendState:   UiState<Unit>,
    isTeamsConnected: Boolean,
    onRerun:          () -> Unit,
    onCancel:         () -> Unit,
    onViewLogs:       () -> Unit,
    onShareToTeams:   () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Rerun / Cancel + View Logs Row ────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pipeline.status == PipelineStatus.RUNNING) {
                DangerButton(
                    label    = "Cancel Pipeline",
                    onClick  = onCancel,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PrimaryButton(
                    label    = "Re-run Pipeline",
                    onClick  = onRerun,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlineButton(
                label    = "View Full Logs",
                onClick  = onViewLogs,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Share Logs to Teams ───────────────────────────────
        Button(
            onClick  = onShareToTeams,
            modifier = Modifier.fillMaxWidth(),
            enabled  = teamsSendState !is UiState.Loading,
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFF6264A7),
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFF6264A7).copy(alpha = 0.5f),
                disabledContentColor   = Color.White.copy(alpha = 0.7f)
            )
        ) {
            when (teamsSendState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sending to Teams...", fontWeight = FontWeight.SemiBold)
                }
                is UiState.Success -> {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logs Sent to Teams!", fontWeight = FontWeight.SemiBold)
                }
                is UiState.Error -> {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Failed — Tap to retry", fontWeight = FontWeight.SemiBold)
                }
                else -> {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = if (isTeamsConnected) "Share Logs to Teams"
                        else "Connect Teams & Share",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
