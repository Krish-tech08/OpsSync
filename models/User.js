// ─────────────────────────────────────────
//  models/User.js  –  User schema
//
//  Stores registered users.
//  Password is hashed with bcrypt before saving.
//  The toJSON transform strips the password from
//  any response automatically.
// ─────────────────────────────────────────

const mongoose = require('mongoose');
const bcrypt   = require('bcryptjs');

const userSchema = new mongoose.Schema(
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
      select: false, // Never returned in queries by default
    },

    role: {
      type: String,
      enum: ['engineer', 'admin'],
      default: 'engineer',
    },
  },
  {
    timestamps: true, // Adds createdAt and updatedAt automatically
  }
);

// ── Pre-save hook: hash password before storing ───────────────────
userSchema.pre('save', async function (next) {
  // Only hash if the password field was actually modified
  if (!this.isModified('password')) return next();

  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// ── Instance method: compare plain password with hash ────────────
userSchema.methods.comparePassword = async function (candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

// ── Transform: remove password when converting to JSON ───────────
userSchema.set('toJSON', {
  transform: (_doc, ret) => {
    delete ret.password;
    return ret;
  },
});

module.exports = mongoose.model('User', userSchema);
