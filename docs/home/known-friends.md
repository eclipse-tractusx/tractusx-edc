# Known friends

Tractus-X EDC stands on the shoulders of upstream Eclipse Dataspace Components, and many projects build on it in turn.
This page lists the ones we know of, inside Eclipse Tractus-X and beyond.

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

## Inside Eclipse Tractus-X

| Project | What it is | How it uses Tractus-X EDC |
|---|---|---|
| [Tractus-X Umbrella](https://github.com/eclipse-tractusx/tractus-x-umbrella) | The Helm chart that brings all Tractus-X components together | `tractusx-connector` Helm chart (dataspace connector bundle) |
| [Industry Core Hub](https://github.com/eclipse-tractusx/industry-core-hub) | Speedway for KITs and use cases | `tractusx-connector` Helm chart for its data provider, consumer and manufacturer |
| [Tractus-X SDK](https://github.com/eclipse-tractusx/tractusx-sdk) | Python library for apps that use the connector, the Digital Twin Registry and discovery services | Management API client with adapters per connector version |
| [Trace-X](https://github.com/eclipse-tractusx/traceability-foss) | Traceability application | `tractusx-connector` Helm chart |
| [Item Relationship Service](https://github.com/eclipse-tractusx/item-relationship-service) | Builds the tree of parts across companies | `org.eclipse.tractusx.edc` Maven artefacts (EDR API) |
| [PURIS](https://github.com/eclipse-tractusx/puris) | Short-term demand and capacity management | Control plane and data plane images for its local setup |
| [Knowledge Agents EDC](https://github.com/eclipse-tractusx/knowledge-agents-edc) | EDC extensions for federated queries (CX-0084) | Builds on the Tractus-X EDC runtimes (Maven) |
| [Tractus-X EDC Kafka extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension) | Kafka streaming for Tractus-X EDC | Maven artefacts and the `tractusx-connector` Helm chart |
| [Policy Builder](https://github.com/eclipse-tractusx/tractusx-edc-dashboard) | Graphical builder for Catena-X policies | `cx-policy`, `bdrs-client` and other Maven artefacts for policy validation |
| [Tutorial resources (MXD)](https://github.com/eclipse-tractusx/tutorial-resources) | The Minimum Tractus-X Dataspace and other tutorials | Runtimes built on the Tractus-X EDC control plane (Maven) |
| [Tractus-X TestLab](https://github.com/eclipse-tractusx/tractusx-testlab) | Dataspace-native test framework | `tractusx-connector-memory` Helm chart in its end-to-end tests |
| [BPN-DID Resolution Service](https://github.com/eclipse-tractusx/bpn-did-resolution-service) | Resolves BPN ↔ DID for the dataspace | Starts `edc-runtime-memory` in its CI tests |
| [Managed Simple Data Exchanger](https://github.com/eclipse-tractusx/managed-simple-data-exchanger-backend) | Lets companies provide data in the Tractus-X network | Management API client |

## Beyond Tractus-X

| Project | What it is | How it uses Tractus-X EDC |
|---|---|---|
| [Construct-X EDC](https://github.com/project-construct-x/constructx-edc) | The Construct-X connector, based on Tractus-X EDC | A downstream distribution; deployed with charts from the [Construct-X script collection](https://github.com/project-construct-x/script-collection) |
| [sovity EDC Community Edition](https://github.com/sovity/edc-ce) | sovity's open-source EDC distribution | BDRS client, BPN validation, Catena-X policies and other Maven artefacts |
| [Dataspace Connector on AWS](https://github.com/awslabs/dataspace-connector-on-aws) | Dataspace connector for AWS (AWS Labs) | Control plane and data plane base runtimes (Maven) |
| [EDC Extension for AAS](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS) | Asset Administration Shells in EDC (Fraunhofer IOSB) | A Tractus-X launcher on `edc-runtime-memory` (Maven) |
| [Catena-X Test Suite infrastructure](https://github.com/catenax-eV/cx-playground-infra) | Infrastructure of the Test Suite hosted by Catena-X e.V. | `tractusx-connector` Helm chart and images derived from the control plane image |
| [EDC Management Console](https://github.com/ARENA2036/edc-management-console) | Deploys and manages connectors (ARENA2036) | Provisions Tractus-X EDC with the `tractusx-connector` Helm chart |
| [AWS EDC pattern](https://github.com/Think-iT-Labs/aws-patterns-edc) | Helm, Kubernetes and Terraform patterns for EDC on AWS (Think-iT) | `tractusx-connector` Helm chart from Terraform |
| [Catena-X profile for EDC](https://github.com/Metaform/profile-edc-cx) | Catena-X profile for EDC (Metaform Systems, draft) | `tractusx-connector` Helm chart |
| [Data plane workshop](https://github.com/FraunhoferISST/tx-dataplane-workshop) | Data plane workshop of the Tractus-X Community Days (Fraunhofer ISST) | Builds on the Tractus-X EDC control plane (Maven) |
| [py-cx-ids](https://github.com/boschresearch/py-cx-ids) | Python implementation of the protocol, for learning (Bosch Research) | Control plane and data plane images in its development environment |
| [tx-bootstrap](https://github.com/connected-intelligent-systems/tx-bootstrap) | Bootstrap and deployment tooling for a Tractus-X style dataspace | Control plane and data plane images in its participant chart |

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
