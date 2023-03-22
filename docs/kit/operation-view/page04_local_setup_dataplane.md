# Setting up a local EDC Data Plane

## Basics

The EDC is split into control and data plane.
The data plane handles the actual data transfer between parties.
The control plane manages the following:

- Resource Management (e.g. Assets, Policies & Contract Definitions CRUD)
- Contract Offering & Contract Negotiation
- Data Transfer Coordination / Management

The EDC data plane can run as a single container on your local machine.
The following is a short overview of the necessary steps to start up the default configuration.

## Building

TractusX EDC is build with Maven. The following command creates the default data plane as a docker container:

```shell
./mvnw -pl .,edc-dataplane/edc-dataplane-hashicorp-vault -am package -Pwith-docker-image
```

## Example Configuration

The following commands can be used to create the necessary configuration files for the EDC container.
They assume sane - but unsafe - defaults. An explanation of the respective parameters can be found [here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-dataplane/edc-dataplane-hashicorp-vault/README.md).

::: Caution

The following configuration is for testing purposes only. Do not use it in production.
:::

### Example configuration.properties

```shell
# Create configuration.properties
export CONFIGURATION_PROPERTIES_FILE=$(mktemp /tmp/configuration.properties.XXXXXX)
cat << 'EOF' > ${CONFIGURATION_PROPERTIES_FILE}

web.http.default.port=8080
web.http.default.path=/api
web.http.public.port=8185
web.http.public.path=/public
web.http.control.port=9999
web.http.control.path=/api/dataplane/control

# Validation endpoint of controlplane
edc.dataplane.token.validation.endpoint=http://controlplane:8182/validation/token

# EDC hostname
edc.hostname=localhost

# HashiCorp vault related configuration
edc.vault.hashicorp.url=http://vault
edc.vault.hashicorp.token=55555555-6666-7777-8888-999999999999
edc.vault.hashicorp.timeout.seconds=30
EOF
```

### Example logging.properties

```shell
# Create logging.properties
export LOGGING_PROPERTIES_FILE=$(mktemp /tmp/logging.properties.XXXXXX)
cat << 'EOF' > ${LOGGING_PROPERTIES_FILE}
.level=INFO
org.eclipse.edc.level=ALL
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
java.util.logging.ConsoleHandler.level=ALL
java.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n
EOF
```

### Example opentelemetry.properties

```shell
# Create opentelemetry.properties
export OPENTELEMETRY_PROPERTIES_FILE=$(mktemp /tmp/opentelemetry.properties.XXXXXX)
cat << 'EOF' > ${OPENTELEMETRY_PROPERTIES_FILE}
otel.javaagent.enabled=true
otel.javaagent.debug=false
EOF
```

## Running

Once the configuration is created, the container can be run directly via docker.

```shell
docker run \
  -p 8080:8080 -p 8185:8185 -p 9999:9999 -p 9090:9090 \
  -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
  -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
  -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
  -i edc-dataplane-hashicorp-vault:latest
```
