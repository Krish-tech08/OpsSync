// ─────────────────────────────────────────
//  config/jwt.js  –  JWT sign & verify helpers
// ─────────────────────────────────────────

const jwt = require('jsonwebtoken');

/**
 * Sign a JWT with the app secret.
 * @param {object} payload - Data to embed (e.g. { id, email })
 * @returns {string} Signed JWT string
 */
const signToken = (payload) => {
  return jwt.sign(payload, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
  });
};

/**
 * Verify and decode a JWT.
 * Throws JsonWebTokenError if invalid or expired.
 * @param {string} token - JWT string from the Authorization header
 * @returns {object} Decoded payload
 */
const verifyToken = (token) => {
  return jwt.verify(token, process.env.JWT_SECRET);
};

// Export functions AND raw values so both
// auth.controller.js and middleware/auth.js work correctly
module.exports = {
  signToken,
  verifyToken,
  secret:    process.env.JWT_SECRET,
  expiresIn: process.env.JWT_EXPIRES_IN || '7d',
};