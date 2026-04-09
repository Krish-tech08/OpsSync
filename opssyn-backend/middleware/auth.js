// middleware/auth.js
// Owner: Backend Developer (Priya)
// Purpose: JWT token verification gate. Sits in front of any protected route.
//          If the token is missing or invalid, the request is rejected here
//          and never reaches the controller.

const jwt     = require('jsonwebtoken');
const jwtConfig = require('../config/jwt');

// ── FUNCTION: protect ────────────────────────────────────────
// What it does : Reads the Authorization header, extracts the Bearer token,
//                verifies it using the JWT secret, and attaches the decoded
//                user payload to req.user for downstream controllers to use.
//
// How to use   : Add as middleware to any route that needs authentication.
//   Example    : router.get('/pipelines', protect, pipelineController.getAll)
//
// Flow         :
//   Request → check header → extract token → verify signature
//   → attach req.user → call next() → controller runs
//
// Rejects with : 401 if token is missing, malformed, or expired

const protect = async (req, res, next) => {
  try {
    // 1. Check if the Authorization header exists and starts with "Bearer"
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Access denied. No token provided.',
      });
    }

    // 2. Extract the token string (everything after "Bearer ")
    const token = authHeader.split(' ')[1];

    // 3. Verify the token — throws an error if expired or tampered with
    const decoded = jwt.verify(token, jwtConfig.secret);

    // 4. Attach the decoded payload (id, email, role) to req.user
    //    Controllers can now access req.user.id, req.user.role, etc.
    req.user = decoded;

    // 5. Token is valid — pass control to the next middleware or controller
    next();
  } catch (err) {
    // jwt.verify throws JsonWebTokenError or TokenExpiredError
    return res.status(401).json({
      success: false,
      message: 'Invalid or expired token. Please log in again.',
    });
  }
};

// ── FUNCTION: authorise ──────────────────────────────────────
// What it does : Role-based access control layer that runs AFTER protect().
//                Checks if the logged-in user's role is in the allowed list.
//
// How to use   : Pass allowed roles as arguments.
//   Example    : router.delete('/users/:id', protect, authorise('admin'), ...)
//
// Rejects with : 403 if the user's role is not in the allowed list

const authorise = (...allowedRoles) => {
  return (req, res, next) => {
    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `Role '${req.user.role}' is not authorised to access this route.`,
      });
    }
    next();
  };
};

module.exports = { protect, authorise };