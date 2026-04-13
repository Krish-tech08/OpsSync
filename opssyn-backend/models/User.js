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
      select:    false,
    },
    role: {
      type:    String,
      enum:    ['engineer', 'manager', 'admin'],
      default: 'engineer',
    },
    teamsWebhookUrl: {
      type:    String,
      default: null,
      select:  false,
    },
    // ── FCM TOKEN ─────────────────────────────────────────────
    // Device push notification token from Firebase Cloud Messaging.
    // Updated on every app launch to stay current.
    fcmToken: {
      type:    String,
      default: null,
      select:  false,
    },
    // ── GITHUB OAUTH FIELDS ──────────────────────────────────
    githubId: {
      type:   String,
      unique: true,
      sparse: true,
    },
    githubUsername: { type: String },
    githubAccessToken: {
      type:   String,
      select: false,
    },
    avatarUrl:    { type: String },
    authProvider: {
      type:    String,
      enum:    ['local', 'github'],
      default: 'local',
    },
  },
  { timestamps: true }
);

UserSchema.pre('save', async function (next) {
  if (!this.password || !this.isModified('password')) return next();
  const salt    = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

UserSchema.methods.comparePassword = async function (candidatePassword) {
  if (!this.password) return false;
  return bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', UserSchema);
