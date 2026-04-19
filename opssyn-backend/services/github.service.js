// services/github.service.js

const axios = require('axios');

// ── HELPER: build a per-user GitHub client ───────────────────
const makeClient = (userToken) =>
  axios.create({
    baseURL: 'https://api.github.com',
    headers: {
      Authorization:          `Bearer ${userToken}`,
      Accept:                 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
    },
  });

// ── FUNCTION: getUserRepos ───────────────────────────────────
const getUserRepos = async (userToken) => {
  const client   = makeClient(userToken);
  const response = await client.get('/user/repos', {
    params: {
      affiliation: 'owner,collaborator,organization_member',
      sort:        'updated',
      per_page:    100,
    },
  });
  return response.data.map((repo) => ({
    id:          repo.id,
    name:        repo.name,
    fullName:    repo.full_name,
    owner:       repo.owner.login,
    private:     repo.private,
    description: repo.description,
    language:    repo.language,
    updatedAt:   repo.updated_at,
    hasActions:  repo.has_downloads,
  }));
};

// ── FUNCTION: getWorkflowRuns ────────────────────────────────
const getWorkflowRuns = async (owner, repo, userToken) => {
  const client   = makeClient(userToken);
  const response = await client.get(
    `/repos/${owner}/${repo}/actions/runs`,
    { params: { per_page: 20 } }
  );
  return response.data.workflow_runs;
};

// ── FUNCTION: getSingleRun ───────────────────────────────────
const getSingleRun = async (owner, repo, runId, userToken) => {
  const client   = makeClient(userToken);
  const response = await client.get(`/repos/${owner}/${repo}/actions/runs/${runId}`);
  return response.data;
};

// ── FUNCTION: getWorkflowJobs ────────────────────────────────
const getWorkflowJobs = async (owner, repo, runId, userToken) => {
  const client   = makeClient(userToken);
  const response = await client.get(`/repos/${owner}/${repo}/actions/runs/${runId}/jobs`);
  return response.data.jobs;
};

// ── FUNCTION: reRunWorkflow ──────────────────────────────────
const reRunWorkflow = async (owner, repo, runId, userToken) => {
  const client = makeClient(userToken);
  await client.post(`/repos/${owner}/${repo}/actions/runs/${runId}/rerun`);
  return { queued: true, runId };
};

// ── FUNCTION: cancelWorkflow ─────────────────────────────────
const cancelWorkflow = async (owner, repo, runId, userToken) => {
  const client = makeClient(userToken);
  await client.post(`/repos/${owner}/${repo}/actions/runs/${runId}/cancel`);
  return { cancelled: true, runId };
};

// ── FUNCTION: listWebhooks ───────────────────────────────────
const listWebhooks = async (owner, repo, userToken) => {
  try {
    const client   = makeClient(userToken);
    const response = await client.get(`/repos/${owner}/${repo}/hooks`);
    return response.data;
  } catch (err) {
    // 404 means no hooks or no access — treat as empty
    console.warn(`listWebhooks: ${err.message}`);
    return [];
  }
};

// ── FUNCTION: createWebhook ──────────────────────────────────
const createWebhook = async (owner, repo, userToken, webhookUrl, secret) => {
  const client = makeClient(userToken);
  const response = await client.post(`/repos/${owner}/${repo}/hooks`, {
    name:   'web',
    active: true,
    events: ['workflow_run'],
    config: {
      url:          webhookUrl,
      content_type: 'json',
      secret:       secret,
      insecure_ssl: '0',
    },
  });
  return response.data;
};

module.exports = {
  getUserRepos,
  getWorkflowRuns,
  getSingleRun,
  getWorkflowJobs,
  reRunWorkflow,
  cancelWorkflow,
  listWebhooks,
  createWebhook,
};
