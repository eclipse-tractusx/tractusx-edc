# Repository Structure

The repository for Tractus-X EDC can be found [here](https://github.com/eclipse-tractusx/tractusx-edc).
It contains the following components:

## EDC Extensions

The core EDC is extensible by design.
Tractus-X EDC provides such extensions.
These extensions and their documentation are available
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/README.md).


### Business Partner Validation

This extension allows for validation of business partners within the access policy.

### Control Plane EDR APIs

The goal of this extension is to simplify the process of retrieving data out of EDC.
It returns `EndpointDataReference` object, hiding all the communication details for contract offers,
contract negotiation, transfer process and retrieving the underlying data through the data-planes.

### Data Plane HTTP OAuth2

The Data Plane HTTP OAuth2 extension permits the data-plane to fetch the data requested from a consumer from an HTTP server
with an OAuth2 authentication layer.

For further documentation, please refer to the extension README:
<https://github.com/eclipse-edc/Connector/tree/main/extensions/data-plane/data-plane-http-oauth2-core>

### Data Plane Proxy

The Data Plane Proxy mechanism is a convenience function to avoid interaction with the EDR cache and automatically
retrieve a valid token.

### Data Encryption

The EDC encrypts sensitive information inside a token it sends to other applications (potentially cross-company).
This extension implements the encryption of this data and should be used with secure keys and algorithms at all times.

### Data Plane Selector

This control plane extension makes it possible to configure one or more data plane instances.
During a transfer the control plane will look for an instance with matching capabilities to transfer data.

### Hashicorp Vault

This extension allows for usage of Hashicorp Vault for secret storage.
It is the default used in Tractus-X EDC.

### PostgreSQL Migration

While the core EDC is able to interact with PostgreSQL databases, it does not automate migrations between schema versions.
This extension adds that functionality.

### Additional Headers

This extension adds additional headers to the requests that the Provider's Data Plane delegates to its backend APIs that
are registered in Assets.

### Transfer Process SFTP

This extension allows for the use of SFTP backends for the data plane (but is not included in the provided control- and data plane).

### SSI

All integration issues with wallets containing self-sovereign identities are handled in this extension.


### Gradle Files for EDC Builds

Builds of Tractus-X EDC are performed via Gradle.
To allow for different configurations, different builds are provided.
For example separate secrets backends are supported, but require separate builds of EDC.
Therefor, different builds are available for both
[data plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-dataplane/README.md)
and [control plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-controlplane/README.md),

### Helm Charts for EDC Deployment

To facilitate deployment of these different builds and their prerequisites,
Helm charts are provided. The charts and their documentation can be found
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/README.md).

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
