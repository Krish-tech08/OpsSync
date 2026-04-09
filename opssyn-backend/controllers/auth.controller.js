// controllers/auth.controller.js
// Owner: Backend Developer (Priya)
// Purpose: Handles all authentication — local email/password AND GitHub OAuth.

const jwt       = require('jsonwebtoken');
const axios     = require('axios');
const User      = require('../models/User');
const jwtConfig = require('../config/jwt');

// ── HELPER: generateToken ────────────────────────────────────
const generateToken = (user) => {
  return jwt.sign(
    { id: user._id, email: user.email, role: user.role },
    jwtConfig.secret,
    { expiresIn: jwtConfig.expiresIn }
  );
};

// ── FUNCTION: registerUser ───────────────────────────────────
// Route        : POST /api/auth/register
// Local email/password registration (unchanged)
const registerUser = async (req, res, next) => {
  try {
    const { name, email, password, role } = req.body;

    const existing = await User.findOne({ email });
    if (existing) {
      return res.status(400).json({
        success: false,
        message: 'An account with this email already exists.',
      });
    }

    const user  = await User.create({ name, email, password, authProvider: 'local', role });
    const token = generateToken(user);

    res.status(201).json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email, role: user.role, avatarUrl: user.avatarUrl },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: loginUser ──────────────────────────────────────
// Route        : POST /api/auth/login
// Local email/password login (unchanged)
const loginUser = async (req, res, next) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required.',
      });
    }

    const user = await User.findOne({ email }).select('+password');
    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    // Block GitHub-only users from using email/password login
    if (user.authProvider === 'github') {
      return res.status(401).json({
        success: false,
        message: 'This account uses GitHub login. Please sign in with GitHub.',
      });
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    const token = generateToken(user);

    res.status(200).json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email, role: user.role, avatarUrl: user.avatarUrl },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: githubCallback ─────────────────────────────────
// Route        : POST /api/auth/github
// What it does : The Android app sends the GitHub OAuth `code` it received
//                from the GitHub OAuth web flow. This backend then:
//                  1. Exchanges the code for a GitHub access token
//                  2. Fetches the user's GitHub profile + primary email
//                  3. Finds or creates a User record in MongoDB
//                  4. Returns our own JWT so the app can call protected routes
//
// Request body : { code }  — the OAuth authorization code from GitHub
// Response     : { success, token, user }

const githubCallback = async (req, res, next) => {
  try {
    const { code } = req.body;

    if (!code) {
      return res.status(400).json({ success: false, message: 'GitHub OAuth code is required.' });
    }

    // ── STEP 1: Exchange code for GitHub access token ────────
    const tokenResponse = await axios.post(
      'https://github.com/login/oauth/access_token',
      {
        client_id:     process.env.GITHUB_CLIENT_ID,
        client_secret: process.env.GITHUB_CLIENT_SECRET,
        code,
      },
      { headers: { Accept: 'application/json' } }
    );

    const githubAccessToken = tokenResponse.data.access_token;

    if (!githubAccessToken) {
      return res.status(401).json({
        success: false,
        message: 'GitHub OAuth failed — could not exchange code for token.',
      });
    }

    // ── STEP 2: Fetch GitHub user profile ───────────────────
    const [profileRes, emailsRes] = await Promise.all([
      axios.get('https://api.github.com/user', {
        headers: { Authorization: `Bearer ${githubAccessToken}` },
      }),
      axios.get('https://api.github.com/user/emails', {
        headers: { Authorization: `Bearer ${githubAccessToken}` },
      }),
    ]);

    const profile = profileRes.data;

    // Pick the primary verified email, fall back to first available
    const primaryEmailObj = emailsRes.data.find((e) => e.primary && e.verified);
    const email           = primaryEmailObj?.email || emailsRes.data[0]?.email;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'No verified email found on your GitHub account. Please add one and try again.',
      });
    }

    // ── STEP 3: Find or create user in MongoDB ───────────────
    // First try to find by githubId (returning user)
    let user = await User.findOne({ githubId: String(profile.id) });

    if (!user) {
      // Maybe they registered with email/password before — link accounts
      user = await User.findOne({ email });

      if (user) {
        // Link existing account to GitHub
        user.githubId          = String(profile.id);
        user.githubUsername    = profile.login;
        user.githubAccessToken = githubAccessToken;
        user.avatarUrl         = profile.avatar_url;
        user.authProvider      = 'github';
        await user.save();
      } else {
        // Brand new user — create from GitHub profile
        user = await User.create({
          name:              profile.name || profile.login,
          email,
          githubId:          String(profile.id),
          githubUsername:    profile.login,
          githubAccessToken,
          avatarUrl:         profile.avatar_url,
          authProvider:      'github',
          role:              'engineer',
        });
      }
    } else {
      // Returning GitHub user — refresh their access token and avatar
      user.githubAccessToken = githubAccessToken;
      user.avatarUrl         = profile.avatar_url;
      await user.save();
    }

    // ── STEP 4: Issue our own JWT ────────────────────────────
    const token = generateToken(user);

    res.status(200).json({
      success: true,
      token,
      user: {
        id:             user._id,
        name:           user.name,
        email:          user.email,
        role:           user.role,
        githubUsername: user.githubUsername,
        avatarUrl:      user.avatarUrl,
        authProvider:   user.authProvider,
      },
    });
  } catch (err) {
    next(err);
  }
};

// ── FUNCTION: getMe ──────────────────────────────────────────
// Route        : GET /api/auth/me
const getMe = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found.' });
    }
    res.status(200).json({
      success: true,
      user: {
        id:             user._id,
        name:           user.name,
        email:          user.email,
        role:           user.role,
        githubUsername: user.githubUsername,
        avatarUrl:      user.avatarUrl,
        authProvider:   user.authProvider,
      },
    });
  } catch (err) {
    next(err);
  }
};

module.exports = { registerUser, loginUser, githubCallback, getMe };
