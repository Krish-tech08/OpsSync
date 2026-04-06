// ─────────────────────────────────────────
//  controllers/incident.controller.js
//
//  Full CRUD for incidents plus:
//   – Filtering by status, severity
//   – Sorting by severity (critical first) or time
//   – Status transition validation
//   – Assignment to users
// ─────────────────────────────────────────

const Incident           = require('../models/Incident');
const User               = require('../models/User');
const { sendNotification } = require('../services/notification.service');

// Severity order used when sorting (higher = more severe)
const SEVERITY_RANK = { critical: 4, high: 3, medium: 2, low: 1 };

/**
 * GET /api/incidents
 * Query params: status, severity, assignedTo, sortBy (severity|createdAt)
 */
const getIncidents = async (req, res, next) => {
  try {
    const { status, severity, assignedTo, sortBy = 'severity' } = req.query;

    // Build the filter object dynamically
    const filter = {};
    if (status)     filter.status     = status;
    if (severity)   filter.severity   = severity;
    if (assignedTo) filter.assignedTo = assignedTo;

    // Determine sort order
    const sortOptions =
      sortBy === 'severity'
        ? { severity: -1, createdAt: -1 }  // MongoDB can't sort by custom rank, so use string sort + re-rank in JS
        : { createdAt: -1 };

    let incidents = await Incident.find(filter)
      .populate('assignedTo', 'name email')
      .populate('createdBy',  'name email')
      .sort(sortOptions);

    // Re-sort by severity rank when requested (MongoDB string sort != priority sort)
    if (sortBy === 'severity') {
      incidents = incidents.sort(
        (a, b) => SEVERITY_RANK[b.severity] - SEVERITY_RANK[a.severity]
      );
    }

    res.json({ success: true, count: incidents.length, incidents });
  } catch (err) {
    next(err);
  }
};

/**
 * GET /api/incidents/:id
 */
const getIncident = async (req, res, next) => {
  try {
    const incident = await Incident.findById(req.params.id)
      .populate('assignedTo', 'name email')
      .populate('createdBy',  'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found' });
    }

    res.json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/incidents
 * Body: { title, description?, severity?, assignedTo? }
 */
const createIncident = async (req, res, next) => {
  try {
    const { title, description, severity, assignedTo } = req.body;

    const incident = await Incident.create({
      title,
      description,
      severity,
      assignedTo: assignedTo || null,
      createdBy:  req.user._id,
    });

    // Notify the assignee if one was set at creation
    if (assignedTo) {
      await sendNotification({
        userId:  assignedTo,
        title:   '📋 Incident Assigned to You',
        message: `"${incident.title}" (${incident.severity}) has been assigned to you.`,
      });
    }

    res.status(201).json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

/**
 * PATCH /api/incidents/:id
 * Allows updating: title, description, severity, status
 * Status transitions enforced:  open → in_progress → resolved
 */
const updateIncident = async (req, res, next) => {
  try {
    const incident = await Incident.findById(req.params.id);
    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found' });
    }

    const { title, description, severity, status } = req.body;

    // Enforce status transition rules
    if (status) {
      const allowed = {
        open:        ['in_progress'],
        in_progress: ['resolved'],
        resolved:    [],           // Terminal state – cannot reopen via this endpoint
      };

      if (!allowed[incident.status].includes(status)) {
        return res.status(400).json({
          success: false,
          message: `Cannot transition from "${incident.status}" to "${status}"`,
        });
      }

      // Record when the incident was resolved
      if (status === 'resolved') incident.resolvedAt = new Date();
      incident.status = status;
    }

    if (title)       incident.title       = title;
    if (description) incident.description = description;
    if (severity)    incident.severity    = severity;

    await incident.save();

    res.json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

/**
 * PATCH /api/incidents/:id/assign
 * Body: { userId }
 * Reassign an incident to a different user.
 */
const assignIncident = async (req, res, next) => {
  try {
    const { userId } = req.body;

    if (!userId) {
      return res.status(400).json({ success: false, message: 'userId is required' });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }

    const incident = await Incident.findByIdAndUpdate(
      req.params.id,
      { assignedTo: userId },
      { new: true }
    ).populate('assignedTo', 'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found' });
    }

    // Notify the newly assigned user
    await sendNotification({
      userId,
      title:   '📋 Incident Assigned to You',
      message: `"${incident.title}" (${incident.severity}) has been assigned to you.`,
    });

    res.json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

/**
 * DELETE /api/incidents/:id
 * Admin only.
 */
const deleteIncident = async (req, res, next) => {
  try {
    const incident = await Incident.findByIdAndDelete(req.params.id);
    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found' });
    }

    res.json({ success: true, message: 'Incident deleted' });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  getIncidents,
  getIncident,
  createIncident,
  updateIncident,
  assignIncident,
  deleteIncident,
};
