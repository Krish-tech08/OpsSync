const express = require('express');
const router  = express.Router();
const {
  register,
  login,
  getMe,
  githubOAuth,
  githubCallback,
} = require('../controllers/auth.controller');
const { protect } = require('../middleware/auth');

// Local auth
router.post('/register', register);
router.post('/login',    login);
router.get('/me',        protect, getMe);

// GitHub OAuth
// Step 1: Android hits this to get the GitHub redirect URL
router.get('/github',          githubOAuth);
// Step 2: GitHub redirects here after user approves
router.get('/github/callback', githubCallback);

module.exports = router;
