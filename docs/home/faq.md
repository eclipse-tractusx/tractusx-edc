# Frequently asked questions

## The basics

??? question "What is the difference between Eclipse EDC and Tractus-X EDC?"

    [Eclipse Dataspace Components](https://github.com/eclipse-edc/Connector) is a generic framework for building
    connectors. Tractus-X EDC is a **ready-to-run distribution** of it for Catena-X: it selects the upstream modules,
    adds Catena-X policies, BPN handling, EDR management, the consumer proxy, token refresh and DSP 0.8 compatibility,
    and ships Docker images and Helm charts. See [Overview](overview.md).

??? question "Do I need to write code to use it?"

    No. Most participants deploy the released Helm chart and use the Management API. You only write code if you need
    your own [extension](../developer/extensions.md).

??? question "Which Dataspace Protocol versions are supported?"

    Both **DSP 2025-1** (participants identified by DID) and **DSP 0.8** (identified by BPNL) are served at the same
    time. Choose the version per request with the `protocol` field; use
    [connector discovery](../architecture/flows.md#connector-discovery) when you do not know what a partner supports.

## Deployment

??? question "What do I need besides the connector?"

    A PostgreSQL database, a HashiCorp Vault, and access to the dataspace's identity services: a DCP wallet with your
    credentials and a Secure Token Service, the BPN-DID Resolution Service (BDRS) and the trusted issuer's DID. See
    [Deployment](../deployment/index.md).

??? question "Which images are released?"

    `tractusx/edc-controlplane-postgresql-hashicorp-vault` and `tractusx/edc-dataplane-hashicorp-vault`. The Helm chart
    `tractusx-connector` uses them by default.

??? question "Can I use a different vault or database?"

    Not with the released images. Build your own runtime from `edc-controlplane-base` / `edc-dataplane-base` and add
    the upstream EDC modules you need. See [Code architecture](../developer/code-architecture.md).

??? question "Can I use the in-memory chart in production?"

    No. `tractusx-connector-memory` stores everything in memory, including secrets. Restarting the pod loses all data.
    Use it for tests and demos only.

??? question "Can I run more than one replica?"

    You can raise `replicaCount`. The chart then disables the self-deregistration of the DID service entry, so that one
    pod shutting down does not remove the connector from the DID document. Do not enable `autoscaling`: its
    templates use an API version that current Kubernetes no longer serves.

??? question "Which version should I run?"

    The latest release. Only the latest release receives fixes. Read the
    [migration guide](../migration/index.md) for each version you skip. See [Upgrades](../deployment/upgrades.md).

## Using the connector

??? question "Why is the catalog of my partner empty?"

    Most often an identity problem: your credentials, the trusted issuer, or the BDRS. Sometimes the access policy
    simply excludes you. Follow the checklist in
    [Troubleshooting](../operations/troubleshooting.md#the-catalog-is-empty-or-the-request-fails).

??? question "What is an EDR and why does it expire?"

    An Endpoint Data Reference is the address plus a short-lived access token for data you negotiated. It expires so
    that a leaked token is only useful briefly; the connector refreshes it for you. See
    [EDRs and token refresh](../architecture/edr-and-token-refresh.md).

??? question "Should I use the consumer proxy or call the provider directly?"

    The [consumer proxy](../tutorials/consume-with-edrs.md) is simpler: your application calls your own data plane,
    which finds the EDR and refreshes tokens. Calling the provider directly with the EDR saves a hop, but your
    application must handle token refresh itself.

??? question "Can I revoke a contract that I already agreed to?"

    Yes, as a provider: [retire the agreement](../architecture/flows.md#agreement-retirement). New transfers for
    that agreement are then rejected, and the policy monitor terminates transfers that are still running.

## Community

??? question "Where do I get help?"

    Join the [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly), ask in
    the [Matrix room](https://chat.eclipse.org/#/room/#tractusx-edc:matrix.eclipse.org) or in
    [GitHub Discussions](https://github.com/eclipse-tractusx/tractusx-edc/discussions), report bugs as
    [issues](https://github.com/eclipse-tractusx/tractusx-edc/issues), and read
    [Contributing](../developer/contributing.md) before you open a pull request. [Get help](get-help.md) lists every
    channel.

??? question "Who is the Connector Sensei?"

    An AI assistant configuration in this repository that answers questions about the connector based on the code and
    the exact upstream EDC version. It is an AI, not a person, and its answers should be checked. See the
    [Connector Sensei](../development/connector-sensei/README.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
