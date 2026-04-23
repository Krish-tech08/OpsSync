# Use lightweight Node image for smaller size
FROM node:18

# Set working directory inside container
WORKDIR /app

# Copy package files first
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy rest of project
COPY . .

# Expose port
EXPOSE 5000

# Start server
CMD ["node", "server.js"]
