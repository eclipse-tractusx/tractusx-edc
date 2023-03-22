# Setting up a local EDC Control Plane

## Basics

The EDC is split into control and data plane.
The data plane handles the actual data transfer between parties.
The control plane manages the following:

- Resource Management (e.g. Assets, Policies & Contract Definitions CRUD)
- Contract Offering & Contract Negotiation
- Data Transfer Coordination / Management

The EDC control plane can run as a single container on your local machine.
The following is a short overview of the necessary steps to start up the default configuration.

## Building

TractusX EDC is build with Maven. The following command creates the default control plane as a docker container:

```shell
./mvnw -pl .,edc-controlplane/edc-controlplane-postgresql-hashicorp-vault -am package -Pwith-docker-image
```

## Example Configuration

The following commands can be used to create the necessary configuration files for the EDC container.
They assume sane - but unsafe - defaults. An explanation of the respective parameters can be found [here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-controlplane/edc-controlplane-postgresql-hashicorp-vault/README.md).

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

# HashiCorp vault related configuration
edc.vault.hashicorp.url=http://vault
edc.vault.hashicorp.token=55555555-6666-7777-8888-999999999999
edc.vault.hashicorp.timeout.seconds=30

# Control- / Data- Plane configuration
edc.transfer.proxy.endpoint=http://dataplane-public-endpoint/public
edc.transfer.proxy.token.signer.privatekey.alias=token-signer-private-key

# Postgresql related configuration
edc.datasource.asset.name=asset
edc.datasource.asset.url=jdbc:postgresql://postgres.svc.cluster.local:5432/edc_asset
edc.datasource.asset.user=user
edc.datasource.asset.password=pass
edc.datasource.contractdefinition.name=contractdefinition
edc.datasource.contractdefinition.url=jdbc:postgresql://postgres.svc.cluster.local:5432/edc_contractdefinition
edc.datasource.contractdefinition.user=user
edc.datasource.contractdefinition.password=pass
edc.datasource.contractnegotiation.name=contractnegotiation
edc.datasource.contractnegotiation.url=jdbc:postgresql://postgres.svc.cluster.local:5432/edc_contractnegotiation
edc.datasource.contractnegotiation.user=user
edc.datasource.contractnegotiation.password=pass
edc.datasource.policy.name=policy
edc.datasource.policy.url=jdbc:postgresql://postgres.svc.cluster.local:5432/edc_policy
edc.datasource.policy.user=user
edc.datasource.policy.password=pass
edc.datasource.transferprocess.name=transferprocess
edc.datasource.transferprocess.url=jdbc:postgresql://postgres.svc.cluster.local:5432/edc_transferprocess
edc.datasource.transferprocess.user=user
edc.datasource.transferprocess.password=pass
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
otel.javaagent.enabled=false
otel.javaagent.debug=false
EOF
```

## Running the Control Plane

Once the configuration is created, the container can be run directly via docker.

```shell
docker run \
  -p 8080:8080 -p 8181:8181 -p 8182:8182 -p 8282:8282 -p 9090:9090 -p 9999:9999 \
  -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
  -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
  -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
  -i edc-controlplane-postgresql-hashicorp-vault:latest
```
