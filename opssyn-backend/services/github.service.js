// services/github.service.js
// Owner: Backend Developer (Priya)
// Purpose: All GitHub API calls live here. Controllers never call GitHub directly —
//          they always go through this service. If GitHub's API changes, only
//          this file needs to be updated.

const githubClient = require('../config/github');

// ── FUNCTION: getWorkflowRuns ────────────────────────────────
// What it does : Fetches a list of recent CI/CD pipeline runs for a given
//                GitHub repository.
// Parameters   : owner  — GitHub username or org (e.g. "octocat")
//                repo   — repository name        (e.g. "hello-world")
// Returns      : Array of workflow run objects from GitHub (id, name, status,
//                conclusion, created_at, html_url, etc.)
// GitHub docs  : GET /repos/{owner}/{repo}/actions/runs

const getWorkflowRuns = async (owner, repo) => {
  const response = await githubClient.get(
    `/repos/${owner}/${repo}/actions/runs`,
    { params: { per_page: 20 } } // Latest 20 runs
  );
  // Return only the runs array from the response
  return response.data.workflow_runs;
};

// ── FUNCTION: getSingleRun ───────────────────────────────────
// What it does : Fetches details of one specific workflow run by its ID.
// Parameters   : owner  — GitHub username or org
//                repo   — repository name
//                runId  — the numeric ID of the workflow run
// Returns      : Single workflow run object with full details
// GitHub docs  : GET /repos/{owner}/{repo}/actions/runs/{run_id}

const getSingleRun = async (owner, repo, runId) => {
  const response = await githubClient.get(
    `/repos/${owner}/${repo}/actions/runs/${runId}`
  );
  return response.data;
};

// ── FUNCTION: reRunWorkflow ──────────────────────────────────
// What it does : Triggers a re-run of a previously completed workflow run.
//                Useful for retrying failed pipelines without pushing new code.
// Parameters   : owner  — GitHub username or org
//                repo   — repository name
//                runId  — the numeric ID of the workflow run to re-run
// Returns      : 201 from GitHub means re-run was successfully queued.
//                Returns true to signal success to the controller.
// GitHub docs  : POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun

const reRunWorkflow = async (owner, repo, runId) => {
  await githubClient.post(
    `/repos/${owner}/${repo}/actions/runs/${runId}/rerun`
  );
  // GitHub returns 201 with empty body on success
  return { queued: true, runId };
};

// ── FUNCTION: cancelWorkflow ─────────────────────────────────
// What it does : Cancels a workflow run that is currently in progress.
//                Only works on runs with status "in_progress" or "queued".
// Parameters   : owner  — GitHub username or org
//                repo   — repository name
//                runId  — the numeric ID of the in-progress run to cancel
// Returns      : 202 from GitHub means cancellation request was accepted.
// GitHub docs  : POST /repos/{owner}/{repo}/actions/runs/{run_id}/cancel

const cancelWorkflow = async (owner, repo, runId) => {
  await githubClient.post(
    `/repos/${owner}/${repo}/actions/runs/${runId}/cancel`
  );
  // GitHub returns 202 with empty body on success
  return { cancelled: true, runId };
};

// ── FUNCTION: getWorkflowJobs ────────────────────────────────
// What it does : Fetches the individual jobs inside a workflow run.
//                Each job has its own status, steps, and logs URL.
// Parameters   : owner  — GitHub username or org
//                repo   — repository name
//                runId  — the numeric ID of the workflow run
// Returns      : Array of job objects (id, name, status, conclusion, steps)
// GitHub docs  : GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs

const getWorkflowJobs = async (owner, repo, runId) => {
  const response = await githubClient.get(
    `/repos/${owner}/${repo}/actions/runs/${runId}/jobs`
  );
  return response.data.jobs;
};

module.exports = {
  getWorkflowRuns,
  getSingleRun,
  reRunWorkflow,
  cancelWorkflow,
  getWorkflowJobs,
};