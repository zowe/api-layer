/**
 * The name of the master branch
 */
def MASTER_BRANCH = "master"

/**
 * The result string for a successful build
 */
def BUILD_SUCCESS = 'SUCCESS'

/**
 * The result string for an unstable build
 */
def BUILD_UNSTABLE = 'UNSTABLE'

/**
 * The result string for a failed build
 */
def BUILD_FAILURE = 'FAILURE'

/**
 * The user's name for git commits
 */
def GIT_USER_NAME = 'zowe-robot'

/**
 * The user's email address for git commits
 */
def GIT_USER_EMAIL = 'zowe.robot@gmail.com'

/**
 * The base repository url for github
 */
def GIT_REPO_URL = 'https://github.com/zowe/api-layer.git'

/**
 * The credentials id field for the authorization token for GitHub stored in Jenkins
 */
def GIT_CREDENTIALS_ID = 'zowe-robot-github'

/**
 * A command to be run that gets the current revision pulled down
 */
def GIT_REVISION_LOOKUP = 'git log -n 1 --pretty=format:%h'

/**
 * The credentials id field for the artifactory username and password
 */
def ARTIFACTORY_CREDENTIALS_ID = 'zowe.jfrog.io'

/**
 * The email address for the artifactory
 */
def ARTIFACTORY_EMAIL = GIT_USER_EMAIL

// Setup conditional build options. Would have done this in the options of the declarative pipeline, but it is pretty
// much impossible to have conditional options based on the branch :/
def opts = []

if (BRANCH_NAME == MASTER_BRANCH) {
    // Only keep 20 builds
    opts.push(buildDiscarder(logRotator(numToKeepStr: '20')))

    // Concurrent builds need to be disabled on the master branch because
    // it needs to actively commit and do a build. There's no point in publishing
    // twice in quick succession
    opts.push(disableConcurrentBuilds())
} else {
    // Only keep 5 builds on other branches
    opts.push(buildDiscarder(logRotator(numToKeepStr: '5')))
}
properties(opts)

pipeline {
    agent {
        label 'apiml-jenkins-agent-swarm'
    }

    options {
        timestamps ()
    }

    parameters {
        booleanParam(name: 'PUBLISH_PR_ARTIFACTS', defaultValue: 'false', description: 'If true it will publish the pull requests artifacts', )
    }

    stages {
        stage('Build and unit test with coverage') {
            steps {
                timeout(time: 20, unit: 'MINUTES') {
                    withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDENTIALS_ID, usernameVariable: 'ARTIFACTORY_USERNAME', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                        withSonarQubeEnv('sonarcloud-server') {
                            sh './gradlew --info --scan clean build coverage sonarqube -Psonar.host.url=${SONAR_HOST_URL} -Dsonar.login=${SONAR_AUTH_TOKEN} -Pgradle.cache.push=true -Penabler=v1 -Partifactory_user=${ARTIFACTORY_USERNAME} -Partifactory_password=${ARTIFACTORY_PASSWORD}'
                        }
                    }
                }
            }
        }

        stage ('Run Integration Tests') {
            steps {
                sh 'npm install'
                sh 'npm run api-layer & > integration-instances.log'
                sh './gradlew runCITests'
            }
        }

        stage('Publish coverage reports') {
            steps {
                   publishHTML(target: [
                       allowMissing         : false,
                       alwaysLinkToLastBuild: false,
                       keepAll              : true,
                       reportDir            : 'build/reports/jacoco/jacocoFullReport/html',
                       reportFiles          : 'index.html',
                       reportName           : "Java Coverage Report"
                   ])
                    publishHTML(target: [
                        allowMissing         : false,
                        alwaysLinkToLastBuild: false,
                        keepAll              : true,
                        reportDir            : 'api-catalog-ui/frontend/coverage/lcov-report',
                        reportFiles          : 'index.html',
                        reportName           : "UI JavaScript Test Coverage"
                    ])
            }
        }

        stage('Package api-layer source code') {
            steps {
                sh "git archive --format tar.gz -9 --output api-layer.tar.gz HEAD"
            }
        }

        stage('Publish snapshot version to Artifactory for master') {
            when {
                expression {
                    return BRANCH_NAME.equals(MASTER_BRANCH);
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDENTIALS_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                 sh '''
                 ./gradlew publishAllVersions -Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD -Partifactory_user=$USERNAME -Partifactory_password=$PASSWORD
                 '''
                }
            }
        }

        stage('Publish snapshot version to Artifactory for Pull Request') {
            when {
                expression {
                    return BRANCH_NAME.contains("PR-") && params.PUBLISH_PR_ARTIFACTS;
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDENTIALS_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh '''
                    sudo sed -i '/version=/ s/-SNAPSHOT/-'"$BRANCH_NAME"'-SNAPSHOT/' ./gradle.properties
                    ./gradlew publishAllVersions -Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD  -Partifactory_user=$USERNAME -Partifactory_password=$PASSWORD -PpullRequest=$env.BRANCH_NAME
                    '''
                }
            }
        }

        stage('Publish UI test results') {
            steps {
                publishHTML(target: [
                    allowMissing         : false,
                    alwaysLinkToLastBuild: false,
                    keepAll              : true,
                    reportDir            : 'api-catalog-ui/frontend/test-results',
                    reportFiles          : 'test-report-unit.html',
                    reportName           : "UI Unit Test Results"
                ])
            }
        }

        stage ('Codecov') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'Codecov', usernameVariable: 'CODECOV_USERNAME', passwordVariable: 'CODECOV_TOKEN')]) {
                    sh 'curl -s https://codecov.io/bash | bash -s'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
            publishHTML (target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'gateway-service/build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: "Unit Tests Report - gateway-service"
            ])
            publishHTML (target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'discovery-service/build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: "Unit Tests Report - discovery-service"
            ])
            publishHTML (target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'api-catalog-services/build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: "Unit Tests Report - api-catalog-services"
            ])
        }

        success {
            archiveArtifacts artifacts: 'api-catalog-services/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'discoverable-client/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'discovery-service/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'gateway-service/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'integration-enabler-spring-v1-sample-app/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'api-layer.tar.gz'
            archiveArtifacts artifacts: 'integration-instances.log'

            withCredentials([usernamePassword(credentialsId: 'zowe-robot-github', usernameVariable: 'ZOWE_GITHUB_USERID', passwordVariable: 'ZOWE_GITHUB_APIKEY')]) {
                sh """
                    curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
                    python3 get-pip.py --user
                    python3 -m pip install --user requests
                    python3 -m pip freeze
                    python3 -c 'import requests'
                    python3 scripts/post_actions.py $env.BRANCH_NAME $ZOWE_GITHUB_APIKEY full
                    """
            }
        }
    }
}
