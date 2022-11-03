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
    --set sokratesedccontrolplane.image.tag=latest \
    --set sokratesedccontrolplane.image.pullPolicy=Never \
    --set sokratesedccontrolplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set sokratesedcdataplane.image.tag=latest \
    --set sokratesedcdataplane.image.pullPolicy=Never \
    --set sokratesedcdataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault \
    --set platoedccontrolplane.image.tag=latest \
    --set platoedccontrolplane.image.pullPolicy=Never \
    --set platoedccontrolplane.image.repository=docker.io/library/edc-controlplane-postgresql-hashicorp-vault \
    --set platoedcdataplane.image.tag=latest \
    --set platoedcdataplane.image.pullPolicy=Never \
    --set platoedcdataplane.image.repository=docker.io/library/edc-dataplane-hashicorp-vault
kubectl get pods -n edc-all-in-one
