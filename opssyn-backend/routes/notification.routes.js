// routes/notification.routes.js
const express = require('express');
const router  = express.Router();
const {
  registerFcmToken,
  removeFcmToken,
  notifyIncident,
  notifyPipeline,
} = require('../controllers/notification.controller');
const { protect } = require('../middleware/auth');

// Register device FCM token (call on every app launch)
router.post('/fcm/register',   protect, registerFcmToken);

// Remove FCM token on logout
router.delete('/fcm/register', protect, removeFcmToken);
router.delete('/fcm/remove', protect, removeFcmToken);

// Manually trigger incident push notification
router.post('/incident/:incidentId', protect, notifyIncident);

// Manually trigger pipeline push notification
router.post('/pipeline', protect, notifyPipeline);

module.exports = router;
