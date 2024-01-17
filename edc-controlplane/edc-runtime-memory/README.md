# EDC Control-Plane backed by In-Memory Stores

## Security

### In-memory Vault implementation

The goal of this extension is to provide an ephemeral, memory-based vault implementation that can be used in testing or
demo scenarios.

Please not that this vault does not encrypt the secrets, they are held in memory in plain text at runtime! In addition,
its ephemeral nature makes it unsuitable for replicated/multi-instance scenarios, i.e. Kubernetes.

> It is not a secure secret store, please do NOT use it in production workloads!

## Building

```shell
./gradlew :edc-controlplane:edc-runtime-memory:dockerize
```

## Configuration
Details regarding each configuration property can be found in the [docs for the chart](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/tractusx-connector-memory/README.md#values).

## Running

```shell
docker run \
    -e SECRETS="key1:secret1,key2:secret2" \
    -p 8080:8080 -p 8181:8181 -p 8182:8182 -p 8282:8282 -p 9090:9090 -p 9999:9999 \
    -v ${CONFIGURATION_PROPERTIES_FILE:-/dev/null}:/app/configuration.properties \
    -v ${LOGGING_PROPERTIES_FILE:-/dev/null}:/app/logging.properties \
    -i edc-runtime-memory:latest
```
