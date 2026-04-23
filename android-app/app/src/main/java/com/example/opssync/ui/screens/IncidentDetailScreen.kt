package com.example.opssync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.opssync.ui.theme.*
import com.example.opssync.ui.state.UiState
import com.example.opssync.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidentId: String,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val incident = (viewModel.incidentsState as? UiState.Success)
        ?.data
        ?.find { it.id == incidentId }

    if (incident == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Incident not found", color = ColorFailed)
        }
        return
    }

    var showAcknowledgeDialog  by remember { mutableStateOf(false) }
    var showResolveDialog      by remember { mutableStateOf(false) }
    var showTeamsDialog        by remember { mutableStateOf(false) }
    var showWebhookSetupDialog by remember { mutableStateOf(false) }
    var teamsWebhookInput      by remember { mutableStateOf("") }
    val teamsSendState         = viewModel.teamsSendState
    val teamsStatus            = viewModel.teamsWebhookStatus

    // Fetch Teams connection status when screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchTeamsWebhookStatus()
    }

    // Reset Teams send state when leaving screen
    DisposableEffect(Unit) {
        onDispose { viewModel.resetTeamsSendState() }
    }

    Scaffold(
        topBar = {
            OpsTopBar(
                title  = "Incident Detail",
                onBack = onBack
            )
        },
        containerColor = Background
    ) { padding ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item { SeverityBanner(incident = incident) }

            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text       = incident.title,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = Primary,
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            SeverityBadge(incident.severity)
                            IncidentStatusBadge(incident.status)
                            Text("• ${incident.id}", fontSize = 12.sp, color = Secondary)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text       = incident.description,
                            fontSize   = 14.sp,
                            color      = Secondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Incident Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
                        Spacer(Modifier.height(12.dp))
                        InfoRow("INCIDENT ID",  incident.id)
                        InfoRow("SERVICE",      incident.service)
                        InfoRow("REGION",       incident.region)
                        InfoRow("DURATION",     incident.duration)
                        InfoRow("ASSIGNED TO",  incident.assignedTo)
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                AssignedTeamCard(incident = incident)
            }

            item {
                Spacer(Modifier.height(12.dp))
                IncidentTimelineCard(incident = incident)
            }

            item {
                Spacer(Modifier.height(20.dp))
                val isConnected = (teamsStatus as? UiState.Success)?.data?.isConnected == true
                IncidentActionButtons(
                    incident         = incident,
                    onAcknowledge    = { showAcknowledgeDialog = true },
                    onResolve        = { showResolveDialog = true },
                    teamsSendState   = teamsSendState,
                    isTeamsConnected = isConnected,
                    onSendToTeams    = {
                        if (isConnected) {
                            showTeamsDialog = true
                        } else {
                            showWebhookSetupDialog = true
                        }
                    }
                )
            }
        }
    }

    // ── Acknowledge Dialog ────────────────────────────────────
    if (showAcknowledgeDialog) {
        AlertDialog(
            onDismissRequest = { showAcknowledgeDialog = false },
            title = { Text("Acknowledge Incident", fontWeight = FontWeight.Bold) },
            text  = {
                Text("You are about to acknowledge \"${incident.title}\". This signals your team that you are investigating.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acknowledgeIncident(incidentId)
                        showAcknowledgeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Acknowledge") }
            },
            dismissButton = {
                TextButton(onClick = { showAcknowledgeDialog = false }) {
                    Text("Cancel", color = Secondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // ── Resolve Dialog ────────────────────────────────────────
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Resolve Incident", fontWeight = FontWeight.Bold, color = ColorSuccess) },
            text  = {
                Text("Mark \"${incident.title}\" as resolved? This will close the incident.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveIncident(incidentId)
                        showResolveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess)
                ) { Text("Resolve") }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel", color = Secondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // ── Teams Send Confirmation Dialog ────────────────────────
    if (showTeamsDialog) {
        AlertDialog(
            onDismissRequest = { showTeamsDialog = false },
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
                    Text("Send to Microsoft Teams", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text     = "The following details will be sent to your Teams channel:",
                        fontSize = 13.sp,
                        color    = Secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    TeamsPreviewRow("Incident ID", incident.id)
                    TeamsPreviewRow("Title",       incident.title)
                    TeamsPreviewRow("Priority",    incident.severity.name)
                    TeamsPreviewRow("Status",      incident.status.name)
                    TeamsPreviewRow("Assigned To", incident.assignedTo)
                    TeamsPreviewRow("Service",     incident.service)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendIncidentToTeams(incidentId)
                        showTeamsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6264A7))
                ) { Text("Send Now") }
            },
            dismissButton = {
                TextButton(onClick = { showTeamsDialog = false }) {
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
                        text     = "Paste your Teams incoming webhook URL to send incident alerts to your channel.",
                        fontSize = 13.sp,
                        color    = Secondary,
                        lineHeight = 18.sp
                    )
                    Card(
                        shape  = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF6264A7).copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text       = "How to get your webhook URL:",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF6264A7)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("1. Open Teams → your channel", fontSize = 11.sp, color = Secondary)
                            Text("2. Click ··· → Connectors",    fontSize = 11.sp, color = Secondary)
                            Text("3. Find Incoming Webhook → Configure", fontSize = 11.sp, color = Secondary)
                            Text("4. Copy the URL and paste below", fontSize = 11.sp, color = Secondary)
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
                        modifier  = Modifier.fillMaxWidth(),
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
                            // Auto-send incident after saving webhook
                            viewModel.sendIncidentToTeams(incidentId)
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

// ─── Severity Banner ───────────────────────────────────────────
@Composable
fun SeverityBanner(incident: Incident) {
    val (bg, fg) = when (incident.severity) {
        IncidentSeverity.CRITICAL -> Pair(ColorFailed,       Color.White)
        IncidentSeverity.HIGH     -> Pair(ColorWarning,      Color.White)
        IncidentSeverity.MEDIUM   -> Pair(Color(0xFFF9A825), Color.White)
        IncidentSeverity.LOW      -> Pair(ColorPending,      Color.White)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Warning, null, tint = fg, modifier = Modifier.size(18.dp))
            Text(
                text          = "${incident.severity.name} PRIORITY",
                color         = fg,
                fontWeight    = FontWeight.Bold,
                fontSize      = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text       = incident.status.name,
            color      = fg,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Assigned Team Card ────────────────────────────────────────
@Composable
fun AssignedTeamCard(incident: Incident) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Assigned Team", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(44.dp).clip(CircleShape).background(Secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = incident.assignedTo.take(2).uppercase(),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(incident.assignedTo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Primary)
                    Text("Response Engineer", fontSize = 12.sp, color = Secondary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick  = {},
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Accent.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Send, "Contact", tint = Primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─── Incident Timeline Card ────────────────────────────────────
@Composable
fun IncidentTimelineCard(incident: Incident) {
    val events = buildList {
        add(Triple("Incident Reported", "Automatically detected by monitoring", ColorFailed))
        if (incident.status != IncidentStatus.OPEN) {
            add(Triple("Acknowledged", "Engineer ${incident.assignedTo} began investigation", ColorRunning))
        }
        if (incident.status == IncidentStatus.RESOLVED) {
            add(Triple("Resolved", "Issue fixed and verified by ${incident.assignedTo}", ColorSuccess))
        }
    }
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
            Spacer(Modifier.height(12.dp))
            events.forEachIndexed { i, (title, desc, color) ->
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier         = Modifier.size(24.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        if (i < events.size - 1) {
                            Box(Modifier.width(2.dp).height(32.dp).background(DividerColor))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(bottom = if (i < events.size - 1) 8.dp else 0.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Primary)
                        Text(desc,  fontSize = 12.sp, color = Secondary)
                    }
                }
            }
        }
    }
}

// ─── Incident Action Buttons ───────────────────────────────────
@Composable
fun IncidentActionButtons(
    incident:         Incident,
    onAcknowledge:    () -> Unit,
    onResolve:        () -> Unit,
    onSendToTeams:    () -> Unit,
    isTeamsConnected: Boolean,
    teamsSendState:   UiState<Unit>
) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Acknowledge ───────────────────────────────────────
        if (incident.status == IncidentStatus.OPEN) {
            Button(
                onClick  = onAcknowledge,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ColorRunning,
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Acknowledge Incident", fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Resolve ───────────────────────────────────────────
        if (incident.status != IncidentStatus.RESOLVED) {
            Button(
                onClick  = onResolve,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ColorSuccess,
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark as Resolved", fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Send to Teams ─────────────────────────────────────
        Button(
            onClick  = onSendToTeams,
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
                    Text("Sent to Teams!", fontWeight = FontWeight.SemiBold)
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
                        text       = if (isTeamsConnected) "Send to Teams" else "Connect Teams & Send",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Escalate + Share Row ──────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = {},
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Escalate", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick  = {},
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share", fontSize = 13.sp)
            }
        }
    }
}
