package com.example.opssync.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.opssync.data.Incident
import com.example.opssync.data.IncidentStatus
import com.example.opssync.data.Pipeline
import com.example.opssync.data.PipelineStatus
import com.example.opssync.data.AppNotification
import com.example.opssync.data.local.TokenManager
import com.example.opssync.data.mappers.toDomain
import com.example.opssync.data.models.AuthResponse
import com.example.opssync.data.models.GitHubOAuthUrlResponse
import com.example.opssync.data.models.PipelineRunDto
import com.example.opssync.data.models.RepoDto
import com.example.opssync.data.models.TeamsWebhookStatus
import com.example.opssync.repository.AuthRepository
import com.example.opssync.repository.IncidentRepository
import com.example.opssync.repository.PipelineRepository
import com.example.opssync.network.NetworkModule
import com.example.opssync.repository.TeamsRepository
import com.example.opssync.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application.applicationContext)

    private val authRepository = AuthRepository(
        authApi      = NetworkModule.provideAuthApi(tokenManager),
        tokenManager = tokenManager
    )
    private val incidentRepository = IncidentRepository(
        incidentApi = NetworkModule.provideIncidentApi(tokenManager)
    )
    private val pipelineRepository = PipelineRepository(
        pipelineApi = NetworkModule.providePipelineApi(tokenManager)
    )
    private val notificationApi = NetworkModule.provideNotificationApi(tokenManager)
    // ═══════════════════════════════════════════════════════════
    // AUTH STATE
    // ═══════════════════════════════════════════════════════════
    private val createdIncidentRunIds = mutableSetOf<Long>()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    var authState by mutableStateOf<UiState<AuthResponse>>(UiState.Idle)
        private set

    init {
        viewModelScope.launch {
            tokenManager.tokenFlow.collect { token ->
                _isLoggedIn.value = token != null
                // Register FCM token whenever user is logged in
                if (token != null) {
                    registerFcmTokenIfAvailable()
                }
            }
        }
    }

    private fun registerFcmTokenIfAvailable() {
        viewModelScope.launch {
            try {
                val fcmToken = tokenManager.getFcmToken()
                if (fcmToken != null) {
                    notificationApi.registerFcmToken(
                        com.example.opssync.data.models.FcmTokenRequest(fcmToken)
                    )
                } else {
                    // Request a new FCM token if we don't have one yet
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            viewModelScope.launch {
                                tokenManager.saveFcmToken(token)
                                notificationApi.registerFcmToken(
                                    com.example.opssync.data.models.FcmTokenRequest(token)
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Registration failed: ${e.message}")
            }
        }
    }
    fun login(email: String, password: String) {
        viewModelScope.launch {
            authState = UiState.Loading
            authState = authRepository.login(email, password)
        }
    }

    fun logout() {
        viewModelScope.launch {
            selectedRepo = null
            webhookState = UiState.Idle
            // Remove FCM token from backend before clearing auth
            try { notificationApi.removeFcmToken() } catch (e: Exception) { /* ignore */ }
            authRepository.logout()
        }
    }

    fun resetAuthState() { authState = UiState.Idle }

    // ═══════════════════════════════════════════════════════════
    // GITHUB OAUTH
    // ═══════════════════════════════════════════════════════════

    var gitHubUrlState by mutableStateOf<UiState<GitHubOAuthUrlResponse>>(UiState.Idle)
        private set

    fun fetchGitHubOAuthUrl() {
        viewModelScope.launch {
            gitHubUrlState = UiState.Loading
            gitHubUrlState = authRepository.getGitHubOAuthUrl()
        }
    }

    fun handleGitHubCallback(token: String) {
        viewModelScope.launch {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(
                    android.util.Base64.decode(
                        parts[1],
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
                    )
                )
                val json   = org.json.JSONObject(payload)
                val userId = json.optString("id", "")
                val email  = json.optString("email", "")
                authRepository.loginWithToken(token, userId, email)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REPO PICKER STATE
    // ═══════════════════════════════════════════════════════════

    var reposState by mutableStateOf<UiState<List<RepoDto>>>(UiState.Idle)
        private set

    var selectedRepo by mutableStateOf<RepoDto?>(null)
        private set

    // ── Webhook connection state ─────────────────────────────
    // Exposed so RepoPickerScreen can show connecting indicator
    var webhookState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

    fun fetchUserRepos() {
        viewModelScope.launch {
            reposState = UiState.Loading
            reposState = pipelineRepository.getUserRepos()
        }
    }

    fun selectRepo(repo: RepoDto) {
        selectedRepo = repo

        // 1. Load pipelines immediately
        fetchPipelines()
        fetchIncidents()
        // 2. Auto-connect webhook silently in background
        // This registers a GitHub webhook on the selected repo so any
        // future pipeline failure automatically creates an incident
        viewModelScope.launch {
            webhookState = UiState.Loading
            val owner = repo.owner?.login?.takeIf { it.isNotBlank() }
                ?: repo.fullName?.substringBefore("/")
                ?: ""
            val repoName = repo.name ?: ""

            webhookState = pipelineRepository.connectWebhook(owner, repoName)

            // Log result but don't block the user — webhook is best-effort
            when (webhookState) {
                is UiState.Success -> android.util.Log.d("OpsSync", "✅ Webhook connected for $owner/$repoName")
                is UiState.Error   -> android.util.Log.w("OpsSync", "⚠️ Webhook connect failed: ${(webhookState as UiState.Error).message}")
                else -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PIPELINE STATE
    // ═══════════════════════════════════════════════════════════

    val pipelines: List<Pipeline>
        get() = (pipelinesState as? UiState.Success)?.data ?: emptyList()

    var pipelinesState by mutableStateOf<UiState<List<Pipeline>>>(UiState.Idle)
        private set

    var selectedPipelineState by mutableStateOf<UiState<Pipeline>>(UiState.Idle)
        private set

    var recentRunsState by mutableStateOf<UiState<List<Pipeline>>>(UiState.Idle)
        private set

    val recentRuns: List<Pipeline>
        get() = (recentRunsState as? UiState.Success)?.data ?: emptyList()

    fun fetchRecentRuns() {
        val repo = selectedRepo ?: return
        viewModelScope.launch {
            recentRunsState = UiState.Loading
            recentRunsState = when (
                val result = pipelineRepository.getPipelines(
                    owner = repo.owner?.login?.takeIf { it.isNotBlank() } ?: repo.fullName?.substringBefore("/") ?: "",
                    repo  = repo.name ?: ""
                )
            ) {
                is UiState.Success -> UiState.Success(result.data.map { it.toDomain() })
                is UiState.Error   -> UiState.Error(result.message)
                else               -> UiState.Error("Unexpected state")
            }
        }
    }

    fun fetchPipelines() {
        val repo = selectedRepo ?: return
        viewModelScope.launch {
            pipelinesState = UiState.Loading
            val result = pipelineRepository.getPipelines(
                owner = repo.owner?.login?.takeIf { it.isNotBlank() } ?: repo.fullName?.substringBefore("/") ?: "",
                repo  = repo.name ?: ""
            )
            pipelinesState = when (result) {
                is UiState.Success -> UiState.Success(result.data.map { it.toDomain() })
                is UiState.Error   -> UiState.Error(result.message)
                else               -> UiState.Error("Unexpected state")
            }

            // ── Auto-create incident for any newly failed pipeline ──
            if (result is UiState.Success) {
                result.data
                    .filter { it.conclusion == "failure" }
                    .forEach { run ->
                        autoCreateIncidentIfNeeded(run, repo)
                    }
            }
        }
    }

    private fun autoCreateIncidentIfNeeded(run: PipelineRunDto, repo: RepoDto) {
        // Skip if we already created an incident for this run this session
        if (run.id in createdIncidentRunIds) return
        createdIncidentRunIds.add(run.id)

        viewModelScope.launch {
            val repoFullName = "${repo.owner?.login ?: ""}/${repo.name ?: ""}"
            val priority = when {
                run.headCommit?.message?.contains("main")   == true -> "critical"
                run.headCommit?.message?.contains("master") == true -> "critical"
                else -> "high"
            }

            val result = incidentRepository.createIncident(
                title       = "Pipeline Failed: ${run.name ?: "Unnamed"} on ${repo.name ?: ""}",
                description = "Workflow \"${run.name}\" failed.\n\nRepo: $repoFullName\nRun #${run.runNumber}\nCommit: ${run.headCommit?.id?.take(7) ?: "unknown"}\nTriggered by: ${run.actor?.login ?: "unknown"}",
                priority    = priority
            )

            if (result is UiState.Success) {
                android.util.Log.d("OpsSync", "✅ Incident auto-created for run ${run.id}")
                fetchIncidents() // refresh list so it shows immediately
            } else {
                android.util.Log.e("OpsSync", "❌ Failed to create incident: ${(result as? UiState.Error)?.message}")
                // Remove from set so it retries next time
                createdIncidentRunIds.remove(run.id)
            }
        }
    }

    fun fetchPipelineById(runId: String) {
        val repo = selectedRepo ?: return
        val id   = runId.toLongOrNull() ?: return
        viewModelScope.launch {
            selectedPipelineState = UiState.Loading
            selectedPipelineState = when (
                val result = pipelineRepository.getPipelineById(
                    owner = repo.owner?.login?.takeIf { it.isNotBlank() } ?: repo.fullName?.substringBefore("/") ?: "",
                    repo  = repo.name ?: "",
                    runId = id
                )
            ) {
                is UiState.Success -> UiState.Success(result.data.toDomain())
                is UiState.Error   -> UiState.Error(result.message)
                else               -> UiState.Error("Unexpected state")
            }
        }
    }

    fun getPipelineById(id: String): Pipeline? =
        (pipelinesState as? UiState.Success)?.data?.find { it.id == id }

    fun rerunPipeline(runId: String) {
        val repo = selectedRepo ?: return
        val id   = runId.toLongOrNull() ?: return
        viewModelScope.launch {
            pipelineRepository.rerunPipeline(
                owner = repo.owner?.login?.takeIf { it.isNotBlank() } ?: repo.fullName?.substringBefore("/") ?: "",
                repo  = repo.name ?: "",
                runId = id
            )
            fetchPipelines()
        }
    }

    fun cancelPipeline(runId: String) {
        val repo = selectedRepo ?: return
        val id   = runId.toLongOrNull() ?: return
        viewModelScope.launch {
            pipelineRepository.cancelPipeline(
                owner = repo.owner?.login?.takeIf { it.isNotBlank() } ?: repo.fullName?.substringBefore("/") ?: "",
                repo  = repo.name ?: "",
                runId = id
            )
            fetchPipelines()
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INCIDENT STATE
    // ═══════════════════════════════════════════════════════════

    var incidentsState by mutableStateOf<UiState<List<Incident>>>(UiState.Idle)
        private set

    var selectedIncidentState by mutableStateOf<UiState<Incident>>(UiState.Idle)
        private set

    val incidents: List<Incident>
        get() = (incidentsState as? UiState.Success)?.data ?: emptyList()

    fun fetchIncidents(
        status: String?   = null,
        priority: String? = null,
        sortBy: String?   = "createdAt",
        order: String?    = "desc"
    ) {
        viewModelScope.launch {
            incidentsState = UiState.Loading
            incidentsState = when (val result = incidentRepository.getIncidents(status, priority, sortBy, order)) {
                is UiState.Success -> UiState.Success(result.data.map { it.toDomain() })
                is UiState.Error   -> UiState.Error(result.message)
                else               -> UiState.Error("Unexpected state")
            }
        }
    }

    fun fetchIncidentById(id: String) {
        viewModelScope.launch {
            selectedIncidentState = UiState.Loading
            selectedIncidentState = when (val result = incidentRepository.getIncidentById(id)) {
                is UiState.Success -> UiState.Success(result.data.toDomain())
                is UiState.Error   -> UiState.Error(result.message)
                else               -> UiState.Error("Unexpected state")
            }
        }
    }

    fun getIncidentById(id: String): Incident? =
        incidents.find { it.id == id }
            ?: (selectedIncidentState as? UiState.Success)?.data?.takeIf { it.id == id }

    fun acknowledgeIncident(incidentId: String) {
        viewModelScope.launch {
            val result = incidentRepository.acknowledgeIncident(incidentId)
            if (result is UiState.Success) {
                val updated     = result.data.toDomain()
                val currentList = incidents.toMutableList()
                val idx         = currentList.indexOfFirst { it.id == incidentId }
                if (idx != -1) {
                    currentList[idx] = updated
                    incidentsState   = UiState.Success(currentList)
                }
                if ((selectedIncidentState as? UiState.Success)?.data?.id == incidentId)
                    selectedIncidentState = UiState.Success(updated)
            }
        }
    }

    fun resolveIncident(incidentId: String) {
        viewModelScope.launch {
            val result = incidentRepository.resolveIncident(incidentId)
            if (result is UiState.Success) {
                val updated     = result.data.toDomain()
                val currentList = incidents.toMutableList()
                val idx         = currentList.indexOfFirst { it.id == incidentId }
                if (idx != -1) {
                    currentList[idx] = updated
                    incidentsState   = UiState.Success(currentList)
                }
                if ((selectedIncidentState as? UiState.Success)?.data?.id == incidentId)
                    selectedIncidentState = UiState.Success(updated)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DASHBOARD STATS
    // ═══════════════════════════════════════════════════════════

    val dashboardStats: Map<String, String>
        get() {
            val pipes      = pipelines
            val incs       = incidents
            val total      = pipes.size
            val successCnt = pipes.count { it.status == PipelineStatus.SUCCESS }
            val uptime     = if (total > 0) "${"%.1f".format(successCnt * 100.0 / total)}%" else "—"
            return mapOf(
                "uptime"       to uptime,
                "activeNodes"  to total.toString(),
                "latency"      to "—",
                "successCount" to successCnt.toString(),
                "runningCount" to pipes.count { it.status == PipelineStatus.RUNNING }.toString(),
                "failedCount"  to incs.count  { it.status == IncidentStatus.OPEN }.toString()
            )
        }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════

    val notifications = mutableStateOf<List<AppNotification>>(emptyList())

    val unreadCount: Int get() = notifications.value.count { !it.isRead }

    fun markNotificationRead(id: String) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsRead() {
        notifications.value = notifications.value.map { it.copy(isRead = true) }
    }

    // ═══════════════════════════════════════════════════════════
    // INITIAL LOAD
    // ═══════════════════════════════════════════════════════════

    fun loadDashboardData() {
        fetchPipelines()
        fetchIncidents()
    }
    // ═══════════════════════════════════════════════════════════════
// TEAMS INTEGRATION
// ═══════════════════════════════════════════════════════════════

    var teamsWebhookStatus  by mutableStateOf<UiState<TeamsWebhookStatus>>(UiState.Idle)
        private set

    var teamsSendState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

    fun fetchTeamsWebhookStatus() {
        viewModelScope.launch {
            teamsWebhookStatus = teamsRepository.getWebhookStatus()
        }
    }

    fun saveTeamsWebhookUrl(url: String) {
        viewModelScope.launch {
            teamsSendState    = UiState.Loading
            teamsSendState    = teamsRepository.saveWebhookUrl(url)
            // Refresh status after save
            if (teamsSendState is UiState.Success) {
                fetchTeamsWebhookStatus()
            }
        }
    }

    fun sendIncidentToTeams(incidentId: String) {
        viewModelScope.launch {
            teamsSendState = UiState.Loading
            teamsSendState = teamsRepository.sendIncidentToTeams(incidentId)
        }
    }

    fun sendPipelineLogsToTeams(pipeline: Pipeline) {
        val repo = selectedRepo
        viewModelScope.launch {
            teamsSendState = UiState.Loading
            teamsSendState = teamsRepository.sendLogsToTeams(
                pipelineId   = pipeline.id,
                pipelineName = pipeline.name,
                status       = pipeline.status.name,
                logs         = pipeline.logs,
                gitHash      = pipeline.gitHash,
                triggeredBy  = pipeline.triggeredBy,
                repoName     = "${repo?.owner?.login ?: ""}/${repo?.name ?: ""}"
            )
        }
    }

    fun resetTeamsSendState() {
        teamsSendState = UiState.Idle
    }
    private val teamsRepository = TeamsRepository(
        teamsApi = NetworkModule.provideTeamsApi(tokenManager)
    )
}
