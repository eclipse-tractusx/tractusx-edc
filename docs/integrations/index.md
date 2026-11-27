# Integrations

Tractus-X EDC moves data over HTTP out of the box, and the data plane image also contains the upstream EDC
extensions for **cloud object storage**. These guides show how to use them, and where to find extensions for other
technologies.

<div class="grid cards" markdown>

-   :fontawesome-brands-aws:{ .lg .middle } __Amazon S3__

    ---

    Offer objects from an S3 bucket and push them to the consumer's bucket (`AmazonS3-PUSH`), with credentials in
    the vault or from the pod's IAM role.

    [:octicons-arrow-right-24: Amazon S3 guide](aws-s3.md)

-   :material-microsoft-azure:{ .lg .middle } __Azure Blob Storage__

    ---

    Offer blobs from a storage account and push them to the consumer's container (`AzureStorage-PUSH`), with
    account keys in the vault and short-lived SAS tokens.

    [:octicons-arrow-right-24: Azure Blob guide](azure-blob.md)

-   :material-cloud-outline:{ .lg .middle } __Run on a cloud platform__

    ---

    Deploy the connector itself on AWS, Azure or another Kubernetes platform.

    [:octicons-arrow-right-24: Platforms](../deployment/platforms.md)

</div>

## What the standard images contain

| Technology | Transfer type | Data address `type` | In the image |
|---|---|---|---|
| HTTP | `HttpData-PULL`, `HttpData-PUSH` | `HttpData` | Control and data plane |
| Amazon S3 and S3-compatible storage | `AmazonS3-PUSH` | `AmazonS3` | Data plane (`data-plane-aws-s3`, S3 provisioner), control plane validator |
| Azure Blob Storage | `AzureStorage-PUSH` | `AzureStorage` | Data plane (`data-plane-azure-storage`, blob provisioner) |

Each data plane announces the transfer types it supports to the control plane, and the provider's catalog lists them
as distribution formats (see [Catalog](../usage/management-api-walkthrough/04_catalog.md)). Mixing works: a provider
can serve an S3 asset into an Azure container, and the other way round.

## More technologies

| Technology | Where |
|---|---|
| Apache Kafka streaming | [Tractus-X EDC Kafka extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension) |
| Asset Administration Shells | [EDC Extension for AAS](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS) (Fraunhofer IOSB) |
| Federated queries (SPARQL) | [Knowledge Agents EDC](https://github.com/eclipse-tractusx/knowledge-agents-edc) |
| Anything else | Write a data plane extension: see [Writing extensions](../developer/extensions.md) and the upstream [data plane documentation](https://eclipse-edc.github.io/documentation/for-contributors/data-plane/) |

!!! todo "Guides to come"

    More guides are planned, for example for Google Cloud Storage via its S3-compatible API, MinIO on premises and
    the Kafka extension. Missing one you need? [Open an issue](https://github.com/eclipse-tractusx/tractusx-edc/issues/new/choose)
    or contribute it.


## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
