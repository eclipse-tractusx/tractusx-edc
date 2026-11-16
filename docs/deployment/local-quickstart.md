# Local quick start (kind)

In this guide you build the images, install the production chart on a local [kind](https://kind.sigs.k8s.io/) cluster
with PostgreSQL and Vault, and check that the connector is healthy. The steps follow the repository's own deployment test
([`.github/actions/run-deployment-test/action.yml`](../../.github/actions/run-deployment-test/action.yml) and
[`.github/workflows/deployment-test.yaml`](../../.github/workflows/deployment-test.yaml)), so they are exercised on every
CI run.

!!! info "What you get, and what you don't"

    The test values use **dummy identity settings** (DID, STS, BDRS). The connector starts, its APIs answer and the Helm
    tests pass, but it cannot talk to a real dataspace. For a complete local dataspace with two connectors and identity
    services, use the [MXD tutorial](https://github.com/eclipse-tractusx/tutorial-resources/tree/main/mxd).

## Prerequisites

| Tool | Version used in CI |
|---|---|
| JDK | 21 |
| Docker | any recent version |
| kind | v0.32.0 or newer (needed for current `kindest/node` images) |
| kubectl | v1.31 or newer |
| Helm | v3.16 or newer |

## 1. Create the cluster

```bash
kind create cluster --image kindest/node:v1.36.1
```

## 2. Build the images

From the repository root:

```bash
./gradlew -p edc-controlplane/edc-controlplane-postgresql-hashicorp-vault dockerize
./gradlew -p edc-dataplane/edc-dataplane-hashicorp-vault dockerize
```

Each `dockerize` call builds a shadow jar and a local image tagged `<module>:latest` and `<module>:<version>`.

## 3. Load the images into kind

```bash
kind load docker-image \
  edc-controlplane-postgresql-hashicorp-vault:latest \
  edc-dataplane-hashicorp-vault:latest
```

## 4. Install the chart

```bash
helm install tx-prod charts/tractusx-connector \
  -f edc-tests/deployment/src/main/resources/helm/tractusx-connector-test.yaml \
  --dependency-update --wait-for-jobs --timeout=120s

kubectl rollout status deployment tx-prod-controlplane
kubectl rollout status deployment tx-prod-dataplane
```

The test values file
([`tractusx-connector-test.yaml`](../../edc-tests/deployment/src/main/resources/helm/tractusx-connector-test.yaml)):

- uses the local images (`pullPolicy: Never`, tag `latest`);
- keeps the chart defaults `install.postgresql: true` and `install.vault: true`, so a PostgreSQL and a **dev-mode**
  Vault are installed next to the connector;
- seeds the Vault with the secrets the connector expects (`client-secret`, `aes-keys`) in a `postStart` hook;
- uses dummy DID, BPN, STS and BDRS values.

## 5. Run the Helm tests

```bash
helm test tx-prod
```

The chart's tests check that:

1. the data plane answers `GET /api/check/readiness`;
2. the Management API accepts a request **with** the API key (`POST /management/v3/business-partner-groups`);
3. the Management API rejects a request **without** the API key.

## 6. Talk to the Management API

```bash
kubectl port-forward deployment/tx-prod-controlplane 8081:8081 &

curl -s -X POST http://localhost:8081/management/v3/assets/request \
  -H "X-Api-Key: password" -H "Content-Type: application/json" \
  -d '{"@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"}, "@type": "QuerySpec"}'
```

An empty JSON array `[]` means the connector is up and you are authenticated. `password` is the chart default for
`controlplane.endpoints.management.authKey`. Never keep it outside your laptop.

Continue with [Tutorial 1: your first data exchange](../tutorials/first-data-exchange.md) to create an asset and an offer.

## Even simpler: the in-memory chart

To try the API without PostgreSQL and Vault, use the in-memory runtime, again as CI does:

```bash
./gradlew -p edc-controlplane/edc-runtime-memory dockerize
kind load docker-image edc-runtime-memory:latest

helm install tx-inmem charts/tractusx-connector-memory \
  -f edc-tests/deployment/src/main/resources/helm/tractusx-connector-memory-test.yaml \
  --wait-for-jobs --timeout=120s --dependency-update
kubectl rollout status deployment tx-inmem
helm test tx-inmem
```

Secrets for the in-memory vault are seeded with the `vault.secrets` value, in the form `key1:secret1;key2:secret2`.

## Clean up

```bash
helm uninstall tx-prod tx-inmem
kind delete cluster
```

## Without Kubernetes

Each runtime is a plain Java application (main class `org.eclipse.edc.boot.system.runtime.BaseRuntime`). The image
entrypoint reads settings from `/app/configuration.properties` and from environment variables, so you can also use
`docker run` with a mounted properties file. See the README of each runtime module for an example:
[memory runtime](../../edc-controlplane/edc-runtime-memory/README.md),
[control plane](../../edc-controlplane/edc-controlplane-postgresql-hashicorp-vault/README.md),
[data plane](../../edc-dataplane/edc-dataplane-hashicorp-vault/README.md). The ports a runtime listens on are whatever
you configure in the `web.http.*` settings; the chart defaults are listed in [Helm chart](helm-chart.md#ports-and-paths).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
