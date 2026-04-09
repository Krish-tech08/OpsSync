// ============================================================
//  PocketOps — server.js
//  Entry point of the entire application
//  Owner: Backend Developer (Kanishka) + DevOps (Ishita) for env/port
// ============================================================

// ── 1. LOAD ENVIRONMENT VARIABLES ───────────────────────────
// Must be the very first line so every file below can access .env values
require('dotenv').config();

// ── 2. CORE IMPORTS ─────────────────────────────────────────
const express = require('express');   // Web framework
const cors    = require('cors');      // Allow Android app to call this API
const helmet  = require('helmet');    // Set secure HTTP headers automatically
const morgan  = require('morgan');    // Log every incoming request to the console

// ── 3. DATABASE CONNECTION ───────────────────────────────────
// Owned by DevOps (Ishita) — lives in config/db.js
const connectDB = require('./config/db');

// ── 4. ROUTE IMPORTS ────────────────────────────────────────
// Owned by Backend Developer (Kanishka) — each file handles one feature area
const authRoutes         = require('./routes/auth.routes');
const pipelineRoutes     = require('./routes/pipeline.routes');
const incidentRoutes     = require('./routes/incident.routes');
const escalationRoutes   = require('./routes/escalation.routes');
const notificationRoutes = require('./routes/notification.routes');

// ── 5. ERROR HANDLER IMPORT ──────────────────────────────────
// Owned by Incident Engineer (Kani) — catches all unhandled errors
const errorHandler = require('./middleware/errorHandler');

// ── 6. INITIALISE EXPRESS APP ────────────────────────────────
const app = express();

// ── 7. CONNECT TO DATABASE ───────────────────────────────────
/*
 * connectDB()
 * -----------
 * What it does : Opens a connection to the database using the DB_URL
 *                stored in .env. Logs success or exits the process on failure.
 * Why it's here: Database must be connected before the server starts
 *                accepting any requests.
 * Owned by     : DevOps (Ishita) — the function lives in config/db.js
 */
connectDB();

// ── 8. GLOBAL MIDDLEWARE ─────────────────────────────────────
/*
 * helmet()
 * --------
 * What it does : Automatically adds security-related HTTP headers to every
 *                response (e.g. prevents clickjacking, hides server info).
 * Why it's here: One line that handles many common web security basics.
 */
app.use(helmet());

/*
 * cors()
 * ------
 * What it does : Allows the Android app (running on a different origin) to
 *                make HTTP requests to this server without being blocked.
 * Why it's here: Browsers and mobile HTTP clients block cross-origin
 *                requests by default — CORS headers explicitly permit them.
 */
app.use(cors());

/*
 * morgan('dev')
 * -------------
 * What it does : Prints a one-line log for every request that arrives —
 *                e.g. "GET /api/incidents 200 12ms"
 * Why it's here: Helps developers see what's happening in real time
 *                without adding console.log() everywhere.
 */
app.use(morgan('dev'));

/*
 * express.json()
 * --------------
 * What it does : Reads the JSON body of incoming POST/PUT requests and
 *                makes it available as req.body inside controllers.
 * Why it's here: Without this, req.body is always undefined.
 */
app.use(express.json());

// ── 9. HEALTH CHECK ROUTE ────────────────────────────────────
/*
 * GET /health
 * -----------
 * What it does : Returns a simple { status: "ok" } response.
 * Why it's here: DevOps (Ishita) uses this to verify the server is running
 *                inside Docker or any deployment environment.
 *                No auth required — purely for uptime checks.
 */
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', project: 'PocketOps' });
});

// ── 10. MOUNT FEATURE ROUTES ─────────────────────────────────
/*
 * Each app.use() call below attaches a router to a URL prefix.
 * All requests that start with that prefix are handed to the matching
 * routes file, which then calls the right controller function.
 *
 * Pattern  : app.use('/api/<feature>', <routerFile>)
 * Example  : POST /api/auth/login → auth.routes.js → auth.controller.js
 *
 * Owned by : Backend Developer (Kanishka)
 */
app.use('/api/auth',          authRoutes);         // Login, Register
app.use('/api/pipelines',     pipelineRoutes);     // Fetch, Re-run, Cancel
app.use('/api/incidents',     incidentRoutes);     // Create, Update, Assign
app.use('/api/escalations',   escalationRoutes);   // Escalate, View history
app.use('/api/notifications', notificationRoutes); // Trigger, Fetch

// ── 11. 404 HANDLER ─────────────────────────────────────────
/*
 * What it does : If a request reaches here, no route above matched the URL.
 *                Returns a clean 404 JSON response instead of Express's
 *                default HTML error page.
 * Why it's here: Must come AFTER all routes so it only fires when nothing
 *                else matched.
 */
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Route not found' });
});

// ── 12. GLOBAL ERROR HANDLER ────────────────────────────────
/*
 * errorHandler (from middleware/errorHandler.js)
 * -----------------------------------------------
 * What it does : Catches any error thrown anywhere in the app using
 *                next(error) and returns a clean JSON error response.
 *                Prevents the server from crashing on unexpected errors.
 * Why it's here: Must be the LAST middleware — Express identifies a
 *                4-argument function (err, req, res, next) as an error handler.
 * Owned by     : Incident Engineer (Kani)
 */
app.use(errorHandler);

// ── 13. START THE SERVER ─────────────────────────────────────
/*
 * PORT
 * ----
 * Reads the port number from .env (set by DevOps / Ishita).
 * Falls back to 5000 if .env doesn't define it.
 *
 * app.listen()
 * ------------
 * What it does : Starts the HTTP server and begins accepting incoming
 *                requests on the specified port.
 * Why it's here: Without this call, the Express app is configured but
 *                never actually starts listening for connections.
 */
const PORT = process.env.PORT || 5000;

connectDB()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`✅ PocketOps server running on port ${PORT}`);
      console.log(`Environment: ${process.env.NODE_ENV}`);
    });
  })
  .catch((err) => {
    console.error("❌ Failed to connect DB:", err.message);
    process.exit(1); // Important for Render (fail fast)
  });
