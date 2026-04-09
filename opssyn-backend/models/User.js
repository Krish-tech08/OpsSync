// models/User.js
// Owner: Backend Developer (Priya)
// Purpose: Blueprint for how a User is stored in the database.

const mongoose = require('mongoose');
const bcrypt   = require('bcryptjs');

const UserSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Name is required'],
      trim: true,
    },
    email: {
      type: String,
      required: [true, 'Email is required'],
      unique: true,
      lowercase: true,
      trim: true,
    },
    password: {
      type: String,
      required: [true, 'Password is required'],
      minlength: 6,
      select: false, // Never returned in queries unless explicitly asked
    },
    role: {
      type: String,
      enum: ['engineer', 'manager', 'admin'],
      default: 'engineer',
    },
  },
  { timestamps: true } // Adds createdAt and updatedAt automatically
);

// ── PRE-SAVE HOOK ────────────────────────────────────────────
// Runs automatically before every .save() call.
// Hashes the plain-text password so it is never stored as-is in the DB.
UserSchema.pre('save', async function (next) {
  // Only re-hash if the password field was actually changed
  if (!this.isModified('password')) return next();
  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// ── INSTANCE METHOD: comparePassword ────────────────────────
// What it does : Compares a plain-text password (from login request)
//                against the hashed password stored in the DB.
// Returns      : true if they match, false otherwise.
// Used in      : auth.controller.js → loginUser()
UserSchema.methods.comparePassword = async function (candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', UserSchema);