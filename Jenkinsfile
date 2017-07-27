node() {

    stage("Checkout source code") {
	checkout([$class: 'GitSCM', branches: [[name: COMMITID]], userRemoteConfigs: [[url: 'https://github.com/dajudge/testee.fi.git']]])
    }

    withBuildEnv() {
        stage("Build, Test and publish to Nexus") {
            try {
                sh 'gpg --import $GPG_KEY_FILE'
                sh 'TESTEEFI_IGNORE_TEST_FAILURES=true TESTEEFI_SIGN_ARTIFACTS=true ./gradlew -Dorg.gradle.project.signing.keyId=CBC58EE1 -Dorg.gradle.project.signing.password=$GPG_PASSWORD -Dorg.gradle.project.signing.secretKeyRingFile=$HOME/.gnupg/secring.gpg --no-daemon clean build uploadArchives --stacktrace'
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
                docker.build("testeefi-usage-$imageVersion")
            }

            def versionToTest = readFile("version.txt")
            dir("usageTests/maven/") {
                dockerImage.inside {
                    withCredentials([
                                usernamePassword(
                                credentialsId: 'maven',
                                usernameVariable: 'MAVEN_USER',
                                passwordVariable: 'MAVEN_PASSWORD'
                    )]) {
                        sh "chmod 755 build.sh"
                        def status = sh(
                            script: "./build.sh $versionToTest",
                            returnStatus: true
                        )
                        println status
                        if(status != 0) {
                            println "Script returned nonzero status, build is unstable"
                            currentBuild.result == 'UNSTABLE'
                        }
                    }
                }
            }
        }
    }
}

def withBuildEnv(closure) {
    def jdkImage, psqlImage

    dir("builder/jdk8") {
        jdkImage = docker.build "testeefi-jdk8:latest"
    }

    dir("builder/psql") {
        psqlImage = docker.build "testeefi-psql:latest"
    }

    def psqlContainer = "testeefi-psql-${System.currentTimeMillis()}"
    psqlImage.withRun("--name $psqlContainer -e POSTGRES_DB=testeefi -e POSTGRES_PASSWORD=testeefi -e POSTGRES_USER=testeefi") {
        jdkImage.inside(
            [
                "--link ${psqlContainer}:psql",
                "-e TESTEEFI_PSQL_HOSTNAME=psql",
                "-e TESTEEFI_PSQL_DB=testeefi",
                "-e TESTEEFI_PSQL_USER=testeefi",
                "-e TESTEEFI_PSQL_PASSWORD=testeefi"
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
