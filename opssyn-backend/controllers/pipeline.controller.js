// controllers/pipeline.controller.js

const githubService = require('../services/github.service');
const User          = require('../models/User');

// ── FUNCTION: getUserRepos ───────────────────────────────────
// Route     : GET /api/pipelines/repos
// What it does: Uses the logged-in user's stored GitHub token to fetch
//               all their repos (own + member of). Returns a clean list
//               so the Android app can show a repo picker.
// Protected : Yes — req.user is set by protect middleware
const getUserRepos = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;

    if (!token) {
      return res.status(403).json({
        success: false,
        message: 'No GitHub token available.',
      });
    }

    const repos = await githubService.getUserRepos(token);

    // github.service already returns owner as a string (e.g. "krishna")
    // Shape it as an object so Android RepoOwnerDto parses correctly
    const shaped = repos.map((repo) => ({
      id:          repo.id,
      name:        repo.name,
      full_name:   repo.fullName,   // service returns camelCase fullName
      private:     repo.private,
      description: repo.description,
      language:    repo.language,
      owner: {
        login:      repo.owner,     // repo.owner is already the login string
        avatar_url: '',
      },
    }));

    res.status(200).json({ success: true, count: shaped.length, data: shaped });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getPipelines ───────────────────────────────────
// Route     : GET /api/pipelines/:owner/:repo
// Uses the logged-in user's GitHub token (not a global env token)
const getPipelines = async (req, res, next) => {
  try {
    const { owner, repo } = req.params;
    const user = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;
    if (!token) return res.status(403).json({ success: false, message: 'No GitHub token.' });

    const runs = await githubService.getWorkflowRuns(owner, repo, token);

    const shaped = runs.map((run) => ({
      id:          run.id,
      name:        run.name        ?? 'Unnamed Run',
      status:      run.status,
      conclusion:  run.conclusion  ?? null,
      run_number:  run.run_number,
      created_at:  run.created_at  ?? null,
      updated_at:  run.updated_at  ?? null,
      html_url:    run.html_url    ?? null,
      actor: run.triggering_actor ? {
        login:      run.triggering_actor.login      ?? 'unknown',
        avatar_url: run.triggering_actor.avatar_url ?? '',
      } : null,
      head_commit: run.head_commit ? {
        id:      run.head_commit.id      ?? '',
        message: run.head_commit.message ?? '',
      } : null,
    }));

    res.status(200).json({ success: true, count: shaped.length, data: shaped });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getPipelineDetail ──────────────────────────────
// Route     : GET /api/pipelines/:owner/:repo/:runId
const getPipelineDetail = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;
    if (!token) return res.status(403).json({ success: false, message: 'No GitHub token.' });

    const [run, jobs] = await Promise.all([
      githubService.getSingleRun(owner, repo, runId, token),
      githubService.getWorkflowJobs(owner, repo, runId, token),
    ]);

    const shapedJobs = jobs.map((job) => ({
      id:           job.id,
      name:         job.name        ?? 'Unnamed Job',
      status:       job.status,
      conclusion:   job.conclusion  ?? null,
      started_at:   job.started_at  ?? null,
      completed_at: job.completed_at ?? null,
      steps: (job.steps ?? []).map((step) => ({
        number:       step.number,
        name:         step.name       ?? 'Unnamed Step',
        status:       step.status,
        conclusion:   step.conclusion ?? null,
        started_at:   step.started_at  ?? null,
        completed_at: step.completed_at ?? null,
      })),
    }));

    // ── Return FLAT shape matching PipelineRunDto ──────────
    res.status(200).json({
      success: true,
      data: {
        id:          run.id,
        name:        run.name        ?? 'Unnamed Run',
        status:      run.status,
        conclusion:  run.conclusion  ?? null,
        run_number:  run.run_number,
        created_at:  run.created_at  ?? null,
        updated_at:  run.updated_at  ?? null,
        html_url:    run.html_url    ?? null,
        actor: run.triggering_actor ? {
          login:      run.triggering_actor.login      ?? 'unknown',
          avatar_url: run.triggering_actor.avatar_url ?? '',
        } : null,
        head_commit: run.head_commit ? {
          id:      run.head_commit.id      ?? '',
          message: run.head_commit.message ?? '',
        } : null,
        jobs: shapedJobs,
      },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: reRunPipeline ──────────────────────────────────
// Route     : POST /api/pipelines/:owner/:repo/:runId/rerun
const reRunPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user = await User.findById(req.user.id).select('+githubAccessToken');
    const result = await githubService.reRunWorkflow(owner, repo, runId, user.githubAccessToken);
    res.status(200).json({ success: true, message: `Run ${runId} queued for re-run.`, data: result });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: cancelPipeline ─────────────────────────────────
// Route     : POST /api/pipelines/:owner/:repo/:runId/cancel
const cancelPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user = await User.findById(req.user.id).select('+githubAccessToken');
    const result = await githubService.cancelWorkflow(owner, repo, runId, user.githubAccessToken);
    res.status(200).json({ success: true, message: `Run ${runId} cancellation requested.`, data: result });
  } catch (err) {
    next(err);
  }
};

module.exports = { getUserRepos, getPipelines, getPipelineDetail, reRunPipeline, cancelPipeline };
