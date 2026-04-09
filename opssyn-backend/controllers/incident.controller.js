// controllers/incident.controller.js
// Owner: Incident Engineer (Riya)
// Purpose: Handles all HTTP requests for incident management —
//          create, fetch, update, assign, and sort incidents.

const Incident = require('../models/Incident');

// ── FUNCTION: createIncident ─────────────────────────────────
// Route        : POST /api/incidents
// What it does : Creates a new incident in the database.
//                Automatically sets createdBy to the logged-in user.
// Request body : { title, description, priority }
// Response     : { success, incident }

const createIncident = async (req, res, next) => {
  try {
    const { title, description, priority } = req.body;

    const incident = await Incident.create({
      title,
      description,
      priority,
      createdBy: req.user.id, // Set from JWT token by protect middleware
    });

    res.status(201).json({ success: true, incident });
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
// Response     : { success, count, incidents }

const getAllIncidents = async (req, res, next) => {
  try {
    const { status, priority, sortBy = 'createdAt', order = 'desc' } = req.query;

    // Build filter object dynamically based on query params
    const filter = {};
    if (status)   filter.status   = status;
    if (priority) filter.priority = priority;

    // Build sort object — e.g. { createdAt: -1 } for newest first
    const sortOrder = order === 'asc' ? 1 : -1;
    const sort = { [sortBy]: sortOrder };

    const incidents = await Incident.find(filter)
      .sort(sort)
      .populate('createdBy', 'name email')   // Replace ID with user name+email
      .populate('assignedTo', 'name email');

    res.status(200).json({ success: true, count: incidents.length, incidents });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getIncidentById ────────────────────────────────
// Route        : GET /api/incidents/:id
// What it does : Fetches a single incident by its MongoDB ID.
// URL params   : id — the incident's MongoDB _id
// Response     : { success, incident }

const getIncidentById = async (req, res, next) => {
  try {
    const incident = await Incident.findById(req.params.id)
      .populate('createdBy', 'name email')
      .populate('assignedTo', 'name email');

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: updateIncident ─────────────────────────────────
// Route        : PUT /api/incidents/:id
// What it does : Updates incident fields — status, description, priority.
//                Automatically sets resolvedAt timestamp when status → resolved.
// Request body : { status?, description?, priority? }
// Response     : { success, incident }

const updateIncident = async (req, res, next) => {
  try {
    const { status, description, priority } = req.body;

    // Build update object with only provided fields
    const updates = {};
    if (status)      updates.status      = status;
    if (description) updates.description = description;
    if (priority)    updates.priority    = priority;

    // Auto-stamp resolvedAt when status is set to resolved
    if (status === 'resolved') updates.resolvedAt = new Date();

    const incident = await Incident.findByIdAndUpdate(
      req.params.id,
      updates,
      { new: true, runValidators: true } // Return updated doc + run schema validation
    );

    if (!incident) {
      return res.status(404).json({ success: false, message: 'Incident not found.' });
    }

    res.status(200).json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: assignIncident ─────────────────────────────────
// Route        : PATCH /api/incidents/:id/assign
// What it does : Assigns an incident to a specific user and sets
//                status to in_progress automatically.
// Request body : { userId } — the MongoDB _id of the user to assign to
// Response     : { success, incident }

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

    res.status(200).json({ success: true, incident });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: deleteIncident ─────────────────────────────────
// Route        : DELETE /api/incidents/:id
// What it does : Permanently deletes an incident. Admin only.
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