package com.example.opssync.data.remote

import com.example.opssync.data.models.*
import retrofit2.Response
import retrofit2.http.*

// ═══════════════════════════════════════════════════════════════
// AUTH API
// ═══════════════════════════════════════════════════════════════
interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @GET("auth/github")
    suspend fun getGitHubOAuthUrl(): Response<ApiResponse<GitHubOAuthUrlResponse>>
}

// ═══════════════════════════════════════════════════════════════
// INCIDENTS API
// ═══════════════════════════════════════════════════════════════
interface IncidentApi {

    @POST("incidents")
    suspend fun createIncident(
        @Body request: CreateIncidentRequest
    ): Response<ApiResponse<IncidentDto>>

    @GET("incidents")
    suspend fun getIncidents(
        @Query("status")   status: String?   = null,
        @Query("priority") priority: String? = null,
        @Query("sortBy")   sortBy: String?   = null,
        @Query("order")    order: String?    = null
    ): Response<ApiResponse<List<IncidentDto>>>

    @GET("incidents/{id}")
    suspend fun getIncidentById(
        @Path("id") id: String
    ): Response<ApiResponse<IncidentDto>>

    @PUT("incidents/{id}")
    suspend fun updateIncident(
        @Path("id")   id: String,
        @Body request: UpdateIncidentRequest
    ): Response<ApiResponse<IncidentDto>>

    @PATCH("incidents/{id}/assign")
    suspend fun assignIncident(
        @Path("id")   id: String,
        @Body request: AssignIncidentRequest
    ): Response<ApiResponse<IncidentDto>>
}

// ═══════════════════════════════════════════════════════════════
// PIPELINES API
// ═══════════════════════════════════════════════════════════════
interface PipelineApi {

    @GET("pipelines/repos")
    suspend fun getUserRepos(): Response<ApiResponse<List<RepoDto>>>

    @GET("pipelines/{owner}/{repo}")
    suspend fun getPipelines(
        @Path("owner") owner: String,
        @Path("repo")  repo: String
    ): Response<ApiResponse<List<PipelineRunDto>>>

    @GET("pipelines/{owner}/{repo}/{runId}")
    suspend fun getPipelineById(
        @Path("owner") owner: String,
        @Path("repo")  repo: String,
        @Path("runId") runId: Long
    ): Response<ApiResponse<PipelineRunDto>>

    @POST("pipelines/{owner}/{repo}/{runId}/rerun")
    suspend fun rerunPipeline(
        @Path("owner") owner: String,
        @Path("repo")  repo: String,
        @Path("runId") runId: Long
    ): Response<ApiResponse<Unit>>

    @POST("pipelines/{owner}/{repo}/{runId}/cancel")
    suspend fun cancelPipeline(
        @Path("owner") owner: String,
        @Path("repo")  repo: String,
        @Path("runId") runId: Long
    ): Response<ApiResponse<Unit>>

    // ── AUTO WEBHOOK CONNECT ─────────────────────────────────
    // Called when user selects a repo — registers GitHub webhook
    // so pipeline failures automatically create incidents
    @POST("pipelines/{owner}/{repo}/webhook/connect")
    suspend fun connectWebhook(
        @Path("owner") owner: String,
        @Path("repo")  repo: String
    ): Response<ApiResponse<Unit>>
}

// ═══════════════════════════════════════════════════════════════
// ESCALATIONS API
// ═══════════════════════════════════════════════════════════════
interface EscalationApi {

    @POST("escalations")
    suspend fun createEscalation(
        @Body request: CreateEscalationRequest
    ): Response<ApiResponse<EscalationDto>>

    @GET("escalations/{incidentId}")
    suspend fun getEscalationsForIncident(
        @Path("incidentId") incidentId: String
    ): Response<ApiResponse<List<EscalationDto>>>

    @PATCH("escalations/{id}/acknowledge")
    suspend fun acknowledgeEscalation(
        @Path("id") id: String
    ): Response<ApiResponse<EscalationDto>>

    @GET("escalations/auto-check")
    suspend fun autoCheck(): Response<ApiResponse<Unit>>
}
// ═══════════════════════════════════════════════════════════════
// TEAMS API
// ═══════════════════════════════════════════════════════════════
interface TeamsApi {

    // Save user's Teams incoming webhook URL
    @POST("teams/webhook")
    suspend fun saveWebhookUrl(
        @Body request: TeamsWebhookRequest
    ): Response<ApiResponse<Unit>>

    // Check if user has Teams webhook configured
    @GET("teams/webhook/status")
    suspend fun getWebhookStatus(): Response<ApiResponse<TeamsWebhookStatus>>

    // Send incident details to Teams
    @POST("teams/notify/incident/{incidentId}")
    suspend fun sendIncidentToTeams(
        @Path("incidentId") incidentId: String
    ): Response<ApiResponse<Unit>>

    // Send pipeline logs to Teams
    @POST("teams/notify/logs")
    suspend fun sendLogsToTeams(
        @Body request: TeamsLogsRequest
    ): Response<ApiResponse<Unit>>
}
