// controllers/teams.controller.js
// Handles sending incident and pipeline log notifications to Microsoft Teams

const axios    = require('axios');
const User     = require('../models/User');
const Incident = require('../models/Incident');

// ── HELPER: get color hex for priority ──────────────────────
const priorityColor = (priority) => {
  switch (priority) {
    case 'critical': return 'FF0000';
    case 'high':     return 'FF6600';
    case 'medium':   return 'FFA500';
    case 'low':      return '00AA00';
    default:         return '808080';
  }
};

// ── HELPER: get color hex for status ────────────────────────
const statusColor = (status) => {
  switch (status) {
    case 'open':        return 'FF0000';
    case 'in_progress': return 'FFA500';
    case 'resolved':    return '00AA00';
    case 'closed':      return '808080';
    default:            return '808080';
  }
};

// ── FUNCTION: saveTeamsWebhook ───────────────────────────────
// Route     : POST /api/teams/webhook
// What it does: Saves the user's Microsoft Teams incoming webhook URL
// Body      : { webhookUrl }
const saveTeamsWebhook = async (req, res, next) => {
  try {
    const { webhookUrl } = req.body;

    if (!webhookUrl || !webhookUrl.startsWith('https://')) {
      return res.status(400).json({
        success: false,
        message: 'Invalid webhook URL. Must be a valid HTTPS URL from Microsoft Teams.',
      });
    }

    // Validate it's actually a Teams webhook URL
    if (!webhookUrl.includes('webhook.office.com') &&
        !webhookUrl.includes('outlook.office.com')) {
      return res.status(400).json({
        success: false,
        message: 'URL must be a valid Microsoft Teams webhook URL.',
      });
    }

    await User.findByIdAndUpdate(req.user.id, { teamsWebhookUrl: webhookUrl });

    res.status(200).json({
      success: true,
      message: 'Teams webhook URL saved successfully.',
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getTeamsWebhookStatus ─────────────────────────
// Route     : GET /api/teams/webhook/status
// What it does: Returns whether user has a Teams webhook configured
const getTeamsWebhookStatus = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id).select('+teamsWebhookUrl');

    res.status(200).json({
      success:      true,
      data: {
        isConnected: !!user?.teamsWebhookUrl,
      },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: sendIncidentToTeams ────────────────────────────
// Route     : POST /api/teams/notify/incident/:incidentId
// What it does: Sends formatted incident details to the user's Teams channel
const sendIncidentToTeams = async (req, res, next) => {
  try {
    // 1. Get user's Teams webhook URL
    const user = await User.findById(req.user.id).select('+teamsWebhookUrl');
    if (!user?.teamsWebhookUrl) {
      return res.status(400).json({
        success: false,
        message: 'No Teams webhook configured. Please add your Teams webhook URL in settings.',
      });
    }

    // 2. Fetch incident with populated fields
    const incident = await Incident.findById(req.params.incidentId)
      .populate('assignedTo', 'name email')
      .populate('createdBy',  'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    // 3. Build Teams Adaptive Card payload
    // Uses the legacy "MessageCard" format which works with all Teams webhook versions
    const timestamp = new Date().toISOString();
    const teamsPayload = {
      '@type':      'MessageCard',
      '@context':   'http://schema.org/extensions',
      themeColor:   priorityColor(incident.priority),
      summary:      `OpsSync Incident: ${incident.title}`,
      sections: [
        {
          activityTitle:    `🚨 **${incident.title}**`,
          activitySubtitle: `Reported via OpsSync • ${timestamp}`,
          activityImage:    'https://img.icons8.com/color/48/000000/high-priority.png',
          facts: [
            { name: 'Incident ID', value: incident._id.toString() },
            { name: 'Priority',    value: incident.priority.toUpperCase() },
            { name: 'Status',      value: incident.status.replace('_', ' ').toUpperCase() },
            { name: 'Assigned To', value: incident.assignedTo?.name || 'Unassigned' },
            { name: 'Created By',  value: incident.createdBy?.name  || 'Unknown' },
            { name: 'Created At',  value: new Date(incident.createdAt).toLocaleString() },
            ...(incident.resolvedAt ? [{ name: 'Resolved At', value: new Date(incident.resolvedAt).toLocaleString() }] : []),
          ],
          markdown: true,
        },
        {
          title: '📋 Description',
          text:  incident.description,
        },
      ],
      potentialAction: [
        {
          '@type': 'OpenUri',
          name:    'View in OpsSync',
          targets: [
            {
              os:  'default',
              uri: `https://opssync-npmv.onrender.com/incidents/${incident._id}`,
            },
          ],
        },
      ],
    };

    // 4. POST to Teams webhook
    await axios.post(user.teamsWebhookUrl, teamsPayload, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 10000,
    });

    res.status(200).json({
      success: true,
      message: 'Incident details sent to Microsoft Teams successfully.',
    });
  } catch (err) {
    // Teams webhook errors
    if (err.response) {
      return res.status(502).json({
        success: false,
        message: `Teams webhook rejected the request: ${err.response.status} ${err.response.data}`,
      });
    }
    next(err);
  }
};

// ── FUNCTION: sendLogsToTeams ────────────────────────────────
// Route     : POST /api/teams/notify/logs
// What it does: Sends pipeline logs/error summary to Teams
// Body      : { pipelineId, pipelineName, status, logs, gitHash, triggeredBy, repoName }
const sendLogsToTeams = async (req, res, next) => {
  try {
    // 1. Get user's Teams webhook URL
    const user = await User.findById(req.user.id).select('+teamsWebhookUrl');
    if (!user?.teamsWebhookUrl) {
      return res.status(400).json({
        success: false,
        message: 'No Teams webhook configured. Please add your Teams webhook URL in settings.',
      });
    }

    const {
      pipelineId,
      pipelineName,
      status,
      logs,
      gitHash,
      triggeredBy,
      repoName,
    } = req.body;

    if (!pipelineName || !status || !logs) {
      return res.status(400).json({
        success: false,
        message: 'pipelineName, status, and logs are required.',
      });
    }

    // 2. Extract error lines from logs for summary
    const allLogs     = Array.isArray(logs) ? logs : [logs];
    const errorLines  = allLogs.filter(l => l.startsWith('ERROR') || l.startsWith('WARN'));
    const logSummary  = allLogs.slice(0, 15).join('\n');  // First 15 lines max
    const timestamp   = new Date().toISOString();

    const statusEmoji = status === 'SUCCESS' ? '✅' :
                        status === 'FAILED'  ? '❌' :
                        status === 'RUNNING' ? '🔄' : '⏳';

    // 3. Build Teams payload
    const teamsPayload = {
      '@type':    'MessageCard',
      '@context': 'http://schema.org/extensions',
      themeColor: status === 'SUCCESS' ? '00AA00' :
                  status === 'FAILED'  ? 'FF0000' : 'FFA500',
      summary:    `OpsSync Pipeline ${status}: ${pipelineName}`,
      sections: [
        {
          activityTitle:    `${statusEmoji} **Pipeline ${status}: ${pipelineName}**`,
          activitySubtitle: `${repoName || 'Unknown Repo'} • ${timestamp}`,
          facts: [
            { name: 'Pipeline ID',   value: pipelineId   || '—' },
            { name: 'Status',        value: status },
            { name: 'Git Hash',      value: gitHash      || '—' },
            { name: 'Triggered By',  value: triggeredBy  || '—' },
            { name: 'Repo',          value: repoName     || '—' },
            { name: 'Error Count',   value: `${errorLines.length} error(s) / warning(s)` },
          ],
          markdown: true,
        },
        // Error summary section — only if there are errors
        ...(errorLines.length > 0 ? [{
          title: '⚠️ Errors & Warnings',
          text:  `\`\`\`\n${errorLines.slice(0, 5).join('\n')}\n\`\`\``,
        }] : []),
        {
          title: '📄 Log Output (first 15 lines)',
          text:  `\`\`\`\n${logSummary}\n\`\`\``,
        },
      ],
      potentialAction: [
        {
          '@type': 'OpenUri',
          name:    'View Full Logs on GitHub',
          targets: [
            {
              os:  'default',
              uri: `https://github.com/${repoName}/actions/runs/${pipelineId}`,
            },
          ],
        },
      ],
    };

    // 4. POST to Teams
    await axios.post(user.teamsWebhookUrl, teamsPayload, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 10000,
    });

    res.status(200).json({
      success: true,
      message: 'Pipeline logs sent to Microsoft Teams successfully.',
    });
  } catch (err) {
    if (err.response) {
      return res.status(502).json({
        success: false,
        message: `Teams webhook rejected the request: ${err.response.status}`,
      });
    }
    next(err);
  }
};

module.exports = {
  saveTeamsWebhook,
  getTeamsWebhookStatus,
  sendIncidentToTeams,
  sendLogsToTeams,
};
