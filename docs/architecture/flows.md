# Data exchange flows

This page shows, as sequence diagrams, what happens between the control planes and data planes of consumer and
provider. Identity checks (see [Identity and trust](identity-and-trust.md)) happen on every DSP call and are drawn only
once.

```mermaid
flowchart LR
  d[Discovery<br/>optional] --> c[Catalog] --> n[Negotiation] --> t[Transfer] --> data[Data access]
```

## Catalog

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant PCP as Provider control plane
  App->>CCP: POST /v3/catalog/request (counterPartyAddress, counterPartyId, protocol)
  CCP->>CCP: compute credential scopes, get token from STS
  CCP->>PCP: DSP CatalogRequestMessage + token
  PCP->>PCP: verify identity (DCP), then for each contract definition evaluate its ACCESS policy
  PCP-->>CCP: DCAT catalog: datasets with offers (odrl:hasPolicy) and distributions
  CCP-->>App: catalog
```

The offers contain the **contract policy**. Distributions tell you the transfer types the provider supports, such as
`HttpData-PULL`, `HttpData-PUSH`, `AmazonS3-PUSH` or `AzureStorage-PUSH`.

## Contract negotiation

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant PCP as Provider control plane
  App->>CCP: POST /v3/contractnegotiations (offer policy, counterPartyAddress, protocol)
  CCP-->>App: 200 { @id: negotiationId }
  CCP->>PCP: DSP ContractRequestMessage
  PCP->>PCP: check offer matches contract definition, evaluate CONTRACT policy
  PCP->>CCP: ContractAgreementMessage
  CCP->>PCP: ContractAgreementVerificationMessage
  PCP->>CCP: ContractNegotiationEventMessage (FINALIZED)
  App->>CCP: GET /v3/contractnegotiations/{id}
  CCP-->>App: state FINALIZED, contractAgreementId
```

The negotiation is **asynchronous**: poll its state, or register a callback. Both sides store the agreement, and
Tractus-X EDC also records the BPNs of both parties with it.

## Transfer: consumer pull (HTTP)

The most common pattern in Catena-X. The provider data plane becomes a protected proxy in front of the provider's
backend, and the consumer receives an **EDR** (endpoint + token).

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant PCP as Provider control plane
  participant PDP as Provider data plane
  participant BE as Provider backend
  App->>CCP: POST /v3/transferprocesses (contractId, transferType HttpData-PULL)
  CCP->>PCP: DSP TransferRequestMessage
  PCP->>PCP: evaluate agreement policy (transfer scope), check agreement not retired
  PCP->>PDP: start data flow (/v1/dataflows)
  PDP-->>PCP: EDR: endpoint, access token, refresh token, refresh endpoint
  PCP->>CCP: TransferStartMessage with EDR
  CCP->>CCP: store EDR in cache
  App->>CCP: GET /v3/edrs/{transferProcessId}/dataaddress
  CCP-->>App: endpoint + token (refreshed if needed)
  App->>PDP: GET {endpoint}/{path} with Authorization: {token}
  PDP->>PDP: validate token
  PDP->>BE: proxied request
  BE-->>PDP: data
  PDP-->>App: data
```

The transfer stays in state `STARTED` until it is terminated or suspended, or until the policy monitor ends it (for
example when `DataUsageEndDate` is reached). Tokens are short-lived and **refreshed**; see
[EDRs and token refresh](edr-and-token-refresh.md).

## Transfer: provider push

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant PCP as Provider control plane
  participant PDP as Provider data plane
  participant Dest as Consumer destination
  App->>CCP: POST /v3/transferprocesses (transferType e.g. HttpData-PUSH, dataDestination)
  CCP->>PCP: DSP TransferRequestMessage
  PCP->>PDP: start data flow
  PDP->>Dest: push data (HTTP, S3, Azure Blob)
  PDP->>PCP: complete
  PCP->>CCP: TransferCompletionMessage
```

**Non-finite push.** If the asset's data address has `"isNonFinite": "true"`, the flow stays open after the first push.
The provider re-pushes the full current content with `POST /v3/dataflows/{transferProcessId}/trigger` on the
**data plane** management API. See the
[walkthrough](../usage/management-api-walkthrough/10_nonfiniteproviderpush/10_nonfiniteproviderpush.md).

## One call: negotiate and transfer with the EDR API

`POST /v3/edrs` starts a negotiation; when it is finalized, the connector automatically starts an `HttpData-PULL`
transfer and caches the resulting EDR.

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant PCP as Provider control plane
  App->>CCP: POST /v3/edrs (offer, counterPartyAddress, protocol)
  CCP->>PCP: negotiation …
  PCP-->>CCP: agreement (FINALIZED)
  CCP->>PCP: transfer request HttpData-PULL (automatic)
  PCP-->>CCP: TransferStartMessage with EDR
  CCP->>CCP: cache EDR
  App->>CCP: POST /v3/edrs/request (filter by assetId, agreementId, …)
  CCP-->>App: EDR entries (transferProcessId)
  App->>CCP: GET /v3/edrs/{transferProcessId}/dataaddress
```

## Connector discovery

Before the first catalog request you need the partner's DSP address and protocol version.

```mermaid
sequenceDiagram
  autonumber
  participant App as Consumer app
  participant CCP as Consumer control plane
  participant DID as Partner DID document
  participant PCP as Partner control plane
  App->>CCP: POST /v3/connectordiscovery/connectors (counterPartyId = partner DID)
  CCP->>DID: resolve DID, read services of type DataService
  loop each connector
    CCP->>PCP: GET /.well-known/dspace-version
    PCP-->>CCP: supported DSP versions
  end
  CCP-->>App: [{ protocol, counterPartyId, counterPartyAddress }, …]
```

For a single known address, `POST /v3/connectordiscovery/dspversionparams` returns the parameters to use directly. A
partner that only speaks DSP 0.8 is detected too, and you get its BPNL as `counterPartyId`.

## Agreement retirement

A provider can **retire** an agreement before its natural end with
`POST /v3/contractagreements/retirements`. New transfers for it are rejected and the policy monitor terminates running
ones. `DELETE /v3/contractagreements/retirements/{agreementId}` reactivates it. See the
[agreements README](../../edc-extensions/agreements/README.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
