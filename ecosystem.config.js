module.exports = {
  apps: [
    {
      name: 'manipulate-pdf-api',
      script: 'java',
      args: [
        '-jar',
        '-Xmx' + (process.env.JVM_MAX_MEMORY || '512m'),
        '-Xms' + (process.env.JVM_MIN_MEMORY || '256m'),
        'target/manipulate-pdf-0.0.1-SNAPSHOT.jar'
      ],
      cwd: process.cwd(),
      instances: 1,
      exec_mode: 'fork',
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      env: {
        NODE_ENV: 'production',
        JAVA_HOME: process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-arm64',
        PATH: (process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-arm64') + '/bin:' + process.env.PATH,
        // Spring Boot configuration
        SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'prod',
        PORT: process.env.PORT || '8080',
        // AWS Configuration - load from environment
        AWS_ACCESS_KEY: process.env.AWS_ACCESS_KEY,
        AWS_SECRET_KEY: process.env.AWS_SECRET_KEY,
        AWS_S3_BUCKET_NAME: process.env.AWS_S3_BUCKET_NAME,
        AWS_S3_BUCKET_REGION: process.env.AWS_S3_BUCKET_REGION || 'us-east-1',
        // Logging
        LOG_LEVEL: process.env.LOG_LEVEL || 'INFO'
      },
      env_production: {
        NODE_ENV: 'production',
        JAVA_HOME: process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-arm64',
        PATH: (process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-arm64') + '/bin:' + process.env.PATH,
        // Spring Boot configuration
        SPRING_PROFILES_ACTIVE: 'prod',
        PORT: process.env.PORT || '8080',
        // AWS Configuration - load from environment
        AWS_ACCESS_KEY: process.env.AWS_ACCESS_KEY,
        AWS_SECRET_KEY: process.env.AWS_SECRET_KEY,
        AWS_S3_BUCKET_NAME: process.env.AWS_S3_BUCKET_NAME,
        AWS_S3_BUCKET_REGION: process.env.AWS_S3_BUCKET_REGION || 'us-east-1',
        // Logging
        LOG_LEVEL: 'INFO'
      },
      log_file: './logs/combined.log',
      out_file: './logs/out.log',
      error_file: './logs/error.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      merge_logs: true,
      kill_timeout: 5000,
      listen_timeout: 3000,
      restart_delay: 4000,
      max_restarts: 10,
      min_uptime: '10s'
    }
  ],

  deploy: {
    production: {
      user: process.env.DEPLOY_USER || 'ubuntu',
      host: [process.env.DEPLOY_HOST || 'your-production-server.com'],
      ref: 'origin/master',
      repo: 'https://github.com/fastvisa/generate_form_pdf.git',
      path: process.env.DEPLOY_PATH || '/var/www/generate_form_pdf',
      'post-deploy': 'mvn clean package -DskipTests && pm2 reload ecosystem.config.js --env production',
      'pre-setup': 'apt update && apt install -y openjdk-21-jdk maven'
    }
  }
};