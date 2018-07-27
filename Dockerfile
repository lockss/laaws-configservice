FROM openjdk:8-jdk

MAINTAINER "LOCKSS Buildmaster" <buildmaster@lockss.org>

WORKDIR /opt/lockss

ARG SVC_PORT
EXPOSE ${SVC_PORT}

ARG JAR_FILE
ADD ${JAR_FILE} /opt/lockss/spring-app.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/lockss/spring-app.jar"]

