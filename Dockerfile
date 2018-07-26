FROM openjdk:8-jre

MAINTAINER "LOCKSS Buildmaster" <buildmaster@lockss.org>

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/lockss/spring-app.jar"]

EXPOSE 54420

ARG JAR_FILE

WORKDIR /opt/lockss

ADD ${JAR_FILE} /opt/lockss/spring-app.jar
