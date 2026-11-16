# What is Tractus-X EDC?

Tractus-X EDC is a **dataspace connector**: a piece of software every company runs to take part in a dataspace such as
[Catena-X](https://catena-x.net/). It lets a company **offer** data to partners under conditions it defines, and
**obtain** data from partners under conditions they define, without uploading anything to a central platform.

It is the [Eclipse Tractus-X](https://eclipse-tractusx.github.io/) distribution of the
[Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector) connector. EDC is a framework; Tractus-X
EDC turns it into ready-to-run Docker images and Helm charts, and adds what Catena-X needs: Catena-X policies, the
Business Partner Number (BPN) identity, verifiable credential checks, EDR caching with token refresh, and more.

## The problem it solves

Two companies want to share data, for example a supplier's quality data with a car maker. Both need answers to the
same questions before a single byte moves:

- **Who are you?** The provider must be sure who is asking, without a shared user database.
- **Are you allowed?** Only partners who are Catena-X members, signed the right framework agreement, or belong to a
  given business partner group may see an offer.
- **Under which terms?** Usage purpose, duration and other conditions must be agreed and recorded.
- **How do I get the data?** Once agreed, the consumer needs a secure, short-lived way to call the provider's backend.

A connector answers all four, in a standard way, so every participant can talk to every other participant.

## The four ideas you need

<div class="grid cards" markdown>

-   :material-storefront-outline: **Catalog and offers**

    ---

    A provider registers **assets** (pointers to its backend APIs), attaches **policies**, and publishes them as
    **contract offers** in its **catalog**. Consumers browse the catalog.

-   :material-handshake-outline: **Contract negotiation**

    ---

    The consumer requests an offer. The provider checks the consumer's credentials against the policy and, if all
    is fine, both sides store the same **contract agreement**.

-   :material-swap-horizontal: **Transfer and EDR**

    ---

    With an agreement the consumer starts a **transfer process**. For pull transfers it receives an
    **Endpoint Data Reference (EDR)**: an endpoint plus a short-lived token for the provider's data plane.

-   :material-shield-account-outline: **Decentralised identity**

    ---

    Nobody logs in. Each connector proves who it is with **verifiable credentials** held in its own wallet, using
    the Decentralized Claims Protocol (DCP) and Decentralized Identifiers (DIDs).

</div>

## Two planes

A Tractus-X EDC deployment has two runtimes:

| | Control plane | Data plane |
|---|---|---|
| **Job** | Decides *whether* data may flow | Moves the data |
| **Talks to** | Other control planes (Dataspace Protocol), your apps (Management API), wallets | Your backend systems, consumers holding an EDR |
| **Stores** | Assets, policies, contract definitions, negotiations, agreements, transfer processes, EDRs | Data flows and token state |
| **Scales with** | Number of negotiations and transfers | Data volume |

Keeping them separate means the data path can scale and be placed close to the data, while the negotiation logic stays
in one well-protected place. See [Architecture](../architecture/index.md) for the full picture.

## What Tractus-X adds on top of EDC

- **Catena-X policies**: the constraints of standard CX-0152 (`Membership`, `FrameworkAgreement`, `UsagePurpose`,
  `BusinessPartnerNumber`, `BusinessPartnerGroup`, …) with validation on creation.
  [Read more](../architecture/policies.md)
- **BPN identity**: business partner numbers resolved to DIDs through the BPN-DID Resolution Service (BDRS), and
  business partner groups for access control.
- **EDR management**: one call to negotiate and start a transfer, an EDR cache, and **token refresh** so consumers
  keep access without renegotiating. [Read more](../architecture/edr-and-token-refresh.md)
- **Connector discovery**: find a partner's connector and supported protocol versions from its DID document.
- **Operations**: PostgreSQL schema migrations, HashiCorp Vault integration, Helm charts, and backward compatibility
  for older Dataspace Protocol versions.

The [extension catalogue](../reference/extensions.md) lists every Tractus-X module.

## What Tractus-X EDC is not

- **Not a wallet.** Credentials are held by a credential service (wallet) that you operate or rent. The connector
  asks it for verifiable presentations.
- **Not a business application.** The connector exposes APIs; a business application (or a tool such as the
  [Tractus-X SDK](https://github.com/eclipse-tractusx/tractusx-sdk)) drives them.
- **Not a data store.** Data stays in your backend; the data plane proxies or pushes it on request.

## Where to go next

- Want to try it? Start with the [local quick start](../deployment/local-quickstart.md) and
  [Tutorial 1](../tutorials/first-data-exchange.md).
- Want the details? Read the [architecture overview](../architecture/index.md).
- Unsure about a term? See the [glossary](glossary.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
