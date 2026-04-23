// controllers/notification.controller.js
// Handles FCM token registration and sending push notifications
// for incidents and pipeline events.

const messaging = require('../config/firebase');
const User      = require('../models/User');
const Incident  = require('../models/Incident');

// ── HELPER: send a single FCM notification ───────────────────
const sendFcmNotification = async (fcmToken, title, body, data = {}) => {
  const message = {
    token: fcmToken,
    notification: { title, body },
    data: {
      // All data values must be strings for FCM
      ...Object.fromEntries(
        Object.entries(data).map(([k, v]) => [k, String(v)])
      ),
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'opssync_alerts',
        sound:     'default',
        priority:  'max',
        // Makes notification heads-up on Android
        visibility: 'public',
      },
    },
  };

  return messaging.send(message);
};

// ── FUNCTION: registerFcmToken ───────────────────────────────
// Route     : POST /api/notifications/fcm/register
// What it does: Saves the device FCM token for the logged-in user.
//               Called on every app launch so token stays fresh.
// Body      : { fcmToken }
const registerFcmToken = async (req, res, next) => {
  try {
    const { fcmToken } = req.body;

    if (!fcmToken || typeof fcmToken !== 'string') {
      return res.status(400).json({
        success: false,
        message: 'fcmToken is required and must be a string.',
      });
    }

    await User.findByIdAndUpdate(req.user.id, { fcmToken });

    res.status(200).json({
      success: true,
      message: 'FCM token registered successfully.',
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: removeFcmToken ─────────────────────────────────
// Route     : DELETE /api/notifications/fcm/register
// What it does: Clears FCM token on logout so user stops receiving
//               notifications after signing out.
const removeFcmToken = async (req, res, next) => {
  try {
    await User.findByIdAndUpdate(req.user.id, { fcmToken: null });
    res.status(200).json({ success: true, message: 'FCM token removed.' });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: notifyIncident ─────────────────────────────────
// Route     : POST /api/notifications/incident/:incidentId
// What it does: Sends a push notification to the assigned user
//               (or creator) when an incident is created or updated.
//               Called internally from incident controller.
// Can also be called externally for manual triggers.
const notifyIncident = async (req, res, next) => {
  try {
    const incident = await Incident.findById(req.params.incidentId)
      .populate('assignedTo', 'name fcmToken')
      .populate('createdBy',  'name fcmToken');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    // Target: assignedTo if set, otherwise createdBy
    const targetUser = incident.assignedTo || incident.createdBy;
    const fcmToken   = targetUser?.fcmToken;

    if (!fcmToken) {
      return res.status(200).json({
        success: true,
        message: 'No FCM token for target user — notification skipped.',
      });
    }

    const priorityEmoji = {
      critical: '🔴',
      high:     '🟠',
      medium:   '🟡',
      low:      '🟢',
    }[incident.priority] || '⚪';

    const priority = (incident.priority || 'unknown').toUpperCase();
    const title = `${priorityEmoji} ${priority} Incident`;
    const body  = incident.title;
    console.log('[NOTIFY] Sending incident notification to:', fcmToken);

    await sendFcmNotification(fcmToken, title, body, {
      type:       'incident',
      incidentId: incident._id.toString(),
      priority:   incident.priority,
      status:     incident.status,
    });

    res.status(200).json({ success: true, message: 'Incident notification sent.' });
  } catch (err) {
    // FCM token invalid / expired
    if (err.code === 'messaging/registration-token-not-registered') {
      await User.findByIdAndUpdate(req.user.id, { fcmToken: null });
      return res.status(200).json({
        success: true,
        message: 'FCM token was invalid — cleared from database.',
      });
    }
    next(err);
  }
};

// ── FUNCTION: notifyPipeline ─────────────────────────────────
// Route     : POST /api/notifications/pipeline
// What it does: Sends a push notification about a pipeline status change.
//               Called from pipeline webhook or manually.
// Body      : { userId, pipelineName, status, repoName, runId }
const notifyPipeline = async (req, res, next) => {
  try {
    const { userId, pipelineName, status, repoName, runId } = req.body;

    if (!userId || !pipelineName || !status) {
      return res.status(400).json({
        success: false,
        message: 'userId, pipelineName, and status are required.',
      });
    }

    const user = await User.findById(userId).select('+fcmToken');
    if (!user?.fcmToken) {
      return res.status(200).json({
        success: true,
        message: 'No FCM token for user — notification skipped.',
      });
    }

    const statusEmoji = {
      SUCCESS:   '✅',
      FAILED:    '❌',
      RUNNING:   '🔄',
      CANCELLED: '⛔',
    }[status.toUpperCase()] || '⏳';

    const title = `${statusEmoji} Pipeline ${status}`;
    const body  = `${pipelineName} · ${repoName || 'unknown repo'}`;

    await sendFcmNotification(user.fcmToken, title, body, {
      type:         'pipeline',
      pipelineId:   runId    || '',
      pipelineName: pipelineName,
      status:       status,
      repoName:     repoName || '',
    });

    res.status(200).json({ success: true, message: 'Pipeline notification sent.' });
  } catch (err) {
    if (err.code === 'messaging/registration-token-not-registered') {
      await User.findByIdAndUpdate(req.body.userId, { fcmToken: null });
      return res.status(200).json({ success: true, message: 'FCM token cleared.' });
    }
    next(err);
  }
};

// ── EXPORTED HELPER: notifyIncidentInternal ──────────────────
// Called directly from incident.controller.js — no HTTP req/res.
// Used to auto-notify on incident create/update without extra HTTP round-trip.
const notifyIncidentInternal = async (incident) => {
  try {
    const populated = await Incident.findById(incident._id || incident.id)
      .populate('assignedTo', 'fcmToken')
      .populate('createdBy',  'fcmToken');

    const targetUser = populated?.assignedTo || populated?.createdBy;
    const fcmToken   = targetUser?.fcmToken;
    if (!fcmToken) return;

    const priorityEmoji = {
      critical: '🔴', high: '🟠', medium: '🟡', low: '🟢',
    }[populated.priority] || '⚪';

    await sendFcmNotification(
      fcmToken,
      `${priorityEmoji} ${populated.priority.toUpperCase()} Incident`,
      populated.title,
      {
        type:       'incident',
        incidentId: populated._id.toString(),
        priority:   populated.priority,
        status:     populated.status,
      }
    );
  } catch (err) {
    // Swallow — notification failure should never block incident operations
    console.error('FCM internal notify error:', err.message);
  }
};

// ── EXPORTED HELPER: notifyPipelineInternal ──────────────────
// Called from webhook.controller.js after a pipeline failure.
const notifyPipelineInternal = async (userId, pipelineName, status, repoName, runId) => {
  try {
    const user = await User.findById(userId).select('+fcmToken');
    if (!user?.fcmToken) return;

    const statusEmoji = { SUCCESS: '✅', FAILED: '❌', RUNNING: '🔄' }[status.toUpperCase()] || '⏳';

    await sendFcmNotification(
      user.fcmToken,
      `${statusEmoji} Pipeline ${status}`,
      `${pipelineName} · ${repoName}`,
      {
        type:         'pipeline',
        pipelineId:   String(runId || ''),
        pipelineName,
        status,
        repoName,
      }
    );
  } catch (err) {
    console.error('FCM pipeline notify error:', err.message);
  }
};

module.exports = {
  registerFcmToken,
  removeFcmToken,
  notifyIncident,
  notifyPipeline,
  notifyIncidentInternal,
  notifyPipelineInternal,
};
