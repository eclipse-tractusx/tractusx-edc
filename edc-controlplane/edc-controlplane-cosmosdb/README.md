# EDC Control-Plane backed by [Azure CosmosDB](https://docs.microsoft.com/en-us/azure/cosmos-db/introduction)

### Building

```shell
./mvnw -pl .,edc-controlplane/edc-controlplane-cosmosdb -am package -Pwith-docker-image
```

### Key Vault Setup

The connector will lookup a secret in the key vault, that has the same alias as the `account-name` setting for CosmosDB (e.g. `edc.assetindex.cosmos.account-name`).
This secret must contain the primary or the secondard CosmosDB Read-write key.

### Configuration

Listed below are configuration keys needed to get the `edc-controlplane-cosmosdb` up and running. 
Details regarding each configuration property can be found at the [documentary section of the EDC](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/main/docs).

| Key  	                                                        | Required  | Example | Description |
|---	                                                        |---        |---	  |---          |
| web.http.default.port                                         | X         | 8080    | |
| web.http.default.path                                         | X         | /api    | |
| web.http.data.port                                            | X         | 8181    | |
| web.http.data.path                                            | X         | /data   | |
| web.http.validation.port                                      | X         | 8182    | |
| web.http.validation.path                                      | X         | /validation | |
| web.http.control.port                                         | X         | 9999 | |
| web.http.control.path                                         | X         | /api/controlplane/control | |
| web.http.ids.port                                             | X         | 8282 | |
| web.http.ids.path                                             | X         | /api/v1/ids | |
| edc.receiver.http.endpoint                                    | X         | http://backend-service | |
| edc.ids.title                                                 |           | Eclipse Dataspace Connector | |
| edc.ids.description                                           |           | Eclipse Dataspace Connector | |
| edc.ids.id                                                    |           | urn:connector:edc | |
| edc.ids.security.profile                                      |           | base | |
| edc.ids.endpoint                                              |           | http://localhost:8282/api/v1/ids | |
| edc.ids.maintainer                                            |           | http://localhost | |
| edc.ids.curator                                               |           | http://localhost | |
| edc.ids.catalog.id                                            |           | urn:catalog:default | |
| ids.webhook.address                                           |           | http://localhost:8282/api/v1/ids | |
| edc.api.control.auth.apikey.key                               |           | X-Api-Key | |
| edc.api.control.auth.apikey.value                             |           | super-strong-api-key | |
| edc.hostname                                                  |           | localhost | |
| edc.oauth.token.url                                           | X         | https://daps.catena-x.net | |
| edc.oauth.public.key.alias                                    | X         | key-to-daps-certificate-in-keyvault | |
| edc.oauth.private.key.alias                                   | X         | key-to-private-key-in-keyvault | |
| edc.oauth.client.id                                           | X         | daps-oauth-client-id | |
| edc.vault.clientid                                            | X         | 00000000-1111-2222-3333-444444444444 | |
| edc.vault.tenantid                                            | X         | 55555555-6666-7777-8888-999999999999 | |
| edc.vault.name                                                | X         | my-vault-name | |
| edc.vault.clientsecret                                        | X         | 34-chars-secret | |
| edc.assetindex.cosmos.account-name                            | X         | cosmosdb-assetindex-account-name | |
| edc.assetindex.cosmos.database-name                           | X         | asset-index | |
| edc.assetindex.cosmos.preferred-region                        | X         | westeurope | |
| edc.assetindex.cosmos.container-name                          | X         | cosmosdb-assetindex-container-name | |
| edc.contractdefinitionstore.cosmos.account-name               | X         | cosmosdb-contractdefinitionstore-account-name | |
| edc.contractdefinitionstore.cosmos.database-name              | X         | contract-definition-store | |
| edc.contractdefinitionstore.cosmos.preferred-region           | X         | westeurope | |
| edc.contractdefinitionstore.cosmos.container-name             | X         | cosmosdb-contractdefinitionstore-container-name | |
| edc.contractnegotiationstore.cosmos.account-name              | X         | cosmosdb-contractnegotiationstore-account-name | |
| edc.contractnegotiationstore.cosmos.database-name             | X         | contract-negotiation-store | |
| edc.contractnegotiationstore.cosmos.preferred-region          | X         | westeurope | |
| edc.contractnegotiationstore.cosmos.container-name            | X         | cosmosdb-contractnegotiationstore-container-name | |
| edc.contractnegotiationstore.cosmos.allow.sproc.autoupload    |           | true | |
| edc.transfer-process-store.cosmos.account.name                | X         | cosmosdb-contractnegotiationstore-account-name | |
| edc.transfer-process-store.database.name                      | X         | transfer-process-store | |
| edc.transfer-process-store.cosmos.preferred-region            | X         | westeurope | |
| edc.transfer-process-store.cosmos.container-name              | X         | cosmosdb-transfer-process-store-container-name | |
| edc.transfer-process-store.cosmos.allow.sproc.autoupload      |           | true | |
| edc.transfer.proxy.endpoint                                   | X         | http://dataplane-public-endpoint/public | |
| edc.transfer.proxy.token.signer.privatekey.alias              | X         | key-of-private-key-in-keyvault-to-sign-transfer-token | |

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

edc.api.control.auth.apikey.key=X-Api-Key
edc.api.control.auth.apikey.value=pass

edc.hostname=localhost

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

# Azure CosmosDB related configuration
edc.assetindex.cosmos.account-name=cosmosdb-assetindex-account-name
edc.assetindex.cosmos.database-name=asset-index
edc.assetindex.cosmos.preferred-region=westeurope
edc.assetindex.cosmos.container-name=cosmosdb-assetindex-container-name
edc.contractdefinitionstore.cosmos.account-name=cosmosdb-contractdefinitionstore-account-name
edc.contractdefinitionstore.cosmos.database-name=contract-definition-store
edc.contractdefinitionstore.cosmos.preferred-region=westeurope
edc.contractdefinitionstore.cosmos.container-name=cosmosdb-contractdefinitionstore-container-name
edc.contractnegotiationstore.cosmos.account-name=cosmosdb-contractnegotiationstore-account-name
edc.contractnegotiationstore.cosmos.database-name=contract-negotiation-store
edc.contractnegotiationstore.cosmos.preferred-region=westeurope
edc.contractnegotiationstore.cosmos.container-name=cosmosdb-contractnegotiationstore-container-name
edc.transfer-process-store.cosmos.account.name=cosmosdb-contractnegotiationstore-account-name
edc.transfer-process-store.database.name=transfer-process-store
edc.transfer-process-store.cosmos.preferred-region=westeurope
edc.transfer-process-store.cosmos.container-name=cosmosdb-transfer-process-store-container-name

EOF
```

#### Example logging.properties
```shell
# Create logging.properties
export LOGGING_PROPERTIES_FILE=$(mktemp /tmp/logging.properties.XXXXXX)
cat << 'EOF' > ${LOGGING_PROPERTIES_FILE}
.level=INFO
org.eclipse.dataspaceconnector.level=ALL
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
  -i edc-controlplane-cosmosdb:latest
```