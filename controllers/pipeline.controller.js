// ─────────────────────────────────────────
//  controllers/pipeline.controller.js
//
//  Delegates all GitHub Actions work to
//  github.service.js and formats the response.
//  Also auto-creates incidents when a pipeline
//  run has a 'failure' conclusion.
// ─────────────────────────────────────────

const githubService      = require('../services/github.service');
const Incident           = require('../models/Incident');
const { sendNotification } = require('../services/notification.service');

/**
 * GET /api/pipelines/workflows
 * List all workflow definitions in the repo.
 */
const listWorkflows = async (req, res, next) => {
  try {
    const workflows = await githubService.getWorkflows();
    res.json({ success: true, count: workflows.length, workflows });
  } catch (err) {
    next(err);
  }
};

/**
 * GET /api/pipelines/runs
 * Get recent runs across all workflows.
 * Query params: status, branch, per_page
 */
const listRuns = async (req, res, next) => {
  try {
    const data = await githubService.getAllRuns(req.query);
    res.json({ success: true, data });
  } catch (err) {
    next(err);
  }
};

/**
 * GET /api/pipelines/workflows/:workflowId/runs
 * Get runs for a specific workflow.
 */
const listWorkflowRuns = async (req, res, next) => {
  try {
    const data = await githubService.getWorkflowRuns(req.params.workflowId, req.query);
    res.json({ success: true, data });
  } catch (err) {
    next(err);
  }
};

/**
 * GET /api/pipelines/runs/:runId
 * Get details for a single run. If it failed,
 * check whether an incident already exists and
 * surface that alongside the run data.
 */
const getRunDetails = async (req, res, next) => {
  try {
    const run = await githubService.getRunDetails(req.params.runId);

    // Attach any linked incident for convenience
    let linkedIncident = null;
    if (run.conclusion === 'failure') {
      linkedIncident = await Incident.findOne({ pipelineRunId: String(run.id) });
    }

    res.json({ success: true, run, linkedIncident });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/pipelines/runs/:runId/rerun
 * Re-trigger a failed or cancelled workflow run.
 */
const reRunWorkflow = async (req, res, next) => {
  try {
    const result = await githubService.reRunWorkflow(req.params.runId);
    res.json({ success: true, ...result });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/pipelines/runs/:runId/cancel
 * Cancel an in-progress run.
 */
const cancelWorkflow = async (req, res, next) => {
  try {
    const result = await githubService.cancelWorkflow(req.params.runId);
    res.json({ success: true, ...result });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/pipelines/runs/:runId/create-incident
 * Manually create an incident from a specific pipeline run.
 * Auto-create (event-driven) happens in the polling flow above.
 */
const createIncidentFromRun = async (req, res, next) => {
  try {
    const run = await githubService.getRunDetails(req.params.runId);

    // Prevent duplicate incidents for the same run
    const existing = await Incident.findOne({ pipelineRunId: String(run.id) });
    if (existing) {
      return res.status(409).json({
        success: false,
        message: 'An incident for this run already exists',
        incident: existing,
      });
    }

    const incident = await Incident.create({
      title:         `Pipeline failure: ${run.name} #${run.run_number}`,
      description:   `Workflow "${run.name}" failed on branch "${run.head_branch}". Run ID: ${run.id}`,
      severity:      'high',
      pipelineRunId: String(run.id),
      createdBy:     req.user._id,
    });

    // Notify the triggering user
    await sendNotification({
      userId:  req.user._id.toString(),
      title:   '🚨 Pipeline Incident Created',
      message: incident.title,
    });

    res.status(201).json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  listWorkflows,
  listRuns,
  listWorkflowRuns,
  getRunDetails,
  reRunWorkflow,
  cancelWorkflow,
  createIncidentFromRun,
};
