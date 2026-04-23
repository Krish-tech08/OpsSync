package com.example.opssync.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opssync.data.models.RepoDto
import com.example.opssync.ui.state.UiState
import com.example.opssync.ui.theme.*
import com.example.opssync.viewmodel.AppViewModel

@Composable
fun RepoPickerScreen(
    viewModel: AppViewModel,
    onRepoPicked: () -> Unit
) {
    val reposState   = viewModel.reposState
    val webhookState = viewModel.webhookState
    var searchQuery  by remember { mutableStateOf("") }

    // Navigate as soon as pipelines start loading (webhook runs in background)
    LaunchedEffect(viewModel.pipelinesState) {
        if (viewModel.pipelinesState is UiState.Loading ||
            viewModel.pipelinesState is UiState.Success) {
            if (viewModel.selectedRepo != null) {
                onRepoPicked()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserRepos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector        = Icons.Default.AccountTree,
            contentDescription = null,
            tint               = Primary,
            modifier           = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = "Select a Repository",
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp,
            color      = Primary
        )
        Text(
            text     = "Choose which GitHub repo to monitor for pipelines",
            fontSize = 13.sp,
            color    = Secondary
        )

        // ── Webhook status indicator ──────────────────────────
        // Shows briefly while webhook is being registered
        if (webhookState is UiState.Loading) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    color    = Accent,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text     = "Connecting incident alerts...",
                    fontSize = 12.sp,
                    color    = Secondary
                )
            }
        }

        if (webhookState is UiState.Success) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint     = ColorSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text     = "Incident alerts connected",
                    fontSize = 12.sp,
                    color    = ColorSuccess
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder   = { Text("Search repos...", color = Secondary) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Secondary) },
            trailingIcon  = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = Secondary)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape      = RoundedCornerShape(10.dp),
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Primary,
                unfocusedBorderColor    = DividerColor,
                unfocusedContainerColor = SurfaceCard,
                focusedContainerColor   = SurfaceCard
            )
        )

        Spacer(Modifier.height(16.dp))

        when (reposState) {
            is UiState.Loading, is UiState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading your repositories...", color = Secondary, fontSize = 13.sp)
                    }
                }
            }

            is UiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ColorFailed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(reposState.message, color = ColorFailed, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchUserRepos() },
                            colors  = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Retry") }
                    }
                }
            }

            is UiState.Success -> {
                val filtered = reposState.data.filter {
                    searchQuery.isBlank() ||
                        it.name.orEmpty().contains(searchQuery, ignoreCase = true) ||
                        it.fullName.orEmpty().contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No repositories found", color = Secondary)
                    }
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = "${filtered.size} repositories",
                            fontSize = 12.sp,
                            color    = Secondary
                        )
                        Text(
                            text     = "Tap to monitor",
                            fontSize = 11.sp,
                            color    = Accent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered) { repo ->
                            RepoCard(
                                repo    = repo,
                                onClick = {
                                    viewModel.selectRepo(repo)
                                    // Navigation handled by LaunchedEffect above
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoCard(repo: RepoDto, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.FolderOpen,
                contentDescription = null,
                tint               = if (repo.isPrivate) ColorWarning else Primary,
                modifier           = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = repo.name.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = Primary
                )
                Text(
                    text     = repo.fullName.orEmpty(),
                    fontSize = 12.sp,
                    color    = Secondary
                )
                if (!repo.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = repo.description.orEmpty(),
                        fontSize = 12.sp,
                        color    = Secondary,
                        maxLines = 1
                    )
                }
                if (!repo.language.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            null,
                            tint     = Accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text       = repo.language.orEmpty(),
                            fontSize   = 11.sp,
                            color      = Accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = Secondary,
                    modifier           = Modifier.size(20.dp)
                )
                if (repo.isPrivate) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "Private",
                        fontSize = 10.sp,
                        color    = ColorWarning
                    )
                }
            }
        }
    }
}
