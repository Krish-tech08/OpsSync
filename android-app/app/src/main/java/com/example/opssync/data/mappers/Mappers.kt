package com.example.opssync.data.mappers

import com.example.opssync.data.*
import com.example.opssync.data.models.*

// ═══════════════════════════════════════════════════════════════
// INCIDENT MAPPERS
// ═══════════════════════════════════════════════════════════════

fun IncidentDto.toDomain(): Incident {
    return Incident(
        id          = this.id,
        title       = this.title,
        description = this.description,
        severity    = this.priority.toIncidentSeverity(),
        status      = this.status.toIncidentStatus(),
        service     = this.service ?: "Unknown",
        assignedTo  = this.assignedTo?.name ?: "Unassigned",
        duration    = this.duration ?: "—",
        region      = this.region ?: "global"
    )
}

private fun String.toIncidentSeverity(): IncidentSeverity = when (this.lowercase()) {
    "critical" -> IncidentSeverity.CRITICAL
    "high"     -> IncidentSeverity.HIGH
    "medium"   -> IncidentSeverity.MEDIUM
    else       -> IncidentSeverity.LOW
}

private fun String.toIncidentStatus(): IncidentStatus = when (this.lowercase()) {
    "open"         -> IncidentStatus.OPEN
    "acknowledged" -> IncidentStatus.ACKNOWLEDGED
    "resolved"     -> IncidentStatus.RESOLVED
    else           -> IncidentStatus.OPEN
}

// ═══════════════════════════════════════════════════════════════
// PIPELINE MAPPERS
// ═══════════════════════════════════════════════════════════════

fun PipelineRunDto.toDomain(): Pipeline {
    return Pipeline(
        id          = this.id.toString(),
        name        = this.name ?: "Unnamed Run",
        status      = this.toPipelineStatus(),
        environment = this.headCommit?.message?.take(30) ?: "PROD-ENV",
        triggeredBy = this.actor?.login ?: "unknown",
        startedAt   = this.createdAt?.toRelativeTime() ?: "unknown",
        duration    = this.toDurationString(),
        steps       = this.jobs?.flatMap { job ->
            job.steps?.map { step -> step.toDomain() } ?: emptyList()
        } ?: emptyList(),
        gitHash     = this.headCommit?.id?.take(7) ?: "",
        region      = "us-east-1",
        runnerId    = "run-${this.runNumber}",
        logs        = this.jobs?.flatMap { job ->
            buildJobLogs(job)
        } ?: emptyList()
    )
}

// Build readable log lines from job data
private fun buildJobLogs(job: JobDto): List<String> {
    val logs = mutableListOf<String>()
    logs.add("==> Job: ${job.name} [${job.status.uppercase()}]")
    job.steps?.forEach { step ->
        val icon = when (step.conclusion) {
            "success" -> "✓"
            "failure" -> "ERROR"
            "skipped" -> "-"
            else      -> "→"
        }
        logs.add("$icon Step ${step.number}: ${step.name} — ${step.conclusion ?: step.status}")
    }
    if (job.conclusion == "failure") {
        logs.add("ERROR Job '${job.name}' failed — check step details above")
    }
    return logs
}

// status: "queued"|"in_progress"|"completed"
// conclusion: "success"|"failure"|"cancelled"|null
private fun PipelineRunDto.toPipelineStatus(): PipelineStatus = when {
    this.status == "in_progress"   -> PipelineStatus.RUNNING
    this.status == "queued"        -> PipelineStatus.PENDING
    this.conclusion == "success"   -> PipelineStatus.SUCCESS
    this.conclusion == "failure"   -> PipelineStatus.FAILED
    this.conclusion == "cancelled" -> PipelineStatus.FAILED
    else                           -> PipelineStatus.PENDING
}

private fun PipelineRunDto.toDurationString(): String {
    if (this.status == "in_progress" || this.status == "queued") return "Running..."
    return "—"
}

// ═══════════════════════════════════════════════════════════════
// STEP MAPPERS
// ═══════════════════════════════════════════════════════════════

fun StepDto.toDomain(): PipelineStep {
    return PipelineStep(
        id          = this.number.toString(),
        name        = this.name ?: "Unnamed Step",
        status      = this.toStepStatus(),
        duration    = calcDuration(this.startedAt, this.completedAt),
        description = when (this.conclusion) {
            "failure" -> "Step failed"
            "skipped" -> "Step was skipped"
            "success" -> ""
            else      -> ""
        }
    )
}

private fun calcDuration(start: String?, end: String?): String {
    if (start == null || end == null) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val s = sdf.parse(start) ?: return ""
        val e = sdf.parse(end)   ?: return ""
        val diff = (e.time - s.time) / 1000
        if (diff < 60) "${diff}s" else "${diff / 60}m ${diff % 60}s"
    } catch (ex: Exception) { "" }
}

private fun StepDto.toStepStatus(): StepStatus = when {
    this.status == "in_progress" -> StepStatus.RUNNING
    this.status == "queued"      -> StepStatus.PENDING
    this.conclusion == "success" -> StepStatus.COMPLETED
    this.conclusion == "failure" -> StepStatus.FAILED
    else                         -> StepStatus.PENDING
}

// ═══════════════════════════════════════════════════════════════
// UTIL: RELATIVE TIME
// ═══════════════════════════════════════════════════════════════

private fun String.toRelativeTime(): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val date    = sdf.parse(this) ?: return this
        val diff    = System.currentTimeMillis() - date.time
        val minutes = diff / 60_000
        val hours   = minutes / 60
        val days    = hours / 24

        when {
            minutes < 1  -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours   < 24 -> "${hours}h ago"
            else         -> "${days}d ago"
        }
    } catch (e: Exception) {
        this
    }
}
