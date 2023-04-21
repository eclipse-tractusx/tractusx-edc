# Setup in Kubernetes via Helm

## Introduction

While the local setup described earlier is sufficient to test basic EDC functionality, it is not appropriate for any actual environments.
For a more complete setup, Helm charts are provided.

## Setup

To set up an example environment, you can use the following Helm commands:

```shell
helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
helm install my-release tractusx-edc/tractusx-connector-memory --version 0.3.3
```

## Configuration

The Helm chart used above can be found [here](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts).
Configuring that deployment requires the same parameters as the local setup described previously.
Helm expects these parameters in the relevant `values.yaml`.
Similar example configurations can be found with the respective charts under the above link.
