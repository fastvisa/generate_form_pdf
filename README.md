# PDF Manipulation API

A Spring Boot REST API for manipulating PDF forms and generating receipts using AWS S3 for file storage.

## 🚀 Quick Start

### Prerequisites
- Java 11
- Maven 3.6+
- Node.js and npm (for PM2 deployment)
- AWS S3 bucket and credentials

### Development Setup

1. **Clone the repository**
    ```bash
    git clone https://github.com/fastvisa/generate_form_pdf.git
    cd generate_form_pdf
    ```

2. **Configure environment**
    ```bash
    # Create environment file from template
    cp .env.template .env
    
    # Edit with your AWS credentials
    nano .env
    ```

3. **Run the application**
    ```bash
    mvn spring-boot:run
    ```

4. **Test the health endpoint**
    ```bash
    curl http://localhost:8080/health
    ```

## 🚀 Production Deployment

### Server Prerequisites

```bash
# Install Java 11
sudo apt update
sudo apt install -y openjdk-11-jdk

# Install Maven
sudo apt install -y maven

# Install Node.js and PM2
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g pm2
```

### Deployment Steps

1. **Clone and setup**
   ```bash
   git clone https://github.com/fastvisa/generate_form_pdf.git
   cd generate_form_pdf
   ```

2. **Configure environment**
   ```bash
   # Create environment file from template
   cp .env.template .env
   
   # Edit with your production values
   nano .env
   ```

3. **Deploy**
   ```bash
   # Make deploy script executable
   chmod +x deploy.sh
   
   # Run deployment
   ./deploy.sh
   ```

4. **Verify**
   ```bash
   # Check status
   pm2 status
   
   # Test health endpoint
   ./health-check.sh
   ```

## 📋 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check with version info |
| `/api/v1/fillform` | POST | Fill PDF forms with provided data |
| `/api/v1/combineform` | POST | Combine multiple PDF forms into one |
| `/api/v1/generate-receipt` | POST | Generate receipts in PDF format |

### Health Check Response
```json
{
  "service": "manipulate-pdf",
  "version": "0.0.1-SNAPSHOT",
  "status": "UP",
  "timestamp": 1758560545425
}
```

### Fill Form Request
```json
{
  "form_data": { /* form field data */ },
  "template_path": "path/to/template.pdf",
  "structure_inputs": { /* structure configuration */ }
}
```

### Combine Forms Request
```json
{
  "pdf_data": [
    "path/to/pdf1.pdf",
    "path/to/pdf2.pdf"
  ]
}
```

### Generate Receipt Request
```json
{
  "form_data": { /* receipt data */ },
  "output_name": "receipt-filename",
  "receipt_type": "standard"
}
```

### API Response Format
All POST endpoints return JSON with the processed file URL:
```json
{
  "form_data": { /* original data */ },
  "template_path": "template.pdf",
  "structure_inputs": { /* structure data */ },
  "url_download": "https://s3-bucket-url/generated-file.pdf"
}
```

## ⚙️ Configuration

**⚠️ Security Notice**: Never commit AWS credentials or other sensitive data to version control.

### Environment Variables

All configuration is loaded from the `.env` file. No dev/prod profiles are used.

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | Yes | AWS Access Key ID | - |
| `AWS_SECRET_ACCESS_KEY` | Yes | AWS Secret Access Key | - |
| `AWS_S3_BUCKET_NAME` | Yes | S3 bucket name | - |
| `AWS_S3_BUCKET_REGION` | No | S3 bucket region | `us-east-1` |
| `PORT` | No | Server port | `8080` |
| `LOG_LEVEL` | No | Logging level | `INFO` |
| `DEPLOY_USER` | No | Deployment user (for PM2 deploy) | `ubuntu` |
| `DEPLOY_HOST` | No | Deployment host (for PM2 deploy) | - |
| `DEPLOY_PATH` | No | Deployment path (for PM2 deploy) | `/var/www/generate_form_pdf` |

### Configuration Files

- `application.properties` - Base configuration (tracked in git)
- `.env.template` - Environment variables template (tracked in git)
- `.env` - Actual environment variables (git-ignored)

### AWS IAM Permissions

Create an IAM user with minimal required S3 permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject", 
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::your-bucket-name/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": "arn:aws:s3:::your-bucket-name"
        }
    ]
}
```

## 🔒 Security

This repository follows security best practices:

- ✅ No sensitive data in git history
- ✅ AWS credentials via environment variables only
- ✅ Sensitive files are git-ignored
- ✅ Configuration templates provided
- ✅ Security validation script included

### Security Validation

Run the security check before committing:

```bash
./security-check.sh
```

To automatically fix common issues:

```bash
./security-check.sh --fix
```

## ️ Development

### Building

```bash
# Compile and package
mvn clean package

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

### PM2 Management

```bash
# Check status
pm2 status

# View logs  
pm2 logs manipulate-pdf-api

# Restart
pm2 restart manipulate-pdf-api

# Stop
pm2 stop manipulate-pdf-api

# Monitor
pm2 monit
```

