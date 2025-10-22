# STEP 1: Build the Next.js app (compile everything for production)
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package.json first so Docker can cache node_modules
# If package.json doesn't change, we skip reinstalling everything (huge time saver!)
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Now copy all the code and build it
COPY . .
RUN npm run build

# STEP 2: Create a smaller container with just what we need to run
# We don't need all the build tools anymore
FROM node:20-alpine
WORKDIR /app

# Copy over the built Next.js files from the previous step
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/public ./public

# Tell Docker this container listens on port 3000
EXPOSE 3000

# When the container starts, fire up the Next.js server
CMD ["npm", "start"]

