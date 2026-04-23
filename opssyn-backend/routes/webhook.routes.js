const express = require('express');
const router  = express.Router();
const { handleGitHubWebhook } = require('../controllers/webhook.controller');


router.post('/github', handleGitHubWebhook);

module.exports = router;
