# Repository Structure

The repository for Tractus-X EDC contains the following:

## Tractus-X EDC Connector distributions

The repository provides distributions of the EDC Connector. The configurations for these can be found under the folders
edc-controlplane and edc-dataplane.

- **edc-runtime-memory:** Bundles a control and dataplane in a single runtime with in-memory persistence.
  This distribution is mostly suitable for local development and testing.
- **edc-controlplane-base** and **edc-dataplane-base:** This configuration bundles all core components of the EDC
  Connector control plane as well as specific tractus-x EDC extensions. They are used as base for other distributions.
- **edc-controlplane-postgresql-hashicorp-vault:**  Starts with the controlplane-base but adds PostgreSQL as persistence
  and HashiCorp Vault as secrets backend.
- **edc-dataplane-hashicorp-vault:** Similar to the control plane distribution but for an EDC Connector data plane.

These distributions are packaged and published to maven central and to docker hub.

## EDC Connector Extensions

The core EDC Connector is extensible by design.
Tractus-X EDC provides a myriad of extensions that serve specific use cases.
These extensions and their documentation are available under the edc-extensions folder and each should provide its own
documentation.

## Helm Charts for EDC Deployment

To facilitate deployment of these different builds and their prerequisites,
Helm charts are provided. The charts and their documentation can be found
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/README.md).

## End-to-End Tests

A test suite composed of a set of end-to-end tests is provided under the edc-tests folder.
They test the functionality of the EDC Connector in a black-box manner and should cover most of the tractus-x use case 
requirements.
The tests instantiate runtimes built using the base control and dataplane distributions.

## Core and spi
Contains core tractusx-edc modules and interfaces that are used by tractusx-edc extensions.

## Samples
Might contain sample code that demonstrates how to use certain extensions or functionalities of tractusx-edc.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
