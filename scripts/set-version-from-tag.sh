#!/bin/bash

# Script to set the project version from the latest git tag
# Usage: ./scripts/set-version-from-tag.sh [--force] [--increment-if-needed]

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

# Check for flags
FORCE=false
INCREMENT_IF_NEEDED=false
for arg in "$@"; do
  case $arg in
    --force)
      FORCE=true
      ;;
    --increment-if-needed)
      INCREMENT_IF_NEEDED=true
      ;;
  esac
done

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

# Function to compare version numbers
# Returns: 0 if equal, 1 if $1 > $2, 2 if $1 < $2
compare_versions() {
    if [[ "$1" == "$2" ]]; then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # Fill empty fields with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++)); do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++)); do
        if [[ -z ${ver2[i]} ]]; then
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]})); then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]})); then
            return 2
        fi
    done
    return 0
}

# Check if versions are the same
if [ "$CURRENT_VERSION" == "$VERSION" ] && [ "$FORCE" == "false" ]; then
    print_success "Version is already set to $VERSION. No changes needed."
    exit 0
fi

# If current version is greater than latest tag and --increment-if-needed is set, increment from latest tag
if [ "$INCREMENT_IF_NEEDED" == "true" ]; then
    compare_versions "$CURRENT_VERSION" "$VERSION"
    COMPARE_RESULT=$?
    
    if [ $COMPARE_RESULT -eq 1 ]; then
        print_status "Current version ($CURRENT_VERSION) is greater than latest tag ($VERSION)"
        print_status "Incrementing version from latest tag..."
        
        # Parse version numbers
        IFS='.' read -r -a version_parts <<< "$VERSION"
        major=${version_parts[0]}
        minor=${version_parts[1]}
        patch=${version_parts[2]}
        
        # Increment patch version
        patch=$((patch + 1))
        VERSION="$major.$minor.$patch"
        
        print_status "New version: $VERSION"
        
        # Check if the new version tag already exists
        while git rev-parse --verify "refs/tags/v$VERSION" >/dev/null 2>&1; do
            print_status "Tag v$VERSION already exists, incrementing patch version..."
            patch=$((patch + 1))
            VERSION="$major.$minor.$patch"
            print_status "New version: $VERSION"
        done
    elif [ $COMPARE_RESULT -eq 2 ]; then
        # Current version is less than latest tag - this is an error
        print_error "Current version ($CURRENT_VERSION) is less than latest tag ($VERSION)"
        print_error "This indicates a version mismatch. Please update the pom.xml version to be greater than or equal to the latest tag."
        print_error "Suggested version: $VERSION (or higher)"
        exit 1
    fi
fi

# Update pom.xml version
print_status "Updating pom.xml version to $VERSION..."
./mvnw versions:set -DnewVersion=$VERSION -q
./mvnw versions:commit -q

# Verify the update was successful
UPDATED_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
if [ "$UPDATED_VERSION" != "$VERSION" ]; then
    print_error "Failed to update version. Expected: $VERSION, Got: $UPDATED_VERSION"
    exit 1
fi

print_success "Version successfully updated to $VERSION"
print_status "You can now build the project with: mvn clean package"
