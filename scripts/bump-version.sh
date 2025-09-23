#!/bin/bash

# Version bump script for manipulate-pdf project
# Usage: ./scripts/bump-version.sh [major|minor|patch]
# If no argument is provided, defaults to patch

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

# Get current version from pom.xml
print_status "Getting current version from pom.xml..."
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)

if [ -z "$CURRENT_VERSION" ]; then
    print_error "Could not determine current version from pom.xml"
    exit 1
fi

print_status "Current version: $CURRENT_VERSION"

# Remove -SNAPSHOT if present
BASE_VERSION=$(echo $CURRENT_VERSION | sed 's/-SNAPSHOT//')

# Parse version numbers
IFS='.' read -r -a version_parts <<< "$BASE_VERSION"

if [ ${#version_parts[@]} -ne 3 ]; then
    print_error "Version format must be X.Y.Z, got: $BASE_VERSION"
    exit 1
fi

major=${version_parts[0]}
minor=${version_parts[1]}
patch=${version_parts[2]}

# Validate version numbers
if ! [[ "$major" =~ ^[0-9]+$ ]] || ! [[ "$minor" =~ ^[0-9]+$ ]] || ! [[ "$patch" =~ ^[0-9]+$ ]]; then
    print_error "Version parts must be numeric. Got: major=$major, minor=$minor, patch=$patch"
    exit 1
fi

# Determine bump type
BUMP_TYPE=${1:-patch}

case $BUMP_TYPE in
    major)
        major=$((major + 1))
        minor=0
        patch=0
        ;;
    minor)
        minor=$((minor + 1))
        patch=0
        ;;
    patch)
        patch=$((patch + 1))
        ;;
    *)
        print_error "Invalid bump type: $BUMP_TYPE. Must be 'major', 'minor', or 'patch'"
        exit 1
        ;;
esac

NEW_VERSION="$major.$minor.$patch"

print_status "Bumping version from $BASE_VERSION to $NEW_VERSION ($BUMP_TYPE bump)"

# Check for uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
    print_warning "You have uncommitted changes. Please commit or stash them first."
    git status --short
    exit 1
fi

# Update pom.xml version
print_status "Updating pom.xml version to $NEW_VERSION..."
mvn versions:set -DnewVersion=$NEW_VERSION -q
mvn versions:commit -q

# Run tests to ensure everything still works
print_status "Running tests..."
if ! mvn clean test -q; then
    print_error "Tests failed. Rolling back version change..."
    git checkout pom.xml
    exit 1
fi

# Commit the version change
print_status "Committing version change..."
git add pom.xml
git commit -m "chore: bump version to $NEW_VERSION"

# Create and push tag
print_status "Creating tag v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Release version $NEW_VERSION"

print_success "Version successfully bumped to $NEW_VERSION"
print_status "To push the changes and tag, run:"
print_status "  git push origin $(git branch --show-current)"
print_status "  git push origin v$NEW_VERSION"

# Ask if user wants to push automatically
read -p "Do you want to push the changes and tag now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    CURRENT_BRANCH=$(git branch --show-current)
    print_status "Pushing changes to $CURRENT_BRANCH..."
    git push origin $CURRENT_BRANCH
    
    print_status "Pushing tag v$NEW_VERSION..."
    git push origin "v$NEW_VERSION"
    
    print_success "All changes pushed successfully!"
else
    print_warning "Remember to push your changes and tag when ready!"
fi