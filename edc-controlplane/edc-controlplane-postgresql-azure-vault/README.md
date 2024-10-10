# EDC Control-Plane PostgreSQL & Azure Key Vault

DEPRECATED: this module won't be available anymore after version 0.8.0

This version of the EDC Control-Plane is backed by [PostgreSQL](https://www.postgresql.org/) and [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview).

## Building

```shell
./gradlew :edc-controlplane:edc-controlplane-postgresql-azure-vault:dockerize
```

## Configuration

Details regarding each configuration property can be found in the [docs for the chart](../../charts/tractusx-connector-azure-vault/README.md).

Please note that the properties list may not be complete as the tractusx-edc may elect to fall back to the default behavior of an
extension. When in doubt, check the extensions' README that will likely be in [this repo's](../../edc-extensions) or in the [eclipse-edc's](https://github.com/eclipse-edc/Connector/tree/main/extensions) 
`extensions` folder.

## Running

```shell
docker run \
  -p 8080:8080 -p 8181:8181 -p 8282:8282 -p 9090:9090 -p 9999:9999 \
  -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
  -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
  -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
  -i edc-controlplane-postgresql-azure-vault:latest
```
