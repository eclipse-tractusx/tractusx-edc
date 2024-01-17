# EDC Data-Plane with Hashicorp Vault

This build of the EDC Data-Plane utilizes [Hashicorp Vault](https://www.vaultproject.io/) for secret storage.

## Building

```shell
./gardlew :edc-dataplane:edc-dataplane-hashicorp-vault:dockerize
```

## Configuration

Details regarding each configuration property can be found in the [docs for the chart](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector#values).

## Running

```shell
docker run \
  -p 8080:8080 -p 8185:8185 -p 9999:9999 -p 9090:9090 \
  -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
  -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
  -v ${OPENTELEMETRY_PROPERTIES_FILE:-/dev/null}:/app/opentelemetry.properties \
  -i edc-dataplane-hashicorp-vault:latest
```
