# Extensions

Every module of the repository in one line. For how they fit together read
[Code architecture](../developer/code-architecture.md); for what a module needs to run, open its README.

## Runtimes

| Module | What it is |
|---|---|
| [`edc-controlplane-base`](../../edc-controlplane/edc-controlplane-base) | All control plane extensions, without persistence or vault |
| [`edc-controlplane-postgresql-hashicorp-vault`](../../edc-controlplane/edc-controlplane-postgresql-hashicorp-vault) | **Released control plane image**: base + PostgreSQL + HashiCorp Vault |
| [`edc-dataplane-base`](../../edc-dataplane/edc-dataplane-base) | All data plane extensions, without vault |
| [`edc-dataplane-hashicorp-vault`](../../edc-dataplane/edc-dataplane-hashicorp-vault) | **Released data plane image**: base + HashiCorp Vault |
| [`edc-runtime-memory`](../../edc-controlplane/edc-runtime-memory) | Control and data plane in one process, in memory. For testing only |

## Core and SPI

| Module | Purpose |
|---|---|
| [`core/core-utils`](../../core/core-utils) | Shared helpers (configuration, paths, credential type predicates) |
| [`core/edr-core`](../../core/edr-core) | EDR service, EDR cache wiring and the refresh logic |
| [`core/json-ld-core`](../../core/json-ld-core) | Registers the `tx` and `tx-auth` namespaces and caches common JSON-LD documents |
| [`core/json-ld-cx`](../../core/json-ld-cx) | Registers the Catena-X policy namespace and context |
| [`spi/core-spi`](../../spi/core-spi) | Constants (namespaces, EDR properties), `ProxyHttpData` address type |
| [`spi/bdrs-client-spi`](../../spi/bdrs-client-spi) | `BdrsClient`: resolve BPN ↔ DID |
| [`spi/callback-spi`](../../spi/callback-spi) | In-process callbacks for local events |
| [`spi/dataflow-spi`](../../spi/dataflow-spi) | `DataFlowService.trigger` |
| [`spi/dcp-spi`](../../spi/dcp-spi) | `VerifiablePresentationCache` |
| [`spi/did-document-service-spi`](../../spi/did-document-service-spi) | `DidDocumentServiceClient` |
| [`spi/dsp-spi-08`](../../spi/dsp-spi-08) | DSP 0.8 constants |
| [`spi/edr-spi`](../../spi/edr-spi) | `EdrService`, refresh modes, EDR lock |
| [`spi/tokenrefresh-spi`](../../spi/tokenrefresh-spi) | Token refresh on consumer and provider side |

## Control plane extensions

| Module | Purpose |
|---|---|
| [`agreements`](../../edc-extensions/agreements) | Retire contract agreements before their policy ends |
| [`agreements-bpns`](../../edc-extensions/agreements-bpns) | Stores the provider and consumer BPN of each agreement |
| [`bdrs-client`](../../edc-extensions/bdrs-client) | Resolves BPN ↔ DID against the BPN-DID Resolution Service, with cache |
| [`bpn-validation`](../../edc-extensions/bpn-validation) | `BusinessPartnerNumber` / `BusinessPartnerGroup` constraints and the groups API |
| [`connector-discovery`](../../edc-extensions/connector-discovery) | Discovers DSP versions and connector addresses of a participant |
| [`cx-policy`](../../edc-extensions/cx-policy) | Catena-X policy constraints (CX-0152) and policy validation |
| [`cx-policy-legacy`](../../edc-extensions/cx-policy-legacy) | The same constraints under the deprecated namespace |
| [`data-flow-properties-provider`](../../edc-extensions/data-flow-properties-provider) | Adds consumer DID and BPN to data flow properties |
| [`dataspace-protocol`](../../edc-extensions/dataspace-protocol) | DSP 2025-1 (DID) and DSP 0.8 (BPN) protocol profiles |
| [`dcp`](../../edc-extensions/dcp) | Default credential scopes, remote STS client and presentation cache |
| [`did-document`](../../edc-extensions/did-document) | Registers the connector endpoint in its own DID document |
| [`dsp`](../../edc-extensions/dsp) | DSP 0.8 backport, running next to DSP 2025-1 |
| [`edr/edr-api-v2`](../../edc-extensions/edr/edr-api-v2) | `/v3/edrs` Management API |
| [`edr/edr-callback`](../../edc-extensions/edr/edr-callback) | Starts the transfer automatically after an EDR negotiation |
| [`edr/edr-index-lock-sql`](../../edc-extensions/edr/edr-index-lock-sql) | Database row lock for concurrent EDR refresh |
| [`migrations`](../../edc-extensions/migrations) | Flyway database migrations |
| [`token-interceptor`](../../edc-extensions/token-interceptor) | Adapts the `Authorization` header between DSP 0.8 and 2025-1 |
| [`tokenrefresh-handler`](../../edc-extensions/tokenrefresh-handler) | Consumer side token refresh against the provider |
| [`validators`](../../edc-extensions/validators) | Contract definition checks (access vs usage policy, empty asset selector) |

## Data plane extensions

| Module | Purpose |
|---|---|
| [`dataplane/dataflow`](../../edc-extensions/dataplane/dataflow) | `/v3/dataflows/{id}/trigger` for non-finite push transfers |
| [`dataplane-public-api-v2`](../../edc-extensions/dataplane/dataplane-proxy/dataplane-public-api-v2) | Provider public API: token-checked access to the data source |
| [`edc-dataplane-proxy-consumer-api`](../../edc-extensions/dataplane/dataplane-proxy/edc-dataplane-proxy-consumer-api) | Consumer proxy `/proxy/aas/request` |
| [`dataplane-proxy-http`](../../edc-extensions/dataplane/dataplane-proxy/dataplane-proxy-http) | Data source used by the consumer proxy |
| [`dataplane/dataplane-token-refresh`](../../edc-extensions/dataplane/dataplane-token-refresh) | Provider token issuing and refresh (`POST /token`) |
| [`dataplane/dataplane-util`](../../edc-extensions/dataplane/dataplane-util) | Shared data plane utilities |
| [`dataplane/kafka`](../../edc-extensions/dataplane/kafka) | Kafka PULL transfers. **Not** included in the released images |
| [`non-finite-provider-push`](../../edc-extensions/non-finite-provider-push) | Keeps push flows of non-finite assets open |

## Cross-cutting

| Module | Purpose |
|---|---|
| [`event-subscriber`](../../edc-extensions/event-subscriber) | Sends connector events to an OpenTelemetry collector |
| [`log4j2-monitor`](../../edc-extensions/log4j2-monitor) | Logging through Log4j 2 |
| [`single-participant-vault`](../../edc-extensions/single-participant-vault) | In-memory vault for the memory runtime |
| [`backport`](../../edc-extensions/backport) | Place for temporary backports of upstream fixes |

!!! tip "Is a module in my image?"

    Open the `build.gradle.kts` of the runtime (for example
    [`edc-controlplane-base`](../../edc-controlplane/edc-controlplane-base/build.gradle.kts)) and look for the
    project dependency.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
