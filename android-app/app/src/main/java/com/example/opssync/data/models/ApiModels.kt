package com.example.opssync.data.models

import com.google.gson.annotations.SerializedName

// ── GENERIC WRAPPER ──────────────────────────────────────────
// Matches backend shape: { success: Boolean, data: T, message: String }
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: T? = null,
    @SerializedName("message") val message: String? = null
)
// ── REPOS ─────────────────────────────────────────────────────
data class RepoDto(
    @SerializedName("id")          val id: Long,
    @SerializedName("name")        val name: String?,           // ← nullable
    @SerializedName("full_name")   val fullName: String? = null,// ← nullable
    @SerializedName("private")     val isPrivate: Boolean = false,
    @SerializedName("description") val description: String? = null,
    @SerializedName("language")    val language: String? = null,
    @SerializedName("owner")       val owner: RepoOwnerDto? = null
)

data class RepoOwnerDto(
    @SerializedName("login")      val login: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)
// ── AUTH ─────────────────────────────────────────────────────
data class LoginRequest(
    @SerializedName("email")    val email: String,
    @SerializedName("password") val password: String
)
// ── GITHUB OAUTH ─────────────────────────────────────────────
data class GitHubOAuthUrlResponse(
    @SerializedName("url") val url: String
)
data class RegisterRequest(
    @SerializedName("name")     val name: String,
    @SerializedName("email")    val email: String,
    @SerializedName("password") val password: String
)

// Backend returns: { data: { token: "...", user: { _id, name, email, role } } }
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user")  val user: UserDto
)

data class UserDto(
    // ✅ FIX: backend sends "_id" (MongoDB default), not "id"
    @SerializedName("_id")   val id: String,
    @SerializedName("name")  val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role")  val role: String? = null
)

// ── INCIDENTS ────────────────────────────────────────────────
data class CreateIncidentRequest(
    @SerializedName("title")       val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority")    val priority: String
)

data class UpdateIncidentRequest(
    @SerializedName("title")       val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priority")    val priority: String? = null,
    @SerializedName("status")      val status: String? = null
)

data class AssignIncidentRequest(
    @SerializedName("userId") val userId: String
)

data class IncidentDto(
    @SerializedName("_id")         val id: String,
    @SerializedName("title")       val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority")    val priority: String,
    @SerializedName("status")      val status: String,
    @SerializedName("service")     val service: String? = null,
    @SerializedName("region")      val region: String? = null,
    @SerializedName("assignedTo")  val assignedTo: AssignedUserDto? = null,
    @SerializedName("duration")    val duration: String? = null,
    @SerializedName("createdAt")   val createdAt: String? = null,
    @SerializedName("updatedAt")   val updatedAt: String? = null
)

data class AssignedUserDto(
    @SerializedName("_id")  val id: String,
    @SerializedName("name") val name: String
)

// ── PIPELINES ────────────────────────────────────────────────
data class PipelineRunDto(
    @SerializedName("id")           val id: Long,
    @SerializedName("name")         val name: String?,       // ← nullable
    @SerializedName("status")       val status: String,
    @SerializedName("conclusion")   val conclusion: String?,
    @SerializedName("run_number")   val runNumber: Int,
    @SerializedName("created_at")   val createdAt: String?,  // ← nullable
    @SerializedName("updated_at")   val updatedAt: String?,  // ← nullable
    @SerializedName("html_url")     val htmlUrl: String? = null,
    @SerializedName("actor")        val actor: ActorDto? = null,
    @SerializedName("head_commit")  val headCommit: HeadCommitDto? = null,
    @SerializedName("jobs")         val jobs: List<JobDto>? = null
)
// ── TEAMS INTEGRATION ────────────────────────────────────────
data class TeamsWebhookRequest(
    @SerializedName("webhookUrl") val webhookUrl: String
)
// ── FCM NOTIFICATIONS ────────────────────────────────────────
data class FcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

data class TeamsWebhookStatus(
    @SerializedName("isConnected") val isConnected: Boolean
)
data class TeamsLogsRequest(
    @SerializedName("pipelineId")   val pipelineId:   String,
    @SerializedName("pipelineName") val pipelineName: String,
    @SerializedName("status")       val status:       String,
    @SerializedName("logs")         val logs:         List<String>,
    @SerializedName("gitHash")      val gitHash:      String,
    @SerializedName("triggeredBy")  val triggeredBy:  String,
    @SerializedName("repoName")     val repoName:     String
)
data class ActorDto(
    @SerializedName("login")      val login: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class HeadCommitDto(
    @SerializedName("id")      val id: String,
    @SerializedName("message") val message: String? = null
)

data class JobDto(
    @SerializedName("id")           val id: Long,
    @SerializedName("name")         val name: String,
    @SerializedName("status")       val status: String,
    @SerializedName("conclusion")   val conclusion: String? = null,
    @SerializedName("started_at")   val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("steps")        val steps: List<StepDto>? = null
)

data class StepDto(
    @SerializedName("name")         val name: String,
    @SerializedName("status")       val status: String,
    @SerializedName("conclusion")   val conclusion: String? = null,
    @SerializedName("number")       val number: Int,
    @SerializedName("started_at")   val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null
)

// ── ESCALATIONS ──────────────────────────────────────────────
data class CreateEscalationRequest(
    @SerializedName("incidentId")  val incidentId: String,
    @SerializedName("escalatedTo") val escalatedTo: String,
    @SerializedName("reason")      val reason: String,
    @SerializedName("priority")    val priority: String
)

data class EscalationDto(
    @SerializedName("_id")          val id: String,
    @SerializedName("incidentId")   val incidentId: String,
    @SerializedName("escalatedTo")  val escalatedTo: AssignedUserDto? = null,
    @SerializedName("reason")       val reason: String,
    @SerializedName("priority")     val priority: String,
    @SerializedName("acknowledged") val acknowledged: Boolean,
    @SerializedName("createdAt")    val createdAt: String? = null
)
