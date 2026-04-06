// controllers/auth.controller.js
// Owner: Backend Developer (Priya)
// Purpose: Handles all authentication logic — register, login, and get current user.
//          Each function maps 1-to-1 with a route in routes/auth.routes.js

const jwt       = require('jsonwebtoken');
const User      = require('../models/User');
const jwtConfig = require('../config/jwt');

// ── HELPER: generateToken ────────────────────────────────────
// What it does : Creates and signs a JWT token with the user's id, email,
//                and role baked into the payload.
// Returns      : A signed JWT string (e.g. "eyJhbGci...")
// Used by      : registerUser() and loginUser() below

const generateToken = (user) => {
  return jwt.sign(
    { id: user._id, email: user.email, role: user.role },
    jwtConfig.secret,
    { expiresIn: jwtConfig.expiresIn }
  );
};

// ── FUNCTION: registerUser ───────────────────────────────────
// Route        : POST /api/auth/register
// What it does : Creates a new user account in the database.
//                The User model's pre-save hook automatically hashes the password.
// Request body : { name, email, password, role? }
// Response     : { success, token, user: { id, name, email, role } }
// Fails with   : 400 if email already exists

const registerUser = async (req, res, next) => {
  try {
    const { name, email, password, role } = req.body;

    // Check if a user with this email already exists
    const existing = await User.findOne({ email });
    if (existing) {
      return res.status(400).json({
        success: false,
        message: 'An account with this email already exists.',
      });
    }

    // Create the user — password is hashed automatically by the pre-save hook
    const user = await User.create({ name, email, password, role });

    // Generate JWT token for immediate login after registration
    const token = generateToken(user);

    res.status(201).json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email, role: user.role },
    });
  } catch (err) {
    next(err); // Pass to global error handler (middleware/errorHandler.js)
  }
};

// ── FUNCTION: loginUser ──────────────────────────────────────
// Route        : POST /api/auth/login
// What it does : Verifies email + password and returns a JWT token if valid.
//                Uses comparePassword() instance method on the User model.
// Request body : { email, password }
// Response     : { success, token, user: { id, name, email, role } }
// Fails with   : 401 if email not found or password doesn't match

const loginUser = async (req, res, next) => {
  try {
    const { email, password } = req.body;

    // Validate input early
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required.',
      });
    }

    // Find user — must explicitly select password since it's select:false in schema
    const user = await User.findOne({ email }).select('+password');
    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.',
      });
    }

    // Compare the plain password against the stored hash
    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.',
      });
    }

    // Credentials are valid — issue token
    const token = generateToken(user);

    res.status(200).json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email, role: user.role },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getMe ──────────────────────────────────────────
// Route        : GET /api/auth/me
// What it does : Returns the profile of the currently logged-in user.
//                Requires a valid JWT — protect middleware populates req.user.
// Request body : none (token in Authorization header)
// Response     : { success, user: { id, name, email, role } }
// Fails with   : 401 if no token / token expired (handled by protect middleware)

const getMe = async (req, res, next) => {
  try {
    // req.user.id is set by middleware/auth.js → protect()
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found.' });
    }

    res.status(200).json({
      success: true,
      user: { id: user._id, name: user.name, email: user.email, role: user.role },
    });
  } catch (err) {
    next(err);
  }
};

module.exports = { registerUser, loginUser, getMe };