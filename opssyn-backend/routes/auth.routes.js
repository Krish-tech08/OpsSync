// routes/auth.routes.js
// Owner: Backend Developer (Priya)
// Purpose: Declares the Auth API endpoints and maps them to controller functions.
//          This file only wires URLs to functions — no logic lives here.

const express = require('express');
const router  = express.Router();

const { registerUser, loginUser, getMe } = require('../controllers/auth.controller');
const { protect } = require('../middleware/auth');

// POST /api/auth/register → Create new account
// No auth required — this is the sign-up endpoint
router.post('/register', registerUser);

// POST /api/auth/login → Verify credentials and get token
// No auth required — user doesn't have a token yet
router.post('/login', loginUser);

// GET /api/auth/me → Get current logged-in user's profile
// protect middleware runs first: verifies token → attaches req.user → getMe runs
router.get('/me', protect, getMe);

module.exports = router;