package com.example.opssync.data

object DummyData {

    // ─────────────────────────────────────────────────────────
    // PIPELINES
    // ─────────────────────────────────────────────────────────

    val pipelines = listOf(

        Pipeline(
            id = "pipe-001",
            name = "Core-API-Deploy",
            status = PipelineStatus.SUCCESS,
            environment = "PROD-ENV",
            triggeredBy = "admin_jdoe",
            startedAt = "2m ago",
            duration = "1m 24s",
            runnerId = "i-0a83e921b1",
            region = "us-east-1",
            gitHash = "f4a12c8",
            steps = listOf(
                PipelineStep(
                    id = "s1", name = "Pre-flight Checks",
                    status = StepStatus.COMPLETED, duration = "0.4s",
                    description = "Security scan and environment validation successful."
                ),
                PipelineStep(
                    id = "s2", name = "Artifact Build",
                    status = StepStatus.COMPLETED, duration = "45.2s",
                    description = "Docker image tagged: ops-sync-web:v2.4.1-prod"
                ),
                PipelineStep(
                    id = "s3", name = "Kubernetes Deployment",
                    status = StepStatus.COMPLETED, duration = "38s",
                    description = "Updating deployment web-server in namespace production"
                ),
                PipelineStep(
                    id = "s4", name = "Post-Deployment Tests",
                    status = StepStatus.COMPLETED, duration = "0.8s",
                    description = "E2E Smoke tests and health check verification."
                )
            ),
            logs = listOf(
                "INFO [14:22:01] Authenticating with cluster 'k8s-us-east-1'...",
                "INFO [14:22:02] Fetching current deployment manifest...",
                "INFO [14:22:05] Applying update for deployment 'web-server'",
                "WARN [14:22:08] Node 'ip-10-0-24-112' reported high CPU pressure",
                "INFO [14:22:12] Scaling up replica count to 3"
            )
        ),

        Pipeline(
            id = "pipe-002",
            name = "Auth-Service-Build",
            status = PipelineStatus.RUNNING,
            environment = "STAGING",
            triggeredBy = "dev_sarah",
            startedAt = "5m ago",
            duration = "Running...",
            runnerId = "i-0b94f032c2",
            region = "us-west-2",
            gitHash = "a9c34e1",
            steps = listOf(
                PipelineStep(
                    id = "s1", name = "Pre-flight Checks",
                    status = StepStatus.COMPLETED, duration = "0.6s",
                    description = "Security scan passed."
                ),
                PipelineStep(
                    id = "s2", name = "Artifact Build",
                    status = StepStatus.COMPLETED, duration = "32.1s",
                    description = "Docker image tagged: auth-service:v1.8.3-staging"
                ),
                PipelineStep(
                    id = "s3", name = "Integration Tests",
                    status = StepStatus.RUNNING, duration = "",
                    description = "Running auth flow integration tests..."
                ),
                PipelineStep(
                    id = "s4", name = "Deploy to Staging",
                    status = StepStatus.PENDING, duration = "",
                    description = "Waiting for tests to complete."
                )
            ),
            logs = listOf(
                "INFO [14:30:01] Build started by dev_sarah",
                "INFO [14:30:45] Docker build complete",
                "INFO [14:35:22] Starting integration test suite..."
            )
        ),

        Pipeline(
            id = "pipe-003",
            name = "Legacy-Data-Migration",
            status = PipelineStatus.FAILED,
            environment = "PROD-ENV",
            triggeredBy = "ops_mike",
            startedAt = "15m ago",
            duration = "8m 12s",
            runnerId = "i-0c73d821a4",
            region = "eu-west-1",
            gitHash = "3bc98f2",
            steps = listOf(
                PipelineStep(
                    id = "s1", name = "Pre-flight Checks",
                    status = StepStatus.COMPLETED, duration = "1.2s",
                    description = "Validation passed."
                ),
                PipelineStep(
                    id = "s2", name = "Database Backup",
                    status = StepStatus.COMPLETED, duration = "3m 40s",
                    description = "Snapshot created: db-backup-20231024"
                ),
                PipelineStep(
                    id = "s3", name = "Data Migration Script",
                    status = StepStatus.FAILED, duration = "4m 30s",
                    description = "ERROR: TIMEOUT_EXCEPTION – Connection timed out after 270s"
                ),
                PipelineStep(
                    id = "s4", name = "Verification",
                    status = StepStatus.PENDING, duration = "",
                    description = "Skipped due to previous step failure."
                )
            ),
            logs = listOf(
                "INFO [13:50:01] Starting data migration for legacy_users table",
                "INFO [13:50:05] Connected to source DB",
                "WARN [13:54:20] Query taking longer than expected...",
                "ERROR [13:58:13] TIMEOUT_EXCEPTION: connection timed out after 270s",
                "ERROR [13:58:13] Pipeline aborted. Rolling back changes."
            )
        ),

        Pipeline(
            id = "pipe-004",
            name = "Deploy Backend",
            status = PipelineStatus.FAILED,
            environment = "PROD-ENV",
            triggeredBy = "admin_jdoe",
            startedAt = "1h ago",
            duration = "3m 45s",
            runnerId = "i-0d12e654b5",
            region = "us-east-1",
            gitHash = "7ef21d9",
            steps = listOf(
                PipelineStep(
                    id = "s1", name = "Pre-flight Checks",
                    status = StepStatus.COMPLETED, duration = "0.3s",
                    description = "All checks passed."
                ),
                PipelineStep(
                    id = "s2", name = "Build Backend",
                    status = StepStatus.COMPLETED, duration = "2m 10s",
                    description = "Gradle build successful."
                ),
                PipelineStep(
                    id = "s3", name = "Deploy",
                    status = StepStatus.FAILED, duration = "1m 32s",
                    description = "ERROR: OOMKilled – container ran out of memory"
                ),
                PipelineStep(
                    id = "s4", name = "Health Check",
                    status = StepStatus.PENDING, duration = "",
                    description = "Skipped."
                )
            ),
            logs = listOf(
                "INFO [13:00:01] Deploying backend v3.1.2",
                "ERROR [13:03:45] OOMKilled – container exceeded memory limit 512Mi"
            )
        ),

        Pipeline(
            id = "pipe-005",
            name = "Test Suite",
            status = PipelineStatus.RUNNING,
            environment = "STAGING",
            triggeredBy = "ci_bot",
            startedAt = "3m ago",
            duration = "Running...",
            runnerId = "i-0e45f987c6",
            region = "us-east-1",
            gitHash = "2ad56bc",
            steps = listOf(
                PipelineStep(
                    id = "s1", name = "Unit Tests",
                    status = StepStatus.COMPLETED, duration = "45s",
                    description = "412 tests passed, 0 failed."
                ),
                PipelineStep(
                    id = "s2", name = "Integration Tests",
                    status = StepStatus.RUNNING, duration = "",
                    description = "Running 86 integration tests..."
                ),
                PipelineStep(
                    id = "s3", name = "E2E Tests",
                    status = StepStatus.PENDING, duration = "",
                    description = "Waiting..."
                ),
                PipelineStep(
                    id = "s4", name = "Coverage Report",
                    status = StepStatus.PENDING, duration = "",
                    description = "Waiting..."
                )
            ),
            logs = listOf(
                "INFO [14:40:01] Starting test suite",
                "INFO [14:40:46] Unit tests complete: 412/412 passed",
                "INFO [14:41:02] Starting integration tests..."
            )
        )
    )

