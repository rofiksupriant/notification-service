#!/bin/bash
# Build and Push Docker Image Script
# Usage: ./build-and-push.sh your_username 1.0.0

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Functions for colored output
print_header() {
    echo -e "${CYAN}"
    echo "======================================================================"
    echo "$1"
    echo "======================================================================"
    echo -e "${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

# Check arguments
if [ $# -lt 1 ]; then
    print_error "Usage: $0 <docker_username> [version]"
    echo "Example: $0 rofiksupriant 1.0.0"
    exit 1
fi

DOCKER_USERNAME=$1
VERSION=${2:-latest}
IMAGE_NAME="${DOCKER_USERNAME}/notification-service"
FULL_IMAGE="${IMAGE_NAME}:${VERSION}"

print_header "Docker Build & Push Script"

# Ensure we're in the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Check if we're in a subdirectory (like docker/)
if [ -f "$SCRIPT_DIR/pom.xml" ]; then
    PROJECT_ROOT="$SCRIPT_DIR"
    print_info "Running from project root: $PROJECT_ROOT"
elif [ -f "$SCRIPT_DIR/../pom.xml" ]; then
    PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
    print_info "Changing to project root: $PROJECT_ROOT"
    cd "$PROJECT_ROOT" || exit 1
else
    print_error "Cannot find pom.xml. Please run this script from the project root directory."
    exit 1
fi

# Validate Docker is installed
print_info "Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi
print_success "Docker found"

# Validate Maven is installed
print_info "Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi
print_success "Maven found"

print_info "Image: ${FULL_IMAGE}"
print_info "Version: ${VERSION}"

# Step 1: Clean and compile
print_header "Step 1/4: Cleaning and Compiling Project"
print_info "Running Maven clean install..."

if mvn clean install -DskipTests -q; then
    print_success "Project compiled successfully"
else
    print_error "Compilation failed"
    exit 1
fi

# Step 2: Build Docker image
print_header "Step 2/4: Building Docker Image"
print_info "Building image: ${FULL_IMAGE}"

if docker build -t "${FULL_IMAGE}" -t "${IMAGE_NAME}:latest" .; then
    print_success "Docker image built successfully"
else
    print_error "Docker build failed"
    exit 1
fi

# Step 3: Verify image
print_header "Step 3/4: Verifying Docker Image"
print_info "Checking image size and details..."

IMAGE_SIZE=$(docker images --filter "reference=${FULL_IMAGE}" --format "{{.Size}}")
if [ -n "${IMAGE_SIZE}" ]; then
    print_success "Image verified: Size = ${IMAGE_SIZE}"
else
    print_error "Image not found after build"
    exit 1
fi

# Step 4: Push to Docker Hub
print_header "Step 4/4: Pushing to Docker Hub"
print_info "Pushing: ${FULL_IMAGE}"
print_warning "Make sure you're logged in: docker login"

if docker push "${FULL_IMAGE}"; then
    print_success "Image pushed successfully"
else
    print_error "Docker push failed"
    print_warning "Make sure you're logged in to Docker Hub: docker login"
    exit 1
fi

if docker push "${IMAGE_NAME}:latest"; then
    print_success "Latest tag pushed successfully"
else
    print_warning "Failed to push latest tag (non-critical)"
fi

# Final summary
print_header "✓ Build & Push Complete!"

cat << EOF

${GREEN}Your Docker image is now available on Docker Hub:${NC}

  Repository: https://hub.docker.com/r/${DOCKER_USERNAME}/notification-service
  Image:      ${FULL_IMAGE}
  Size:       ${IMAGE_SIZE}

${CYAN}Next steps:${NC}

1. On your server, copy the deployment files:
   - docker/docker-compose.prod.yml
   - .env.example → .env (fill in your secrets)

2. SSH to server and run:
   docker compose -f docker-compose.prod.yml --env-file .env up -d

3. Verify deployment:
   curl http://localhost:8080/actuator/health

${YELLOW}For detailed instructions, see: docs/deployment/WORKFLOW.md${NC}

EOF

exit 0
