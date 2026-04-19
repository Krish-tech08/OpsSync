// controllers/incident.controller.js
// Owner: Incident Engineer (Riya)
// Purpose: Handles all HTTP requests for incident management —
//          create, fetch, update, assign, and sort incidents.

const Incident = require('../models/Incident');
const { notifyIncidentInternal } = require('./notification.controller');

// ── FUNCTION: createIncident ─────────────────────────────────
// Route        : POST /api/incidents
// What it does : Creates a new incident in the database.
// Request body : { title, description, priority }
// Response     : { success, data }

const createIncident = async (req, res, next) => {
  try {
    const { title, description, priority } = req.body;

    const incident = await Incident.create({
      title,
      description,
      priority,
      createdBy: req.user?._id ?? undefined,
    });

    notifyIncidentInternal(incident);

    res.status(201).json({ success: true, data: incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getAllIncidents ─────────────────────────────────
// Route        : GET /api/incidents
// What it does : Fetches all incidents with optional filtering by status
//                or priority, and sorting by date or priority.
// Query params : status, priority, sortBy (createdAt | priority), order (asc | desc)
// Example      : GET /api/incidents?status=open&priority=critical&sortBy=createdAt&order=desc
// Response     : { success, count, data }

const getAllIncidents = async (req, res, next) => {
  try {
    const { status, priority, sortBy = 'createdAt', order = 'desc' } = req.query;

    const filter = {};
    if (status)   filter.status   = status;
    if (priority) filter.priority = priority;

    const sortOrder = order === 'asc' ? 1 : -1;
    const sort = { [sortBy]: sortOrder };

    const incidents = await Incident.find(filter)
      .sort(sort)
      .populate('createdBy', 'name email')
      .populate('assignedTo', 'name email');

    // ✅ FIX: key changed from "incidents" to "data" to match ApiResponse<T> wrapper
    res.status(200).json({ success: true, count: incidents.length, data: incidents });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getIncidentById ────────────────────────────────
// Route        : GET /api/incidents/:id
// Response     : { success, data }

const getIncidentById = async (req, res, next) => {
  try {
    const incident = await Incident.findById(req.params.id)
      .populate('createdBy', 'name email')
      .populate('assignedTo', 'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, data: incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: updateIncident ─────────────────────────────────
// Route        : PUT /api/incidents/:id
// Request body : { status?, description?, priority? }
// Response     : { success, data }

const updateIncident = async (req, res, next) => {
  try {
    const { status, description, priority } = req.body;

    const updates = {};
    if (status)      updates.status      = status;
    if (description) updates.description = description;
    if (priority)    updates.priority    = priority;

    if (status === 'resolved') updates.resolvedAt = new Date();

    const incident = await Incident.findByIdAndUpdate(
      req.params.id,
      updates,
      { new: true, runValidators: true }
    );

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, data: incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: assignIncident ─────────────────────────────────
// Route        : PATCH /api/incidents/:id/assign
// Request body : { userId }
// Response     : { success, data }

const assignIncident = async (req, res, next) => {
  try {
    const { userId } = req.body;

    const incident = await Incident.findByIdAndUpdate(
      req.params.id,
      { assignedTo: userId, status: 'in_progress' },
      { new: true }
    ).populate('assignedTo', 'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, data: incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: deleteIncident ─────────────────────────────────
// Route        : DELETE /api/incidents/:id
// Response     : { success, message }

const deleteIncident = async (req, res, next) => {
  try {
    const incident = await Incident.findByIdAndDelete(req.params.id);

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, message: 'Incident deleted successfully.' });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  createIncident,
  getAllIncidents,
  getIncidentById,
  updateIncident,
  assignIncident,
  deleteIncident,
};
