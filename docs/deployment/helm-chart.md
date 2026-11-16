# Helm chart

The `tractusx-connector` chart deploys a control plane and a data plane. This page explains the values you will touch
in almost every installation. For the complete, generated list of every value and its default, see the chart README:
[`charts/tractusx-connector/README.md`](../../charts/tractusx-connector/README.md).

## Install from the Tractus-X Helm repository

```bash
helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
helm repo update
helm search repo tractusx-edc/tractusx-connector -l   # list the available versions

helm install my-connector tractusx-edc/tractusx-connector \
  --version <chart-version> \
  -f my-values.yaml
```

## A minimal production values file

The example below assumes an **existing** PostgreSQL and Vault. Every `CHANGEME`-style default must be replaced.

```yaml title="my-values.yaml"
install:
  postgresql: false            # bring your own database
  vault: false                 # bring your own Vault

participant:
  id: "did:web:connector.example.com"      # your DID
  bpnl: "BPNL000000000001"                 # your BPNL
  contextId: "5b9e7e4e-…"                  # generate once, never change

dcp:
  trustedIssuers:
    - "did:web:issuer.example.org"         # DID of the credential issuer of your dataspace
  sts:
    oauth:
      token_url: "https://div.example.org/oauth/token"
      client:
        id: "my-client-id"
        secret_alias: "client-secret"      # Vault key holding the client secret

controlplane:
  endpoints:
    management:
      authKey: "set-from-secret"           # placeholder, overridden from a Secret (see "Configuration")
  bdrs:
    server:
      url: "https://bdrs.example.org/api/directory"
  ingresses:
    - enabled: true                        # public: DSP protocol endpoint only
      hostname: "edc-control.example.com"
      endpoints: [protocol]
      className: nginx
      tls:
        enabled: true
  logs:
    level: INFO

dataplane:
  token:
    signer:
      privatekey_alias: "tx-token-signer"      # Vault key: private key for signing EDR tokens
    verifier:
      publickey_alias: "tx-token-verifier"     # Vault key: matching public key
  ingresses:
    - enabled: true                        # public: data plane public API only
      hostname: "edc-data.example.com"
      endpoints: [public]
      className: nginx
      tls:
        enabled: true
  logs:
    level: INFO

postgresql:
  jdbcUrl: "jdbc:postgresql://postgres.example.com:5432/edc"
  # credentials: see "Configuration" for passing them as Secrets

vault:
  hashicorp:
    url: "https://vault.example.com"
    # token: see "Configuration" for passing it as a Secret
```

!!! tip "Hostnames decide the URLs your partners see"

    The DSP callback address and the data plane public URL are derived from the **first** ingress of each plane,
    and switch to `https://` when that ingress has `tls.enabled: true`. To set them explicitly, use
    `controlplane.url.protocol` and `dataplane.url.public`.

## The values that matter

### Identity

| Value | Default | Meaning |
|---|---|---|
| `participant.id` | `did:web:changeme` | Your connector's **DID**. Since 0.13 this is the DID, not the BPNL. |
| `participant.bpnl` | `BPNLCHANGEME` | Your BPNL. |
| `participant.contextId` | `UUID CHANGEME` | Participant context id. Used as a key in the database. **Never change it once set.** |
| `dcp.trustedIssuers` | `[]` | DIDs of trusted credential issuers. Each entry is a string or `{id, supportedTypes}`. |
| `dcp.sts.oauth.token_url`, `dcp.sts.oauth.client.id`, `dcp.sts.oauth.client.secret_alias` | `CHANGEME` | OAuth client for the STS/DIV. The secret itself is read from Vault under the alias. |
| `dcp.sts.div.url` | empty | Base URL of the DIV, if used. |
| `dcp.cache.enabled`, `dcp.cache.validity` | `true`, `86400` | Cache for verifiable presentations (seconds). |
| `controlplane.bdrs.server.url` | `CHANGEME` | BDRS base URL. |

### Security of the APIs

