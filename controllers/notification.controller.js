// ─────────────────────────────────────────
//  controllers/notification.controller.js
//
//  Allows the mobile app to manually trigger
//  notifications – useful for testing and for
//  admin broadcasts to all engineers.
// ─────────────────────────────────────────

const { sendNotification, broadcastNotification } = require('../services/notification.service');
const User = require('../models/User');

/**
 * POST /api/notifications/send
 * Send a notification to a specific user.
 * Body: { userId, title, message }
 */
const sendToUser = async (req, res, next) => {
  try {
    const { userId, title, message } = req.body;

    if (!userId || !title || !message) {
      return res.status(400).json({
        success: false,
        message: 'userId, title, and message are required',
      });
    }

    const result = await sendNotification({ userId, title, message });
    res.json({ success: true, result });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/notifications/broadcast
 * Broadcast a notification to all engineers (or all users).
 * Body: { title, message, role? }
 * role defaults to 'engineer' – pass 'all' to include admins
 */
const broadcast = async (req, res, next) => {
  try {
    const { title, message, role } = req.body;

    if (!title || !message) {
      return res.status(400).json({
        success: false,
        message: 'title and message are required',
      });
    }

    // Find target users
    const filter = role && role !== 'all' ? { role } : {};
    const users  = await User.find(filter).select('_id');
    const userIds = users.map((u) => u._id.toString());

    if (userIds.length === 0) {
      return res.status(404).json({ success: false, message: 'No users found to notify' });
    }

    const results = await broadcastNotification(userIds, title, message);

    res.json({
      success: true,
      message: `Notification sent to ${userIds.length} user(s)`,
      results,
    });
  } catch (err) {
    next(err);
  }
};

module.exports = { sendToUser, broadcast };
