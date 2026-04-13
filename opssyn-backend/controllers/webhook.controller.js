const crypto  = require('crypto');
const User    = require('../models/User');
const Incident = require('../models/Incident');

// ✅ Correct verifySignature (FIXED)

  // Skip verification for demo
 const verifySignature = (req) => {
  const secret = process.env.GITHUB_WEBHOOK_SECRET;

  console.log("🔥 Secret:", secret);

  // ✅ Force skip if no valid secret
  if (!secret || secret.trim() === "") {
    return true;
  }

  const signature = req.headers['x-hub-signature-256'];

  console.log("🔥 Signature:", signature);

  if (!signature) return false;

  const payload = Buffer.isBuffer(req.body)
    ? req.body
    : Buffer.from(JSON.stringify(req.body));

  const hmac = crypto.createHmac('sha256', secret);
  const digest = 'sha256=' + hmac.update(payload).digest('hex');

  return signature === digest;
};

const handleGitHubWebhook = async (req, res, next) => {
  try {
    if (!verifySignature(req)) {
      return res.status(401).json({ success: false, message: 'Invalid webhook signature' });
    }

    const event   = req.headers['x-github-event'];
 const payload = Buffer.isBuffer(req.body)
      ? JSON.parse(req.body.toString())
      : req.body;
    // 2. We only care about workflow_run events
    if (event !== 'workflow_run') {
      return res.status(200).json({ success: true, message: `Event ${event} ignored` });
    }

    const run    = payload.workflow_run;
    const repo   = payload.repository;
    const sender = payload.sender;

    // 3. Only act on completed + failed runs
    if (run.conclusion !== 'failure') {
      return res.status(200).json({ success: true, message: 'Run not failed, ignored' });
    }

    // 4. Find the user who owns this repo by GitHub username
    // Match sender login OR repo owner login to a user in our DB
    const githubLogin = repo.owner.login;
    const user = await User.findOne({
      $or: [
        { githubUsername: githubLogin },
        { githubUsername: sender.login },
      ]
    });

    if (!user) {
      // User not in our system — still return 200 so GitHub doesn't retry
      console.log(`Webhook: No user found for GitHub login ${githubLogin}`);
      return res.status(200).json({ success: true, message: 'User not found, ignored' });
    }

    // 5. Avoid duplicate incidents for the same run
    const existingIncident = await Incident.findOne({
      'metadata.runId': run.id.toString()
    });
    if (existingIncident) {
      return res.status(200).json({ success: true, message: 'Incident already exists for this run' });
    }

    // 6. Determine priority based on branch + workflow name
    let priority = 'high';
    if (run.head_branch === 'main' || run.head_branch === 'master') {
      priority = 'critical'; // main branch failures are critical
    } else if (run.name?.toLowerCase().includes('security')) {
      priority = 'critical';
    } else if (run.name?.toLowerCase().includes('deploy')) {
      priority = 'high';
    } else {
      priority = 'medium';
    }

    // 7. Create the incident
    const incident = await Incident.create({
      title:       `Pipeline Failed: ${run.name} on ${repo.name}`,
      description: `Workflow "${run.name}" failed on branch "${run.head_branch}".\n\nRepo: ${repo.full_name}\nRun #${run.run_number}\nCommit: ${run.head_sha?.slice(0, 7)}\nTriggered by: ${run.triggering_actor?.login || 'unknown'}\n\nView run: ${run.html_url}`,
      priority,
      status:      'open',
      service:     `${repo.name}/${run.name}`,
      region:      'github-actions',
      duration:    '—',
      assignedTo:  user._id,
      metadata: {
        runId:    run.id.toString(),
        repoName: repo.full_name,
        branch:   run.head_branch,
        runUrl:   run.html_url,
      }
    });
const { notifyPipelineInternal } = require('./notification.controller');
notifyPipelineInternal(
  user._id,
  run.name ?? 'Unnamed Run',
  'FAILED',
  repo.full_name,
  run.id
);
    console.log(`✅ Incident created: ${incident.title}`);
    res.status(200).json({ success: true, data: incident });

  } catch (err) {
    // Always return 200 to GitHub even on error — prevent infinite retries
    console.error('Webhook error:', err.message);
    res.status(200).json({ success: true, message: 'Webhook received' });
  }
};

module.exports = { handleGitHubWebhook };
