# Why Tractus-X EDC?

[Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector) is a **framework**: a set of modules
and extension points from which you assemble your own connector. It implements the Dataspace Protocol (DSP) and the
Decentralized Claims Protocol (DCP), but it leaves the runtime, the packaging, the dataspace rules and the operations
to you.

**Tractus-X EDC is that connector, ready for Catena-X.** It is built on a pinned EDC version and adds the extensions,
the release artefacts, the tests and the community that a company needs to join the Catena-X dataspace, or any other
dataspace that speaks DSP and DCP, and to stay in it across releases.

<div class="grid cards" markdown>

-   :material-package-variant-closed: **Ready to run**

    ---

    Released control plane, data plane and in-memory runtimes, as OSS images on
    [Docker Hub](https://hub.docker.com/u/tractusx): multi-architecture, non-root, for every release.

-   :material-kubernetes: **Helm charts**

    ---

    The `tractusx-connector` chart (production, with optional PostgreSQL and Vault) and `tractusx-connector-memory`
    (testing), published to the Tractus-X Helm repository and tested on every CI run.

    [:octicons-arrow-right-24: Deploy in five minutes](../deployment/index.md#deploy-in-five-minutes)

-   :material-scale-balance: **Catena-X rules built in**

    ---

    The Catena-X policy constraints (CX-0152), Business Partner Number and BPN group checks, the BPN-DID Resolution
    Service (BDRS) client and the Catena-X credential scopes.

    [:octicons-arrow-right-24: Catena-X policies](../architecture/policies.md)

-   :material-account-group-outline: **An open community**

    ---

    Developed in the open in [Eclipse Tractus-X](https://eclipse-tractusx.github.io/), with a regular, public
    [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly) where anyone can
    ask questions.

    [:octicons-arrow-right-24: Get help](get-help.md)

</div>

## What you get on top of upstream EDC

| | Upstream EDC on its own | Tractus-X EDC |
|---|---|---|
| **Runtime** | Assemble your own from modules | Released runtimes: [control plane, data plane and in-memory](../reference/extensions.md#runtimes) |
| **Container images** | Build and host your own | OSS images on [Docker Hub](https://hub.docker.com/u/tractusx) for every release, multi-architecture, non-root |
| **Kubernetes** | Write your own manifests | [Helm charts](../deployment/helm-chart.md) with PostgreSQL and Vault options and built-in `helm test` checks |
| **Catena-X policies** | Write your own policy functions | [CX-0152 constraints](../architecture/policies.md), BPN and [BPN group](../tutorials/access-control-bpn-groups.md) checks, contract definition validation |
| **Identity in Catena-X** | Generic DCP | [BDRS client](../architecture/identity-and-trust.md) (BPN ↔ DID), Catena-X credential scopes, remote STS client for a wallet such as DIV, presentation cache |
| **Protocol versions** | The current DSP version | DSP 2025-1 **and** DSP 0.8 side by side, with connector discovery, so partners on older releases stay reachable |
| **Data access** | EDR building blocks | The [`/v3/edrs` API](../architecture/edr-and-token-refresh.md), automatic transfer after negotiation and token refresh |
| **Agreements** | Agreements end with their policy | [Agreement retirement](../architecture/flows.md#agreement-retirement) and the BPNs of both parties stored with each agreement |
| **Upgrades** | Follow upstream release notes | Flyway [database migrations](../architecture/persistence.md), a [migration guide](../deployment/upgrades.md) per release and an upgradeability test in CI |
| **Quality and compliance** | Your own pipeline | Deployment, end-to-end and upgradeability tests, CodeQL, Trivy, KICS and secret scans, Eclipse license checks of every dependency |
| **Help** | The EDC community | The [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly), a [Matrix room](https://chat.eclipse.org/#/room/#tractusx-edc:matrix.eclipse.org), GitHub Discussions, this documentation, the [Connector KIT](https://eclipse-tractusx.github.io/docs-kits/kits/connector-kit/adoption-view/) and the [Connector Sensei](../development/connector-sensei/README.md) |

!!! tip "Not either-or"

    Tractus-X EDC **is** EDC: every upstream extension point still works, and you can add your own extensions to the
    Tractus-X runtimes. See [Tutorial 4: your first extension](../tutorials/first-extension.md). Fixes and features
    that are not Catena-X specific are meant to go upstream.

## When upstream EDC alone is the better fit

- You build a connector for a dataspace with **different rules** than Catena-X and want to assemble every module
  yourself.
- You work on EDC itself, or on a feature that belongs upstream.

Everyone else, and in particular every Catena-X participant, saves time with the released runtimes, images and charts
of Tractus-X EDC.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
