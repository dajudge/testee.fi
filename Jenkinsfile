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

    stage("Usage testing") {
        @NonCPS def imageDirs = findFiles(glob:"usageTests/images/**/Dockerfile").collect { f ->
            def split = f.path.split("/")
            "${split[2]}:${split[3]}"
        }
        imageDirs.each { imageVersion ->
            def dockerImage = dir("usageTests/images/${imageVersion.replace(":", "/")}") {
                docker.build("testee-usage-$imageVersion")
            }

            dir("usageTests/maven/") {
                dockerImage.inside {
                    try {
                        withCredentials([
                                    usernamePassword(
                                    credentialsId: 'maven',
                                    usernameVariable: 'MAVEN_USER',
                                    passwordVariable: 'MAVEN_PASSWORD'
                                    )
                                ]) {
                            sh "chmod 755 build.sh && ./build.sh"
                        }
                    }   catch(Throwable t) {
                        currentBuild.result == 'UNSTABLE'
                        println t.message
                    }
                }
            }
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
        jdkImage.inside(
            [
                "--link ${psqlContainer}:psql",
                "-e TESTEE_PSQL_HOSTNAME=psql",
                "-e TESTEE_PSQL_DB=testee",
                "-e TESTEE_PSQL_USER=testee",
                "-e TESTEE_PSQL_PASSWORD=testee"
            ].join(" "),
            closure
         )
    }
}
