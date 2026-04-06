// ─────────────────────────────────────────
//  routes/incident.routes.js
//
//  GET    /api/incidents               – list (filterable + sortable)
//  GET    /api/incidents/:id           – single incident
//  POST   /api/incidents               – create
//  PATCH  /api/incidents/:id           – update title/description/severity/status
//  PATCH  /api/incidents/:id/assign    – assign to a user
//  DELETE /api/incidents/:id           – delete (admin only)
// ─────────────────────────────────────────

const express = require('express');
const router  = express.Router();

const {
  getIncidents,
  getIncident,
  createIncident,
  updateIncident,
  assignIncident,
  deleteIncident,
} = require('../controllers/incident.controller');

const { protect, restrictTo } = require('../middleware/auth');

// All incident routes require authentication
router.use(protect);

router.get ('/',            getIncidents);
router.post('/',            createIncident);
router.get ('/:id',         getIncident);
router.patch('/:id',        updateIncident);
router.patch('/:id/assign', assignIncident);
router.delete('/:id',       restrictTo('admin'), deleteIncident);

module.exports = router;
