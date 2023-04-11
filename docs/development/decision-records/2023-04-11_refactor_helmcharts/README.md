# Refactor TractusX-EDC Helm charts

## Decision

The Helm charts provided by Tractusx-EDC will be refactored to be more focused and opinionated. Specifically, there will
be the following charts:

1. `tractusx-connector-memory`: all backing stores are memory-based and thus ephemeral. The vault will also be
   memory-based. _This chart is intended for testing/demo purposes only!_
2. `tractusx-connector`: this is the "production-ready" chart that uses PostgreSQL and Hashicorp-Vault
3. `tractusx-connector-azure-vault`: this is a variant of `tractusx-connector-azure-vault` that uses Azure KeyVault ("
   AZKV") instead
   of Hashicorp as some stakeholders still use AZKV.

These charts and their default configuration will be fully [tested](#testing).

In addition to that, the Docker images will undergo some [refactoring](#docker-image-refactoring) as well.

## Rationale

The current "dynamically composed" helm chart has proven to be a source for issues, and it is difficult to isolate
errors due to the great number of variations. Further, only one particular variant (i.e. postgres+hashicorp) is put to
any semblance of testing (i.e. business tests).

The official recommendation of TractusX-EDC is to use PostgreSQL and HashiCorp Vault, and alongside it, we will provide
charts for easy testing and setting up demos as well as an Azure KeyVault variant for legacy use cases.

> Note: using Azure KeyVault is not officially supported or recommended by TractusX-EDC!

This will also reduce the number of Docker images that need to be published.

## Approach

### Variant 1: `tractusx-connector-memory`

This chart is intended for blackbox-testing or for easily setting up demos etc. It is **not** recommended for anything
else. It will have the following properties:

- all backing stores (Asset Index, Policy Store etc.) are ephemeral in-memory stores
- the vault implementation will either be based also on memory, or on the `FsVault`, which uses local storage to store
  secrets
- an embedded data plane will be used
- no scalability or replication is possible
- DAPS will be used as identity provider, so there is an implicit dependency onto a DAPS instance
- the `edc-runtime-memory` Docker image will be used. That image contains both control plane and data plane.

### Variant 2: `tractusx-connector`

This is the production-ready chart that is published by TractusX-EDC, and it will actually consist of two charts. One is
the `tractusx-runtime` sub-chart, that contains all configuration for data plane and control plane, and the other one is
the top-level `tractusx-connector` chart, that pulls in other charts as dependencies that are needed for one TractusX
connector application. This is sometimes referred to
as ["umbrella chart"](https://helm.sh/docs/howto/charts_tips_and_tricks/#complex-charts-with-many-dependencies).

> Note: this will **not** include sub-charts for DAPS or MinIO.

```shell
tractusx-connector
  |-> tractusx-runtime
  |-> postgres
  |-> hashicorp-vault
```

The `tractusx-runtime` chart has the following properties:

- PostgreSQL is used as persistence backend
- HashiCorp Vault is used as secret store
- the data plane is a separate runtime, i.e. separate pod
- DAPS is used as identity provider
- the `edc-controlplane-postgresql-hashicorp-vault` and `edc-dataplane-hashicorp-vault` Docker images will be used

### Variant 3: `tractusx-connector-azure-vault`

This variant is essentially identical to `tractusx-connector` except for dropping the HashiCorp Vault chart, and
replacing the HashiCorp Vault configuration with Azure KeyVault configuration.

For this, the `edc-controlplane-postgresql-azure-vault` and `edc-dataplane-azure-vault` Docker images will be used.

### Testing

There are several steps to testing our Helm charts:

1. waiting for all pods to come up: using an exemplary configuration, this relies on the health checks, i.e. liveness
   and readiness probe (i.e. the runtime`s observability endpoints) to ensure that (most of) the static
   configuration is correct, no values are missing etc.
2. executing a set of HTTP requests against the management API and assert a successful HTTP status code. For that we
   use [Helm chart tests](https://helm.sh/docs/topics/chart_tests/)

> Note: we refer to this kind of testing as "deployment testing"

### Docker image refactoring

The following changes need to be made to our Docker images:

- rename `edc-controlplane-memory` -> `-edc-runtime-memory`
- in `edc-runtime-memory` use `FsVault` instead of `AzureVault`
- `edc-runtime-memory` contains an embedded data plane
- rename `edc-controlplane-postgresql` -> `edc-controlplane-postgresql-azure-vault`
- delete `edc-controlplane-memory-hashicorp-vault`

thus effectively resulting in the following structure:

```shell
edc-controlplane
|-> edc-runtime-memory
|-> edc-controlplane-postgresql-hashicorp-vault
|-> edc-controlplane-postgresql-azure-vault

edc-dataplane
|-> edc-dataplane-hashicorp-vault
|-> edc-dataplane-azure-vaul
```
