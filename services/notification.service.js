// ─────────────────────────────────────────
//  services/notification.service.js
//
//  Handles outbound notifications.
//  In development (NOTIFICATION_MODE=log) it
//  simply logs to the console.
//  Set NOTIFICATION_MODE=firebase and drop in
//  the Firebase Admin SDK to go live.
//
//  sendNotification({ userId, title, message })
// ─────────────────────────────────────────

/**
 * Send a notification to a user.
 *
 * @param {object} options
 * @param {string} options.userId  - Recipient's user ID
 * @param {string} options.title   - Notification title
 * @param {string} options.message - Notification body
 */
const sendNotification = async ({ userId, title, message }) => {
  const mode = process.env.NOTIFICATION_MODE || 'log';

  if (mode === 'firebase') {
    // ── Firebase push notification (plug in your Firebase Admin SDK here) ──
    //
    // const admin = require('firebase-admin');
    // await admin.messaging().send({
    //   token: await getUserFCMToken(userId),
    //   notification: { title, body: message },
    // });
    //
    console.log(`🔔  [Firebase] → User ${userId}: "${title}"`);
  } else {
    // ── Development mode: log to console ──────────────────────────────────
    console.log('─────────────────────────────');
    console.log(`🔔  Notification`);
    console.log(`   To:      User ${userId}`);
    console.log(`   Title:   ${title}`);
    console.log(`   Message: ${message}`);
    console.log('─────────────────────────────');
  }

  // Return a consistent shape regardless of mode
  return { sent: true, userId, title, message };
};

/**
 * Broadcast a notification to multiple users.
 * @param {string[]} userIds
 * @param {string} title
 * @param {string} message
 */
const broadcastNotification = async (userIds, title, message) => {
  const results = await Promise.allSettled(
    userIds.map((userId) => sendNotification({ userId, title, message }))
  );
  return results;
};

module.exports = { sendNotification, broadcastNotification };
