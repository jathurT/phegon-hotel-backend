pipeline {
    agent any

    tools {
        maven 'Maven 3.8.6'
        jdk 'JDK 17'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['staging', 'production'], description: 'Select deployment environment')
        string(name: 'SERVER_PORT', defaultValue: '8081', description: 'Port for the application to run on EC2')
        string(name: 'MYSQL_PORT', defaultValue: '3306', description: 'Port for MySQL to run on EC2')
    }

    environment {
        AWS_CREDENTIALS = credentials('aws-credentials-hotel')
        DB_CREDENTIALS = credentials('db-credentials')
        MYSQL_ROOT_PASSWORD = credentials('mysql-root-password')
        DOCKER_CREDENTIALS = credentials('docker-hub-credentials')
        DOCKER_IMAGE = "jathurt/myapp-backend-hotel-bookings"
        EC2_HOST = credentials('ec2-host-hotel')
        EC2_USER = 'ubuntu'
        DEPLOY_ENV = "${params.DEPLOY_ENV ?: 'staging'}"
        SERVER_PORT = "${params.SERVER_PORT}"
        MYSQL_PORT = "${params.MYSQL_PORT}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests --no-transfer-progress'
            }
        }

        stage('Prepare .env File') {
            steps {
                script {
                    sh '''
                        # Create .env file with secure permissions
                        touch .env && chmod 600 .env

                        cat > .env << EOL
SPRING_APPLICATION_NAME=backend
SERVER_PORT=${SERVER_PORT}
MYSQL_PORT=${MYSQL_PORT}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/dental
SPRING_DATASOURCE_USERNAME=${DB_CREDENTIALS_USR}
SPRING_DATASOURCE_PASSWORD=${DB_CREDENTIALS_PSW}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_SHOW_SQL=false
AWS_ACCESSKEYID=${AWS_CREDENTIALS_USR}
AWS_SECRETKEY=${AWS_CREDENTIALS_PSW}
AWS_REGION=eu-north-1
AWS_S3_BUCKET=phegon-hotel-images-jathur
EOL

                        # Verify file was created successfully
                        if [ ! -f .env ]; then
                            echo "Failed to create .env file"
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Check Monitoring Directories') {
            steps {
                script {
                    sh '''
                        # Check if prometheus and grafana directories exist, create if they don't
                        if [ ! -d "prometheus" ]; then
                            mkdir -p prometheus
                            echo "Created prometheus directory"
                        fi

                        if [ ! -d "grafana/provisioning/dashboards" ]; then
                            mkdir -p grafana/provisioning/dashboards
                            echo "Created grafana dashboards directory"
                        fi

                        if [ ! -d "grafana/provisioning/datasources" ]; then
                            mkdir -p grafana/provisioning/datasources
                            echo "Created grafana datasources directory"
                        fi

                        # Verify the directories were created successfully
                        ls -la prometheus/
                        ls -la grafana/provisioning/dashboards/
                        ls -la grafana/provisioning/datasources/
                    '''
                }
            }
        }

        stage('Prepare Monitoring Files') {
            steps {
                script {
                    sh '''
                        # Create a basic prometheus.yml if it doesn't exist
                        if [ ! -f "prometheus/prometheus.yml" ]; then
                            cat > prometheus/prometheus.yml << EOL
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:${SERVER_PORT}']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
EOL
                            echo "Created prometheus.yml"
                        fi

                        # Create a basic Grafana datasource configuration if it doesn't exist
                        if [ ! -f "grafana/provisioning/datasources/prometheus.yml" ]; then
                            cat > grafana/provisioning/datasources/prometheus.yml << EOL
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
EOL
                            echo "Created Grafana datasource configuration"
                        fi

                        # Create a basic Grafana dashboard configuration if it doesn't exist
                        if [ ! -f "grafana/provisioning/dashboards/dashboard.yml" ]; then
                            cat > grafana/provisioning/dashboards/dashboard.yml << EOL
apiVersion: 1

providers:
  - name: 'Default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/provisioning/dashboards
EOL
                            echo "Created Grafana dashboard provider configuration"
                        fi

                        # Create a basic Grafana dashboard if it doesn't exist
                        if [ ! -f "grafana/provisioning/dashboards/spring-boot.json" ]; then
                            cat > grafana/provisioning/dashboards/spring-boot.json << EOL
{
  "annotations": {
    "list": []
  },
  "editable": true,
  "gnetId": null,
  "graphTooltip": 0,
  "hideControls": false,
  "links": [],
  "refresh": "5s",
  "rows": [
    {
      "collapse": false,
      "height": "250px",
      "panels": [
        {
          "aliasColors": {},
          "bars": false,
          "dashLength": 10,
          "dashes": false,
          "datasource": "Prometheus",
          "fill": 1,
          "id": 1,
          "legend": {
            "avg": false,
            "current": false,
            "max": false,
            "min": false,
            "show": true,
            "total": false,
            "values": false
          },
          "lines": true,
          "linewidth": 1,
          "links": [],
          "nullPointMode": "null",
          "percentage": false,
          "pointradius": 5,
          "points": false,
          "renderer": "flot",
          "seriesOverrides": [],
          "spaceLength": 10,
          "span": 12,
          "stack": false,
          "steppedLine": false,
          "targets": [
            {
              "expr": "system_cpu_usage",
              "format": "time_series",
              "intervalFactor": 2,
              "legendFormat": "CPU Usage",
              "refId": "A"
            }
          ],
          "thresholds": [],
          "timeFrom": null,
          "timeShift": null,
          "title": "CPU Usage",
          "tooltip": {
            "shared": true,
            "sort": 0,
            "value_type": "individual"
          },
          "type": "graph",
          "xaxis": {
            "buckets": null,
            "mode": "time",
            "name": null,
            "show": true,
            "values": []
          },
          "yaxes": [
            {
              "format": "short",
              "label": null,
              "logBase": 1,
              "max": null,
              "min": null,
              "show": true
            },
            {
              "format": "short",
              "label": null,
              "logBase": 1,
              "max": null,
              "min": null,
              "show": true
            }
          ]
        }
      ],
      "repeat": null,
      "repeatIteration": null,
      "repeatRowId": null,
      "showTitle": false,
      "title": "Dashboard Row",
      "titleSize": "h6"
    }
  ],
  "schemaVersion": 14,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": []
  },
  "time": {
    "from": "now-15m",
    "to": "now"
  },
  "timepicker": {
    "refresh_intervals": [
      "5s",
      "10s",
      "30s",
      "1m",
      "5m",
      "15m",
      "30m",
      "1h",
      "2h",
      "1d"
    ],
    "time_options": [
      "5m",
      "15m",
      "1h",
      "6h",
      "12h",
      "24h",
      "2d",
      "7d",
      "30d"
    ]
  },
  "timezone": "",
  "title": "Spring Boot",
  "version": 0
}
EOL
                            echo "Created Grafana dashboard"
                        fi

                        # Verify files exist
                        ls -la prometheus/
                        ls -la grafana/provisioning/dashboards/
                        ls -la grafana/provisioning/datasources/
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                sh 'echo $DOCKER_CREDENTIALS_PSW | docker login -u $DOCKER_CREDENTIALS_USR --password-stdin'
                sh "docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                sh "docker push ${DOCKER_IMAGE}:latest"
            }
        }

        stage('Deploy to EC2') {
            steps {
                withEnv([
                    "REMOTE_USER=${EC2_USER}",
                    "REMOTE_HOST=${EC2_HOST}",
                    "DOCKER_USERNAME=${DOCKER_CREDENTIALS_USR}",
                    "DOCKER_PASSWORD=${DOCKER_CREDENTIALS_PSW}"
                ]) {
                    sshagent(['ec2-ssh-key-hotel']) {
                        sh '''
                            # Ensure remote directories exist with correct permissions
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/prometheus && chmod 755 ~/app-deployment/prometheus"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/grafana/provisioning/dashboards && chmod 755 ~/app-deployment/grafana/provisioning/dashboards"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/grafana/provisioning/datasources && chmod 755 ~/app-deployment/grafana/provisioning/datasources"

                            # Copy deployment files
                            scp -o StrictHostKeyChecking=no docker-compose.yml .env $REMOTE_USER@$REMOTE_HOST:~/app-deployment/
                            scp -o StrictHostKeyChecking=no -r prometheus/* $REMOTE_USER@$REMOTE_HOST:~/app-deployment/prometheus/
                            scp -o StrictHostKeyChecking=no -r grafana/provisioning/dashboards/* $REMOTE_USER@$REMOTE_HOST:~/app-deployment/grafana/provisioning/dashboards/
                            scp -o StrictHostKeyChecking=no -r grafana/provisioning/datasources/* $REMOTE_USER@$REMOTE_HOST:~/app-deployment/grafana/provisioning/datasources/

                            # Ensure proper permissions on copied files
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "find ~/app-deployment/prometheus ~/app-deployment/grafana -type f -exec chmod 644 {} \\;"

                            # Docker deployment commands
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST bash << 'EOF'
                                cd ~/app-deployment
                                sudo usermod -aG docker ubuntu
                                echo "$DOCKER_PASSWORD" | sudo docker login --username "$DOCKER_USERNAME" --password-stdin
                                sudo docker-compose down --remove-orphans || true
                                sudo docker ps -aq | xargs sudo docker rm -f 2>/dev/null || true
                                sudo docker network prune -f || true
                                sudo docker-compose pull
                                sudo docker-compose up -d
                                if ! sudo docker-compose ps | grep -q "Up"; then
                                    echo "Containers failed to start properly"
                                    sudo docker-compose logs
                                    exit 1
                                fi
                                echo "Deployment completed successfully!"
EOF
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                sh 'docker logout || true'
                sh 'docker system prune -f || true'
                sh 'rm -f .env || true'
                cleanWs()
            }
        }
        success {
            echo "Successfully deployed to ${DEPLOY_ENV} environment at ${EC2_HOST}"
        }
        failure {
            echo "Deployment to ${DEPLOY_ENV} failed"
        }
    }
}
