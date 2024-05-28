# EDC Control-Plane PostgreSQL & Hashicorp Vault

This version of the EDC Control-Plane is backed by [PostgreSQL](https://www.postgresql.org/) and [HashiCorp Vault](https://www.vaultproject.io/docs).

## Building

```shell
./gradlew :edc-controlplane:edc-controlplane-postgresql-hashicorp-vault:dockerize
```

## Configuration

Details regarding each configuration property can be found in the [docs for the chart](../../charts/tractusx-connector/README.md).

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
  -i edc-controlplane-postgresql-hashicorp-vault:latest
```
