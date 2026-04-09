// routes/pipeline.routes.js

const express = require('express');
const router  = express.Router();
const {
  getUserRepos,
  getPipelines,
  getPipelineDetail,
  reRunPipeline,
  cancelPipeline,
} = require('../controllers/pipeline.controller');
const { protect } = require('../middleware/auth');

// GET /api/pipelines/repos
// Returns all GitHub repos belonging to the logged-in user.
// Android uses this to show the repo picker screen after login.
router.get('/repos', protect, getUserRepos);

// GET  /api/pipelines/:owner/:repo          → List latest runs
router.get('/:owner/:repo', protect, getPipelines);

// GET  /api/pipelines/:owner/:repo/:runId   → Single run + jobs
router.get('/:owner/:repo/:runId', protect, getPipelineDetail);

// POST /api/pipelines/:owner/:repo/:runId/rerun
router.post('/:owner/:repo/:runId/rerun', protect, reRunPipeline);

// POST /api/pipelines/:owner/:repo/:runId/cancel
router.post('/:owner/:repo/:runId/cancel', protect, cancelPipeline);

module.exports = router;
