# Repository Structure

The repository for TractusX EDC can be found [here](https://github.com/eclipse-tractusx/tractusx-edc).
It contains the following components:

## EDC Extensions

The core EDC is extensible by design.
TractusX EDC provides such extensions.
These extensions and their documentation are available
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/README.md).

## Gradle Files for EDC Builds

Builds of TractusX EDC are performed via Gradle.
To allow for different configurations, different builds are provided.
For example separate secrets backends are supported, but require separate builds of EDC.
Therefor, different builds are available for both
[data plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-dataplane/README.md)
and [control plane](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-controlplane/README.md),

## Helm Charts for EDC Deployment

To facilitate deployment of these different builds and their prerequisites,
Helm charts are provided. The charts and their documentation can be found
[here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/README.md).
