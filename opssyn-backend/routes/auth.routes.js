const express = require('express');
const router  = express.Router();
const {
  register,
  login,
  getMe,
  githubOAuth,
  githubCallback,
} = require('../controllers/auth.controller');
const { protect } = require('../middleware/auth');

// Local auth
router.post('/register', register);
router.post('/login',    login);
router.get('/me',        protect, getMe);


router.get('/github',          githubOAuth);

router.get('/github/callback', githubCallback);

module.exports = router;
