# Glossary

Terms used throughout this documentation, in alphabetical order. Abbreviations are expanded the first time they appear
on each page.

Access policy
:   The policy of a contract definition that decides whether a consumer **sees** an offer in the catalog. Evaluated on
    every catalog request. See [Catena-X policies](../architecture/policies.md).

Asset
:   The provider's description of something it offers, usually a backend API. It holds public `properties` (shown in the
    catalog), `privateProperties` and a `dataAddress` that tells the data plane how to reach the backend.

BDRS
:   BPN-DID Resolution Service. A Catena-X dataspace service that maps business partner numbers (BPNs) to DIDs and back.

BPN, BPNL
:   Business Partner Number. The Catena-X identifier of a legal entity (`BPNL…`), site (`BPNS…`) or address (`BPNA…`).
    Connectors identify participants by BPNL or by DID.

Business partner group
:   A named group of BPNs that a provider maintains in its connector and uses in access or contract policies through the
    `BusinessPartnerGroup` constraint.

Catalog
:   The list of datasets and contract offers a provider shows to a specific consumer. Filtered by access policies.

Consumer
:   The participant that requests data. The same connector can act as consumer and provider.

Contract agreement
:   The signed result of a successful negotiation: which asset, which policy, between which participants. Required to
    start a transfer.

Contract definition
:   The provider-side link between a set of assets (selected by an `assetsSelector`), one access policy and one contract
    policy. Creates the offers in the catalog.

Contract negotiation
:   The state machine in which consumer and provider agree on an offer. Ends in `FINALIZED` (agreement) or `TERMINATED`.

Contract policy (usage policy)
:   The policy of a contract definition that is checked during negotiation and becomes part of the agreement.

Control plane
:   The runtime that manages catalog, negotiations, agreements and transfer processes, and exposes the Management API
    and the Dataspace Protocol endpoints.

Credential service (wallet)
:   The component that stores a participant's verifiable credentials and issues verifiable presentations on request.

Data address
:   A typed description of where data lives (for example `HttpData` with a `baseUrl`). Assets have one, and the EDR is
    one too.

Data plane
:   The runtime that moves data once a transfer is started. For HTTP pull it exposes a public endpoint that consumers
    call with the token from the EDR.

Data plane signaling (DPS)
:   The upstream EDC protocol between control plane and data plane to start, suspend and terminate data flows.

DCP
:   [Decentralized Claims Protocol](https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/): how
    connectors request and verify verifiable presentations from each other's credential services.

DID
:   Decentralized Identifier, for example `did:web:example.com:bpnl000000000001`. Resolves to a DID document with keys and
    service endpoints.

DSP
:   [Dataspace Protocol](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/): the protocol control
    planes use with each other for catalog, negotiation and transfer.

EDR
:   Endpoint Data Reference. A data address with an endpoint and a short-lived access token that the provider sends to
    the consumer when a pull transfer starts. Tractus-X EDC caches EDRs and can refresh their tokens. See
    [EDRs and token refresh](../architecture/edr-and-token-refresh.md).

Extension
:   A Java module that plugs into an EDC runtime through the `ServiceExtension` interface. Tractus-X EDC is a set of
    extensions on top of upstream EDC. See [Writing extensions](../developer/extensions.md).

JSON-LD
:   JSON for Linked Data. All Management API and DSP payloads are JSON-LD; terms are expanded to IRIs using the
    `@context`. See [JSON-LD and namespaces](../reference/json-ld.md).

Management API
:   The authenticated HTTP API of the control plane through which your own applications manage assets, policies,
    negotiations, transfers and EDRs.

ODRL
:   [Open Digital Rights Language](https://www.w3.org/TR/odrl-model/), the W3C policy language EDC policies are
    written in.

Participant
:   A company in the dataspace, identified by a DID (and in Catena-X by a BPNL).

Provider
:   The participant that offers data.

STS
:   Secure Token Service. Issues the self-issued identity tokens a connector attaches to DSP requests in DCP.

Transfer process
:   The state machine that starts, suspends and terminates the data flow for an agreement. Types include HTTP pull and
    provider push.

Verifiable credential (VC) / verifiable presentation (VP)
:   A signed statement about a participant (for example "is a Catena-X member"), and a signed bundle of credentials
    presented to a verifier.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
