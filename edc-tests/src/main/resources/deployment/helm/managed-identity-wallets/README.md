# Introduction
This Helm chart is designed for automated test porpusses, the default setup
is starting an Kubernetes Deployment of the Managed-Identity-Wallets, inclusive
Identity Management via Keycloak and one Database container which contains the
persistence of the Managed-Identity-Wallets (MIW).

After each restart, the cluster will be started with a default Deployment, inclusively
an Authority Wallet and two test Wallets.

# 1. Starting of the Helm chart
Steps needed before starting the solution:

## 1.1 Build the Docker Image for the Managed-Identity-Wallets
Based on the [official documentation](https://ktor.io/docs/docker.html#getting-the-application-ready)
below the steps to build and run this service via Docker.

First step is to create the distribution of the Managed-Identity-Wallet application.
Can be found in the Submodule 'product-core-managed-identity-wallets' and build it, in this example using Gradle:
```
./gradlew installDist
```

Next step is to build and tag the Docker image of the MIW:
```
docker build -t catena-x/managed-identity-wallets:0.4.5 .
```

## 1.2 Setup the Kubernetes Secrets for the Helm deployment
For the deployment the following secrets are needed. Note that the secrets are
created for the namespace 'managed-identity-wallets' in this test setup.

### Secrets for MIW-application
``` bash
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-secrets \
  --from-literal=cx-db-jdbc-url='jdbc:postgresql://managed-identity-wallets-local-postgresql:5432/postgres?user=postgres&password=cx-password' \
  --from-literal=cx-auth-client-id='Custodian' \
  --from-literal=bpdm-auth-client-id='testID' \
  --from-literal=bpdm-auth-client-secret='testSecret' \
  --from-literal=cx-auth-client-secret='Custodian-Secret'
```

### Secrets for Acapy
```bash
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-acapy-secrets \
  --from-literal=acapy-wallet-key='issuerKeySecret19' \
  --from-literal=acapy-agent-wallet-seed='00000000000000000000000111111119' \
  --from-literal=acapy-jwt-secret='jwtSecret19' \
  --from-literal=acapy-db-account='postgres' \
  --from-literal=acapy-db-password='cx-password' \
  --from-literal=acapy-db-admin='postgres' \
  --from-literal=acapy-db-admin-password='cx-password' \
  --from-literal=acapy-admin-api-key='Hj23iQUsstG!dde'
```

### Secrets for MIW-Postgres
```bash
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-postgresql \
--from-literal=password='cx-password' \
--from-literal=postgres-password='cx-password' \
--from-literal=user='postgres'
```

### Secrets for Acapy-Postgres
```bash
kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-acapy-postgresql \
--from-literal=password='cx-password' \
--from-literal=postgres-password='cx-password' \
--from-literal=user='postgres'
```

## 1.3 Apply Configmap for init SQLs
The default state of the test container comes from SQL-Dumps which get executed
on each deployment of the Chart. For the Mapping a configmap is used.
```shell
kubectl create configmap miw-map --from-file=templates/configmap.yaml
```

## 1.4 Install/Upgrade for local testing
The Helm file is now ready for deployment, execute the following Command
and the System should be ready.
```shell
helm dependency build
helm upgrade --install managed-identity-wallets-local --namespace managed-identity-wallets -f values.yaml -f values-local.yaml .
```

# 2 Help
## Accessing Keycloak Token
This setup is purposed to be accessed via an EDC-Instance in the Cluster. So if you want to test it
with Postman, you have to get the Keycloak Token via Container Shell, so that the issuer URL is the 
same as in the Test-Container defined.

Create a Pod for getting the Keycloak token:
```shell
kubectl run --rm -it client --image alpine
```

#### Within the pod:
This command is for fetching the accessToken.
```
curl --location --request POST 'http://catenax-keycloak.managed-identity-wallets/realms/catenax/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=ManagedIdentityWallets' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_secret=ManagedIdentityWallets-Secret' \
--data-urlencode 'scope=openid'
```

## Expose via loadbalancer
For viewing the Database with a DB-Viewer tool or testing via Keycloak, a Loadbalancer
is needed.

```bash
kubectl -n managed-identity-wallets apply -f templates/loadbalancer.yaml
```
