# Supported Proprietary Extensions

## Decision

The Tractus-X EDC dataplane will support the following proprietary extensions:

1. AWS S3
2. Azure Blob Storage

## Rationale

### Requirements

1. In addition to transfer over HTTP we need to be able to transfer larger files (i.e. Buckets).
2. The packaged artefact (docker image, helm chart) should not require additional compilation or packaging.
3. Broad coverage of use-cases with different extensions.

### Solution

Previously, there was only support for AWS S3 which could be seen as problematic and favouritism.
As such, the available support was extended with Azure Blob Storage to offer alternatives and cover a wider range of use-cases.
According to this decision, the following protocols will be supported in the future:

1. HTTP
2. AWS S3
3. Azure Blob Storage

## Approach

To properly incorporate Azure Blob Storage and enable interoperability, all required extensions, i.e. HTTP, AWS S3 and Azure Blob Storage, will be bundled into the data plane image.
This dataplane is then configured based on which extensions the user wants to use.
Configuring specific features is entirely optional, and omitting to do so will cause that feature to be inactive at runtime.

Going forward, the Tractus-X EDC team will limit its efforts to critical bug fixes in the respective upstream repositories.
As these extensions are proprietary in nature, the Tractus-X EDC Team will add no additional features beyond basic support.

The Sftp Protocol is another contender for integrating a more accessible protocol.
It is however a low priority feature at the moment and will be postponed for future iterations.

> Note: Azure Blob Storage will be integrated with Tractus-X EDC version 0.6.0

An application process for possible additional integrations will follow at a later date.

## Open Questions

What is the process for including/supporting additional Cloud Vendors?

Who will maintain the vendor specific solutions in the long-term (after the consortia phase)?

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
