FROM debian:jessie

ENV JAVA_VERSION=8 \
    JAVA_UPDATE=141 \
    JAVA_BUILD=15 \
    JAVA_PATH=336fa29ff2bb4ef291e347e091f7f4a7 \
    JAVA_HOME="/usr/lib/jvm/default-jvm"

RUN apt-get update -q && \
    apt-get install -q -y --no-install-recommends wget ca-certificates unzip && \
    cd "/tmp" && \
    wget -q --header "Cookie: oraclelicense=accept-securebackup-cookie;" \
        "http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION}u${JAVA_UPDATE}-b${JAVA_BUILD}/${JAVA_PATH}/jdk-${JAVA_VERSION}u${JAVA_UPDATE}-linux-x64.tar.gz" && \
    tar -xzf "jdk-${JAVA_VERSION}u${JAVA_UPDATE}-linux-x64.tar.gz" && \
    mkdir -p "/usr/lib/jvm" && \
    mv "/tmp/jdk1.${JAVA_VERSION}.0_${JAVA_UPDATE}" "/usr/lib/jvm/java-${JAVA_VERSION}-oracle" && \
    ln -s "/usr/lib/jvm/java-${JAVA_VERSION}-oracle" "$JAVA_HOME" && \
    rm "/tmp/"*
