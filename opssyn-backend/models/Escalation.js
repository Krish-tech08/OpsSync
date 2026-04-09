// models/Escalation.js
// Owner: Incident Engineer (Riya)
// Purpose: Tracks every escalation event — who escalated, which incident,
//          to whom, and why. Gives a full audit trail.

const mongoose = require('mongoose');

const EscalationSchema = new mongoose.Schema(
  {
    incident: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Incident',
      required: true,
    },
    escalatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    escalatedTo: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    reason: {
      type: String,
      required: [true, 'Escalation reason is required'],
    },
    priority: {
      type: String,
      enum: ['medium', 'high', 'critical'],
      default: 'high',
    },
    status: {
      type: String,
      enum: ['pending', 'acknowledged', 'resolved'],
      default: 'pending',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Escalation', EscalationSchema);