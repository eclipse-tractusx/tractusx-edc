#!/bin/bash

# UNINSTALL
helm uninstall cx-infra --namespace cx

# INSTALL
helm dependency update edc-tests/src/main/resources/deployment/helm/supporting-infrastructure
helm install cx-infra --namespace cx --create-namespace edc-tests/src/main/resources/deployment/helm/supporting-infrastructure
kubectl get pods -n cx
