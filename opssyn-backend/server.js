require('dotenv').config();
const webhookRoutes = require('./routes/webhook.routes');
const express = require('express');
const cors    = require('cors');
const helmet  = require('helmet');
const morgan  = require('morgan');

const connectDB = require('./config/db');

const authRoutes         = require('./routes/auth.routes');
const pipelineRoutes     = require('./routes/pipeline.routes');
const incidentRoutes     = require('./routes/incident.routes');
const escalationRoutes   = require('./routes/escalation.routes');
const notificationRoutes = require('./routes/notification.routes');
const errorHandler       = require('./middleware/errorHandler');

const app = express();

// Add this BEFORE express.json() middleware
// because GitHub webhook needs raw body for signature verification

app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use('/api/webhooks/github', express.raw({ type: 'application/json' }));
app.use(express.json());

app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', project: 'PocketOps' });
});

app.use('/api/auth',          authRoutes);
app.use('/api/pipelines',     pipelineRoutes);
app.use('/api/incidents',     incidentRoutes);
app.use('/api/escalations',   escalationRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/webhooks', webhookRoutes);
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Route not found' });
});

app.use(errorHandler);

const PORT = process.env.PORT || 5000;

connectDB()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`✅ PocketOps server running on port ${PORT}`);
      console.log(`Environment: ${process.env.NODE_ENV}`);
    });
  })
  .catch((err) => {
    console.error('❌ Failed to connect DB:', err.message);
    process.exit(1);
  });
