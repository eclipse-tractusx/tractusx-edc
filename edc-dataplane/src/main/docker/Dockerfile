FROM alpine:3.14 as otel

ENV OTEL_AGENT_LOCATION "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.1/opentelemetry-javaagent.jar"

RUN wget ${OTEL_AGENT_LOCATION} -O /tmp/opentelemetry-javaagent.jar

FROM gcr.io/distroless/java11-debian11
ARG JAR

WORKDIR /app

COPY --from=otel /tmp/opentelemetry-javaagent.jar .
COPY $JAR edc-dataplane.jar

CMD ["-javaagent:/app/opentelemetry-javaagent.jar", \
     "-Dedc.fs.config=/app/configuration.properties", \
     "-Djava.util.logging.config.file=/app/logging.properties", \
     "-Dotel.javaagent.configuration-file=/app/opentelemetry.properties", \
     "-Dotel.metrics.exporter=prometheus", \
     "-Dotel.exporter.prometheus.port=9090", \
     "-Djava.security.edg=file:/dev/.urandom", \
     "-jar", \
     "edc-dataplane.jar"]