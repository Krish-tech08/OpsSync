// middleware/errorHandler.js
// Owner: Incident Engineer (Riya)
// Purpose: Global error handler. Catches every error passed via next(err) from
//          any controller and returns a consistent, clean JSON error response.
//          Prevents the server from crashing and hides stack traces in production.

const errorHandler = (err, req, res, next) => {
  // Log the full error internally for debugging
  console.error(`[ERROR] ${req.method} ${req.originalUrl} →`, err.message);

     // 🔐 Handle JWT errors safely (ADDED PART)
  if (err.name === 'JsonWebTokenError') {
    err.statusCode = 401;
    err.message = 'Invalid token';
  }

  if (err.name === 'TokenExpiredError') {
    err.statusCode = 401;
    err.message = 'Token expired';
  }
  // Default to 500 Internal Server Error unless the error has a statusCode
  const statusCode = err.statusCode || 500;

  // In development, include the stack trace so devs can debug quickly.
  // In production, never expose stack traces to the client.
  const response = {
    success: false,
    message: err.message || 'Internal Server Error',
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack }),
  };

  res.status(statusCode).json(response);
};

module.exports = errorHandler;