    // ─────────────────────────────────────────────────────────
    // INCIDENTS
    // ─────────────────────────────────────────────────────────

    val incidents = listOf(

        Incident(
            id = "INC-9021",
            title = "Global Authentication Gateway Timeout",
            description = "Identity provider in us-east-1 is returning 504 Gateway Timeouts. All login attempts failing. Affects 100% of users attempting to authenticate. Root cause suspected to be misconfigured load balancer rule after last deployment.",
            severity = IncidentSeverity.CRITICAL,
            status = IncidentStatus.OPEN,
            service = "Auth Gateway",
            assignedTo = "Sarah R.",
            duration = "02:44:12",
            region = "us-east-1"
        ),

        Incident(
            id = "INC-8844",
            title = "High CPU Usage on Prod Nodes",
            description = "Multiple production nodes reporting CPU usage above 90% for the past 30 minutes. Auto-scaling has kicked in but nodes are still under pressure. May be related to the recent data migration job running large queries.",
            severity = IncidentSeverity.HIGH,
            status = IncidentStatus.ACKNOWLEDGED,
            service = "Compute Cluster",
            assignedTo = "Mike T.",
            duration = "00:32:05",
            region = "us-east-1"
        ),

        Incident(
            id = "DB-METRIC-44",
            title = "PostgreSQL Replica Lag in Staging",
            description = "Read replica in staging environment is lagging behind primary by 45 seconds. This is causing stale data reads in the staging test suite. Impact is limited to non-production systems.",
            severity = IncidentSeverity.MEDIUM,
            status = IncidentStatus.ACKNOWLEDGED,
            service = "PostgreSQL",
            assignedTo = "Dev Team",
            duration = "01:10:00",
            region = "us-west-2"
        ),

        Incident(
            id = "LINT-JOB-12",
            title = "Dependency Vulnerability Scan Failure",
            description = "Automated dependency scan found 2 HIGH severity CVEs in third-party packages. Packages affected: log4j-core:2.14.0 and jackson-databind:2.12.0. Patches are available.",
            severity = IncidentSeverity.LOW,
            status = IncidentStatus.RESOLVED,
            service = "CI Pipeline",
            assignedTo = "Security Bot",
            duration = "00:05:00",
            region = "global"
        ),

        Incident(
            id = "API-RT-88",
            title = "Slow Response Times on /v2/orders",
            description = "The /v2/orders endpoint is showing P99 latency of 4200ms, well above the 500ms SLA. Database query profiling shows a missing index on the orders.customer_id column. Fix is being prepared.",
            severity = IncidentSeverity.MEDIUM,
            status = IncidentStatus.OPEN,
            service = "Orders API",
            assignedTo = "Backend Team",
            duration = "00:45:30",
            region = "us-east-1"
        ),

        Incident(
            id = "NET-007",
            title = "VPN Tunnel Instability – EU Region",
            description = "Site-to-site VPN connections between eu-west-1 and on-prem datacenter showing intermittent drops every 8-12 minutes. Network team is investigating BGP route flapping.",
            severity = IncidentSeverity.HIGH,
            status = IncidentStatus.OPEN,
            service = "Network",
            assignedTo = "NetOps Team",
            duration = "00:18:45",
            region = "eu-west-1"
        )
    )

