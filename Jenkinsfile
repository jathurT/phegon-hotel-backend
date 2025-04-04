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
        booleanParam(name: 'PROVISION_INFRASTRUCTURE', defaultValue: false, description: 'Provision new infrastructure with Terraform')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip running tests')
    }

    environment {
        AWS_CREDENTIALS = credentials('aws-credentials')
        DB_CREDENTIALS = credentials('db-credentials')
        MYSQL_ROOT_PASSWORD = credentials('mysql-root-password')
        DOCKER_CREDENTIALS = credentials('docker-hub-credentials')
        ADMIN_PASSWORD = credentials('admin-password')
        ADMIN_USERNAME = credentials('admin-username')
        DOCKER_IMAGE = "jathurt/myapp-backend-hotel-bookings"
        JWT_SECRET = credentials('jwt-secret')
//         EC2_HOST = credentials('ec2-host-hotel')
        EC2_USER = 'ubuntu'
        DEPLOY_ENV = "${params.DEPLOY_ENV ?: 'staging'}"
        SERVER_PORT = "${params.SERVER_PORT}"
        MYSQL_PORT = "${params.MYSQL_PORT}"
        TERRAFORM_DIR = "${WORKSPACE}/terraform"
        ANSIBLE_DIR = "${WORKSPACE}/ansible"
        SONAR_PROJECT_KEY = "com.phegondev:PhegonHotel"
        SONAR_PROJECT_NAME = "PhegonHotel"
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

        stage('Test') {
            when {
                expression { return !params.SKIP_TESTS }
            }
            steps {
                sh './mvnw test --no-transfer-progress'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            environment {
                SONAR_CREDENTIALS = credentials('sonar-token')
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        ./mvnw sonar:sonar \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                        -Dsonar.host.url=http://localhost:9000 \
                        -Dsonar.login=${SONAR_CREDENTIALS} \
                        -Dsonar.java.coveragePlugin=jacoco \
                        -Dsonar.junit.reportsPath=target/surefire-reports \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.sources=src/main/java
                    '''
                }
            }
        }

//         stage('Quality Gate') {
//             steps {
//                 timeout(time: 10, unit: 'MINUTES') {
//                     waitForQualityGate abortPipeline: true
//                 }
//             }
//         }

        stage('Prepare .env File') {
            steps {
                script {
                    withCredentials([
                        [$class: 'AmazonWebServicesCredentialsBinding',
                         credentialsId: 'aws-credentials',
                         accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                         secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]) {
                        sh '''
                            # Create .env file with secure permissions
                            touch .env && chmod 600 .env

                            cat > .env << EOL
SPRING_APPLICATION_NAME=backend
SERVER_PORT=${SERVER_PORT}
MYSQL_PORT=${MYSQL_PORT}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/phegon_hotel_db
SPRING_DATASOURCE_USERNAME=${DB_CREDENTIALS_USR}
SPRING_DATASOURCE_PASSWORD=${DB_CREDENTIALS_PSW}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_SHOW_SQL=false
SPRING_APP_JWTSECRET=${JWT_SECRET}
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
AWS_REGION=eu-north-1
AWS_S3_BUCKET=phegon-hotel-images-jathur
SPRING_APP_ADMIN_PASSWORD=${ADMIN_PASSWORD}
SPRING_APP_ADMIN_USERNAME=${ADMIN_USERNAME}
EOL
                        '''
                    }
                }
            }
        }

        stage('Check Monitoring Directories') {
            steps {
                script {
                    sh '''
                        # Verify that the required directories exist
                        if [ ! -d "prometheus" ]; then
                            echo "Error: prometheus directory not found in project"
                            exit 1
                        fi

                        if [ ! -d "grafana/provisioning/dashboards" ]; then
                            echo "Error: grafana/provisioning/dashboards directory not found in project"
                            exit 1
                        fi

                        if [ ! -d "grafana/provisioning/datasources" ]; then
                            echo "Error: grafana/provisioning/datasources directory not found in project"
                            exit 1
                        fi

                        # Verify the directories contents
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

        stage('Provision Infrastructure') {
            when {
                expression { params.PROVISION_INFRASTRUCTURE == true }
            }
            steps {
                script {
                    // Create terraform directory if it doesn't exist
                    sh "mkdir -p ${TERRAFORM_DIR}"

                    // Create Terraform files
                    writeFile file: "${TERRAFORM_DIR}/main.tf", text: readFile('./terraform/main.tf')
                    writeFile file: "${TERRAFORM_DIR}/variables.tf", text: readFile('./terraform/variables.tf')

                    // Initialize and apply Terraform
                    dir(TERRAFORM_DIR) {
                        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                         credentialsId: 'aws-credentials',
                                         accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                         secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                            sh 'terraform init'
                            sh 'terraform apply -auto-approve'

                            // Capture the instance IP for Ansible
                            def instance_ip = sh(script: 'terraform output -raw instance_public_ip', returnStdout: true).trim()

                            // Store the instance IP for later use
                            env.EC2_HOST = instance_ip

                            // Update the Jenkins credential
                            // Note: This is pseudo-code and may need to be adapted
                            // withCredentials([string(credentialsId: 'ec2-host-hotel', variable: 'EC2_HOST_CRED')]) {
                            //     // Update the credential with the new IP
                            // }

                            echo "New EC2 instance provisioned at IP: ${instance_ip}"
                        }
                    }
                }
            }
        }

        stage('Configure Infrastructure with Ansible') {
            when {
                expression { params.PROVISION_INFRASTRUCTURE == true }
            }
            steps {
                script {
                    // Create ansible directory if it doesn't exist
                    sh "mkdir -p ${ANSIBLE_DIR}"

                    // Create Ansible files
                    writeFile file: "${ANSIBLE_DIR}/playbook.yml", text: readFile('./ansible/playbook.yml')

                    // Create inventory file with dynamic EC2 IP - WITHOUT ssh key reference
                    def inventory_content = """[web_servers]
webserver ansible_host=${EC2_HOST} ansible_user=ubuntu
"""
                    writeFile file: "${ANSIBLE_DIR}/inventory.ini", text: inventory_content

                    sshagent(['ec2-ssh-key']) {
                        // Wait for SSH to be available
                        sh """
                            # Wait for SSH to become available
                            echo "Waiting for SSH to become available on ${EC2_HOST}..."
                            timeout 300 bash -c 'until ssh -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} echo SSH is up; do sleep 5; done'
                        """

                        // Run Ansible playbook with SSH agent
                        dir(ANSIBLE_DIR) {
                            sh "ansible-playbook -i inventory.ini playbook.yml"
                        }
                    }
                }
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
                    sshagent(['ec2-ssh-key']) {
                        sh '''
                            set -e  # Exit on any error

                            echo "Preparing deployment on ${REMOTE_USER}@${REMOTE_HOST}..."

                            # First check and clean existing deployment directory if needed
                            echo "Checking and cleaning existing directories if needed..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "sudo rm -rf ~/app-deployment || true"

                            # Create remote directory structure with proper permissions
                            echo "Creating remote directories with proper permissions..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/prometheus"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/grafana/provisioning/dashboards"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "mkdir -p ~/app-deployment/grafana/provisioning/datasources"

                            # Ensure the ubuntu user owns all these directories
                            echo "Setting ownership of remote directories..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "sudo chown -R $REMOTE_USER:$REMOTE_USER ~/app-deployment"

                            # Ensure proper permissions on remote directories
                            echo "Setting permissions on remote directories..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "chmod -R 755 ~/app-deployment"

                            # List directories to verify
                            echo "Verifying remote directories..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "ls -la ~/app-deployment/"

                            # Copy main config files
                            echo "Copying docker-compose.yml and .env..."
                            scp -o StrictHostKeyChecking=no docker-compose.yml .env $REMOTE_USER@$REMOTE_HOST:~/app-deployment/

                            # Copy the existing prometheus and grafana configuration files
                            echo "Copying prometheus configuration..."
                            scp -o StrictHostKeyChecking=no prometheus/prometheus.yml $REMOTE_USER@$REMOTE_HOST:~/app-deployment/prometheus/

                            echo "Copying grafana dashboard configurations..."
                            scp -o StrictHostKeyChecking=no grafana/provisioning/dashboards/dashboard.yml $REMOTE_USER@$REMOTE_HOST:~/app-deployment/grafana/provisioning/dashboards/
                            scp -o StrictHostKeyChecking=no grafana/provisioning/dashboards/booking_dashboard.json $REMOTE_USER@$REMOTE_HOST:~/app-deployment/grafana/provisioning/dashboards/

                            echo "Copying grafana datasource configurations..."
                            scp -o StrictHostKeyChecking=no grafana/provisioning/datasources/datasource.yml $REMOTE_USER@$REMOTE_HOST:~/app-deployment/grafana/provisioning/datasources/

                            # Verify files on remote server
                            echo "Verifying remote files..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "ls -la ~/app-deployment/prometheus/"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "ls -la ~/app-deployment/grafana/provisioning/dashboards/"
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST "ls -la ~/app-deployment/grafana/provisioning/datasources/"

                            # Deploy the application
                            echo "Deploying application..."
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$REMOTE_HOST bash << 'EOF'
cd ~/app-deployment
echo "Current directory: $(pwd)"
echo "Logging into Docker Hub..."
echo "$DOCKER_PASSWORD" | sudo docker login --username "$DOCKER_USERNAME" --password-stdin

echo "Stopping existing services..."
sudo docker-compose down --remove-orphans || true

echo "Cleaning up any stale containers..."
sudo docker ps -aq | xargs sudo docker rm -f 2>/dev/null || true
sudo docker network prune -f || true

echo "Pulling latest images..."
sudo docker-compose pull

echo "Starting services..."
sudo docker-compose up -d

echo "Checking if services started successfully..."
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