// config/firebase.js
// Initialises Firebase Admin SDK once and exports the messaging instance.
const admin = require('firebase-admin');
const path  = require('path');

// Only initialise once — prevents re-init errors on hot reload
if (!admin.apps.length) {
  const serviceAccount = require(path.join(__dirname, '../serviceAccountKey.json'));

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

module.exports = admin.messaging();
