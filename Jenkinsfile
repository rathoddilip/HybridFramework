pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'ENV', choices: ['dev', 'staging', 'prod'], description: 'Target environment')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'], description: 'Browser for web tests')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run browser tests in headless mode')
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                bat 'mvn -q clean'
            }
        }

        stage('Run TestNG Suite') {
            steps {
                withCredentials([
                    string(credentialsId: 'automation-api-base-url', variable: 'API_BASE_URL'),
                    string(credentialsId: 'automation-app-base-url', variable: 'APP_BASE_URL'),
                    string(credentialsId: 'automation-auth-origin', variable: 'AUTH_ORIGIN'),
                    string(credentialsId: 'automation-auth-referer', variable: 'AUTH_REFERER'),
                    string(credentialsId: 'automation-partner-url', variable: 'PARTNER_URL'),
                    string(credentialsId: 'automation-mobile', variable: 'TEST_MOBILE'),
                    string(credentialsId: 'automation-otp', variable: 'TEST_OTP')
                ]) {
                    bat """
                        mvn -q test ^
                          -Denv=%ENV% ^
                          -Dbrowser=%BROWSER% ^
                          -Dheadless=%HEADLESS% ^
                          -Dapi.baseUrl=%API_BASE_URL% ^
                          -Dapp.baseUrl=%APP_BASE_URL% ^
                          -Dauth.origin=%AUTH_ORIGIN% ^
                          -Dauth.referer=%AUTH_REFERER% ^
                          -Dauth.partnerUrl=%PARTNER_URL% ^
                          -Dusers.admin.username=%TEST_MOBILE% ^
                          -Dusers.admin.password=%TEST_OTP% ^
                          -Dallure.autoGenerate=false ^
                          -Dallure.autoOpen=false
                    """
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'

            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])

            archiveArtifacts artifacts: 'target/surefire-reports/**, target/allure-results/**', allowEmptyArchive: true
        }
    }
}
