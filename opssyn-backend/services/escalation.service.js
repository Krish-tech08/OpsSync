// services/escalation.service.js
// Owner: Incident Engineer (Riya)
// Purpose: Contains all escalation business logic and rules.
//          Controllers call these functions — no rules live in the controller.

const Escalation = require('../models/Escalation');
const Incident   = require('../models/Incident');

// ── FUNCTION: createEscalation ───────────────────────────────
// What it does : Creates an escalation record for an incident.
//                Also bumps the incident priority to 'critical' automatically
//                if the escalation priority is critical.
// Parameters   : incidentId, escalatedBy (userId), escalatedTo (userId),
//                reason, priority
// Returns      : The saved escalation document

const createEscalation = async ({ incidentId, escalatedBy, escalatedTo, reason, priority }) => {
  // Create the escalation record
  const escalation = await Escalation.create({
    incident:    incidentId,
    escalatedBy,
    escalatedTo,
    reason,
    priority,
  });

  // If escalation is critical, auto-upgrade the incident priority too
  if (priority === 'critical') {
    await Incident.findByIdAndUpdate(incidentId, { priority: 'critical' });
  }

  return escalation;
};

// ── FUNCTION: getEscalationsByIncident ───────────────────────
// What it does : Fetches all escalation history for a specific incident.
//                Sorted by newest first so the latest escalation shows first.
// Parameters   : incidentId
// Returns      : Array of escalation documents with user details populated

const getEscalationsByIncident = async (incidentId) => {
  return Escalation.find({ incident: incidentId })
    .sort({ createdAt: -1 })
    .populate('escalatedBy', 'name email')
    .populate('escalatedTo', 'name email');
};

// ── FUNCTION: acknowledgeEscalation ─────────────────────────
// What it does : Marks an escalation as acknowledged by the assigned person.
// Parameters   : escalationId
// Returns      : Updated escalation document

const acknowledgeEscalation = async (escalationId) => {
  return Escalation.findByIdAndUpdate(
    escalationId,
    { status: 'acknowledged' },
    { new: true }
  );
};

// ── FUNCTION: autoEscalateByPriority ────────────────────────
// What it does : Business rule — checks all open critical incidents that
//                have no escalation yet and flags them.
//                Can be called by a scheduled job (cron) later.
// Returns      : Array of incident IDs that need escalation

const autoEscalateByPriority = async () => {
  // Find all critical open incidents
  const criticalIncidents = await Incident.find({
    priority: 'critical',
    status:   'open',
  });

  // For each, check if an escalation already exists
  const needsEscalation = [];
  for (const incident of criticalIncidents) {
    const existing = await Escalation.findOne({ incident: incident._id });
    if (!existing) {
      needsEscalation.push(incident._id);
    }
  }

  return needsEscalation;
};

module.exports = {
  createEscalation,
  getEscalationsByIncident,
  acknowledgeEscalation,
  autoEscalateByPriority,
};