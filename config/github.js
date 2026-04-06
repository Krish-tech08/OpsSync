// ─────────────────────────────────────────
//  config/github.js  –  GitHub API client
//
//  Creates a pre-configured Axios instance
//  for the GitHub REST API.  All requests
//  include the auth token and correct headers.
//
//  Usage:
//    const githubClient = require('./config/github');
//    const { data } = await githubClient.get('/runs');
// ─────────────────────────────────────────

const axios = require('axios');

const githubClient = axios.create({
  // Base URL scoped to the owner/repo defined in .env
  baseURL: `https://api.github.com/repos/${process.env.GITHUB_OWNER}/${process.env.GITHUB_REPO}/actions`,

  headers: {
    Authorization: `Bearer ${process.env.GITHUB_TOKEN}`,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
  },
});

module.exports = githubClient;