| Value | Default | Meaning |
|---|---|---|
| `controlplane.endpoints.management.authKey` | `password` | Value of the `X-Api-Key` header for the Management API. |
| `controlplane.endpoints.management.jwksUrl` | empty | If set, the Management API uses **delegated** (JWT) authentication instead of the API key. |
| `dataplane.endpoints.proxy.authKey` | `password` | API key of the consumer proxy API. |
| `dataplane.token.signer.privatekey_alias`, `dataplane.token.verifier.publickey_alias` | `CHANGEME` | Vault aliases of the key pair that signs and verifies data plane access tokens. |
| `controlplane.policy.validation.enabled` | `true` | Reject policies that do not follow the Catena-X policy rules. See [Catena-X policies](../architecture/policies.md). |

### Infrastructure

| Value | Default | Meaning |
|---|---|---|
| `install.postgresql`, `install.vault` | `true`, `true` | Install PostgreSQL and Vault as subcharts. **Set both to `false` for production.** |
| `postgresql.jdbcUrl` | `jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/edc` | Used when `install.postgresql` is `false`. |
| `vault.hashicorp.url`, `vault.hashicorp.token` | `http://{{ .Release.Name }}-vault:8200`, `root` | Vault address and token. |
| `vault.hashicorp.paths.secret`, `vault.hashicorp.paths.folder` | `/v1/secret`, empty | KV path and an optional folder for all keys. |
| `networkPolicy.enabled` | `false` | Create ingress-only NetworkPolicies for both planes. |

!!! danger "The bundled Vault is a dev-mode Vault"

    With `install.vault: true` the chart installs Vault in **dev mode** with root token `root`, and the bundled
    PostgreSQL has persistence **disabled**. Both are for tests only.

## Ports and paths

| Plane | Endpoint | Port | Path | Exposure |
|---|---|---|---|---|
| Control plane | default (health checks) | 8080 | `/api` | internal |
| Control plane | management | 8081 | `/management` | **internal only**, never on the internet |
| Control plane | control | 8083 | `/control` | internal (data plane ↔ control plane) |
| Control plane | protocol (DSP) | 8084 | `/api/v1/dsp` | **public** |
| Control plane | metrics | 9090 | `/metrics` | internal |
| Data plane | default (health checks) | 8080 | `/api` | internal |
| Data plane | public | 8081 | `/api/public` | **public** |
| Data plane | control | 8084 | `/api/control` | internal |
| Data plane | proxy (consumer proxy) | 8186 | `/proxy` | internal |
| Data plane | metrics | 9090 | `/metrics` | internal |

Health probes call `GET /api/check/liveness` and `GET /api/check/readiness` on the default port.

!!! note "Changing the proxy port"

    The chart does not pass the proxy port to the runtime, which listens on its built-in default `8186`. If you change
    `dataplane.endpoints.proxy.port`, also set `TX_EDC_DPF_CONSUMER_PROXY_PORT` in `dataplane.env`.

## Exposing the endpoints

Each plane has a list of `ingresses` (Kubernetes Ingress) and of `httpRoutes` (Gateway API). Each entry chooses which
`endpoints` it exposes. The chart defaults are already split the right way, but disabled:

- `controlplane.ingresses[0]`: public, exposes `protocol`.
- `controlplane.ingresses[1]`: intranet, exposes `management` and `control`.
- `dataplane.ingresses[0]`: public, exposes `public`.

Each entry supports `className`, `tls.enabled`, `tls.secretName` and cert-manager annotations through
`certManager.issuer` or `certManager.clusterIssuer`.

## Resources and scaling

| Value | Default |
|---|---|
| `*.resources.requests` | `cpu: 500m`, `memory: 1024Mi` |
| `*.resources.limits` | `cpu: 1.5`, `memory: 1024Mi` |
| `*.replicaCount` | `1` |
| `*.autoscaling.enabled` | `false` |

!!! warning "Autoscaling template uses a removed API"

    The HPA templates use `autoscaling/v2beta1`, which current Kubernetes versions no longer serve. Until the chart is
    updated, scale with `replicaCount` instead of `autoscaling.enabled`.

With more than one control plane replica (or autoscaling), the chart disables DID service self-deregistration
automatically.

## The in-memory chart

`tractusx-connector-memory` runs one `edc-runtime-memory` pod. Its values live under `runtime.*` instead of
`controlplane.*`/`dataplane.*`, it has no PostgreSQL or Vault, and secrets are seeded with `vault.secrets`
(`key1:secret1;key2:secret2`). See [`charts/tractusx-connector-memory/README.md`](../../charts/tractusx-connector-memory/README.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
