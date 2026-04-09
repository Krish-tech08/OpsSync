// controllers/pipeline.controller.js
// Owner: Backend Developer (Priya)
// Purpose: Receives HTTP requests for pipeline actions, calls the GitHub service,
//          and sends a clean JSON response back to the Android app.
//          No GitHub API logic lives here — it all lives in services/github.service.js

const githubService = require('../services/github.service');

// ── FUNCTION: getPipelines ───────────────────────────────────
// Route        : GET /api/pipelines/:owner/:repo
// What it does : Fetches the latest 20 workflow runs for a given repo and
//                returns a simplified, clean JSON array to the Android app.
// URL params   : owner — GitHub username/org, repo — repository name
// Response     : { success, count, runs: [...] }
// Protected    : Yes — requires valid JWT (protect middleware on route)

const getPipelines = async (req, res, next) => {
  try {
    const { owner, repo } = req.params;

    const runs = await githubService.getWorkflowRuns(owner, repo);

    // Shape the data — send only what the Android app needs
    const shaped = runs.map((run) => ({
      id:          run.id,
      name:        run.name,
      status:      run.status,       // "queued" | "in_progress" | "completed"
      conclusion:  run.conclusion,   // "success" | "failure" | "cancelled" | null
      branch:      run.head_branch,
      commit:      run.head_sha?.slice(0, 7), // Short commit hash
      triggeredBy: run.triggering_actor?.login,
      startedAt:   run.created_at,
      updatedAt:   run.updated_at,
      url:         run.html_url,
    }));

    res.status(200).json({ success: true, count: shaped.length, runs: shaped });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getPipelineDetail ──────────────────────────────
// Route        : GET /api/pipelines/:owner/:repo/:runId
// What it does : Fetches full details of one specific workflow run including
//                its jobs and steps.
// URL params   : owner, repo, runId
// Response     : { success, run: {...}, jobs: [...] }
// Protected    : Yes

const getPipelineDetail = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;

    // Fetch run details and its jobs in parallel
    const [run, jobs] = await Promise.all([
      githubService.getSingleRun(owner, repo, runId),
      githubService.getWorkflowJobs(owner, repo, runId),
    ]);

    // Shape jobs — each job has steps inside
    const shapedJobs = jobs.map((job) => ({
      id:         job.id,
      name:       job.name,
      status:     job.status,
      conclusion: job.conclusion,
      startedAt:  job.started_at,
      steps: job.steps.map((step) => ({
        number:     step.number,
        name:       step.name,
        status:     step.status,
        conclusion: step.conclusion,
      })),
    }));

    res.status(200).json({
      success: true,
      run: {
        id:         run.id,
        name:       run.name,
        status:     run.status,
        conclusion: run.conclusion,
        branch:     run.head_branch,
        commit:     run.head_sha?.slice(0, 7),
        url:        run.html_url,
      },
      jobs: shapedJobs,
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: reRunPipeline ──────────────────────────────────
// Route        : POST /api/pipelines/:owner/:repo/:runId/rerun
// What it does : Tells GitHub to re-run a previously completed workflow run.
//                Useful when a pipeline fails due to a flaky test or network issue.
// URL params   : owner, repo, runId
// Response     : { success, message, runId }
// Protected    : Yes

const reRunPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;

    const result = await githubService.reRunWorkflow(owner, repo, runId);

    res.status(200).json({
      success: true,
      message: `Workflow run ${runId} has been queued for re-run.`,
      ...result,
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: cancelPipeline ─────────────────────────────────
// Route        : POST /api/pipelines/:owner/:repo/:runId/cancel
// What it does : Cancels a workflow run that is currently in progress.
//                Only works if the run status is "in_progress" or "queued".
// URL params   : owner, repo, runId
// Response     : { success, message, runId }
// Protected    : Yes

const cancelPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;

    const result = await githubService.cancelWorkflow(owner, repo, runId);

    res.status(200).json({
      success: true,
      message: `Workflow run ${runId} cancellation has been requested.`,
      ...result,
    });
  } catch (err) {
    next(err);
  }
};

module.exports = { getPipelines, getPipelineDetail, reRunPipeline, cancelPipeline };