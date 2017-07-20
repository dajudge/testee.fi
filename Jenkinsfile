node() {
    stage("Checkout source code") {
        git url: "https://github.com/dajudge/testee.git"
    }

    stage("Build and Test") {
        try {
            sh "IGNORE_TEST_FAILURES=true ./gradlew clean build"
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

    stage("Publish to Nexus") {
          withCredentials([
                    usernamePassword(
                        credentialsId: 'maven',
                        usernameVariable: 'MAVEN_USER',
                        passwordVariable: 'MAVEN_PASSWORD'
                    )
                ]) {
                    sh "./gradlew uploadArchives"
                }
    }
}
