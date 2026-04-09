// routes/escalation.routes.js
// Owner: Backend Developer (Priya)
// Purpose: Declares all escalation API endpoints.

const express = require('express');
const router  = express.Router();

const {
  escalateIncident,
  getIncidentEscalations,
  acknowledgeEscalation,
  getAutoEscalations,
} = require('../controllers/escalation.controller');

const { protect } = require('../middleware/auth');

// POST  /api/escalations                          → Escalate an incident
router.post('/',                         protect, escalateIncident);

// GET   /api/escalations/auto-check              → Check for unescalated critical incidents
router.get('/auto-check',                protect, getAutoEscalations);

// GET   /api/escalations/:incidentId             → Get escalation history for an incident
router.get('/:incidentId',               protect, getIncidentEscalations);

// PATCH /api/escalations/:id/acknowledge         → Acknowledge an escalation
router.patch('/:id/acknowledge',         protect, acknowledgeEscalation);

module.exports = router;