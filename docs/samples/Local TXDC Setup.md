# Local TXDC Setup

This document describes how to set up two TXDConnector instances locally. The Supporting Infrastructure Deployment, used
by this example, must never be used productively. The deployment of the two TXDConnector instances, done by this example,
is not suitable for productive deployment scenarios.

## Prerequisites

[![Helm][helm-shield]][helm-url]

[![Kubernetes][kubernets-shield]][kubernets-url]

## Local Deployment

The Local TXDC Setup consists of three separate deployments. The Supporting Infrastructure, that is required to
run connectors, and two different TXDC Connector instances, that can communicate with each other.

- [TXDC Supporting Infrastructure](../../edc-tests/src/main/resources/deployment/helm/supporting-infrastructure/README.md)
- [TXDC Connector](../../charts/tractusx-connector/README.md) Plato
- [TXDC Connector](../../charts/tractusx-connector/README.md) Sokrates

[helm-shield]: https://img.shields.io/badge/Helm-URL-lightgrey

[helm-url]: https://helm.sh

[kubernets-shield]: https://img.shields.io/badge/Kubernetes-URL-lightgrey

[kubernets-url]: https://kubernetes.io/

### Supporting Infrastructure

Before the connectors can be setup, the Supporting Infrastructure must be in place. It comes with pre-configured everything
to run two connectors independently.

For this local test scenario,
the [Supporting Infrastructure](../../edc-tests/src/main/resources/deployment/helm/supporting-infrastructure/README.md)
of the TXDC Business Tests can be used.

Install the TXDC Supporting Infrastructure by running the following command from the project root directory. The Minio
set can be skipped, as it's only used by AWS S3 Transfer Business Tests. Also, the PostgreSQL Database is not really
mandatory to try out the EDC. So it can be disabled as well.

```sh
helm dependency update edc-tests/src/main/resources/deployment/helm/supporting-infrastructure
```

```sh
helm install infrastructure edc-tests/src/main/resources/deployment/helm/supporting-infrastructure \
    --namespace cx \
    --create-namespace \
    --set install.minio=false \
    --set install.postgresql=false
```

### Plato Connector

After the supporting infrastructure is deployed the Plato Connector can be added. The Supporting Infrastructure
Deployment has a DAPS Client and Vault Secrets configured accordingly. So that the TXDConnector can use them directly.

Install Plato by running the following command from the project root directory.

```sh
helm install plato charts/tractusx-connector \
    --namespace cx \
    --create-namespace \
    --set fullnameOverride=plato \
    --set controlplane.image.tag=0.2.0 \
    --set controlplane.service.type=NodePort \
    --set controlplane.endpoints.data.authKey=password \
    --set vault.hashicorp.enabled=true \
    --set vault.hashicorp.url=http://vault:8200 \
    --set vault.hashicorp.token=root \
    --set vault.secretNames.transferProxyTokenSignerPublicKey=plato/daps/my-plato-daps-crt \
    --set vault.secretNames.transferProxyTokenSignerPrivateKey=plato/daps/my-plato-daps-key \
    --set vault.secretNames.transferProxyTokenEncryptionAesKey=plato/data-encryption-aes-keys \
    --set vault.secretNames.dapsPrivateKey=plato/daps/my-plato-daps-key \
    --set vault.secretNames.dapsPublicKey=plato/daps/my-plato-daps-crt \
    --set daps.url=http://ids-daps:4567 \
    --set daps.clientId=99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FA:F6:0F:75:0A:23:keyid:99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FA:F6:0F:75:0A:23 \
    --set backendService.httpProxyTokenReceiverUrl=http://backend:8080
```

The different settings are explained in the [TXDC Connector](../../charts/tractusx-connector/README.md) documentation.
Basically this deployment overrides the full name, to avoid naming conflicts, and sets a NodePort, to access the
containers from outside the local Kubernetes cluster. Then it configures a DAPS instance and the corresponding vault,
where the DAPS secrets are persisted, so that the connector has its own identity.

### Sokrates Connector

After Plato is set up the same can be done for Sokrates. The main difference will be, that Sokrates uses another DAPS
Client ID with different public-/private keys.

Install Sokrates by running the following command from the project root directory.

```shell
helm install sokrates charts/tractusx-connector  \
    --namespace cx \
    --create-namespace \
    --set fullnameOverride=sokrates \
    --set controlplane.image.tag=0.2.0 \
    --set controlplane.service.type=NodePort \
    --set controlplane.endpoints.data.authKey=password \
    --set vault.hashicorp.enabled=true \
    --set vault.hashicorp.url=http://vault:8200 \
    --set vault.hashicorp.token=root \
    --set vault.secretNames.transferProxyTokenSignerPublicKey=sokrates/daps/my-sokrates-daps-crt \
    --set vault.secretNames.transferProxyTokenSignerPrivateKey=sokrates/daps/my-sokrates-daps-key \
    --set vault.secretNames.transferProxyTokenEncryptionAesKey=sokrates/data-encryption-aes-keys \
    --set vault.secretNames.dapsPrivateKey=sokrates/daps/my-sokrates-daps-key \
    --set vault.secretNames.dapsPublicKey=sokrates/daps/my-sokrates-daps-crt \
    --set daps.url=http://ids-daps:4567 \
    --set daps.clientId=E7:07:2D:74:56:66:31:F0:7B:10:EA:B6:03:06:4C:23:7F:ED:A6:65:keyid:E7:07:2D:74:56:66:31:F0:7B:10:EA:B6:03:06:4C:23:7F:ED:A6:65 \
    --set backendService.httpProxyTokenReceiverUrl=http://backend:8080
```

## Uninstall

```shell
helm uninstall --namespace cx infrastructure
helm uninstall --namespace cx plato
helm uninstall --namespace cx sokrates
```

> To try out the local setup, have a look at the [Transfer Example Documentation](Transfer%20Data.md)