pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'payment-api'
        SONAR_PROJECT_KEY = 'CICDAssignment2'
        SONAR_PROJECT_NAME = 'CICDAssignment2'
        JAR_FILE = 'target/payment-api-0.0.1-SNAPSHOT.jar'
        IMAGE_NAME = "payment-api:${BUILD_NUMBER}"
        IMAGE_LATEST = 'payment-api:latest'
        DEPLOY_CONTAINER_NAME = 'payment-api'
        DEPLOY_HOST_PORT = '8082'
        DEPLOY_CONTAINER_PORT = '8082'
        DEPLOY_HEALTHCHECK_URL = 'http://localhost:8082/configcheck'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build And Verify') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B clean verify'
                    } else {
                        bat 'mvn -B clean verify'
                    }
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/*.jar,target/site/jacoco/**/*', fingerprint: true, onlyIfSuccessful: false
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('LocalSonar') {
                    script {
                        def sonarCommand = "mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar " +
                                "-Dsonar.projectKey=${env.SONAR_PROJECT_KEY} " +
                                "-Dsonar.projectName=${env.SONAR_PROJECT_NAME} " +
                                "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"

                        if (isUnix()) {
                            sh sonarCommand
                        } else {
                            bat sonarCommand
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def dockerBuildCommand = "docker build --build-arg JAR_FILE=${env.JAR_FILE} -t ${env.IMAGE_NAME} -t ${env.IMAGE_LATEST} ."
                    if (isUnix()) {
                        sh dockerBuildCommand
                    } else {
                        bat dockerBuildCommand
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    if (isUnix()) {
                        sh """
set -e
if docker ps -aq --filter "name=^/${env.DEPLOY_CONTAINER_NAME}\$" | grep -q .; then
  docker rm -f ${env.DEPLOY_CONTAINER_NAME}
fi
docker run -d --name ${env.DEPLOY_CONTAINER_NAME} -p ${env.DEPLOY_HOST_PORT}:${env.DEPLOY_CONTAINER_PORT} ${env.IMAGE_LATEST}
for i in \$(seq 1 30); do
  if curl -fsS ${env.DEPLOY_HEALTHCHECK_URL} >/dev/null; then
    exit 0
  fi
  sleep 2
done
echo "Deployment verification failed for ${env.DEPLOY_HEALTHCHECK_URL}"
exit 1
"""
                    } else {
                        bat """
@echo off
setlocal
set "EXISTING_CONTAINER="
for /f %%i in ('docker ps -aq --filter "name=^/${env.DEPLOY_CONTAINER_NAME}\$"') do set "EXISTING_CONTAINER=%%i"
if defined EXISTING_CONTAINER (
  docker rm -f ${env.DEPLOY_CONTAINER_NAME}
  if errorlevel 1 exit /b %errorlevel%
)
docker run -d --name ${env.DEPLOY_CONTAINER_NAME} -p ${env.DEPLOY_HOST_PORT}:${env.DEPLOY_CONTAINER_PORT} ${env.IMAGE_LATEST}
if errorlevel 1 exit /b %errorlevel%
powershell -NoProfile -Command ^
  "\$ProgressPreference='SilentlyContinue';" ^
  "for (\$i = 0; \$i -lt 30; \$i++) {" ^
  "  try {" ^
  "    \$response = Invoke-WebRequest -UseBasicParsing '${env.DEPLOY_HEALTHCHECK_URL}';" ^
  "    if (\$response.StatusCode -eq 200) { exit 0 }" ^
  "  } catch {}" ^
  "  Start-Sleep -Seconds 2" ^
  "}" ^
  "Write-Error 'Deployment verification failed for ${env.DEPLOY_HEALTHCHECK_URL}';" ^
  "exit 1"
if errorlevel 1 exit /b %errorlevel%
endlocal
"""
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Build completed. SonarQube quality gate passed. Docker image built and deployment verified."
        }
        failure {
            echo 'Build failed. Check test output, SonarQube analysis, quality gate status, Docker build, or deployment verification.'
        }
    }
}
