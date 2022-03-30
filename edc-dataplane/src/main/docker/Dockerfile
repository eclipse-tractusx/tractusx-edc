FROM gcr.io/distroless/java11-debian11
ARG JAR

WORKDIR /app

COPY $JAR edc-dataplane.jar

CMD ["-Djava.security.edg=file:/dev/.urandom", "-Dedc.fs.config=/app/configuration.properties", "-jar", "edc-dataplane.jar"]