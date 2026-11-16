---
title: Welcome
hide:
  - toc
---

<div class="tx-hero" markdown>

<p class="tx-eyebrow">Eclipse Tractus-X · Dataspace connector</p>

# Tractus-X EDC

The open-source **dataspace connector for Catena-X**. Tractus-X EDC packages the
[Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector) with the extensions, policies and
identity integration a company needs to share data **sovereignly** in the Catena-X automotive dataspace, and in any
other dataspace that speaks the Dataspace Protocol.

[Get started :material-rocket-launch-outline:](home/overview.md){ .md-button .md-button--primary }
[Deploy a connector :material-kubernetes:](deployment/index.md){ .md-button }
[Read the code :fontawesome-brands-github:](https://github.com/eclipse-tractusx/tractusx-edc){ .md-button }

</div>

## Install

<div class="tx-install" markdown>

=== "Helm (Kubernetes)"

    ```bash
    helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
    helm repo update
    helm search repo tractusx-edc/tractusx-connector -l   # released chart versions

    helm install my-connector tractusx-edc/tractusx-connector --version <chart-version> -f my-values.yaml
    ```

    No values file yet? [Deploy in five minutes](deployment/index.md#deploy-in-five-minutes) has copy-paste values
    for a local try-out, with PostgreSQL and Vault or fully in memory.

=== "Docker images"

    ```bash
    docker pull tractusx/edc-controlplane-postgresql-hashicorp-vault:<version>
    docker pull tractusx/edc-dataplane-hashicorp-vault:<version>
    docker pull tractusx/edc-runtime-memory:<version>     # testing only
    ```

    Multi-architecture (`linux/amd64`, `linux/arm64`), non-root, published on
    [Docker Hub](https://hub.docker.com/u/tractusx) for every release. Settings come from environment variables; see
    [Configuration](deployment/configuration.md).

=== "From source"

    ```bash
    git clone https://github.com/eclipse-tractusx/tractusx-edc.git
    cd tractusx-edc
    ./gradlew build          # compile, unit tests, Checkstyle (JDK 21)
    ./gradlew dockerize      # local images of every runtime
    ```

    Then follow [Developer setup](developer/setup.md) or the [local quick start](deployment/local-quickstart.md).

</div>

## Choose your path

<div class="grid cards" markdown>

-   :material-lightbulb-on-outline: **New to dataspaces?**

    ---

    Learn what a connector does, why there are two planes, and how contracts, policies and credentials fit
    together, in plain words.

    [:octicons-arrow-right-24: What is Tractus-X EDC?](home/overview.md)

-   :material-sitemap-outline: **Architect**

    ---

    See the components, the trust model (DCP, DIDs, BDRS), the Catena-X policy model and every data exchange flow
    as diagrams.

    [:octicons-arrow-right-24: Architecture](architecture/index.md)

-   :material-kubernetes: **Operator**

    ---

    Install the Helm chart, configure identity, database and vault, harden for production, and upgrade safely.

    [:octicons-arrow-right-24: Deployment](deployment/index.md)

-   :material-school-outline: **Integrator**

    ---

    Offer your first asset, negotiate a contract and fetch data, step by step with copy-paste requests.

    [:octicons-arrow-right-24: Tutorials](tutorials/index.md)

-   :material-code-braces: **Developer**

    ---

    Build the code, understand the module layout and the extension model, write tests, and contribute.

    [:octicons-arrow-right-24: Developers](developer/index.md)

-   :material-api: **API user**

    ---

    Browse the Management API and data plane API, the Tractus-X specific endpoints and the JSON-LD vocabulary.

    [:octicons-arrow-right-24: Reference](reference/index.md)

</div>

## Why Tractus-X EDC?

Upstream [EDC](https://github.com/eclipse-edc/Connector) is a framework you assemble a connector from. Tractus-X EDC
is that connector, **ready for Catena-X**:

<div class="tx-why" markdown>

-   :material-package-variant-closed: <span>**Released runtimes and OSS images** on Docker Hub, for every release</span>
-   :material-kubernetes: <span>**Helm charts** with PostgreSQL and Vault options, tested on every CI run</span>
-   :material-scale-balance: <span>**Catena-X policies, BPN groups and BDRS** built in</span>
-   :material-update: <span>**Migrations and upgrade guides** for every release, upgradeability tested in CI</span>
-   :material-shield-check-outline: <span>**Security scans and Eclipse license checks** of every dependency</span>
-   :material-account-group-outline: <span>**An open community** with a public Connector Open Meeting</span>

</div>

[Why Tractus-X EDC :octicons-arrow-right-24:](home/why-tractusx-edc.md){ .md-button }
[Who uses it :octicons-arrow-right-24:](home/known-friends.md){ .md-button }

## The connector in one picture

```mermaid
flowchart LR
    subgraph consumer["Consumer company"]
        capp["Business application"]
        ccp["Control plane"]
        cdp["Data plane"]
    end
    subgraph provider["Provider company"]
        pcp["Control plane"]
        pdp["Data plane"]
        backend[("Backend system / API")]
    end
    trust{{"Dataspace trust services<br/>wallets · issuers · BDRS"}}

    capp -- "Management API" --> ccp
    ccp <-- "Dataspace Protocol (DSP)<br/>catalog · negotiation · transfer" --> pcp
    ccp -. "verifiable presentations (DCP)" .- trust
    pcp -. "verifiable presentations (DCP)" .- trust
    capp -- "data request with access token" --> pdp
    pdp --> backend

    style consumer fill:#fff7cc,stroke:#b8860b
    style provider fill:#fff7cc,stroke:#b8860b
```

The **control plane** negotiates *whether* data may flow: it publishes offers, checks policies against verifiable
credentials and records contracts. The **data plane** moves the data once a contract exists. Neither company hands
its data to a central platform. [Learn more :octicons-arrow-right-24:](home/overview.md)

## Part of the Connector KIT

<div class="tx-kit" markdown>

![Connector KIT](assets/images/connector-kit.svg){ .tx-kit__image width="240" height="224" }

<div class="tx-kit__body" markdown>

<p class="tx-kit__eyebrow">Eclipse Tractus-X Connector KIT</p>

The [Connector KIT](https://eclipse-tractusx.github.io/docs-kits/kits/connector-kit/adoption-view/) describes the
basic infrastructure for cross-enterprise data exchange in a dataspace: the Dataspace Protocol, identities and
credentials, policies and asset design. **Tractus-X EDC is its reference implementation**: production-ready and in
use throughout the Catena-X dataspace.

[Open the Connector KIT :octicons-arrow-right-24:](https://eclipse-tractusx.github.io/docs-kits/kits/connector-kit/adoption-view/){ .md-button .md-button--primary }
[Operation view](https://eclipse-tractusx.github.io/docs-kits/kits/connector-kit/operation-view/){ .md-button }
[Development view](https://eclipse-tractusx.github.io/docs-kits/kits/connector-kit/software-development-view/architecture/){ .md-button }

</div>

</div>

## Popular pages

| I want to… | Go to |
|---|---|
| Run two connectors on my laptop | [Local quick start](deployment/local-quickstart.md) |
| Exchange data for the first time | [Tutorial 1: your first data exchange](tutorials/first-data-exchange.md) |
| Write a Catena-X compliant policy | [Catena-X policies](architecture/policies.md) |
| Understand EDRs and token refresh | [EDRs and token refresh](architecture/edr-and-token-refresh.md) |
| Configure the Helm chart | [Helm chart](deployment/helm-chart.md) |
| Upgrade to a new release | [Upgrades and migrations](deployment/upgrades.md) |
| Fix a failing negotiation or transfer | [Troubleshooting](operations/troubleshooting.md) |
| Write my own extension | [Tutorial 4: your first extension](tutorials/first-extension.md) |
| Find every page | [Documentation map](home/documentation-map.md) |

!!! tip "Ask the Connector Sensei"

    This repository ships an AI agent, the [Connector Sensei](development/connector-sensei/README.md), for GitHub
    Copilot and Claude Code. It answers questions about Tractus-X EDC and upstream EDC from the source code at the exact
    pinned version, and cites `file:line`.

## Part of Eclipse Tractus-X

Tractus-X EDC is developed in the open by the [Eclipse Tractus-X](https://eclipse-tractusx.github.io/) project.
Releases, Docker images and Helm charts are published from
[GitHub](https://github.com/eclipse-tractusx/tractusx-edc/releases). Questions are welcome in the
[Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly), the
[Matrix room](https://chat.eclipse.org/#/room/#tractusx-edc:matrix.eclipse.org) and
[GitHub Discussions](https://github.com/eclipse-tractusx/tractusx-edc/discussions); bugs and feature requests go to
[GitHub Issues](https://github.com/eclipse-tractusx/tractusx-edc/issues). All channels: [Get help](home/get-help.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
