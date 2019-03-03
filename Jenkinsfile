/**
 * The name of the master branch
 */
def MASTER_BRANCH = "master"

/**
* Is this a release branch? Temporary workaround that won't break everything horribly if we merge.
*/
def RELEASE_BRANCH = false
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
def ARTIFACTORY_CREDENTIALS_ID = 'GizaArtifactory'

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
    if (BRANCH_NAME.equals("1.0.0")){
        RELEASE_BRANCH = true
    }
    // Only keep 5 builds on other branches
    opts.push(buildDiscarder(logRotator(numToKeepStr: '5')))
}
properties(opts)

pipeline {
    agent {
        label 'ca-jenkins-agent'
    }

    parameters {
        string(name: 'CHANGE_CLASS', defaultValue: '', description: 'Override change class - for testing (empty, doc, full, api-catalog)', )
    }

    stages {
        stage('Classify changes') {
            steps {
                script {
                    if (params.CHANGE_CLASS != '') {
                        changeClass = params.CHANGE_CLASS
                    }
                    else {
                        ccOutput = sh(returnStdout: true, script: "python3 scripts/classify_changes.py")
                        echo "${ccOutput}"
                        changeClass = ccOutput.trim().tokenize().last()
                    }
                    allowEmptyArchive = changeClass in ['empty', 'doc'] && !BRANCH_NAME.equals(MASTER_BRANCH) && !RELEASE_BRANCH
                }
                echo "Change class: ${changeClass}"
                sh "echo ${changeClass} > .change_class"
            }
        }

        stage('Copy archives from last successful build') {
            when { expression { changeClass != 'full' } }
            steps {
                script {
                    try {
                        copyArtifacts(projectName: JOB_NAME)
                    }
                    catch (error) {
                        copyArtifacts(projectName: 'API_Mediation/master')
                    }
                }
                sh "echo ${changeClass} > .change_class"
            }
        }
        /**
        *  Stage: Bootstrap Gradlew
        *  ========================
        *
        *  Downloads gradle-wrapper.jar to the gradle/wrapper/ directory.
        **/
        stage('Bootstrap Gradlew') {
            steps {
                sh './bootstrap_gradlew.sh'
            }
        }

        stage ('API Catalog build') {
            when { expression { changeClass in ['api-catalog'] } }
            stages {
                stage('Package api-layer source code') {
                    steps {
                        sh "git archive --format tar.gz -9 --output api-layer.tar.gz HEAD"
                    }
                }

                stage ('Build API Catalog') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            sh './gradlew :api-catalog-services:build'
                        }
                    }
                }
            }
        }

        stage ('Full build') {
            when { expression { changeClass in ['full'] } }
            stages {
                stage('Clean') {
                    when { expression { BRANCH_NAME.equals(MASTER_BRANCH) || RELEASE_BRANCH } }
                    steps {
                        sh "./gradlew clean"
                    }
                }

                stage('Package api-layer source code') {
                    steps {
                        sh "git archive --format tar.gz -9 --output api-layer.tar.gz HEAD"
                    }
                }

                stage('Test apiml_cm.sh') {
                    steps {
                        sh 'npm install'
                        sh 'npm run test-scripts-ci'
                    }
                }

                stage('Build and unit test with coverage') {
                    steps {
                        timeout(time: 20, unit: 'MINUTES') {
                            sh './gradlew build coverage'
                        }
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

                /************************************************************************
                * STAGE
                * -----
                * SonarQube Scanner
                *
                * EXECUTION CONDITIONS
                * --------------------
                * - SHOULD_BUILD is true
                * - The build is still successful and not unstable
                *
                * DESCRIPTION
                * -----------
                * Runs the sonar-scanner analysis tool, which submits the source, test resutls,
                *  and coverage results for analysis in our SonarQube server.
                * TODO: This step does not yet support branch or PR submissions properly.
                ***********************************************************************/
                stage('sonar') {
                    steps {
                        withSonarQubeEnv('sonar-default-server') {
                            // Per Sonar Doc - It's important to add --info because of SONARJNKNS-281
                            sh "./gradlew --info sonarqube -Psonar.host.url=${SONAR_HOST_URL}"
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

                stage('Publish snapshot version to Artifactory for master') {
                    when {
                        expression {
                            return BRANCH_NAME.equals(MASTER_BRANCH) || RELEASE_BRANCH;
                        }
                    }
                    steps {
                        withCredentials([usernamePassword(credentialsId: ARTIFACTORY_CREDENTIALS_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "./gradlew publishAllVersions -Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD"
                        }
                    }
                }
            }
        }

        stage ('Javascript Test and Coverage') {
            when { expression { changeClass in ['full', 'api-catalog'] } }
            steps {
                sh './gradlew :api-catalog-ui:startMockedBackend &'
                sh './gradlew :api-catalog-ui:javaScriptCoverage'
            }
        }

        stage ('Codecov') {
            when { expression { changeClass in ['full', 'api-catalog'] } }
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
            archiveArtifacts '.change_class'
        }

        success {
            archiveArtifacts artifacts: 'api-catalog-services/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'discoverable-client/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'discovery-service/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'gateway-service/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'integration-enabler-spring-v1/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'integration-enabler-spring-v2/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'integration-enabler-spring-v1-sample-app/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'common-service-core/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'gateway-common/build/libs/**/*.jar'
            archiveArtifacts artifacts: 'scripts/apiml_cm.sh'
            archiveArtifacts artifacts: 'api-layer.tar.gz'

            withCredentials([usernamePassword(credentialsId: 'zowe-robot-github', usernameVariable: 'ZOWE_GITHUB_USERID', passwordVariable: 'ZOWE_GITHUB_APIKEY')]) {
                sh """
                    curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
                    python3 get-pip.py --user
                    python3 -m pip install --user requests
                    python3 -m pip freeze
                    python3 -c 'import requests'
                    python3 scripts/post_actions.py $env.BRANCH_NAME $ZOWE_GITHUB_APIKEY $changeClass
                    """
            }
        }
    }
}
