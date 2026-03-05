# EDC Extensions

## Overview
This file contains an overview about all Tractus-X extensions for the Eclipse Dataspace Connector.

---

## Extensions

| Extension | Description | README |
|-----------|-------------|--------|
| **agreements** | Lets a data provider prematurely retire active contract agreements and blocks transfers on retired agreements. | [agreements/README.md](agreements/README.md) |
| **agreements-bpns** | — | — |
| **backport** | Temporary backported modules to be replaced by upstream EDC; changes should be upstreamed. | [backport/README.md](backport/README.md) |
| **bdrs-client** | BPN–DID Resolution Service (BDRS) client with local cache for BPN↔DID mappings in the Catena-X dataspace. | [bdrs-client/README.md](bdrs-client/README.md) |
| **bpn-validation** | Policy evaluation for Business Partner Group and (legacy) Business Partner Number (BPN) policies (catalog, negotiation, transfer). | [bpn-validation/README.md](bpn-validation/README.md) |
| **connector-discovery** | DID-based connector discovery; DSP version params and connector endpoints from DID documents. Submodules: `connector-discovery-api`, `cx-connector-discovery`. | [connector-discovery/README.md](connector-discovery/README.md) · [connector-discovery-api/README.md](connector-discovery/connector-discovery-api/README.md) · [cx-connector-discovery/README.md](connector-discovery/cx-connector-discovery/README.md) |
| **cx-policy** | — | — |
| **cx-policy-legacy** | — | — |
| **data-flow-properties-provider** | — | — |
| **dataplane** | Data plane utilities and proxy. Submodules: `dataplane-util` (async/parallel transfer, response proxying), `dataplane-proxy` (consumer API for fetching via EDR/cache). | [dataplane-util/README.md](dataplane/dataplane-util/README.md) · [edc-dataplane-proxy-consumer-api/README.md](dataplane/dataplane-proxy/edc-dataplane-proxy-consumer-api/README.md) |
| **dataspace-protocol** | — | — |
| **dcp** | Decentralized Credential Protocol: scope extraction, Secure Token Service (incl. DIM), and Verifiable Presentation caching. | [dcp/README.md](dcp/README.md) |
| **did-document** | A client for managing DID Document and automatic self-(de)registration of a DID Document service. Submodules: `did-document-service-self-registration`, `did-document-service-dim`. | [did-document-service-self-registration/README.md](did-document/did-document-service-self-registration/README.md) · [did-document-service-dim/README.md](did-document/did-document-service-dim/README.md) |
| **edr** | — | — |
| **event-subscriber** | Sends EDC domain events to an OpenTelemetry collector (HTTP) for observability. | [event-subscriber/src/README.md](event-subscriber/src/README.md) |
| **federated-catalog** | — | — |
| **log4j2-monitor** | Log4J2-based implementation of the EDC Monitor interface. | [log4j2-monitor/README.md](log4j2-monitor/README.md) |
| **migrations** | PostgreSQL SQL migrations for control plane, data plane and database schema for a Connector. | [control-plane-migration/README.md](migrations/control-plane-migration/README.md) · [data-plane-migration/README.md](migrations/data-plane-migration/README.md) · [connector-migration/README.md](migrations/connector-migration/README.md) |
| **non-finite-provider-push** | — | — |
| **provision-additional-headers** | Adds headers (e.g. contract agreement ID, consumer BPN) to provider backend requests for auditing. | [provision-additional-headers/README.md](provision-additional-headers/README.md) |
| **single-participant-vault** | — | — |
| **token-interceptor** | — | — |
| **tokenrefresh-handler** | — | — |
| **validators** | Additional validators to validate requests that create or update contract definitions via the data management API endpoint. Submodule: `empty-asset-selector` (block empty asset selector). | [empty-asset-selector/README.md](validators/empty-asset-selector/src/main/java/org/eclipse/tractusx/edc/validators/emptyassetselector/README.md) |

---

*Extensions marked with "—" do not have a dedicated README in this repo.*
