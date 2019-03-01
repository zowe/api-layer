#!/usr/bin/env groovy

///**
// * Name of lock for build.
// * It used for make sure that only one build uses deployment environment at the time.
// */
//def BUILD_LOCK = "pullRequestBuild"

pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    /**
     * Set up environment variables
     */
    environment {
        JAVA_HOME = "${tool 'jdk1.8.0_172'}"
        PATH = "${JAVA_HOME}/bin:${PATH}"
        DEPLOYMENT_CONFIGURATION = "deployment-configurations/ca3x-deployment-a.json"
        GATEWAY_HOST = "ca3x.ca.com"
        GATEWAY_PORT = "10010"
        SERVICE_HOST = "ca31.ca.com"
        SERVICE_PORT = "10017"
        LC_ALL = "en_US.UTF-8"
        LANG = "en_US.UTF-8"
    }

    /**
     * Top-level stages of build
     */
    stages {
        /**
         * Change environment variables in case of master branch build
         */
//        stage('Update environment variables') {
//            steps {
//                script {
//                    if (env.BRANCH_NAME == 'master') {
//                        echo 'Master build'
//                        DEPLOYMENT_CONFIGURATION = "deployment-configurations/ca3x-deployment-c.json"
//                        GATEWAY_PORT = "10310"
//                        SERVICE_PORT = "10317"
//                        BUILD_LOCK = "masterBuild"
//                    } else {
//                        echo 'Pull request build'
//                    }
//                }
//            }
//        }

//        /**
//         * Install tool for deployment
//         */
//        stage('Prepare environment') {
//            steps {
//                sh """
//                source activate ${CONDA_ENV}
//                pip install mfaas-auto
//                """
//            }
//        }

        /**
         * Remove all previous Gradle build files
         */
        stage('Clean') {
            steps {
                sh """
                ./gradlew clean
                """
            }
        }

        /**
         * Build service
         */
        stage('Build') {
            steps {
                sh """
                ./gradlew build
                """
            }
        }
//
//        /**
//         * Save Java test coverage
//         */
//        stage('Publish Coverage Reports') {
//            steps {
//                publishHTML(target: [
//                    allowMissing         : false,
//                    alwaysLinkToLastBuild: false,
//                    keepAll              : true,
//                    reportDir            : 'build/reports/jacoco/test/html',
//                    reportFiles          : 'index.html',
//                    reportName           : "JaCoCo Report"
//                ])
//            }
//        }
//
//        /**
//         * Run code analysis with SonarQube.
//         * Check {@code gradle/sonar.gradle} file for SonarQube configuration
//         */
//        stage('Code Analysis') {
//            steps {
//                withSonarQubeEnv('sonarqube-isl-01.ca.com') {
//                    sh """
//                    ./gradlew --info sonarqube ${SONAR_QUBE_EXTRA_OPTIONS}
//                    """
//                }
//            }
//        }
//
//        /**
//         * Deployment nd integration test stage.
//         * This stage contains substages for deployment and integrating tests.
//         * This stage use lock for ensure that only one build is running at the time.
//         */
//        stage('Deployment and Integration tests') {
//            /**
//             * Acquire lock for environment.
//             */
//            options {
//                lock(BUILD_LOCK)
//            }
//
//            stages {
//                /**
//                 * Deploy using configuration from {@code DEPLOYMENT_CONFIGURATION} file
//                 */
//                stage('Deploy to z/OS') {
//                    steps {
//                        sh """
//                        mfaas-auto install ${DEPLOYMENT_CONFIGURATION} --force-update --restart
//                        """
//                    }
//                }
//
//                /**
//                 * Run integration tests for service without API Mediation Layer Gateway
//                 */
//                stage('Run integration tests on z/OS without API Gateway') {
//                    steps {
//                        sh """
//                        ./gradlew integrationTest -Dhost=${SERVICE_HOST} -Dport=${SERVICE_PORT} -Dscheme=https -DwithGateway=false
//                        """
//                    }
//                }
//
//                /**
//                 * Run integration tests for service through API Mediation Layer Gateway
//                 */
//                stage('Run integration tests on z/OS with API Gateway') {
//                    steps {
//                        sh """
//                        ./gradlew integrationTest -Dhost=${GATEWAY_HOST} -Dport=${GATEWAY_PORT} -Dscheme=https -DwithGateway=true
//                        """
//                    }
//                }
//            }
//        }
//
//    }
    }

    post {
        /**
         * Save result of unit tests.
         */
        always {
            junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
        }

        /**
         * Save generated service jar file
         */
        success {
            archiveArtifacts 'build/libs/**/*.jar'
        }
    }
}
