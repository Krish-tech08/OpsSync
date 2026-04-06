// ─────────────────────────────────────────
//  middleware/errorHandler.js
//
//  Global Express error handler.
//  Registered LAST in server.js so it catches
//  any error passed via next(err) from any
//  controller or middleware.
//
//  Returns a consistent JSON shape:
//   { success: false, message: "...", stack: "..." }
// ─────────────────────────────────────────

const errorHandler = (err, req, res, next) => {
  // Default to 500 if the error has no status code
  let statusCode = err.statusCode || 500;
  let message    = err.message    || 'Internal server error';

  // ── Mongoose: bad ObjectId (e.g. /incidents/not-an-id) ───────────
  if (err.name === 'CastError') {
    statusCode = 400;
    message    = `Invalid ${err.path}: ${err.value}`;
  }

  // ── Mongoose: duplicate key (e.g. registering with existing email) ─
  if (err.code === 11000) {
    const field = Object.keys(err.keyValue)[0];
    statusCode  = 409;
    message     = `${field} already exists`;
  }

  // ── Mongoose: validation error ────────────────────────────────────
  if (err.name === 'ValidationError') {
    statusCode = 400;
    message    = Object.values(err.errors)
      .map((e) => e.message)
      .join(', ');
  }

  // ── JWT errors ────────────────────────────────────────────────────
  if (err.name === 'JsonWebTokenError')  { statusCode = 401; message = 'Invalid token'; }
  if (err.name === 'TokenExpiredError')  { statusCode = 401; message = 'Token expired'; }

  res.status(statusCode).json({
    success: false,
    message,
    // Show stack trace only in development
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack }),
  });
};

module.exports = errorHandler;
