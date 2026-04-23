

const Escalation = require('../models/Escalation');
const Incident   = require('../models/Incident');



const createEscalation = async ({ incidentId, escalatedBy, escalatedTo, reason, priority }) => {
  // Create the escalation record
  const escalation = await Escalation.create({
    incident:    incidentId,
    escalatedBy,
    escalatedTo,
    reason,
    priority,
  });

  // If escalation is critical, auto-upgrade the incident priority too
  if (priority === 'critical') {
    await Incident.findByIdAndUpdate(incidentId, { priority: 'critical' });
  }

  return escalation;
};



const getEscalationsByIncident = async (incidentId) => {
  return Escalation.find({ incident: incidentId })
    .sort({ createdAt: -1 })
    .populate('escalatedBy', 'name email')
    .populate('escalatedTo', 'name email');
};


const acknowledgeEscalation = async (escalationId) => {
  return Escalation.findByIdAndUpdate(
    escalationId,
    { status: 'acknowledged' },
    { new: true }
  );
};



const autoEscalateByPriority = async () => {
  // Find all critical open incidents
  const criticalIncidents = await Incident.find({
    priority: 'critical',
    status:   'open',
  });

  // For each, check if an escalation already exists
  const needsEscalation = [];
  for (const incident of criticalIncidents) {
    const existing = await Escalation.findOne({ incident: incident._id });
    if (!existing) {
      needsEscalation.push(incident._id);
    }
  }

  return needsEscalation;
};

module.exports = {
  createEscalation,
  getEscalationsByIncident,
  acknowledgeEscalation,
  autoEscalateByPriority,
};
