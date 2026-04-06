// ─────────────────────────────────────────
//  models/Incident.js  –  Incident schema
//
//  An incident is created manually or auto-
//  generated when a pipeline run fails.
//
//  Lifecycle:  open → in_progress → resolved
//  Severity:   low | medium | high | critical
// ─────────────────────────────────────────

const mongoose = require('mongoose');

const incidentSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: [true, 'Title is required'],
      trim: true,
    },

    description: {
      type: String,
      trim: true,
      default: '',
    },

    severity: {
      type: String,
      enum: ['low', 'medium', 'high', 'critical'],
      default: 'medium',
    },

    status: {
      type: String,
      enum: ['open', 'in_progress', 'resolved'],
      default: 'open',
    },

    // The user currently responsible for this incident
    assignedTo: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },

    // The user who created or reported this incident
    createdBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },

    // Auto-populated when a pipeline run triggers the incident
    pipelineRunId: {
      type: String,
      default: null,
    },

    // Whether this incident was escalated at least once
    escalated: {
      type: Boolean,
      default: false,
    },

    // Timestamp of the last escalation (used by the escalation engine)
    lastEscalatedAt: {
      type: Date,
      default: null,
    },

    resolvedAt: {
      type: Date,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

// ── Index: speed up status + severity queries from mobile app ─────
incidentSchema.index({ status: 1, severity: -1, createdAt: -1 });

module.exports = mongoose.model('Incident', incidentSchema);
