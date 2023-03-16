# EDC Control-Plane backed by In-Memory Stores

### Building

```shell
./gradlew :edc-controlplane:edc-controlplane-memory:dockerize
```

### Configuration (configuration.properties)

Listed below are configuration keys needed to get the `edc-controlplane-memory` up and running.
Details regarding each configuration property can be found at the [documentary section of the EDC](https://github.com/eclipse-edc/Connector/tree/main/docs).

| Key  	                                                | Required  | Example | Description |
|---	                                                |---	    |---	  |---          |
| edc.api.auth.key                                      |           | password | default value: random UUID |
| web.http.default.port                                 | X         | 8080    | |
| web.http.default.path                                 | X         | /api    | |
| web.http.data.port                                    | X         | 8181    | |
| web.http.data.path                                    | X         | /data | |
| web.http.validation.port                              | X         | 8182    | |
| web.http.validation.path                              | X         | /validation | |
| web.http.control.port                                 | X         | 9999 | |
| web.http.control.path                                 | X         | /api/controlplane/control | |
| web.http.ids.port                                     | X         | 8282 | |
| web.http.ids.path                                     | X         | /api/v1/ids | |
| edc.receiver.http.endpoint                            | X         | http://backend-service | |
| edc.ids.title                                         |           | Eclipse Dataspace Connector | |
| edc.ids.description                                   |           | Eclipse Dataspace Connector | |
| edc.ids.id                                            |           | urn:connector:edc | |
| edc.ids.security.profile                              |           | base | |
| edc.ids.endpoint                                      |           | http://localhost:8282/api/v1/ids | |
| edc.ids.maintainer                                    |           | http://localhost | |
| edc.ids.curator                                       |           | http://localhost | |
| edc.ids.catalog.id                                    |           | urn:catalog:default | |
| ids.webhook.address                                   |           | http://localhost:8282/api/v1/ids | |
| edc.hostname                                          |           | localhost | |
| edc.oauth.token.url                                   | X         | https://daps.catena-x.net | |
| edc.oauth.public.key.alias                            | X         | key-to-daps-certificate-in-keyvault | |
| edc.oauth.private.key.alias                           | X         | key-to-private-key-in-keyvault | |
| edc.oauth.client.id                                   | X         | daps-oauth-client-id | |
| edc.vault.clientid                                    | X         | 00000000-1111-2222-3333-444444444444 | | 
| edc.vault.tenantid                                    | X         | 55555555-6666-7777-8888-999999999999 | |
| edc.vault.name                                        | X         | my-vault-name | |
| edc.vault.clientsecret                                | X         | 34-chars-secret | |
| edc.transfer.proxy.endpoint                  | X         | | |
| edc.transfer.proxy.token.signer.privatekey.alias  | X         | | |

#### Example configuration.properties

JDK properties-style configuration of the EDC Control-Plane is expected to be mounted to `/app/configuration.properties` within the container.

```shell
# Create configuration.properties
export CONFIGURATION_PROPERTIES_FILE=$(mktemp /tmp/configuration.properties.XXXXXX)
cat << 'EOF' > ${CONFIGURATION_PROPERTIES_FILE}

web.http.default.port=8080
web.http.default.path=/api
web.http.data.port=8181
web.http.data.path=/data
web.http.validation.port=8182
web.http.validation.path=/validation
web.http.control.port=9999
web.http.control.path=/api/controlplane/control
web.http.ids.port=8282
web.http.ids.path=/api/v1/ids

edc.receiver.http.endpoint=http://backend-service

edc.ids.title=Eclipse Dataspace Connector
edc.ids.description=Eclipse Dataspace Connector
edc.ids.id=urn:connector:edc
edc.ids.security.profile=base
edc.ids.endpoint=http://localhost:8282/api/v1/ids
edc.ids.maintainer=http://localhost
edc.ids.curator=http://localhost
edc.ids.catalog.id=urn:catalog:default
ids.webhook.address=http://localhost:8282/api/v1/ids

edc.hostname=localhost

edc.api.auth.key=password

# OAuth / DAPS related configuration
edc.oauth.token.url=https://daps.catena-x.net
edc.oauth.public.key.alias=key-to-daps-certificate-in-keyvault
edc.oauth.private.key.alias=key-to-private-key-in-keyvault
edc.oauth.client.id=daps-oauth-client-id

# Azure vault related configuration
edc.vault.clientid=00000000-1111-2222-3333-444444444444
edc.vault.tenantid=55555555-6666-7777-8888-999999999999
edc.vault.name=my-vault-name
edc.vault.clientsecret=34-chars-secret

# Control- / Data- Plane configuration
edc.transfer.proxy.endpoint=http://dataplane-public-endpoint/public
edc.transfer.proxy.token.signer.privatekey.alias=azure-vault-token-signer-private-key
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
    -p 8080:8080 -p 8181:8181 -p 8182:8182 -p 8282:8282 -p 9090:9090 -p 9999:9999 \
    -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
    -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
    -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
    -i edc-controlplane-memory:latest
```