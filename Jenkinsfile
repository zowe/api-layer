#!/usr/bin/env groovy

pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    stages {

        stage('Clean') {
            steps {
                sh """
                ./gradlew clean
                """
            }
        }

        stage('Build') {
            steps {
                sh """
                ./gradlew build
                """
            }
        }
    }
}