## 📊 Monitoring

The application includes:

- Health endpoint at `/health`
- Spring Boot Actuator endpoints
- PM2 process monitoring
- Structured logging

### Health Check

```bash
# Manual health check
curl http://localhost:8080/health

# Or use the health check script
./health-check.sh
```

## 📦 Automated Versioning System

This project uses an automated versioning system that ensures the build version always matches the latest released git tag.

### How It Works

#### Tag-Based Version Matching

The build process automatically sets the project version to match the latest git tag:

1. **Fetches the latest tag** from git (e.g., `v1.2.3`)
2. **Extracts the version** number (removes 'v' prefix)
3. **Updates pom.xml** with the extracted version
4. **Builds the application** with the matching version

This ensures consistency between git tags and the application version in all builds.

#### Automatic Versioning (GitHub Actions)

When a PR is merged to the `master` branch, a GitHub Action workflow automatically:

1. **Analyzes the changes** to determine the version bump type:
   - **Major bump** (X.0.0): PR title/commits contain "breaking", "major", or "BREAKING CHANGE"
   - **Minor bump** (X.Y.0): PR title/commits contain "feat", "feature", or "minor"
   - **Patch bump** (X.Y.Z): Default for all other changes

2. **Updates the version**:
   - Removes `-SNAPSHOT` from current version
   - Increments the appropriate version number
   - Updates `pom.xml` with the new version

3. **Creates a release**:
   - Commits the version change
   - Creates a git tag (e.g., `v1.2.3`)
   - Pushes the changes and tag
   - Verifies version consistency
   - Builds the project

#### Manual Version Setting

You can manually set the version from the latest tag:

```bash
# Set version from latest tag
./scripts/set-version-from-tag.sh

# Force update even if versions match
./scripts/set-version-from-tag.sh --force
```

#### Manual Version Bumping

You can also bump versions manually using the provided script:

```bash
# Patch bump (1.0.0 -> 1.0.1) - default
./scripts/bump-version.sh

# Minor bump (1.0.0 -> 1.1.0)
./scripts/bump-version.sh minor

# Major bump (1.0.0 -> 2.0.0)
./scripts/bump-version.sh major
```

The script will:
- Validate the current state (no uncommitted changes)
- Update the pom.xml version
- Run tests to ensure everything works
- Commit the change and create a tag
- Optionally push the changes

### Version Naming Convention

- **Format**: `X.Y.Z` (semantic versioning)
- **Tags**: `vX.Y.Z` (e.g., `v1.2.3`)
- **Development**: Versions end with `-SNAPSHOT` during development

### PR Guidelines for Automatic Versioning

To control automatic version bumping, use these keywords in your PR title or commit messages:

- **Major release**: Include "breaking", "major", or "BREAKING CHANGE"
- **Minor release**: Include "feat", "feature", or "minor"
- **Patch release**: Default behavior, no special keywords needed

#### Examples

**PR Titles that trigger different version bumps:**

- `"Fix login bug"` → Patch bump (1.0.0 → 1.0.1)
- `"feat: Add new user dashboard"` → Minor bump (1.0.0 → 1.1.0)
- `"BREAKING CHANGE: Redesign API endpoints"` → Major bump (1.0.0 → 2.0.0)

**Current Version**

The current version can always be found in:
- `pom.xml` - The project version
- Git tags - All released versions
- GitHub releases - Published versions with artifacts

### Build Integration

The version matching system is integrated into all build processes:

- **[`deploy.sh`](deploy.sh)** - Production deployment
- **[`start-app.sh`](start-app.sh)** - Local development
- **[`entrypoint.sh`](entrypoint.sh)** - Docker containers
- **[`.github/workflows/version-and-release.yml`](.github/workflows/version-and-release.yml)** - CI/CD

## 🤝 Contributing

1. Ensure no sensitive data is committed
2. Run security check: `./security-check.sh`
3. Test all configurations work
4. Follow existing code style
5. Update documentation as needed
6. Use appropriate PR titles for version bumping (see versioning section above)

## 🔧 Troubleshooting

### Common Issues

1. **AWS credentials error**
    ```bash
    # Check environment variables are set
    echo $AWS_ACCESS_KEY_ID
    
    # Or check .env file exists
    cat .env
    ```

2. **Port already in use**
    ```bash
    # Change port in .env
    nano .env  # Set PORT=8081
    ```

3. **Maven build fails**
    ```bash
    # Clean and rebuild
    mvn clean compile
    ```

4. **S3 bucket access denied**
    ```
    ERROR: Access Denied (Service: Amazon S3; Status Code: 403)
    ```
    **Solution**: Check IAM permissions and bucket name configuration.

## 📄 License

This project is licensed under the AGPL-3.0 License - see the LICENSE file for details.

This project uses iText, which is licensed under the AGPLv3. If you use this software in a way that provides it to users over a network, you must provide the source code of your application to those users.