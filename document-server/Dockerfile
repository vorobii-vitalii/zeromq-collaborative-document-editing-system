FROM openjdk:21-jdk

MAINTAINER Vitalii Vorobii

COPY --from=docker.elastic.co/observability/apm-agent-java:latest /usr/agent/elastic-apm-agent.jar /elastic-apm-agent.jar

ADD build/libs/document-server.jar ./app.jar

ENTRYPOINT java -Dcom.sun.management.jmxremote \
	-Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
	-javaagent:/elastic-apm-agent.jar \
	-Delastic.apm.environment=develop \
	-Delastic.apm.secret_token= \
	-Delastic.apm.service_name=document-server \
	-Delastic.apm.application_packages=org.example,com.mongodb,org.zeromq \
	-Delastic.apm.capture_headers=true \
    -Delastic.apm.enable_log_correlation=true \
	-Delastic.apm.server_url=http://apm-agent:8200 \
	-Dcom.sun.management.jmxremote.port=9011 \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Dcom.sun.management.jmxremote.local.only=false \
	-XX:+UseZGC -XX:+ZGenerational \
	-agentlib:jdwp=transport=dt_socket,address=*:8006,server=y,suspend=n -jar app.jar