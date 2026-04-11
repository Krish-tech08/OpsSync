const express = require('express');
const router  = express.Router();
const { handleGitHubWebhook } = require('../controllers/webhook.controller');

// GitHub sends POST here on every workflow event
// No auth needed — GitHub signs the payload with WEBHOOK_SECRET
router.post('/github', handleGitHubWebhook);

module.exports = router;
