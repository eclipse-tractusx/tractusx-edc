#!/bin/bash

# Load miw image
minikube image load catena-x/managed-identity-wallets:2.1.1

# Install and Run
helm dependency build

helm upgrade --install managed-identity-wallets-local --namespace managed-identity-wallets --create-namespace -f values.yaml .

kubectl get pods -n managed-identity-wallets
