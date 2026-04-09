// routes/pipeline.routes.js
// Owner: Backend Developer (Priya)
// Purpose: Declares all pipeline-related API endpoints.
//          All routes here are protected — a valid JWT is required.

const express = require('express');
const router  = express.Router();

const {
  getPipelines,
  getPipelineDetail,
  reRunPipeline,
  cancelPipeline,
} = require('../controllers/pipeline.controller');

const { protect } = require('../middleware/auth');

// All pipeline routes require authentication
// protect runs first → verifies JWT → controller runs

// GET  /api/pipelines/:owner/:repo             → List latest 20 runs
router.get('/:owner/:repo', protect, getPipelines);

// GET  /api/pipelines/:owner/:repo/:runId      → Single run + jobs detail
router.get('/:owner/:repo/:runId', protect, getPipelineDetail);

// POST /api/pipelines/:owner/:repo/:runId/rerun  → Re-run a completed pipeline
router.post('/:owner/:repo/:runId/rerun', protect, reRunPipeline);

// POST /api/pipelines/:owner/:repo/:runId/cancel → Cancel an in-progress pipeline
router.post('/:owner/:repo/:runId/cancel', protect, cancelPipeline);

module.exports = router;