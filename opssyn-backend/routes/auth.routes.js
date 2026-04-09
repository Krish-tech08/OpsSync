// routes/auth.routes.js
// Owner: Backend Developer (Priya)
// Purpose: Auth API endpoints — local login/register + GitHub OAuth.

const express = require('express');
const router  = express.Router();

const { registerUser, loginUser, githubCallback, getMe } = require('../controllers/auth.controller');
const { protect } = require('../middleware/auth');

// POST /api/auth/register  → Local email/password sign-up
router.post('/register', registerUser);

// POST /api/auth/login     → Local email/password login
router.post('/login', loginUser);

// POST /api/auth/github    → GitHub OAuth login
// Android app sends the OAuth `code` it got from GitHub here.
// Backend exchanges it for a GitHub token, fetches the profile,
// finds/creates the user in MongoDB, and returns our own JWT.
router.post('/github', githubCallback);

// GET  /api/auth/me        → Get current user profile (JWT required)
router.get('/me', protect, getMe);

module.exports = router;
