FROM openjdk:11-jre-slim-buster
ARG JAR

ENV CONFIGURATION_PROPERTIES=/app/configuration.properties

RUN useradd --create-home app

WORKDIR /app

USER app

COPY $JAR edc-controlplane.jar

ENTRYPOINT java \
    -Djava.security.edg=file:/dev/.urandom \
    -Dedc.fs.config="${CONFIGURATION_PROPERTIES}" \
    -jar edc-controlplane.jar