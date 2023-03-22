# Setup in Kubernetes via Helm

## Introduction

While the local setup described earlier is sufficient to test basic EDC functionality, it is not appropriate for any actual environments.
For a more complete setup, Helm charts are provided.

## Setup

To set up an example environment, you can use the following Helm commands:

```shell
helm repo add catenax-ng-product-edc https://catenax-ng.github.io/product-edc
helm install tractusx-connector catenax-ng-product-edc/tractusx-connector --version 0.2.0
```

## Configuration

The Helm chart used above can be found [here](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts).
Configuring that deployment requires the same parameters as the local setup described previously.
Helm expects these parameters in the relevant `values.yaml`.
Similar example configurations can be found with the respective charts under the above link.
