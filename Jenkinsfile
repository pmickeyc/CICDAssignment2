pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'payment-api'
        JAR_FILE = 'target/payment-api-0.0.1-SNAPSHOT.jar'
        IMAGE_NAME = "payment-api:${BUILD_NUMBER}"
        IMAGE_LATEST = 'payment-api:latest'
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
    }

    post {
        success {
            echo "Build completed. Docker images created: ${env.IMAGE_NAME} and ${env.IMAGE_LATEST}"
        }
    }
}
