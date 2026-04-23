package com.example.opssync.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.data.*
import com.example.opssync.ui.components.*
import com.example.opssync.ui.theme.*
import com.example.opssync.viewmodel.AppViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NotificationScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications
    val unreadCount    = notifications.count { !it.isRead }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alerts & Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (unreadCount > 0) {
                            Text(
                                text     = "$unreadCount unread",
                                color    = Accent,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Mark all read button
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text(
                            text       = "Mark all read",
                            color      = Accent,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        containerColor = Background
    ) { padding ->

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        null,
                        tint     = Accent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications", color = Secondary, fontSize = 16.sp)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
        ) {
            // ── Critical Alerts Section ────────────────────────
            val criticalNotifs = notifications.filter {
                it.type == NotificationType.CRITICAL && !it.isRead
            }
            if (criticalNotifs.isNotEmpty()) {
                item {
                    NotificationSectionHeader(
                        title   = "🚨 Critical Alerts",
                        count   = criticalNotifs.size,
                        bgColor = FailedBg
                    )
                }
                items(criticalNotifs) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick      = { viewModel.markNotificationRead(notif.id) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Warnings Section ──────────────────────────────
            val warningNotifs = notifications.filter { it.type == NotificationType.WARNING }
            if (warningNotifs.isNotEmpty()) {
                item {
                    NotificationSectionHeader(
                        title   = "⚠️ Warnings",
                        count   = warningNotifs.count { !it.isRead },
                        bgColor = Color(0xFFFFF3E0)
                    )
                }
                items(warningNotifs) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick      = { viewModel.markNotificationRead(notif.id) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Recent Activity Section ────────────────────────
            val otherNotifs = notifications.filter {
                it.type == NotificationType.INFO || it.type == NotificationType.SUCCESS
            }
            if (otherNotifs.isNotEmpty()) {
                item {
                    NotificationSectionHeader(
                        title   = "Recent Activity",
                        count   = otherNotifs.count { !it.isRead },
                        bgColor = SurfaceCard
                    )
                }
                items(otherNotifs) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick      = { viewModel.markNotificationRead(notif.id) }
                    )
                }
            }
        }
    }
}

// ─── Section Header ────────────────────────────────────────────
@Composable
fun NotificationSectionHeader(
    title: String,
    count: Int,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = title,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = Primary
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ColorFailed)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text       = count.toString(),
                    color      = Color.White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Notification Card ─────────────────────────────────────────
@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val (iconBg, iconTint, icon) = getNotificationStyle(notification.type)

    // Unread indicator: slightly highlighted background
    val cardBg = if (!notification.isRead) {
        when (notification.type) {
            NotificationType.CRITICAL -> Color(0xFFFFF5F5)
            NotificationType.WARNING  -> Color(0xFFFFFAF0)
            else                      -> SurfaceCard
        }
    } else {
        SurfaceCard
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Icon ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // ── Content ───────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text       = notification.title,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = Primary,
                    modifier   = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = notification.timeAgo,
                    fontSize = 11.sp,
                    color    = Secondary
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text       = notification.message,
                fontSize   = 12.sp,
                color      = Secondary,
                lineHeight = 17.sp
            )
        }

        // ── Unread Dot ────────────────────────────────────────
        if (!notification.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.CRITICAL -> ColorFailed
                            NotificationType.WARNING  -> ColorWarning
                            NotificationType.SUCCESS  -> ColorSuccess
                            NotificationType.INFO     -> ColorRunning
                        }
                    )
                    .align(Alignment.CenterVertically)
            )
        }
    }

    // Divider between cards
    Divider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = DividerColor,
        thickness = 0.5.dp
    )
}

// ─── Helper: Get icon style per notification type ─────────────
private fun getNotificationStyle(
    type: NotificationType
): Triple<Color, Color, ImageVector> = when (type) {
    NotificationType.CRITICAL -> Triple(FailedBg,  ColorFailed,  Icons.Default.Error)
    NotificationType.WARNING  -> Triple(Color(0xFFFFF3E0), ColorWarning, Icons.Default.Warning)
    NotificationType.SUCCESS  -> Triple(SuccessBg, ColorSuccess, Icons.Default.CheckCircle)
    NotificationType.INFO     -> Triple(RunningBg, ColorRunning, Icons.Default.Info)
}
