package com.example.opssync.data

enum class PipelineStatus {
    SUCCESS,    // Pipeline completed without errors
    FAILED,     // Pipeline encountered an error and stopped
    RUNNING,    // Pipeline is currently executing
    PENDING     // Pipeline is queued but not yet started
}

// ─── Pipeline Step Status ────────────────────────────────────
// Each step inside a pipeline has its own status
enum class StepStatus {
    COMPLETED,  // Step finished successfully
    RUNNING,    // Step is currently executing
    PENDING,    // Step hasn't started yet
    FAILED      // Step encountered an error
}

// ─── Incident Severity ──────────────────────────────────────
// Priority level of an incident
enum class IncidentSeverity {
    CRITICAL,   // Requires immediate attention – service down
    HIGH,       // Significant impact – needs urgent fix
    MEDIUM,     // Moderate impact – fix within hours
    LOW         // Minor issue – fix in next cycle
}

// ─── Incident Status ────────────────────────────────────────
// Lifecycle state of an incident
enum class IncidentStatus {
    OPEN,           // Reported but no action taken
    ACKNOWLEDGED,   // Someone is aware and looking at it
    RESOLVED        // Issue is fixed
}

// ─── Notification Type ──────────────────────────────────────
// Category of a notification/alert
enum class NotificationType {
    CRITICAL,   // Needs immediate action
    WARNING,    // Something to watch out for
    INFO,       // General informational alert
    SUCCESS     // Something completed successfully
}

// ─── Pipeline Data Class ─────────────────────────────────────
// Represents a single CI/CD pipeline
data class Pipeline(
    val id: String,                         // Unique identifier
    val name: String,                       // Display name e.g. "Build App"
    val status: PipelineStatus,             // Current status
    val environment: String,                // e.g. "PROD-ENV", "STAGING"
    val triggeredBy: String,                // User who triggered it
    val startedAt: String,                  // Human-readable time e.g. "2m ago"
    val duration: String,                   // e.g. "45.2s"
    val steps: List<PipelineStep>,          // List of steps in this pipeline
    val gitHash: String = "",               // Git commit hash
    val region: String = "us-east-1",       // Deployment region
    val runnerId: String = "",              // Runner machine ID
    val logs: List<String> = emptyList()    // Live log lines
)

// ─── Pipeline Step ────────────────────────────────────────────
// A single step within a pipeline's execution
data class PipelineStep(
    val id: String,
    val name: String,           // e.g. "Pre-flight Checks"
    val status: StepStatus,     // Current status of this step
    val duration: String = "",  // How long it took e.g. "0.4s"
    val description: String = ""// What this step does
)


// ─── Incident Data Class ──────────────────────────────────────
// Represents a system incident / alert
data class Incident(
    val id: String,                         // e.g. "INC-9021"
    val title: String,                      // Short title
    val description: String,                // Full description
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    val service: String,                    // Affected service name
    val assignedTo: String,                 // Engineer name
    val duration: String,                   // How long it's been active
    val region: String = "us-east-1"
)

// ─── Notification Data Class ──────────────────────────────────
// Represents an alert/notification in the notifications screen
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timeAgo: String,            // e.g. "5m ago"
    val isRead: Boolean = false
)
