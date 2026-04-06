// ─────────────────────────────────────────
//  controllers/escalation.controller.js
//
//  Exposes escalation data and allows manual
//  escalation via the API.
// ─────────────────────────────────────────

const Escalation         = require('../models/Escalation');
const { manualEscalate } = require('../services/escalation.service');

/**
 * GET /api/escalations
 * List all escalation records (audit log).
 * Optional query: incidentId
 */
const getEscalations = async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.incidentId) filter.incident = req.query.incidentId;

    const escalations = await Escalation.find(filter)
      .populate('incident',          'title severity status')
      .populate('previousAssignee',  'name email')
      .populate('newAssignee',       'name email')
      .sort({ createdAt: -1 });

    res.json({ success: true, count: escalations.length, escalations });
  } catch (err) {
    next(err);
  }
};

/**
 * GET /api/escalations/:id
 */
const getEscalation = async (req, res, next) => {
  try {
    const escalation = await Escalation.findById(req.params.id)
      .populate('incident',         'title severity status')
      .populate('previousAssignee', 'name email')
      .populate('newAssignee',      'name email');

    if (!escalation) {
      return res.status(404).json({ success: false, message: 'Escalation not found' });
    }

    res.json({ success: true, escalation });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/escalations/:incidentId/escalate
 * Manually escalate an incident immediately.
 */
const triggerManualEscalation = async (req, res, next) => {
  try {
    const incident = await manualEscalate(req.params.incidentId);

    res.json({
      success: true,
      message: `Incident "${incident.title}" escalated successfully`,
      incident,
    });
  } catch (err) {
    // manualEscalate throws plain Errors – set status code before forwarding
    if (err.message === 'Incident not found') err.statusCode = 404;
    if (err.message.includes('resolved'))     err.statusCode = 400;
    next(err);
  }
};

module.exports = { getEscalations, getEscalation, triggerManualEscalation };
