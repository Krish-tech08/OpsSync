// ─────────────────────────────────────────
//  services/escalation.service.js
//
//  The escalation engine.  Runs as a background
//  cron job (every minute) and checks for
//  incidents that have been open or in_progress
//  past the configured threshold without being
//  resolved.  When found, it reassigns them to
//  an admin and logs an Escalation record.
//
//  Also exposes manualEscalate() for the API.
// ─────────────────────────────────────────

const cron       = require('node-cron');
const Incident   = require('../models/Incident');
const Escalation = require('../models/Escalation');
const User       = require('../models/User');
const { sendNotification } = require('./notification.service');

/**
 * Find the first admin user to escalate to.
 * In a production system you might use a rotation list.
 */
const findEscalationTarget = async () => {
  return User.findOne({ role: 'admin' });
};

/**
 * Core escalation logic – called by both the cron job and manualEscalate().
 *
 * @param {object} incident   - Mongoose Incident document
 * @param {string} triggeredBy - 'auto' | 'manual'
 */
const escalateIncident = async (incident, triggeredBy = 'auto') => {
  const target = await findEscalationTarget();

  const previousAssignee = incident.assignedTo;

  // Reassign incident to the escalation target
  incident.assignedTo       = target ? target._id : null;
  incident.escalated        = true;
  incident.lastEscalatedAt  = new Date();
  await incident.save();

  // Create an audit log entry
  await Escalation.create({
    incident:          incident._id,
    previousAssignee,
    newAssignee:       target ? target._id : null,
    reason:            triggeredBy === 'auto'
      ? `Unresolved past ${process.env.ESCALATION_THRESHOLD_MINUTES || 30} minutes`
      : 'Manual escalation via API',
    triggeredBy,
  });

  // Notify the new assignee
  if (target) {
    await sendNotification({
      userId:  target._id.toString(),
      title:   `🚨 Incident Escalated: ${incident.title}`,
      message: `Severity: ${incident.severity}. This incident was escalated to you.`,
    });
  }

  console.log(`⬆️  Escalated incident "${incident.title}" → ${target?.name || 'unassigned'}`);
};

/**
 * Cron job – runs every minute.
 * Finds all open/in_progress incidents older than the threshold.
 */
const startEscalationJob = () => {
  const thresholdMinutes = parseInt(process.env.ESCALATION_THRESHOLD_MINUTES || '30', 10);

  cron.schedule('* * * * *', async () => {
    try {
      const cutoff = new Date(Date.now() - thresholdMinutes * 60 * 1000);

      // Find incidents that:
      //  – are still open or in_progress
      //  – were created before the cutoff time
      //  – have not been escalated recently (or never escalated)
      const staleIncidents = await Incident.find({
        status:  { $in: ['open', 'in_progress'] },
        createdAt: { $lt: cutoff },
        $or: [
          { lastEscalatedAt: null },
          { lastEscalatedAt: { $lt: cutoff } }, // Prevent re-escalating too quickly
        ],
      });

      for (const incident of staleIncidents) {
        await escalateIncident(incident, 'auto');
      }

      if (staleIncidents.length > 0) {
        console.log(`⏱  Escalation job: processed ${staleIncidents.length} incident(s)`);
      }
    } catch (err) {
      console.error('❌  Escalation job error:', err.message);
    }
  });

  console.log('⏱  Escalation cron job started (runs every minute)');
};

/**
 * Manually escalate a specific incident via the API.
 * @param {string} incidentId
 */
const manualEscalate = async (incidentId) => {
  const incident = await Incident.findById(incidentId);
  if (!incident) throw new Error('Incident not found');
  if (incident.status === 'resolved') throw new Error('Cannot escalate a resolved incident');

  await escalateIncident(incident, 'manual');
  return incident;
};

module.exports = { startEscalationJob, manualEscalate };
