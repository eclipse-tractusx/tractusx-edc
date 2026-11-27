# Known friends

Tractus-X EDC stands on the shoulders of upstream Eclipse Dataspace Components, and many projects build on it in turn.
This page lists the ones we know of: first those beyond Eclipse Tractus-X, in other dataspaces such as Factory-X and
Construct-X and from companies and research, then those inside Eclipse Tractus-X. The contact is the one each project names; where none is listed, use its GitHub issues.

## Upstream

<div class="grid cards" markdown>

-   :material-source-branch: **Eclipse Dataspace Components (EDC)**

    ---

    The framework Tractus-X EDC is built on: the connector core, the Dataspace Protocol and the Decentralized Claims
    Protocol, the Management API and the extension model. Tractus-X EDC pins an exact EDC version
    (`edc` in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml)).

    [:octicons-arrow-right-24: eclipse-edc/Connector](https://github.com/eclipse-edc/Connector)

-   :material-web: **Eclipse EDC project**

    ---

    The Eclipse Foundation project behind EDC, with its other repositories (Identity Hub, Federated Catalog,
    Minimum Viable Dataspace, …), committers and release plans.

    [:octicons-arrow-right-24: projects.eclipse.org](https://projects.eclipse.org/projects/technology.edc)

</div>

## Beyond Tractus-X

| Project | What it is | How it uses Tractus-X EDC | Contact |
|---|---|---|---|
| [Factory-X EDC](https://github.com/factory-x-contributions/factoryx-edc) | Factory-X extensions for the manufacturing sector (proof of concept) | Builds on Tractus-X EDC and adds its extensions | |
| [Construct-X EDC](https://github.com/project-construct-x/constructx-edc) | The Construct-X connector, based on Tractus-X EDC | A downstream distribution; deployed with charts from the [Construct-X script collection](https://github.com/project-construct-x/script-collection) |  |
| [sovity EDC Community Edition](https://github.com/sovity/edc-ce) | sovity's open-source EDC distribution | BDRS client, BPN validation, Catena-X policies and other Maven artefacts | <contact@sovity.de> |
| [Dataspace Connector on AWS](https://github.com/awslabs/dataspace-connector-on-aws) | Dataspace connector for AWS (AWS Labs) | Control plane and data plane base runtimes (Maven) |  |
| [EDC Extension for AAS](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS) | Asset Administration Shells in EDC (Fraunhofer IOSB) | A Tractus-X launcher on `edc-runtime-memory` (Maven) |  |
| [Catena-X Test Suite](https://test-suite.catena-x.net) | Conformance tests for Catena-X participants and applications, hosted by Catena-X e.V. | Runs Tractus-X EDC connectors with the `tractusx-connector` Helm chart and images | |
| [EDC Management Console](https://github.com/ARENA2036/edc-management-console) | Deploys and manages connectors (ARENA2036) | Provisions Tractus-X EDC with the `tractusx-connector` Helm chart |  |
| [AWS EDC pattern](https://github.com/Think-iT-Labs/aws-patterns-edc) | Helm, Kubernetes and Terraform patterns for EDC on AWS (Think-iT) | `tractusx-connector` Helm chart from Terraform |  |
| [Catena-X profile for EDC](https://github.com/Metaform/profile-edc-cx) | Catena-X profile for EDC (Metaform Systems, draft) | `tractusx-connector` Helm chart |  |
| [Data plane workshop](https://github.com/FraunhoferISST/tx-dataplane-workshop) | Data plane workshop of the Tractus-X Community Days (Fraunhofer ISST) | Builds on the Tractus-X EDC control plane (Maven) |  |
| [py-cx-ids](https://github.com/boschresearch/py-cx-ids) | Python implementation of the protocol, for learning (Bosch Research) | Control plane and data plane images in its development environment |  |
| [Tractus-X Go connector](https://github.com/matbmoser/tractusx-go-connector) | A DSP 2025-1 and DCP 1.0 connector written in Go, as a learning pilot. **Pending its first release.** | Interoperates with Tractus-X EDC over DSP and DCP | |
| [tx-bootstrap](https://github.com/connected-intelligent-systems/tx-bootstrap) | Bootstrap and deployment tooling for a Tractus-X style dataspace | Control plane and data plane images in its participant chart |  |

## Inside Eclipse Tractus-X

| Project | What it is | How it uses Tractus-X EDC | Contact |
|---|---|---|---|
| [Tractus-X Umbrella](https://github.com/eclipse-tractusx/tractus-x-umbrella) | The Helm chart that brings all Tractus-X components together | `tractusx-connector` Helm chart: provider and consumer connectors, each with a Tractus-X Identity Hub ([how to deploy](../deployment/index.md#deploy-in-five-minutes)) | [Discussions](https://github.com/eclipse-tractusx/tractus-x-umbrella/discussions) |
| [Tractus-X Identity Hub](https://github.com/eclipse-tractusx/tractusx-identityhub) | Open-source DCP wallet and IssuerService | The wallet and STS a Tractus-X EDC presents its credentials from ([wallets](../architecture/identity-and-trust.md#wallets-you-can-use)) | [Discussions](https://github.com/eclipse-tractusx/tractusx-identityhub/discussions) |
| [Tractus-X Starter Kit](https://github.com/eclipse-tractusx/tractusx-starter-kit) | Configuration wizard that generates a ready-to-use Docker Compose setup of Tractus-X components | A provider and a consumer Tractus-X EDC with PostgreSQL, exchanging a first asset | [Discussions](https://github.com/eclipse-tractusx/tractusx-starter-kit/discussions) |
| [Industry Core Hub](https://github.com/eclipse-tractusx/industry-core-hub) | Speedway for KITs and use cases | `tractusx-connector` Helm chart for its data provider, consumer and manufacturer | [Discussions](https://github.com/eclipse-tractusx/industry-core-hub/discussions) |
| [Tractus-X SDK](https://github.com/eclipse-tractusx/tractusx-sdk) | Python library for apps that use the connector, the Digital Twin Registry and discovery services | Management API client with adapters per connector version | [Matrix](https://matrix.to/#/#tractusx-industry-core-hub:matrix.eclipse.org), [Discussions](https://github.com/eclipse-tractusx/tractusx-sdk/discussions) |
| [Trace-X](https://github.com/eclipse-tractusx/traceability-foss) | Traceability application | `tractusx-connector` Helm chart | [tractusx-dev](https://accounts.eclipse.org/mailing-list/tractusx-dev), [Discussions](https://github.com/eclipse-tractusx/traceability-foss/discussions) |
| [Item Relationship Service](https://github.com/eclipse-tractusx/item-relationship-service) | Builds the tree of parts across companies | `org.eclipse.tractusx.edc` Maven artefacts (EDR API) | [Matrix](https://chat.eclipse.org/#/room/#tractusx-irs:matrix.eclipse.org), [Discussions](https://github.com/eclipse-tractusx/item-relationship-service/discussions) |
| [PURIS](https://github.com/eclipse-tractusx/puris) | Short-term demand and capacity management | Control plane and data plane images for its local setup |  |
| [Knowledge Agents EDC](https://github.com/eclipse-tractusx/knowledge-agents-edc) | EDC extensions for federated queries (CX-0084) | Builds on the Tractus-X EDC runtimes (Maven) |  |
| [Tractus-X EDC Kafka extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension) | Kafka streaming for Tractus-X EDC | Maven artefacts and the `tractusx-connector` Helm chart | [Discussions](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension/discussions) |
| [Policy Builder](https://github.com/eclipse-tractusx/tractusx-edc-dashboard) | Graphical builder for Catena-X policies | `cx-policy`, `bdrs-client` and other Maven artefacts for policy validation | [Discussions](https://github.com/eclipse-tractusx/tractusx-edc-dashboard/discussions) |
| [Tutorial resources (MXD)](https://github.com/eclipse-tractusx/tutorial-resources) | The Minimum Tractus-X Dataspace and other tutorials | Runtimes built on the Tractus-X EDC control plane (Maven) | [Discussions](https://github.com/eclipse-tractusx/tutorial-resources/discussions) |
| [Tractus-X TestLab](https://github.com/eclipse-tractusx/tractusx-testlab) | Dataspace-native test framework | `tractusx-connector-memory` Helm chart in its end-to-end tests | [Discussions](https://github.com/eclipse-tractusx/tractusx-testlab/discussions) |
| [BPN-DID Resolution Service](https://github.com/eclipse-tractusx/bpn-did-resolution-service) | Resolves BPN ↔ DID for the dataspace | Starts `edc-runtime-memory` in its CI tests |  |
| [Managed Simple Data Exchanger](https://github.com/eclipse-tractusx/managed-simple-data-exchanger-backend) | Lets companies provide data in the Tractus-X network | Management API client |  |

!!! tip "Using Tractus-X EDC and not listed?"

    Open a pull request that adds a row to this page
    ([`docs/home/known-friends.md`](https://github.com/eclipse-tractusx/tractusx-edc/edit/main/docs/home/known-friends.md)).
    List open-source projects that are maintained and use Tractus-X EDC as a dependency, chart, image or API.


## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
