#!/bin/bash

# UNINSTALL
helm uninstall cx --namespace cx
sleep 10

# CREATE NEW IMAGE
./mvnw spotless:apply clean package -Pwith-docker-image

# LOAD IMAGE
minikube image load edc-controlplane-postgresql-hashicorp-vault:latest
minikube image load edc-dataplane-hashicorp-vault:latest
minikube image ls | grep edc

# INSTALL
helm dependency update edc-tests/src/main/resources/deployment/helm/all-in-one
helm install cx --namespace cx --create-namespace edc-tests/src/main/resources/deployment/helm/all-in-one \
    --set plato.controlplane.image.tag=latest \
    --set plato.controlplane.image.pullPolicy=Never \
    --set plato.controlplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set plato.dataplane.image.tag=latest \
    --set plato.dataplane.image.pullPolicy=Never \
    --set plato.dataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault \
    --set sokrates.controlplane.image.tag=latest \
    --set sokrates.controlplane.image.pullPolicy=Never \
    --set sokrates.controlplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set sokrates.dataplane.image.tag=latest \
    --set sokrates.dataplane.image.pullPolicy=Never \
    --set sokrates.dataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault
kubectl get pods -n cx
