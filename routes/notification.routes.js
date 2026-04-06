// ─────────────────────────────────────────
//  routes/notification.routes.js
//
//  POST /api/notifications/send        – send to one user
//  POST /api/notifications/broadcast   – send to all users (admin only)
// ─────────────────────────────────────────

const express = require('express');
const router  = express.Router();

const { sendToUser, broadcast }   = require('../controllers/notification.controller');
const { protect, restrictTo }     = require('../middleware/auth');

router.use(protect);

router.post('/send',      sendToUser);
router.post('/broadcast', restrictTo('admin'), broadcast);

module.exports = router;
