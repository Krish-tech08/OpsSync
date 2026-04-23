# Use lightweight Node image for smaller size
FROM node:18

# Set working directory inside container
WORKDIR /app

# Copy package files to install dependencies first (for better caching)
COPY package*.json ./

# Install the dependencies
RUN npm install

# Copy rest of project
COPY . .

# Expose port for container networking
EXPOSE 5000

# Start server
CMD ["node", "server.js"]