    // ─────────────────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────────────────

    val notifications = listOf(

        AppNotification(
            id = "n-001",
            title = "Critical: Auth Gateway Down",
            message = "INC-9021 – Identity provider returning 504 errors. Immediate action required.",
            type = NotificationType.CRITICAL,
            timeAgo = "2m ago",
            isRead = false
        ),

        AppNotification(
            id = "n-002",
            title = "Pipeline Failed: Legacy-Data-Migration",
            message = "TIMEOUT_EXCEPTION on data migration script in PROD-ENV. Check logs.",
            type = NotificationType.CRITICAL,
            timeAgo = "15m ago",
            isRead = false
        ),

        AppNotification(
            id = "n-003",
            title = "High CPU Alert",
            message = "Node ip-10-0-24-112 CPU above 90% for 30+ minutes. Auto-scaling triggered.",
            type = NotificationType.WARNING,
            timeAgo = "32m ago",
            isRead = false
        ),

        AppNotification(
            id = "n-004",
            title = "Pipeline Deployed: Core-API-Deploy",
            message = "deploy-prod-v2.4.1 completed successfully in PROD-ENV.",
            type = NotificationType.SUCCESS,
            timeAgo = "2m ago",
            isRead = true
        ),

        AppNotification(
            id = "n-005",
            title = "Scheduled Maintenance Tonight",
            message = "Database maintenance window: 02:00–04:00 UTC. Expect brief downtime.",
            type = NotificationType.INFO,
            timeAgo = "1h ago",
            isRead = true
        ),

        AppNotification(
            id = "n-006",
            title = "CVE Alert: jackson-databind",
            message = "HIGH severity vulnerability found. Update to 2.15.0 recommended.",
            type = NotificationType.WARNING,
            timeAgo = "2h ago",
            isRead = true
        ),

        AppNotification(
            id = "n-007",
            title = "New Engineer Joined",
            message = "Alex K. has been added to the Core Infrastructure team.",
            type = NotificationType.INFO,
            timeAgo = "3h ago",
            isRead = true
        ),

        AppNotification(
            id = "n-008",
            title = "Incident Resolved: LINT-JOB-12",
            message = "Dependency vulnerability scan failure has been resolved.",
            type = NotificationType.SUCCESS,
            timeAgo = "4h ago",
            isRead = true
        )
    )

    // ─────────────────────────────────────────────────────────
    // DASHBOARD STATS (shown in dashboard header card)
    // ─────────────────────────────────────────────────────────

    val dashboardStats = mapOf(
        "uptime" to "99.98%",
        "activeNodes" to "1,204",
        "latency" to "12ms",
        "successCount" to "482",
        "runningCount" to "12",
        "failedCount" to "3"
    )
}

