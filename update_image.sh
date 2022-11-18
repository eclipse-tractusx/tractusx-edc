#!/bin/bash

# UNINSTALL
helm uninstall edc-all-in-one --namespace edc-all-in-one
sleep 10

# CREATE NEW IMAGE
./mvnw spotless:apply clean package -Pwith-docker-image

# LOAD IMAGE
minikube image load edc-controlplane-postgresql-hashicorp-vault:latest
minikube image load edc-dataplane-hashicorp-vault:latest
minikube image ls | grep edc

# INSTALL
helm dependency update edc-tests/src/main/resources/deployment/helm/all-in-one
helm install edc-all-in-one --namespace edc-all-in-one --create-namespace edc-tests/src/main/resources/deployment/helm/all-in-one \
    --set txdc.controlplane.image.tag=latest \
    --set txdc.controlplane.image.pullPolicy=Never \
    --set txdc.controlplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set txdc.dataplane.image.tag=latest \
    --set txdc.dataplane.image.pullPolicy=Never \
    --set txdc.dataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault
kubectl get pods -n edc-all-in-one
