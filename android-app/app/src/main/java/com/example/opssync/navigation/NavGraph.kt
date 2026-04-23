package com.example.opssync.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.opssync.ui.screens.*
import com.example.opssync.ui.state.UiState
import com.example.opssync.ui.theme.Secondary
import com.example.opssync.viewmodel.AppViewModel

object Routes {
    const val LOGIN           = "login"
    const val REPO_PICKER     = "repo_picker"
    const val DASHBOARD       = "dashboard"
    const val PIPELINE_DETAIL = "pipeline_detail/{pipelineId}"
    const val INCIDENT_LIST   = "incident_list"
    const val INCIDENT_DETAIL = "incident_detail/{incidentId}"
    const val NOTIFICATIONS   = "notifications"

    fun pipelineDetail(id: String) = "pipeline_detail/$id"
    fun incidentDetail(id: String) = "incident_detail/$id"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: AppViewModel,
    isLoggedIn: Boolean
) {
    // Always start at login — auth state drives navigation via LaunchedEffect
    NavHost(
        navController    = navController,
        startDestination = Routes.LOGIN
    ) {

        // ── Login ─────────────────────────────────────────────
        composable(Routes.LOGIN) {
            val context = LocalContext.current

            // When already logged in, skip straight to repo picker
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    navController.navigate(Routes.REPO_PICKER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                viewModel      = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.REPO_PICKER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGitHubLogin  = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }

        // ── Repo Picker ───────────────────────────────────────
        composable(Routes.REPO_PICKER) {
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.REPO_PICKER) { inclusive = true }
                }
            }
            RepoPickerScreen(
                viewModel    = viewModel,
                onRepoPicked = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.REPO_PICKER) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.DASHBOARD) { inclusive = true }
                }
            }

            // Refresh data every time dashboard is entered
            LaunchedEffect(Unit) {
                viewModel.loadDashboardData()
            }

            // Show dashboard regardless of pipeline state —
            // DashboardScreen handles its own empty/loading states internally
            DashboardScreen(
                viewModel            = viewModel,
                onPipelineClick      = { navController.navigate(Routes.pipelineDetail(it)) },
                onIncidentsClick     = { navController.navigate(Routes.INCIDENT_LIST) },
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }

        // ── Pipeline Detail ───────────────────────────────────────
        composable(
            route     = Routes.PIPELINE_DETAIL,
            arguments = listOf(navArgument("pipelineId") { type = NavType.StringType })
        ) { back ->
            val pipelineId = back.arguments?.getString("pipelineId") ?: ""

            // Fetch full detail (with jobs/steps) when screen opens
            LaunchedEffect(pipelineId) {
                viewModel.fetchPipelineById(pipelineId)
            }

            val detailState = viewModel.selectedPipelineState

            when (detailState) {
                is UiState.Loading, is UiState.Idle -> LoadingScreen("Loading pipeline...")
                is UiState.Error -> ErrorScreen(detailState.message)
                is UiState.Success -> PipelineDetailScreen(
                    pipelineId = pipelineId,
                    viewModel  = viewModel,
                    onBack     = { navController.popBackStack() }
                )
                else -> LoadingScreen()
            }
        }

        // ── Incident List ─────────────────────────────────────
        composable(Routes.INCIDENT_LIST) {
            LaunchedEffect(Unit) { viewModel.fetchIncidents() }
            IncidentListScreen(
                viewModel       = viewModel,
                onIncidentClick = { navController.navigate(Routes.incidentDetail(it)) },
                onBack          = { navController.popBackStack() }
            )
        }

        // ── Incident Detail ───────────────────────────────────
        composable(
            route     = Routes.INCIDENT_DETAIL,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { back ->
            val incidentId = back.arguments?.getString("incidentId") ?: ""
            LaunchedEffect(incidentId) { viewModel.fetchIncidentById(incidentId) }
            IncidentDetailScreen(
                incidentId = incidentId,
                viewModel  = viewModel,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Notifications ─────────────────────────────────────
        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LoadingScreen(message: String = "Loading...") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(message, color = Secondary)
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = androidx.compose.ui.graphics.Color.Red)
    }
}
