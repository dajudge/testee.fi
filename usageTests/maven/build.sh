#! /bin/sh

TESTEEFI_VERSION=$1

USER_HOME_DIR="/root"
MAVEN_VERSION=3.5.0
BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries
BASE_DIR=/tmp/maven

mkdir -p $BASE_DIR $BASE_DIR/ref
wget -q -O /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz
tar -xzf /tmp/apache-maven.tar.gz -C $BASE_DIR --strip-components=1
rm -f /tmp/apache-maven.tar.gz

MAVEN_HOME=$BASE_DIR
MAVEN_CONFIG="$USER_HOME_DIR/.m2"

echo "Building with $TESTEEFI_VERSION"

$BASE_DIR/bin/mvn -U -gs settings.xml clean install -Dnexus.username=$MAVEN_USER -Dnexus.password=$MAVEN_PASSWORD -Dtesteefi.version=$TESTEEFI_VERSION
exit $?
