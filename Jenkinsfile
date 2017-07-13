node() {
    stage("Checkout source code") {
        git url: "https://github.com/dajudge/testee.git"
    }

    stage("Build") {
        sh './gradlew clean assemble'
    }

    stage("Test") {
        try {
            sh "IGNORE_TEST_FAILURES=true ./gradlew test"
        } finally {
            junit "**/build/test-results/**/TEST-*.xml"
        }
    }

    stage("Static code analysis") {
        withCredentials([
            usernamePassword(
                credentialsId: 'sonar',
                usernameVariable: 'SONAR_USER',
                passwordVariable: 'SONAR_PASSWORD'
            )
        ]) {
            sh "./gradlew -x test sonarqube"
        }
    }
}
