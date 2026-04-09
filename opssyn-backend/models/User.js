// models/User.js
// Owner: Backend Developer (Priya)
// Purpose: Blueprint for how a User is stored in the database.
//          Supports BOTH GitHub OAuth users AND email/password users.

const mongoose = require('mongoose');
const bcrypt   = require('bcryptjs');

const UserSchema = new mongoose.Schema(
  {
    name: {
      type:     String,
      required: [true, 'Name is required'],
      trim:     true,
    },
    email: {
      type:      String,
      required:  [true, 'Email is required'],
      unique:    true,
      lowercase: true,
      trim:      true,
    },
    password: {
      type:      String,
      minlength: 6,
      select:    false, // Only returned when explicitly requested
      // ✅ Not required — GitHub users have no password
    },
    role: {
      type:    String,
      enum:    ['engineer', 'manager', 'admin'],
      default: 'engineer',
    },

    // ── GITHUB OAUTH FIELDS ──────────────────────────────────
    // Populated when the user signs in via GitHub instead of email/password
    githubId: {
      type:   String,
      unique: true,
      sparse: true, // Allows multiple null values (email/password users)
    },
    githubUsername: {
      type: String,
    },
    githubAccessToken: {
      type:   String,
      select: false, // Never expose in API responses
    },
    avatarUrl: {
      type: String,
    },
    // Which login method this user used
    authProvider: {
      type:    String,
      enum:    ['local', 'github'],
      default: 'local',
    },
  },
  { timestamps: true }
);

// ── PRE-SAVE HOOK ────────────────────────────────────────────
// Only hashes password for local (email/password) users.
// GitHub users skip this entirely since they have no password.
UserSchema.pre('save', async function (next) {
  // Skip if no password set (GitHub OAuth user) or password not changed
  if (!this.password || !this.isModified('password')) return next();
  const salt   = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// ── INSTANCE METHOD: comparePassword ────────────────────────
// Only called for local auth users. Returns false safely for GitHub users.
UserSchema.methods.comparePassword = async function (candidatePassword) {
  if (!this.password) return false; // GitHub user — no password to compare
  return bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', UserSchema);
