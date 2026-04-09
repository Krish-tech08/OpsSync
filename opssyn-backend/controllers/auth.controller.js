// controllers/auth.controller.js

const jwt       = require('jsonwebtoken');
const axios     = require('axios');
const User      = require('../models/User');
const jwtConfig = require('../config/jwt');

const generateToken = (user) => {
  return jwt.sign(
    { id: user._id, email: user.email, role: user.role },
    jwtConfig.secret,
    { expiresIn: jwtConfig.expiresIn }
  );
};

// ── POST /api/auth/register ──────────────────────────────────
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
      data: {
        token,
        user: { _id: user._id, name: user.name, email: user.email, role: user.role },
      },
    });
  } catch (err) {
    next(err);
  }
};

// ── POST /api/auth/login ─────────────────────────────────────
const loginUser = async (req, res, next) => {
  try {
    const { email, password } = req.body;

    // ── DEBUG: remove after confirming login works ───────────
    console.log('🔐 Login attempt  :', { email, password });

    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required.' });
    }

    const user = await User.findOne({ email }).select('+password');

    // ── DEBUG ────────────────────────────────────────────────
    console.log('👤 User found     :', user ? 'YES' : 'NO');
    console.log('🔑 Stored hash    :', user?.password ?? 'N/A');
    console.log('🏷  Auth provider  :', user?.authProvider ?? 'N/A');

    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    if (user.authProvider === 'github') {
      return res.status(401).json({
        success: false,
        message: 'This account uses GitHub login. Please sign in with GitHub.',
      });
    }

    const isMatch = await user.comparePassword(password);

    // ── DEBUG ────────────────────────────────────────────────
    console.log('✅ Password match  :', isMatch);

    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    const token = generateToken(user);

    res.status(200).json({
      success: true,
      data: {
        token,
        user: { _id: user._id, name: user.name, email: user.email, role: user.role },
      },
    });
  } catch (err) {
    // ── DEBUG ──────────────────────────────────────────────
    console.error('❌ Login error:', err);
    next(err);
  }
};

// ── POST /api/auth/github ────────────────────────────────────
const githubCallback = async (req, res, next) => {
  try {
    const { code } = req.body;
    if (!code) {
      return res.status(400).json({ success: false, message: 'GitHub OAuth code is required.' });
    }

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
      return res.status(401).json({ success: false, message: 'GitHub OAuth failed.' });
    }

    const [profileRes, emailsRes] = await Promise.all([
      axios.get('https://api.github.com/user', {
        headers: { Authorization: `Bearer ${githubAccessToken}` },
      }),
      axios.get('https://api.github.com/user/emails', {
        headers: { Authorization: `Bearer ${githubAccessToken}` },
      }),
    ]);

    const profile         = profileRes.data;
    const primaryEmailObj = emailsRes.data.find((e) => e.primary && e.verified);
    const email           = primaryEmailObj?.email || emailsRes.data[0]?.email;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'No verified email found on your GitHub account.',
      });
    }

    let user = await User.findOne({ githubId: String(profile.id) });

    if (!user) {
      user = await User.findOne({ email });
      if (user) {
        user.githubId          = String(profile.id);
        user.githubUsername    = profile.login;
        user.githubAccessToken = githubAccessToken;
        user.avatarUrl         = profile.avatar_url;
        user.authProvider      = 'github';
        await user.save();
      } else {
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
      user.githubAccessToken = githubAccessToken;
      user.avatarUrl         = profile.avatar_url;
      await user.save();
    }

    const token = generateToken(user);

    res.status(200).json({
      success: true,
      data: {
        token,
        user: {
          _id:            user._id,
          name:           user.name,
          email:          user.email,
          role:           user.role,
          githubUsername: user.githubUsername,
          avatarUrl:      user.avatarUrl,
          authProvider:   user.authProvider,
        },
      },
    });
  } catch (err) {
    next(err);
  }
};

// ── GET /api/auth/me ─────────────────────────────────────────
const getMe = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found.' });
    }
    res.status(200).json({
      success: true,
      data: {
        _id:            user._id,
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
