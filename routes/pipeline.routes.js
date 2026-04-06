// ─────────────────────────────────────────
//  routes/pipeline.routes.js
//
//  All routes require a valid JWT (protect).
//
//  GET  /api/pipelines/workflows
//  GET  /api/pipelines/runs
//  GET  /api/pipelines/workflows/:workflowId/runs
//  GET  /api/pipelines/runs/:runId
//  POST /api/pipelines/runs/:runId/rerun
//  POST /api/pipelines/runs/:runId/cancel
//  POST /api/pipelines/runs/:runId/create-incident
// ─────────────────────────────────────────

const express = require('express');
const router  = express.Router();

const {
  listWorkflows,
  listRuns,
  listWorkflowRuns,
  getRunDetails,
  reRunWorkflow,
  cancelWorkflow,
  createIncidentFromRun,
} = require('../controllers/pipeline.controller');

const { protect } = require('../middleware/auth');

// All pipeline routes are protected
router.use(protect);

router.get ('/',                                 listRuns);
router.get ('/workflows',                        listWorkflows);
router.get ('/workflows/:workflowId/runs',       listWorkflowRuns);
router.get ('/runs',                             listRuns);
router.get ('/runs/:runId',                      getRunDetails);
router.post('/runs/:runId/rerun',                reRunWorkflow);
router.post('/runs/:runId/cancel',               cancelWorkflow);
router.post('/runs/:runId/create-incident',      createIncidentFromRun);

module.exports = router;
