#!/bin/bash

# Script to set the project version from the latest git tag
# Usage: ./scripts/set-version-from-tag.sh [--force]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    print_error "This script must be run from the root of a git repository."
    exit 1
fi

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found. This script must be run from a Maven project root."
    exit 1
fi

# Check for --force flag
FORCE=false
if [ "$1" == "--force" ]; then
    FORCE=true
fi

# Get the latest tag
print_status "Fetching latest git tag..."
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$LATEST_TAG" ]; then
    print_error "No git tags found. Please create a tag first."
    print_status "You can create a tag with: git tag v1.0.0"
    exit 1
fi

print_status "Latest tag found: $LATEST_TAG"

# Remove 'v' prefix if present
VERSION=$(echo "$LATEST_TAG" | sed 's/^v//')

# Validate version format (should be X.Y.Z)
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid version format in tag: $VERSION"
    print_error "Tag should be in format vX.Y.Z or X.Y.Z"
    exit 1
fi

print_status "Extracted version: $VERSION"

# Get current version from pom.xml
CURRENT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)

if [ -z "$CURRENT_VERSION" ]; then
    print_error "Could not determine current version from pom.xml"
    exit 1
fi

print_status "Current version in pom.xml: $CURRENT_VERSION"

# Check if versions are the same
if [ "$CURRENT_VERSION" == "$VERSION" ] && [ "$FORCE" == "false" ]; then
    print_success "Version is already set to $VERSION. No changes needed."
    exit 0
fi

# Update pom.xml version
print_status "Updating pom.xml version to $VERSION..."
./mvnw versions:set -DnewVersion=$VERSION -q
./mvnw versions:commit -q

print_success "Version successfully updated to $VERSION"
print_status "You can now build the project with: mvn clean package"
