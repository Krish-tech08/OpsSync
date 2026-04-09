// config/jwt.js
// Owner: DevOps (Arjun)
// Purpose: Central place for all JWT settings. Read from .env — never hardcoded.

module.exports = {
  secret: process.env.JWT_SECRET,         // Secret key used to sign tokens
  expiresIn: process.env.JWT_EXPIRES_IN || '24h', // Token validity window
};