FROM openjdk:8-jre

MAINTAINER "LOCKSS Buildmaster" <buildmaster@lockss.org>

ENTRYPOINT ["/docker-init.sh"]

EXPOSE 54420

ARG JAR_FILE

WORKDIR /opt/lockss

ADD ${JAR_FILE} /opt/lockss/spring-app.jar

ADD scripts/docker-init.sh /docker-init.sh
