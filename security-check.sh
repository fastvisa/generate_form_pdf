#!/bin/bash

# Security validation script
echo "🔒 Security Validation for manipulate-pdf API"
echo "============================================="
echo ""

# Check if sensitive files are properly excluded
echo "🔍 Checking .gitignore configuration..."

if git check-ignore src/main/resources/application-dev.properties >/dev/null 2>&1; then
    echo "✅ application-dev.properties is properly ignored"
else
    echo "❌ application-dev.properties is NOT ignored - this is a security risk!"
fi

if git check-ignore src/main/resources/application-prod.properties >/dev/null 2>&1; then
    echo "✅ application-prod.properties is properly ignored"
else
    echo "❌ application-prod.properties is NOT ignored - this is a security risk!"
fi

if git check-ignore .env >/dev/null 2>&1; then
    echo "✅ .env file is properly ignored"
else
    echo "❌ .env file is NOT ignored - this is a security risk!"
fi

echo ""
echo "🔍 Checking for sensitive data in tracked files..."

# Check if any AWS credentials are in tracked files
if git ls-files | xargs grep -l "AKIA" 2>/dev/null; then
    echo "❌ Found AWS access keys in tracked files!"
    git ls-files | xargs grep -n "AKIA" 2>/dev/null || true
else
    echo "✅ No AWS access keys found in tracked files"
fi

# Check for hardcoded secrets
if git ls-files | xargs grep -l "aws\.accessKey=" 2>/dev/null; then
    echo "❌ Found hardcoded AWS access key configurations!"
    git ls-files | xargs grep -n "aws\.accessKey=" 2>/dev/null || true
    echo "💡 These should use environment variables: \${AWS_ACCESS_KEY}"
else
    echo "✅ No hardcoded AWS configurations found"
fi

echo ""
echo "🔍 Checking configuration files..."

if [ -f "src/main/resources/application-dev.properties.example" ]; then
    echo "✅ Development configuration example exists"
else
    echo "❌ Missing development configuration example"
fi

if [ -f "src/main/resources/application-prod.properties.example" ]; then
    echo "✅ Production configuration example exists"
else
    echo "❌ Missing production configuration example"
fi

if [ -f ".env.template" ]; then
    echo "✅ Environment template exists"
else
    echo "❌ Missing environment template"
fi

if [ -f "README.md" ]; then
    echo "✅ Documentation exists"
else
    echo "❌ Missing documentation"
fi

echo ""
echo "🔍 Checking environment setup..."

# Check if production config exists (it should be created from example)
if [ -f "src/main/resources/application-prod.properties" ]; then
    echo "📄 Production config exists - checking for environment variables..."
    if grep -q "\${AWS_ACCESS_KEY}" src/main/resources/application-prod.properties; then
        echo "✅ Production config uses environment variables"
    else
        echo "❌ Production config may contain hardcoded values"
    fi
else
    echo "⚠️  Production config not found (will be created from example)"
fi

echo ""
echo "📋 Security Checklist:"
echo "====================="
echo ""
echo "Before committing to public repository:"
echo "- [ ] No AWS credentials in git history"
echo "- [ ] All sensitive files are in .gitignore"
echo "- [ ] Configuration examples are provided"  
echo "- [ ] Environment variables are documented"
echo "- [ ] Production uses environment variables only"
echo ""
echo "Before deployment:"
echo "- [ ] AWS credentials are set as environment variables"
echo "- [ ] IAM user has minimal required permissions"
echo "- [ ] .env file is properly configured"
echo "- [ ] Health endpoint doesn't expose sensitive data"
echo ""

if [ "$1" = "--fix" ]; then
    echo "🔧 Running automatic fixes..."
    
    # Ensure sensitive files exist in gitignore
    if ! git check-ignore src/main/resources/application-dev.properties >/dev/null 2>&1; then
        echo "src/main/resources/application-dev.properties" >> .gitignore
        echo "✅ Added application-dev.properties to .gitignore"
    fi
    
    if ! git check-ignore src/main/resources/application-prod.properties >/dev/null 2>&1; then
        echo "src/main/resources/application-prod.properties" >> .gitignore  
        echo "✅ Added application-prod.properties to .gitignore"
    fi
    
    if ! git check-ignore .env >/dev/null 2>&1; then
        echo ".env" >> .gitignore
        echo "✅ Added .env to .gitignore"
    fi
    
    echo "🎉 Automatic fixes applied!"
fi

echo ""
echo "💡 For more information, see README.md"