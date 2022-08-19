# Release Notes Version 0.1.0
19.08.2022

> **BREAKING CHANGES**
> 
> When upgrading from version 0.0.x please consolidate the migration documentation before ([link](../migration/Version_0.0.x_0.1.x.md)).

## 0. Summary

- 1. Eclipse Dataspace Connector Update
- 2. New Extensions
  - 2.1 Data Plane Selector Extensions
  - 2.2 Data Encryption Extension
  - 2.3 AWS S3 Extension
- 3. Bug Fixes

## 1. Eclipse Dataspace Connector Update

Upgraded the Eclipse Dataspace Connector Extensions to version 0.0.1-20220818-SNAPSHOT. Please be aware that this introduces some breaking changes.

Code Repository
https://github.com/eclipse-dataspaceconnector/DataSpaceConnector

Snapshot Artifact Repository
https://oss.sonatype.org/#nexus-search;quick~org.eclipse.dataspaceconnector


## 2. New Extensions

The following extensions are now included in the base image of the connector.

### 2.1 Data Plane Selector Extensions

> **Introduces new mandatory settings**

New extension to add data plane instances by configuration. Each data plane instance must be registered at the control plane.

[Documentation](../../edc-extensions/dataplane-selector-configuration/README.md)

### 2.2 Data Encryption Extension

> **Introduces new mandatory settings**

New extension to protect possibly confidential data, that may be send out to other connectors as transfer tokens.

[Documentation](../../edc-extensions/data-encryption/README.md)

### 2.3 AWS S3 Extension

New extension to transfer data from and to AWS S3 Buckets.

[Documentation](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/main/extensions/aws/data-plane-s3)

## 3. Bug Fixes

This section covers the most relevant bug fixes, included in this version.

- Contract-Offer-Receiving-Connectors must also pass the ContractPolicy of the ContractDefinition before receiving offers([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1331))

- Deletion of Asset becomes impossible when Contract Negotiation exists([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1403))

- Deletion of Policy becomes impossible when Contract Definition exists([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1410))

- DataAddress is passed unencrypted from DataProvider to DataConsumer ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1504))