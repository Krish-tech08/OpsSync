// services/github.service.js
// All functions now accept a userToken parameter so each user's
// requests use their own GitHub access token instead of a global env token.

const axios = require('axios');

// ── HELPER: build a per-user GitHub client ───────────────────
const makeClient = (userToken) =>
  axios.create({
    baseURL: 'https://api.github.com',
    headers: {
      Authorization: `Bearer ${userToken}`,
      Accept:        'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
    },
  });

// ── FUNCTION: getUserRepos ───────────────────────────────────
// Fetches all repos the authenticated user owns or is a member of.
// Returns a clean array: [{ id, name, fullName, owner, private, description, language }]
const getUserRepos = async (userToken) => {
  const client = makeClient(userToken);

  // affiliation=owner,collaborator,organization_member gives broadest list
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
    fullName:    repo.full_name,       // "owner/repo"
    owner:       repo.owner.login,
    private:     repo.private,
    description: repo.description,
    language:    repo.language,
    updatedAt:   repo.updated_at,
    hasActions:  repo.has_downloads,  // rough proxy — actions endpoint will confirm
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

// ── FUNCTION: getWorkflowJobs ────────────────────────────────
const getWorkflowJobs = async (owner, repo, runId, userToken) => {
  const client   = makeClient(userToken);
  const response = await client.get(`/repos/${owner}/${repo}/actions/runs/${runId}/jobs`);
  return response.data.jobs;
};

module.exports = {
  getUserRepos,
  getWorkflowRuns,
  getSingleRun,
  reRunWorkflow,
  cancelWorkflow,
  getWorkflowJobs,
};
