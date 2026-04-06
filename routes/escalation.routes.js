// ─────────────────────────────────────────
//  routes/escalation.routes.js
//
//  GET  /api/escalations                         – audit log (filterable by incidentId)
//  GET  /api/escalations/:id                     – single escalation record
//  POST /api/escalations/:incidentId/escalate    – manually escalate an incident
// ─────────────────────────────────────────

const express = require('express');
const router  = express.Router();

const {
  getEscalations,
  getEscalation,
  triggerManualEscalation,
} = require('../controllers/escalation.controller');

const { protect } = require('../middleware/auth');

router.use(protect);

router.get ('/',                          getEscalations);
router.get ('/:id',                       getEscalation);
router.post('/:incidentId/escalate',      triggerManualEscalation);

module.exports = router;
