// ─────────────────────────────────────────
//  middleware/auth.js  –  JWT guard
//
//  Protects any route it is applied to.
//  Reads the token from the Authorization
//  header (Bearer scheme), verifies it, and
//  attaches the decoded user to req.user.
//
//  Usage in routes:
//    router.get('/protected', protect, handler);
// ─────────────────────────────────────────

const { verifyToken } = require('../config/jwt');
const User            = require('../models/User');

const protect = async (req, res, next) => {
  try {
    // 1. Extract the token from the header
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ success: false, message: 'No token provided' });
    }

    const token = authHeader.split(' ')[1];

    // 2. Verify signature and expiry
    const decoded = verifyToken(token); // Throws if invalid

    // 3. Check the user still exists in the database
    //    (handles the case where a user was deleted after token was issued)
    const user = await User.findById(decoded.id);
    if (!user) {
      return res.status(401).json({ success: false, message: 'User no longer exists' });
    }

    // 4. Attach user to request so controllers can use it
    req.user = user;
    next();
  } catch (error) {
    return res.status(401).json({ success: false, message: 'Invalid or expired token' });
  }
};

/**
 * Restrict access to specific roles.
 * Must be used AFTER protect middleware.
 *
 * Usage:
 *   router.delete('/users/:id', protect, restrictTo('admin'), handler);
 *
 * @param {...string} roles - Allowed roles
 */
const restrictTo = (...roles) => {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'You do not have permission to perform this action',
      });
    }
    next();
  };
};

module.exports = { protect, restrictTo };
