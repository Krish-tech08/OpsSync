// controllers/escalation.controller.js
// Owner: Incident Engineer (Riya)
// Purpose: Handles HTTP requests for escalation actions.
//          Calls escalation service — no business logic lives here.

const escalationService = require('../services/escalation.service');

// ── FUNCTION: escalateIncident ───────────────────────────────
// Route        : POST /api/escalations
// What it does : Creates a new escalation for an incident.
// Request body : { incidentId, escalatedTo, reason, priority }
// Response     : { success, escalation }

const escalateIncident = async (req, res, next) => {
  try {
    const { incidentId, escalatedTo, reason, priority } = req.body;

    const escalation = await escalationService.createEscalation({
      incidentId,
      escalatedBy: req.user.id, // Logged-in user is the one escalating
      escalatedTo,
      reason,
      priority,
    });

    res.status(201).json({ success: true, escalation });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getIncidentEscalations ────────────────────────
// Route        : GET /api/escalations/:incidentId
// What it does : Returns full escalation history for one incident.
// URL params   : incidentId
// Response     : { success, count, escalations }

const getIncidentEscalations = async (req, res, next) => {
  try {
    const escalations = await escalationService.getEscalationsByIncident(
      req.params.incidentId
    );

    res.status(200).json({
      success: true,
      count: escalations.length,
      escalations,
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: acknowledgeEscalation ─────────────────────────
// Route        : PATCH /api/escalations/:id/acknowledge
// What it does : Marks an escalation as acknowledged.
// URL params   : id — escalation MongoDB _id
// Response     : { success, escalation }

const acknowledgeEscalation = async (req, res, next) => {
  try {
    const escalation = await escalationService.acknowledgeEscalation(req.params.id);

    if (!escalation) {
      return res.status(404).json({ success: false, message: 'Escalation not found.' });
    }

    res.status(200).json({ success: true, escalation });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getAutoEscalations ────────────────────────────
// Route        : GET /api/escalations/auto-check
// What it does : Returns a list of critical open incidents that have no
//                escalation yet — useful for monitoring dashboards.
// Response     : { success, count, incidentIds }

const getAutoEscalations = async (req, res, next) => {
  try {
    const incidentIds = await escalationService.autoEscalateByPriority();

    res.status(200).json({
      success: true,
      count: incidentIds.length,
      message: `${incidentIds.length} critical incident(s) need escalation.`,
      incidentIds,
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  escalateIncident,
  getIncidentEscalations,
  acknowledgeEscalation,
  getAutoEscalations,
};