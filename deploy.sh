#!/bin/bash

# Production deployment script for manipulate-pdf API
set -e

echo "🚀 Starting production deployment of manipulate-pdf API..."

# Check if PM2 is installed
if ! command -v pm2 &> /dev/null; then
    echo "❌ PM2 not found. Installing PM2..."
    npm install -g pm2
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install OpenJDK 21 or higher"
    exit 1
fi

# Build the application
echo "📦 Building application..."
mvn clean package -DskipTests

# Get the version from pom.xml
APP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
if [ -z "$APP_VERSION" ]; then
    echo "❌ Could not determine application version from pom.xml"
    exit 1
fi

echo "🔍 Application version: $APP_VERSION"

# Check if JAR file exists
JAR_FILE="target/manipulate-pdf-${APP_VERSION}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found: $JAR_FILE"
    echo "   Build may have failed!"
    exit 1
fi

echo "✅ JAR file found: $JAR_FILE"

# Create logs directory if it doesn't exist
mkdir -p logs

# Stop existing PM2 processes
echo "🛑 Stopping existing processes..."
pm2 stop manipulate-pdf-api 2>/dev/null || true
pm2 delete manipulate-pdf-api 2>/dev/null || true

# Check if required environment variables are set
echo "🔍 Checking environment variables..."
missing_vars=()

if [ -z "$AWS_ACCESS_KEY" ]; then
    missing_vars+=("AWS_ACCESS_KEY")
fi

if [ -z "$AWS_SECRET_KEY" ]; then
    missing_vars+=("AWS_SECRET_KEY")
fi

if [ -z "$AWS_S3_BUCKET_NAME" ]; then
    missing_vars+=("AWS_S3_BUCKET_NAME")
fi

if [ ${#missing_vars[@]} -ne 0 ]; then
    echo "❌ Missing required environment variables:"
    printf '   %s\n' "${missing_vars[@]}"
    echo ""
    echo "💡 Set them with:"
    echo "   export AWS_ACCESS_KEY=your_access_key"
    echo "   export AWS_SECRET_KEY=your_secret_key" 
    echo "   export AWS_S3_BUCKET_NAME=your_bucket_name"
    echo ""
    echo "🔧 Or create a .env file from .env.template"
    exit 1
fi

echo "✅ All required environment variables are set"

# Load .env file if it exists
if [ -f ".env" ]; then
    echo "📄 Loading .env file..."
    export $(cat .env | grep -v '^#' | xargs)
fi

# Set environment variables for production (if not already set)
export NODE_ENV=production
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Start the application with PM2
echo "▶️  Starting application with PM2..."
pm2 start ecosystem.config.js --env production

# Save PM2 configuration
pm2 save

# Setup PM2 startup script
pm2 startup

echo "✅ Application deployed successfully!"
echo "📊 Check status: pm2 status"
echo "📋 View logs: pm2 logs manipulate-pdf-api"
echo "🔄 Restart: pm2 restart manipulate-pdf-api"
echo "🛑 Stop: pm2 stop manipulate-pdf-api"
echo "🌐 Health check: curl http://localhost:8080/health"