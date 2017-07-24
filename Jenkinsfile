node() {
    stage("Checkout source code") {
        git url: "https://github.com/dajudge/testee.git"
    }

    withBuildEnv() {
        stage("Build and Test") {
            try {
                sh 'gpg --import $GPG_KEY_FILE'
                sh 'IGNORE_TEST_FAILURES=true ./gradlew -Dorg.gradle.project.signing.keyId=CBC58EE1 -Dorg.gradle.project.signing.password=$GPG_PASSWORD -Dorg.gradle.project.signing.secretKeyRingFile=$HOME/.gnupg/secring.gpg --no-daemon clean build uploadArchives --stacktrace'
            } finally {
                junit "**/build/test-results/**/TEST-*.xml"
            }
        }

        stage("Static code analysis") {
            sh "./gradlew --no-daemon -x test sonarqube"
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
            {
                withCredentials([
                    file(
                        credentialsId: 'gpg-key',
                        variable: 'GPG_KEY_FILE'
                    ),
                    usernamePassword(
                        credentialsId: 'gpg-password',
                        usernameVariable: 'GPG_USER', // not used
                        passwordVariable: 'GPG_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'maven',
                        usernameVariable: 'MAVEN_USER',
                        passwordVariable: 'MAVEN_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'sonar',
                        usernameVariable: 'SONAR_USER',
                        passwordVariable: 'SONAR_PASSWORD'
                    )
                ]) {
                    closure()
                }
            }
         )
    }
}
