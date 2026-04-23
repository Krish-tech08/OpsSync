package com.example.opssync.repository

import com.example.opssync.data.local.TokenManager
import com.example.opssync.data.models.*
import com.example.opssync.data.remote.AuthApi
import com.example.opssync.data.remote.EscalationApi
import com.example.opssync.data.remote.IncidentApi
import com.example.opssync.data.remote.PipelineApi
import com.example.opssync.data.remote.TeamsApi
import com.example.opssync.ui.state.UiState
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

// ═══════════════════════════════════════════════════════════════
// BASE SAFE-CALL HELPER
// ═══════════════════════════════════════════════════════════════
private suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): UiState<T> {
    return try {
        val response = call()
        val body     = response.body()
        when {
            response.isSuccessful && body?.success == true && body.data != null ->
                UiState.Success(body.data)
            response.isSuccessful ->
                UiState.Error(body?.message ?: "Something went wrong")
            response.code() == 401 ->
                UiState.Error("Session expired. Please log in again.")
            else ->
                UiState.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: Exception) {
        UiState.Error(e.message ?: "Network error. Please check your connection.")
    }
}

// ═══════════════════════════════════════════════════════════════
// AUTH REPOSITORY
// ═══════════════════════════════════════════════════════════════
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun getGitHubOAuthUrl(): UiState<GitHubOAuthUrlResponse> = safeApiCall {
        authApi.getGitHubOAuthUrl()
    }

    suspend fun loginWithToken(token: String, userId: String, email: String) {
        tokenManager.saveAuthData(token = token, userId = userId, email = email)
    }

    suspend fun login(email: String, password: String): UiState<AuthResponse> {
        val result = safeApiCall {
            authApi.login(LoginRequest(email = email, password = password))
        }
        if (result is UiState.Success) {
            tokenManager.saveAuthData(
                token  = result.data.token,
                userId = result.data.user.id,
                email  = result.data.user.email
            )
        }
        return result
    }

    suspend fun register(name: String, email: String, password: String): UiState<AuthResponse> {
        val result = safeApiCall {
            authApi.register(RegisterRequest(name = name, email = email, password = password))
        }
        if (result is UiState.Success) {
            tokenManager.saveAuthData(
                token  = result.data.token,
                userId = result.data.user.id,
                email  = result.data.user.email
            )
        }
        return result
    }

    suspend fun getMe(): UiState<UserDto> = safeApiCall { authApi.getMe() }

    suspend fun logout() { tokenManager.clearAuthData() }
}

// ═══════════════════════════════════════════════════════════════
// INCIDENT REPOSITORY
// ═══════════════════════════════════════════════════════════════
class IncidentRepository(
    private val incidentApi: IncidentApi
) {
    suspend fun getIncidents(
        status: String?   = null,
        priority: String? = null,
        sortBy: String?   = null,
        order: String?    = null
    ): UiState<List<IncidentDto>> = safeApiCall {
        incidentApi.getIncidents(status, priority, sortBy, order)
    }

    suspend fun getIncidentById(id: String): UiState<IncidentDto> = safeApiCall {
        incidentApi.getIncidentById(id)
    }

    suspend fun createIncident(
        title: String,
        description: String,
        priority: String
    ): UiState<IncidentDto> = safeApiCall {
        incidentApi.createIncident(CreateIncidentRequest(title, description, priority))
    }

    suspend fun acknowledgeIncident(id: String): UiState<IncidentDto> = safeApiCall {
        incidentApi.updateIncident(id, UpdateIncidentRequest(status = "acknowledged"))
    }

    suspend fun resolveIncident(id: String): UiState<IncidentDto> = safeApiCall {
        incidentApi.updateIncident(id, UpdateIncidentRequest(status = "resolved"))
    }

    suspend fun assignIncident(incidentId: String, userId: String): UiState<IncidentDto> = safeApiCall {
        incidentApi.assignIncident(incidentId, AssignIncidentRequest(userId))
    }
}

