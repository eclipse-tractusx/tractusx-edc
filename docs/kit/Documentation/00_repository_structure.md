# Repository Structure

The repository for Tractus-X EDC can be found [here](https://github.com/eclipse-tractusx/tractusx-edc).
It contains the following components:

## EDC Extensions

The core EDC is extensible by design.
Tractus-X EDC provides such extensions.
These extensions and their documentation are available
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/README.md).

## Gradle Files for EDC Builds

Builds of Tractus-X EDC are performed via Gradle.
To allow for different configurations, different builds are provided.
For example separate secrets backends are supported, but require separate builds of EDC.
Therefor, different builds are available for both
[data plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-dataplane/README.md)
and [control plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-controlplane/README.md),

## Helm Charts for EDC Deployment

To facilitate deployment of these different builds and their prerequisites,
Helm charts are provided. The charts and their documentation can be found
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/README.md).

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
