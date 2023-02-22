# EDC Data-Plane [Hashicorp Vault](https://www.vaultproject.io/)

### Building

```shell
./mvnw -pl .,edc-dataplane/edc-dataplane-hashicorp-vault -am package -Pwith-docker-image
```

### Configuration

Listed below are configuration keys needed to get the `edc-dataplane-hashicorp-vault` up and running.
Details regarding each configuration property can be found at the [documentary section of the EDC](https://github.com/eclipse-edc/Connector/tree/main/docs).

| Key                                                   | Required  | Example | Description |
|---                                                    |---        |---      |---          |
| web.http.default.port                                 | X         | 8080    | |
| web.http.default.path                                 | X         | /api    | |
| web.http.public.port                                  | X         | 8181    | |
| web.http.public.path                                  | X         |         | |
| web.http.control.port                                 | X         | 9999 | |
| web.http.control.path                                 | X         | /api/controlplane/control | |
| edc.receiver.http.endpoint                            | X         | http://backend-service | |
| edc.hostname                                          |           | localhost | |
| edc.oauth.client.id                                   | X         | daps-oauth-client-id | |
| edc.vault.hashicorp.url                               | X         | http://vault | |
| edc.vault.hashicorp.token                             | X         | 55555555-6666-7777-8888-999999999999 | |
| edc.vault.hashicorp.timeout.seconds                   |           | 30 | |
| edc.dataplane.token.validation.endpoint                  | X         | http://controlplane:8182/validation/token | |

#### Example configuration.properties

JDK properties-style configuration of the EDC Control-Plane is expected to be mounted to `/app/configuration.properties` within the container.

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

#### Example logging.properties
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

#### Example opentelemetry.properties
```shell
# Create opentelemetry.properties
export OPENTELEMETRY_PROPERTIES_FILE=$(mktemp /tmp/opentelemetry.properties.XXXXXX)
cat << 'EOF' > ${OPENTELEMETRY_PROPERTIES_FILE}
otel.javaagent.enabled=true
otel.javaagent.debug=false
EOF
```

### Running

```shell
docker run \
  -p 8080:8080 -p 8185:8185 -p 9999:9999 -p 9090:9090 \
  -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
  -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
  -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
  -i edc-dataplane-hashicorp-vault:latest
```