// routes/incident.routes.js
// Owner: Backend Developer (Priya)
// Purpose: Declares all incident API endpoints.

const express = require('express');
const router  = express.Router();

const {
  createIncident,
  getAllIncidents,
  getIncidentById,
  updateIncident,
  assignIncident,
  deleteIncident,
} = require('../controllers/incident.controller');

const { protect, authorise } = require('../middleware/auth');

// All routes below require a valid JWT token
// POST   /api/incidents              → Create a new incident
router.post('/',          protect, createIncident);

// GET    /api/incidents              → Get all incidents (with filters + sorting)
// Example: /api/incidents?status=open&priority=critical&sortBy=createdAt&order=desc
router.get('/',           protect, getAllIncidents);

// GET    /api/incidents/:id          → Get single incident by ID
router.get('/:id',        protect, getIncidentById);

// PUT    /api/incidents/:id          → Update incident status/description/priority
router.put('/:id',        protect, updateIncident);

// PATCH  /api/incidents/:id/assign   → Assign incident to a user
router.patch('/:id/assign', protect, assignIncident);

// DELETE /api/incidents/:id          → Delete incident (admin only)
router.delete('/:id',     protect, authorise('admin'), deleteIncident);

module.exports = router;