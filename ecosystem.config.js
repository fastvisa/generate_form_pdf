module.exports = {
  apps: [
    {
      name: 'manipulate-pdf-api',
      script: 'start-app.sh',
      cwd: process.cwd(),
      instances: 1,
      exec_mode: 'fork',
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      env: {
        NODE_ENV: 'production',
        JAVA_HOME: process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-amd64',
        PATH: (process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-amd64') + '/bin:' + process.env.PATH,
        // Spring Boot configuration
        SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'prod',
        SERVER_PORT: process.env.SERVER_PORT || '8080',
        // AWS Configuration - load from environment
        AWS_ACCESS_KEY_ID: process.env.AWS_ACCESS_KEY_ID,
        AWS_SECRET_ACCESS_KEY: process.env.AWS_SECRET_ACCESS_KEY,
        AWS_REGION: process.env.AWS_REGION || 'us-east-1',
        // Logging
        LOG_LEVEL: process.env.LOG_LEVEL || 'INFO'
      },
      env_production: {
        NODE_ENV: 'production',
        JAVA_HOME: process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-amd64',
        PATH: (process.env.JAVA_HOME || '/usr/lib/jvm/java-21-openjdk-amd64') + '/bin:' + process.env.PATH,
        // Spring Boot configuration
        SPRING_PROFILES_ACTIVE: 'prod',
        SERVER_PORT: process.env.SERVER_PORT || '8080',
        // AWS Configuration - load from environment
        AWS_ACCESS_KEY_ID: process.env.AWS_ACCESS_KEY_ID,
        AWS_SECRET_ACCESS_KEY: process.env.AWS_SECRET_ACCESS_KEY,
        AWS_REGION: process.env.AWS_REGION || 'us-east-1',
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
  ]
};