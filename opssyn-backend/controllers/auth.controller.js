const jwt      = require('jsonwebtoken');
const axios    = require('axios');
const User     = require('../models/User');
const jwtConfig = require('../config/jwt');

// ── Helper: sign a JWT ───────────────────────────────────────
const signToken = (user) =>
  jwt.sign(
    { id: user._id, email: user.email, role: user.role },
    jwtConfig.secret,
    { expiresIn: jwtConfig.expiresIn }
  );

// ── POST /api/auth/register ──────────────────────────────────
const register = async (req, res, next) => {
  try {
    const { name, email, password } = req.body;
    const existing = await User.findOne({ email });
    if (existing) {
      return res.status(400).json({ success: false, message: 'Email already in use.' });
    }
    const user  = await User.create({ name, email, password, authProvider: 'local' });
    const token = signToken(user);
    res.status(201).json({
      success: true,
      data: { token, user: { _id: user._id, name: user.name, email: user.email, role: user.role } },
    });
  } catch (err) {
    next(err);
  }
};

// ── POST /api/auth/login ─────────────────────────────────────
const login = async (req, res, next) => {
  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email }).select('+password');
    if (!user || !(await user.comparePassword(password))) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }
    const token = signToken(user);
    res.status(200).json({
      success: true,
      data: { token, user: { _id: user._id, name: user.name, email: user.email, role: user.role } },
    });
  } catch (err) {
    next(err);
  }
};

// ── GET /api/auth/me ─────────────────────────────────────────
const getMe = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id);
    res.status(200).json({ success: true, data: user });
  } catch (err) {
    next(err);
  }
};

// ── GET /api/auth/github ─────────────────────────────────────
// Returns the GitHub OAuth authorization URL.
// Android opens this URL in a Custom Tab / browser.
const githubOAuth = (req, res) => {
  const params = new URLSearchParams({
    client_id:    process.env.GITHUB_CLIENT_ID,
    redirect_uri: process.env.GITHUB_CALLBACK_URL,
    scope:        'read:user user:email repo',
  });
  const githubUrl = `https://github.com/login/oauth/authorize?${params}`;
  res.status(200).json({ success: true, data: { url: githubUrl } });
};

// ── GET /api/auth/github/callback ────────────────────────────
// GitHub redirects here with ?code=...
// We exchange code → access token → fetch GitHub user → upsert in DB
// Then redirect back to the Android app via deep link with JWT
const githubCallback = async (req, res, next) => {
  try {
    const { code } = req.query;
    if (!code) {
      return res.status(400).json({ success: false, message: 'No code provided by GitHub.' });
    }

    // 1. Exchange code for GitHub access token
    const tokenRes = await axios.post(
      'https://github.com/login/oauth/access_token',
      {
        client_id:     process.env.GITHUB_CLIENT_ID,
        client_secret: process.env.GITHUB_CLIENT_SECRET,
        code,
        redirect_uri:  process.env.GITHUB_CALLBACK_URL,
      },
      { headers: { Accept: 'application/json' } }
    );

    const githubAccessToken = tokenRes.data.access_token;
    if (!githubAccessToken) {
      return res.status(400).json({ success: false, message: 'Failed to get GitHub access token.' });
    }

    // 2. Fetch GitHub user profile
    const userRes = await axios.get('https://api.github.com/user', {
      headers: { Authorization: `Bearer ${githubAccessToken}` },
    });
    const githubUser = userRes.data;

    // 3. Fetch primary email if not public
    let email = githubUser.email;
    if (!email) {
      const emailRes = await axios.get('https://api.github.com/user/emails', {
        headers: { Authorization: `Bearer ${githubAccessToken}` },
      });
      const primary = emailRes.data.find((e) => e.primary && e.verified);
      email = primary?.email;
    }

    if (!email) {
      return res.redirect(
        `${process.env.FRONTEND_DEEP_LINK}?error=No verified email on your GitHub account.`
      );
    }

    // 4. Upsert user — find by githubId or email
    let user = await User.findOne({ githubId: String(githubUser.id) });
    if (!user) {
      user = await User.findOne({ email });
    }

    if (user) {
      // Existing user — update GitHub fields
      user.githubId          = String(githubUser.id);
      user.githubUsername    = githubUser.login;
      user.githubAccessToken = githubAccessToken;
      user.avatarUrl         = githubUser.avatar_url;
      user.authProvider      = 'github';
      await user.save();
    } else {
      // New user — create from GitHub profile
      user = await User.create({
        name:              githubUser.name || githubUser.login,
        email,
        githubId:          String(githubUser.id),
        githubUsername:    githubUser.login,
        githubAccessToken,
        avatarUrl:         githubUser.avatar_url,
        authProvider:      'github',
      });
    }

    // 5. Sign our own JWT
    const token = signToken(user);

    // 6. Redirect back to Android app via deep link
    res.redirect(`${process.env.FRONTEND_DEEP_LINK}?token=${token}`);
  } catch (err) {
    next(err);
  }
};

module.exports = { register, login, getMe, githubOAuth, githubCallback };
