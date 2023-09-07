# Supported Proprietary Extensions

## Decision

The Tractus-X EDC dataplane will support the following proprietary extensions:

1. AWS S3
2. Azure Blob Storage

## Rationale

### Requirements

1. In addition to smaller HTTP junks we need to be able to transfer larger files (i.e. Buckets).
2. Container or Product should not require additional compilation or packaging.
3. Broader coverage of use-cases with different extensions.

### Solution

Previously, there was only support for AWS S3 which could be seen as problematic and favouritism.
As such, the available support was extended with Azure Blob Storage to offer alternatives and cover a wider range of use-cases.
According to this decision, the following protocols will be supported in the future:

1. HTTP
2. AWS S3
3. Azure Blob Storage

## Approach

To properly incorporate Azure Blob Storage and enable interoperability, there will be a single dataplane which will support HTTP, AWS S3 and Azure Blob Storage.
This dataplane is then configured based on which extensions, the user wants to use.
In the case that one or more extensions are not configured, the dataplane is then to exclude these extensions and continue working regardless.
This makes the use of AWS S3 and Azure Blob Storage entirely optional.

Going forward, the integration of S3 and Azure Blob Storage will stay at the minimum and only bug fixes will be made after the integration.
As these extensions are proprietary in nature, the Tractus-X EDC Team will add no additional features beyond basic support.

The Sftp Protocol is another contender for integrating a more accessible protocol.
It is however a low priority at the moment and will move on to be an issue for PI 11.

> Note: Azure Blob Storage will be integrated in tune with Tractus-X EDC version 0.6.0

An application process for possible additional integrations will follow at a later date.

## Open Questions

What is the process for including/supporting additional Cloud Vendors?

Who will maintain the vendor specific solutions in the long-term (after the consortia phase)?
