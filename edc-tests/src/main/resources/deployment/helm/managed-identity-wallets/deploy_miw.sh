#!/bin/bash

# Create namespace
kubectl create namespace managed-identity-wallets
sleep 3

# Load miw image
minikube image load catena-x/managed-identity-wallets:2.1.1

# Secrets for MIW-application
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-secrets \
  --from-literal=cx-db-jdbc-url='jdbc:postgresql://managed-identity-wallets-local-postgresql:5432/postgres?user=postgres&password=cx_password' \
  --from-literal=cx-auth-client-id='Custodian' \
  --from-literal=bpdm-auth-client-id='testID' \
  --from-literal=bpdm-auth-client-secret='testSecret' \
  --from-literal=cx-auth-client-secret='Custodian-Secret'

# Secrets for Acapy
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-acapy-secrets \
  --from-literal=acapy-wallet-key='issuerKeySecret19' \
  --from-literal=acapy-agent-wallet-seed='70010550038305040070100111510019' \
  --from-literal=acapy-jwt-secret='jwtSecret19' \
  --from-literal=acapy-db-account='postgres' \
  --from-literal=acapy-db-password='cx_password' \
  --from-literal=acapy-db-admin='postgres' \
  --from-literal=acapy-db-admin-password='cx_password' \
  --from-literal=acapy-admin-api-key='Hj23iQUsstG!dde'

# Secrets for MIW-Postgres
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-postgresql \
    --from-literal=password='cx_password' \
    --from-literal=postgres-password='cx_password' \
    --from-literal=user='postgres'

# Secrets for Acapy-Postgres
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-acapy-postgresql \
--from-literal=password='cx_password' \
--from-literal=postgres-password='cx_password' \
--from-literal=user='postgres'

# Apply Configmap for init SQLs
kubectl create configmap miw-map --from-file=templates/configmap.yaml

# Install and Run
helm dependency build

helm upgrade --install managed-identity-wallets-local --namespace managed-identity-wallets -f values.yaml .

kubectl get pods -n managed-identity-wallets