// ═══════════════════════════════════════════════════════════════
// PIPELINE REPOSITORY
// ═══════════════════════════════════════════════════════════════
class PipelineRepository(
    private val pipelineApi: PipelineApi
) {
    suspend fun getUserRepos(): UiState<List<RepoDto>> = safeApiCall {
        pipelineApi.getUserRepos()
    }

    suspend fun getPipelines(owner: String, repo: String): UiState<List<PipelineRunDto>> = safeApiCall {
        pipelineApi.getPipelines(owner, repo)
    }

    suspend fun getPipelineById(owner: String, repo: String, runId: Long): UiState<PipelineRunDto> = safeApiCall {
        pipelineApi.getPipelineById(owner, repo, runId)
    }

    suspend fun rerunPipeline(owner: String, repo: String, runId: Long): UiState<Unit> = safeApiCall {
        pipelineApi.rerunPipeline(owner, repo, runId)
    }

    suspend fun cancelPipeline(owner: String, repo: String, runId: Long): UiState<Unit> = safeApiCall {
        pipelineApi.cancelPipeline(owner, repo, runId)
    }

    // ── Registers GitHub webhook for automatic incident creation ──
    // Fires when user selects a repo — silent, errors are ignored
    suspend fun connectWebhook(owner: String, repo: String): UiState<Unit> = safeApiCall {
        pipelineApi.connectWebhook(owner, repo)
    }
}

// ═══════════════════════════════════════════════════════════════
// ESCALATION REPOSITORY
// ═══════════════════════════════════════════════════════════════
class EscalationRepository(
    private val escalationApi: EscalationApi
) {
    suspend fun createEscalation(
        incidentId: String,
        escalatedTo: String,
        reason: String,
        priority: String
    ): UiState<EscalationDto> = safeApiCall {
        escalationApi.createEscalation(
            CreateEscalationRequest(incidentId, escalatedTo, reason, priority)
        )
    }

    suspend fun getEscalationsForIncident(incidentId: String): UiState<List<EscalationDto>> = safeApiCall {
        escalationApi.getEscalationsForIncident(incidentId)
    }

    suspend fun acknowledgeEscalation(id: String): UiState<EscalationDto> = safeApiCall {
        escalationApi.acknowledgeEscalation(id)
    }
}
// ═══════════════════════════════════════════════════════════════
// TEAMS REPOSITORY
// ═══════════════════════════════════════════════════════════════
class TeamsRepository(
    private val teamsApi: TeamsApi
) {
    suspend fun saveWebhookUrl(url: String): UiState<Unit> = safeApiCall {
        teamsApi.saveWebhookUrl(TeamsWebhookRequest(webhookUrl = url))
    }

    suspend fun getWebhookStatus(): UiState<TeamsWebhookStatus> = safeApiCall {
        teamsApi.getWebhookStatus()
    }

    suspend fun sendIncidentToTeams(incidentId: String): UiState<Unit> = safeApiCall {
        teamsApi.sendIncidentToTeams(incidentId)
    }

    suspend fun sendLogsToTeams(
        pipelineId:   String,
        pipelineName: String,
        status:       String,
        logs:         List<String>,
        gitHash:      String,
        triggeredBy:  String,
        repoName:     String
    ): UiState<Unit> = safeApiCall {
        teamsApi.sendLogsToTeams(
            TeamsLogsRequest(
                pipelineId   = pipelineId,
                pipelineName = pipelineName,
                status       = status,
                logs         = logs,
                gitHash      = gitHash,
                triggeredBy  = triggeredBy,
                repoName     = repoName
            )
        )
    }
}
// ═══════════════════════════════════════════════════════════════
// NOTIFICATION API
// ═══════════════════════════════════════════════════════════════
interface NotificationApi {

    // Register device FCM token — call on every app launch
    @POST("notifications/fcm/register")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    ): Response<ApiResponse<Unit>>

    // Remove FCM token on logout
    @DELETE("notifications/fcm/register")
    suspend fun removeFcmToken(): Response<ApiResponse<Unit>>
}
