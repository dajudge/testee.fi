node() {
    stage("Checkout source code") {
        git url: "https://github.com/dajudge/testee.git"
    }

    withBuildEnv() {
        stage("Build and Test") {
            try {
                sh "IGNORE_TEST_FAILURES=true ./gradlew --no-daemon clean build"
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
                sh "./gradlew --no-daemon -x test sonarqube"
            }
        }
    }

    try {
        stage("Publish to Nexus") {
            if (currentBuild.result == 'UNSTABLE') {
                throw new RuntimeException("SKIP: Won't deploy because build is unstable");
            }
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
    } catch(Throwable t) {
        if(!t.message.startsWith("SKIP:")) {
            throw t;
        } else {
            println t.message;
        }
    }
}

def withBuildEnv(closure) {
    def jdkImage, psqlImage

    dir("builder/jdk8") {
        jdkImage = docker.build "testee-jdk8:latest"
    }

    dir("builder/psql") {
        psqlImage = docker.build "testee-psql:latest"
    }

    def psqlContainer = "testee-psql-${System.currentTimeMillis()}"
    psqlImage.withRun("--name $psqlContainer -e POSTGRES_DB=testee -e POSTGRES_PASSWORD=testee -e POSTGRES_USER=testee") {
        withEnv([
            "TESTEE_PSQL_HOSTNAME=psql",
            "TESTEE_PSQL_DB=testee",
            "TESTEE_PSQL_USER=testee",
            "TESTEE_PSQL_PASSWORD=testee"
        ]) {
            jdkImage.inside("--link ${psqlContainer}:psql -e TESTEE_PSQL_HOSTNAME=psql -e TESTEE_PSQL_DB=testee -e TESTEE_PSQL_USER=testee -e TESTEE_PSQL_PASSWORD=testee", closure)
        }
    }
}
