#!/bin/bash

set -e

echo "🚀 Starting deployment of Tracking Number Generator API..."

# Configuration
APP_NAME="tracking-number-api"
DOCKER_IMAGE="tracking-api:latest"
NETWORK_NAME="tracking-network"
EXTERNAL_PORT=31257
INTERNAL_PORT=8080

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null && ! command -v docker compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Use docker compose or docker-compose based on availability
if command -v docker compose &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

# Clean up previous deployment
print_status "Cleaning up previous deployment..."
$DOCKER_COMPOSE down --remove-orphans 2>/dev/null || true

# Build the application
print_status "Building application..."
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found. Make sure you're in the project root directory."
    exit 1
fi

# Run tests
print_status "Running tests..."
mvn clean test

# Build Docker image
print_status "Building Docker image..."
docker build -t $DOCKER_IMAGE .

# Start services
print_status "Starting services with Docker Compose..."
$DOCKER_COMPOSE up -d

# Wait for services to be healthy
print_status "Waiting for services to start..."
sleep 45

# Check service health
print_status "Checking service health..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s http://localhost:${EXTERNAL_PORT}/actuator/health > /dev/null; then
        print_status "✅ Application is healthy!"
        break
    else
        print_warning "Attempt $attempt/$max_attempts: Application not ready yet..."
        sleep 10
        ((attempt++))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    print_error "❌ Application failed to start within expected time"
    print_status "Checking logs..."
    $DOCKER_COMPOSE logs app
    exit 1
fi

# Test the API
print_status "Testing API endpoint..."
test_url="http://localhost:${EXTERNAL_PORT}/api/v1/tracking/next-tracking-number"
test_params="origin_country_id=MY&destination_country_id=ID&weight=1.234&created_at=2023-11-20T19:29:32%2B08:00&customer_id=de619854-b59b-425e-9db4-943979e1bd49&customer_name=Test%20Customer&customer_slug=test-customer"

response=$(curl -s "$test_url?$test_params")
if echo "$response" | grep -q "trackingNumber"; then
    print_status "✅ API test successful!"
    echo "Sample response: $response"
else
    print_error "❌ API test failed!"
    echo "Response: $response"
    print_status "Checking application logs..."
    $DOCKER_COMPOSE logs app | tail -20
fi

# Display deployment information
print_status "🎉 Deployment completed successfully!"
echo ""
echo "📋 Service Information:"
echo "  • Application URL: http://localhost:${EXTERNAL_PORT}"
echo "  • Health Check: http://localhost:${EXTERNAL_PORT}/actuator/health"
echo "  • API Documentation: http://localhost:${EXTERNAL_PORT}/actuator/info"
echo "  • MongoDB: localhost:27017"
echo "  • Redis: localhost:6379"
echo ""
echo "🧪 Test the API:"
echo "curl \"${test_url}?${test_params}\""
echo ""
echo "📊 Monitor services:"
echo "$DOCKER_COMPOSE logs -f app    # Application logs"
echo "$DOCKER_COMPOSE ps             # Service status"
echo "$DOCKER_COMPOSE down           # Stop services"