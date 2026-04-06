// ─────────────────────────────────────────
//  models/Escalation.js  –  Escalation log schema
//
//  Every time the escalation engine reassigns
//  an incident it creates one Escalation record.
//  This gives a full audit trail of who had
//  responsibility and when it was reassigned.
// ─────────────────────────────────────────

const mongoose = require('mongoose');

const escalationSchema = new mongoose.Schema(
  {
    // The incident that was escalated
    incident: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Incident',
      required: true,
    },

    // Who had the incident before escalation
    previousAssignee: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },

    // Who it was reassigned to
    newAssignee: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },

    // Human-readable reason for the escalation
    reason: {
      type: String,
      default: 'Incident unresolved past threshold',
    },

    // 'auto' = triggered by cron job | 'manual' = triggered via API
    triggeredBy: {
      type: String,
      enum: ['auto', 'manual'],
      default: 'auto',
    },
  },
  {
    timestamps: true, // createdAt = when the escalation happened
  }
);

module.exports = mongoose.model('Escalation', escalationSchema);
