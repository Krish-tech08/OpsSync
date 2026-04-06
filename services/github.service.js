// ─────────────────────────────────────────
//  services/github.service.js
//
//  All GitHub Actions API calls live here.
//  Controllers call these functions; they
//  never talk to the GitHub API directly.
//
//  Functions:
//   getWorkflows()          → list all workflows
//   getWorkflowRuns(id)     → runs for one workflow
//   getAllRuns(filters)     → recent runs across all workflows
//   reRunWorkflow(runId)    → re-trigger a failed run
//   cancelWorkflow(runId)   → cancel an in-progress run
//   getRunDetails(runId)    → full details for a single run
// ─────────────────────────────────────────

const githubClient = require('../config/github');

/**
 * List all workflows defined in the repository.
 */
const getWorkflows = async () => {
  const { data } = await githubClient.get('/workflows');
  return data.workflows; // Array of workflow objects
};

/**
 * Get all runs for a specific workflow.
 * @param {string|number} workflowId - The workflow's numeric ID or filename
 * @param {object} params - Optional query params: per_page, page, status
 */
const getWorkflowRuns = async (workflowId, params = {}) => {
  const { data } = await githubClient.get(`/workflows/${workflowId}/runs`, {
    params: { per_page: 10, ...params },
  });
  return data;
};

/**
 * Get recent runs across all workflows (useful for dashboard view).
 * @param {object} filters - Optional: { status, branch, per_page }
 */
const getAllRuns = async (filters = {}) => {
  const { data } = await githubClient.get('/runs', {
    params: { per_page: 20, ...filters },
  });
  return data;
};

/**
 * Re-run all jobs in a failed or cancelled workflow run.
 * @param {string|number} runId - The workflow run ID
 */
const reRunWorkflow = async (runId) => {
  // GitHub returns 201 with an empty body on success
  await githubClient.post(`/runs/${runId}/rerun`);
  return { message: `Workflow run ${runId} re-triggered successfully` };
};

/**
 * Cancel an in-progress workflow run.
 * @param {string|number} runId - The workflow run ID
 */
const cancelWorkflow = async (runId) => {
  await githubClient.post(`/runs/${runId}/cancel`);
  return { message: `Workflow run ${runId} cancelled successfully` };
};

/**
 * Get full details for a single workflow run.
 * @param {string|number} runId
 */
const getRunDetails = async (runId) => {
  const { data } = await githubClient.get(`/runs/${runId}`);
  return data;
};

module.exports = {
  getWorkflows,
  getWorkflowRuns,
  getAllRuns,
  reRunWorkflow,
  cancelWorkflow,
  getRunDetails,
};
