// ─────────────────────────────────────────
//  config/db.js  –  MongoDB connection
//
//  Uses Mongoose to connect to the database
//  defined in MONGO_URI.  Exits the process
//  if the connection fails (fail-fast pattern
//  – better to crash loudly than run broken).
// ─────────────────────────────────────────

const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const conn = await mongoose.connect(process.env.MONGO_URI);
    console.log(`✅  MongoDB connected: ${conn.connection.host}`);
  } catch (error) {
    console.error(`❌  MongoDB connection error: ${error.message}`);
    process.exit(1); // Exit with failure code
  }
};

module.exports = connectDB;
