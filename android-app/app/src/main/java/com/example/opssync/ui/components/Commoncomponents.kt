package com.example.opssync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.data.*
import com.example.opssync.ui.theme.*

// ─── Status Badge ─────────────────────────────────────────────
// A small colored pill/chip showing pipeline or incident status
@Composable
fun StatusBadge(status: PipelineStatus) {
    val (bgColor, textColor, label) = when (status) {
        PipelineStatus.SUCCESS -> Triple(SuccessBg, ColorSuccess, "SUCCESS")
        PipelineStatus.FAILED  -> Triple(FailedBg,  ColorFailed,  "FAILED")
        PipelineStatus.RUNNING -> Triple(RunningBg, ColorRunning, "RUNNING")
        PipelineStatus.PENDING -> Triple(PendingBg, ColorPending, "PENDING")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Incident Severity Badge ───────────────────────────────────
@Composable
fun SeverityBadge(severity: IncidentSeverity) {
    val (bgColor, textColor, label) = when (severity) {
        IncidentSeverity.CRITICAL -> Triple(FailedBg,  ColorFailed,  "CRITICAL")
        IncidentSeverity.HIGH     -> Triple(Color(0xFFFFF3E0), ColorWarning, "HIGH")
        IncidentSeverity.MEDIUM   -> Triple(Color(0xFFFFF8E1), Color(0xFFF9A825), "MEDIUM")
        IncidentSeverity.LOW      -> Triple(PendingBg, ColorPending, "LOW")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Incident Status Badge ─────────────────────────────────────
@Composable
fun IncidentStatusBadge(status: IncidentStatus) {
    val (bgColor, textColor, label) = when (status) {
        IncidentStatus.OPEN         -> Triple(FailedBg,  ColorFailed,  "OPEN")
        IncidentStatus.ACKNOWLEDGED -> Triple(RunningBg, ColorRunning, "ACKNOWLEDGED")
        IncidentStatus.RESOLVED     -> Triple(SuccessBg, ColorSuccess, "RESOLVED")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Top App Bar ──────────────────────────────────────────────
// Common top bar used on detail screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpsTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint               = Color.White
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Primary
        )
    )
}

// ─── Section Header ────────────────────────────────────────────
// Used for sections within a screen
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String = "",
    onAction: () -> Unit = {}
) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = Primary
        )
        if (actionLabel.isNotEmpty()) {
            TextButton(onClick = onAction) {
                Text(
                    text       = actionLabel,
                    color      = ColorFailed,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }
        }
    }
}

// ─── Primary Button ────────────────────────────────────────────
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(40.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor   = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text       = label,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp
        )
    }
}

// ─── Danger Button (Cancel / Reject) ──────────────────────────
@Composable
fun DangerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(40.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = ColorFailed,
            contentColor   = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text       = label,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp
        )
    }
}

// ─── Outline Button ────────────────────────────────────────────
@Composable
fun OutlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(40.dp),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text       = label,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp
        )
    }
}

// ─── Info Row ──────────────────────────────────────────────────
// A label-value row used in detail screens
@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Primary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = Secondary,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Text(
            text       = value,
            fontSize   = 13.sp,
            color      = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
    Divider(color = DividerColor, thickness = 0.5.dp)
}

// ─── Dot Indicator ─────────────────────────────────────────────
// A colored dot (used in status indicators and online badges)
@Composable
fun StatusDot(color: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ─── Empty State ───────────────────────────────────────────────
@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = message,
            color     = Secondary,
            fontSize  = 14.sp
        )
    }
}
