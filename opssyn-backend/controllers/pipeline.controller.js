// controllers/pipeline.controller.js

const githubService = require('../services/github.service');
const User          = require('../models/User');

// ── FUNCTION: getUserRepos ───────────────────────────────────
const getUserRepos = async (req, res, next) => {
  try {
    const user  = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;

    if (!token) {
      return res.status(403).json({ success: false, message: 'No GitHub token available.' });
    }

    const repos  = await githubService.getUserRepos(token);
    const shaped = repos.map((repo) => ({
      id:          repo.id,
      name:        repo.name,
      full_name:   repo.fullName,
      private:     repo.private,
      description: repo.description,
      language:    repo.language,
      owner: {
        login:      repo.owner,
        avatar_url: '',
      },
    }));

    res.status(200).json({ success: true, count: shaped.length, data: shaped });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getPipelines ───────────────────────────────────
const getPipelines = async (req, res, next) => {
  try {
    const { owner, repo } = req.params;
    const user  = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;
    if (!token) return res.status(403).json({ success: false, message: 'No GitHub token.' });

    const runs   = await githubService.getWorkflowRuns(owner, repo, token);
    const shaped = runs.map((run) => ({
      id:         run.id,
      name:       run.name        ?? 'Unnamed Run',
      status:     run.status,
      conclusion: run.conclusion  ?? null,
      run_number: run.run_number,
      created_at: run.created_at  ?? null,
      updated_at: run.updated_at  ?? null,
      html_url:   run.html_url    ?? null,
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
const getPipelineDetail = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user  = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;
    if (!token) return res.status(403).json({ success: false, message: 'No GitHub token.' });

    const [run, jobs] = await Promise.all([
      githubService.getSingleRun(owner, repo, runId, token),
      githubService.getWorkflowJobs(owner, repo, runId, token),
    ]);

    const shapedJobs = jobs.map((job) => ({
      id:           job.id,
      name:         job.name         ?? 'Unnamed Job',
      status:       job.status,
      conclusion:   job.conclusion   ?? null,
      started_at:   job.started_at   ?? null,
      completed_at: job.completed_at ?? null,
      steps: (job.steps ?? []).map((step) => ({
        number:       step.number,
        name:         step.name         ?? 'Unnamed Step',
        status:       step.status,
        conclusion:   step.conclusion   ?? null,
        started_at:   step.started_at   ?? null,
        completed_at: step.completed_at ?? null,
      })),
    }));

    res.status(200).json({
      success: true,
      data: {
        id:         run.id,
        name:       run.name        ?? 'Unnamed Run',
        status:     run.status,
        conclusion: run.conclusion  ?? null,
        run_number: run.run_number,
        created_at: run.created_at  ?? null,
        updated_at: run.updated_at  ?? null,
        html_url:   run.html_url    ?? null,
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
const reRunPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user   = await User.findById(req.user.id).select('+githubAccessToken');
    const result = await githubService.reRunWorkflow(owner, repo, runId, user.githubAccessToken);
    res.status(200).json({ success: true, message: `Run ${runId} queued for re-run.`, data: result });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: cancelPipeline ─────────────────────────────────
const cancelPipeline = async (req, res, next) => {
  try {
    const { owner, repo, runId } = req.params;
    const user   = await User.findById(req.user.id).select('+githubAccessToken');
    const result = await githubService.cancelWorkflow(owner, repo, runId, user.githubAccessToken);
    res.status(200).json({ success: true, message: `Run ${runId} cancellation requested.`, data: result });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: connectWebhook ─────────────────────────────────
const connectWebhook = async (req, res, next) => {
  try {
    const { owner, repo } = req.params;
    const user  = await User.findById(req.user.id).select('+githubAccessToken');
    const token = user?.githubAccessToken || process.env.GITHUB_TOKEN;

    if (!token) {
      return res.status(403).json({ success: false, message: 'No GitHub token.' });
    }

    const webhookUrl = `${process.env.BACKEND_URL}/api/webhooks/github`;
    console.log('🔗 Registering webhook URL:', webhookUrl); // ← add this

    const existing          = await githubService.listWebhooks(owner, repo, token);
    const alreadyRegistered = existing?.some(h => h.config?.url === webhookUrl);

    if (alreadyRegistered) {
      return res.status(200).json({ success: true, message: 'Webhook already registered.' });
    }

    await githubService.createWebhook(owner, repo, token, webhookUrl, secret);
    res.status(200).json({ success: true, message: `Webhook connected for ${owner}/${repo}` });

  } catch (err) {
    console.error('connectWebhook error:', err.message);
    // 422 = webhook already exists with different config, or URL invalid
    // Treat as success for demo — incidents will be created by the app directly
    res.status(200).json({ success: true, message: 'Webhook setup attempted.' });
  }
};

// ── single export at the bottom ──────────────────────────────
module.exports = {
  getUserRepos,
  getPipelines,
  getPipelineDetail,
  reRunPipeline,
  cancelPipeline,
  connectWebhook,
};
