// routes/teams.routes.js
const express = require('express');
const router  = express.Router();
const {
  saveTeamsWebhook,
  getTeamsWebhookStatus,
  sendIncidentToTeams,
  sendLogsToTeams,
} = require('../controllers/teams.controller');
const { protect } = require('../middleware/auth');

// Save or update Teams webhook URL for the logged-in user
router.post('/webhook',               protect, saveTeamsWebhook);

// Check if user has a Teams webhook configured
router.get('/webhook/status',         protect, getTeamsWebhookStatus);

// Send incident details to Teams
router.post('/notify/incident/:incidentId', protect, sendIncidentToTeams);

// Send pipeline logs to Teams
router.post('/notify/logs',           protect, sendLogsToTeams);

module.exports = router;
