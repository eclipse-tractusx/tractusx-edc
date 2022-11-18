#!/bin/bash

# UNINSTALL
helm uninstall plato --namespace plato
sleep 10

# CREATE NEW IMAGE
./mvnw spotless:apply clean package -Pwith-docker-image

# LOAD IMAGE
minikube image load edc-controlplane-postgresql-hashicorp-vault:latest
minikube image load edc-dataplane-hashicorp-vault:latest
minikube image ls | grep edc

# INSTALL
helm dependency update edc-tests/src/main/resources/deployment/helm/all-in-one
helm install plato --namespace plato --create-namespace edc-tests/src/main/resources/deployment/helm/all-in-one \
    --set txdc.controlplane.image.tag=latest \
    --set txdc.controlplane.image.pullPolicy=Never \
    --set txdc.controlplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set txdc.dataplane.image.tag=latest \
    --set txdc.dataplane.image.pullPolicy=Never \
    --set txdc.dataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault \
    --set txdc.vault.hashicorp.url=http://plato-vault:8200
kubectl get pods -n plato
