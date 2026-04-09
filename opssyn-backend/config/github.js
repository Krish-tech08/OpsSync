// config/github.js
// Owner: DevOps (Arjun)
// Purpose: GitHub API base config. Attaches the token from .env to every request.

const axios = require('axios');

const githubClient = axios.create({
  baseURL: 'https://api.github.com',
  headers: {
    Authorization: `Bearer ${process.env.GITHUB_TOKEN}`,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
  },
});

module.exports = githubClient;