# PDF Manipulation API

A Spring Boot REST API for manipulating PDF forms and generating receipts using AWS S3 for file storage.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Node.js and npm (for PM2 deployment)
- AWS S3 bucket and credentials

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/fastvisa/generate_form_pdf.git
   cd generate_form_pdf
   ```

2. **Configure for development**
   ```bash
   # Create development configuration from example
   cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
   
   # Edit with your AWS credentials
   nano src/main/resources/application-dev.properties
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
# Install Java 21
sudo apt update
sudo apt install -y openjdk-21-jdk

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
api
### Environment Variables

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | No | Active profile | `dev` |
| `AWS_ACCESS_KEY` | Yes | AWS Access Key ID | - |
| `AWS_SECRET_KEY` | Yes | AWS Secret Access Key | - |
| `AWS_S3_BUCKET_NAME` | Yes | S3 bucket name | - |
| `AWS_S3_BUCKET_REGION` | No | S3 bucket region | `us-east-1` |
| `PORT` | No | Server port | `8080` |
| `LOG_LEVEL` | No | Logging level | `INFO` |
| `DEPLOY_USER` | No | Deployment user (for PM2 deploy) | `ubuntu` |
| `DEPLOY_HOST` | No | Deployment host (for PM2 deploy) | - |
| `DEPLOY_PATH` | No | Deployment path (for PM2 deploy) | `/var/www/generate_form_pdf` |

### Configuration Files

- `application.properties` - Base configuration (tracked in git)
- `application-dev.properties.example` - Development template (tracked in git)
- `application-prod.properties.example` - Production template (tracked in git)
- `application-dev.properties` - Development config with secrets (git-ignored)
- `application-prod.properties` - Production config (git-ignored)
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

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
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

## 🤝 Contributing

1. Ensure no sensitive data is committed
2. Run security check: `./security-check.sh`
3. Test all configurations work
4. Follow existing code style
5. Update documentation as needed

## 🔧 Troubleshooting

### Common Issues

1. **AWS credentials error**
   ```bash
   # Check environment variables are set
   echo $AWS_ACCESS_KEY
   
   # Or check configuration file exists
   ls -la src/main/resources/application-dev.properties
   ```

2. **Port already in use**
   ```bash
   # Change port in .env or environment variables
   export PORT=8081
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

This project is licensed under the MIT License - see the LICENSE file for details.