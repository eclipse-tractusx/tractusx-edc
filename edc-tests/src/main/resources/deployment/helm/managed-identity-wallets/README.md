# Introduction
This Helm chart is designed for automated test porpusses, the default setup
is starting an Kubernetes Deployment of the Managed-Identity-Wallets, inclusive
Identity Management via Keycloak and one Database container which contains the
persistence of the Managed-Identity-Wallets (MIW).

After each restart, the cluster will be started with a default Deployment, inclusively
an Authority Wallet and two test Wallets.

# Starting of the Helm chart
Steps needed before starting the solution:

## 1. Build the Docker Image for the Managed-Identity-Wallets
Based on the [official documentation](https://ktor.io/docs/docker.html#getting-the-application-ready)
below the steps to build and run this service via Docker.

First step is to clone the reppsitory of the Managed-Identity-Wallet application and check out the following commit

```
git clone https://github.com/catenax-ng/product-core-managed-identity-wallets.git

git checkout 3440deaa36441ed5e2bd87f1bacc96ac309cd8b9
``` 

Then build it using Gradle:
```
./gradlew installDist
```

Next step is to build and tag the Docker image of the MIW:
```
docker build -t catena-x/managed-identity-wallets:2.1.1 .
```

## 2. Setup the Kubernetes and Run Helm deployment

### Create Namespace 

```bash
kubectl create namespace managed-identity-wallets
```
If the namespace already exists then you can delete it with

```bash
kubectl delete namespace managed-identity-wallets
```

### Deploy the Release

Run the deployement command to deploy the MIW release with its dependencies


````
./deploy_miw.sh
````

Check the logs of the Acapy container in pod `catenax-managed-identity-wallets` (check the Help section). If it is not running correctly then delete the pod  with command `kubectl delete pod catenax-managed-identity-wallets-<replace-id> -n managed-identity-wallets` 

To uninstall the deployment

```bash
helm uninstall managed-identity-wallets-local --namespace managed-identity-wallets
```

To delete all presistent data

```bash
minikube kubectl -- delete pvc -n managed-identity-wallets --all
```

```bash
minikube kubectl -- delete pv -n managed-identity-wallets --all
```
To delete namespace and everything included

```
kubectl delete namespace managed-identity-wallets
```

## Help

### Check the logs 
To check the logs of MIW and AcaPy open two terminals and run the commands after replacing the pod name

```bash
kubectl logs -f <pod-name> -c catenax-managed-identity-wallets -n managed-identity-wallets

kubectl logs -f <pod-name> -c catenax-acapy -n managed-identity-wallets
```


### Accessing Keycloak Token
This setup is purposed to be accessed via an EDC-Instance in the Cluster. So if you want to test it
with Postman, you have to get the Keycloak Token via Container Shell, so that the issuer URL is the 
same as in the Test-Container defined.

Create a Pod for getting the Keycloak token:
```shell
kubectl run --rm -it client --image alpine
```

#### Within the pod:

Install cURL with 
```
apk --no-cache add curl
```

And run this command is for fetching the accessToken.
```
curl --location --request POST 'http://catenax-keycloak.managed-identity-wallets/realms/catenax/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=ManagedIdentityWallets' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_secret=ManagedIdentityWallets-Secret' \
--data-urlencode 'scope=openid'
```

### Expose via loadbalancer
For viewing the Database with a DB-Viewer tool or testing via Keycloak, a Loadbalancer
is needed.

```bash
kubectl -n managed-identity-wallets apply -f templates/loadbalancer.yaml
```

kubectl logs -f catenax-managed-identity-wallets-5559bcd8d4-7ghvs -c catenax-managed-identity-wallets -n managed-identity-wallets

kubectl logs -f catenax-managed-identity-wallets-5559bcd8d4-7ghvs -c catenax-acapy -n managed-identity-wallets
