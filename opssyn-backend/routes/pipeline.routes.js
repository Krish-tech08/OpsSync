// routes/pipeline.routes.js
const express = require('express');
const router  = express.Router();
const { protect } = require('../middleware/auth');
const {
  getUserRepos,
  getPipelines,
  getPipelineDetail,
  reRunPipeline,
  cancelPipeline,
  connectWebhook,        // ← add this
} = require('../controllers/pipeline.controller');

router.get('/repos',                          protect, getUserRepos);
router.post('/:owner/:repo/webhook/connect',  protect, connectWebhook); // ← add this
router.get('/:owner/:repo',                   protect, getPipelines);
router.get('/:owner/:repo/:runId',            protect, getPipelineDetail);
router.post('/:owner/:repo/:runId/rerun',     protect, reRunPipeline);
router.post('/:owner/:repo/:runId/cancel',    protect, cancelPipeline);

module.exports = router;
