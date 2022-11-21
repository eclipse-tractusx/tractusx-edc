#!/bin/bash

# UNINSTALL
helm uninstall cx-sokrates --namespace cx

# INSTALL
helm install cx-sokrates --namespace cx --create-namespace charts/txdc \
    --set fullnameOverride=sokrates \
    --set controlplane.service.type=NodePort \
    --set controlplane.endpoints.data.authKey=password \
    --set controlplane.image.tag=latest \
    --set controlplane.image.pullPolicy=Never \
    --set controlplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set dataplane.image.tag=latest \
    --set dataplane.image.pullPolicy=Never \
    --set dataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault \
    --set controlplane.debug.enabled=true \
    --set controlplane.suspendOnStart=false \
    --set postgresql.enabled=true \
    --set postgresql.username=user \
    --set postgresql.password=password \
    --set postgresql.jdbcUrl=jdbc:postgresql://sokrates-postgresql:5432/edc \
    --set vault.hashicorp.url=http://vault:8200 \
    --set vault.hashicorp.token=root \
    --set vault.secretNames.transferProxyTokenSignerPublicKey=sokrates/daps/my-sokrates-daps-crt \
    --set vault.secretNames.transferProxyTokenSignerPrivateKey=sokrates/daps/my-sokrates-daps-key \
    --set vault.secretNames.transferProxyTokenEncryptionAesKey=sokrates/data-encryption-aes-keys \
    --set vault.secretNames.dapsPrivateKey=sokrates/daps/my-sokrates-daps-key \
    --set vault.secretNames.dapsPublicKey=sokrates/daps/my-sokrates-daps-crt \
    --set daps.url=http://ids-daps:4567 \
    --set daps.clientId=E7:07:2D:74:56:66:31:F0:7B:10:EA:B6:03:06:4C:23:7F:ED:A6:65:keyid:E7:07:2D:74:56:66:31:F0:7B:10:EA:B6:03:06:4C:23:7F:ED:A6:65 \
    --set dataplane.aws.endpointOverride=http://minio:9000 \
    --set dataplane.aws.secretAccessKey=sokratesqwerty123 \
    --set dataplane.aws.accessKeyId=sokratesqwerty123 \
    --set backendService.httpProxyTokenReceiverUrl=http://example.com

kubectl get pods -n cx
