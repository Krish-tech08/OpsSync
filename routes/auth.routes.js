// ─────────────────────────────────────────
//  routes/auth.routes.js
//
//  POST /api/auth/register  – create account
//  POST /api/auth/login     – get JWT
//  GET  /api/auth/me        – get current user (protected)
// ─────────────────────────────────────────

const express = require('express');
const router  = express.Router();

// Updated function names to match the new auth.controller.js
const { registerUser, loginUser, getMe } = require('../controllers/auth.controller');
const { protect }                        = require('../middleware/auth');

router.post('/register', registerUser);
router.post('/login',    loginUser);
router.get ('/me',       protect, getMe);

module.exports = router;