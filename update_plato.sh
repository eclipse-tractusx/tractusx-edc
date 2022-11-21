#!/bin/bash

# UNINSTALL
helm uninstall cx-plato --namespace cx

# INSTALL
helm install cx-plato --namespace cx --create-namespace charts/txcd \
    --set fullnameOverride=plato \
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
    --set postgresql.jdbcUrl=jdbc:postgresql://plato-postgresql:5432/edc \
    --set vault.hashicorp.url=http://vault:8200 \
    --set vault.hashicorp.token=root \
    --set vault.secretNames.transferProxyTokenSignerPublicKey=plato/daps/my-plato-daps-crt \
    --set vault.secretNames.transferProxyTokenSignerPrivateKey=plato/daps/my-plato-daps-key \
    --set vault.secretNames.transferProxyTokenEncryptionAesKey=plato/data-encryption-aes-keys \
    --set vault.secretNames.dapsPrivateKey=plato/daps/my-plato-daps-key \
    --set vault.secretNames.dapsPublicKey=plato/daps/my-plato-daps-crt \
    --set daps.url=http://ids-daps:4567 \
    --set daps.clientId=99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FA:F6:0F:75:0A:23:keyid:99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FA:F6:0F:75:0A:23 \
    --set backendService.httpProxyTokenReceiverUrl=http://example.com

kubectl get pods -n cx
