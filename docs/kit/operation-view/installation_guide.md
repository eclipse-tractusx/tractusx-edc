Installation Guide for Tractus-X EDC
==================
This guide describes how to install the [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector) via helm chart with Hashicorp Vault + PostgreSQL.

Prerequisite
------------
If you do not have preexisting installations of PostgreSQL or HashiCorp Vault, you will need to configure them first.
For a test environment, simple deployments using the Helm charts `bitnami/postgresql` and `hashicorp/vault` should suffice.
These are not configured to be production-ready in security, durability, availability, etc., but should be enough for initial tests.

For PostgreSQL, decide on a name for the deployment, as well as a database name, username, and password for the connector.
If you have not used it before, add the Bitnami chart repository first:
  ```bash
  helm repo add bitnami https://charts.bitnami.com/bitnami
  ```  
You can then roll out the chart using:
  ```bash
  helm upgrade --install $NAME bitnami/postgresql --set auth.enablePostgresUser=false --set auth.database=$DATABASE --set auth.username=$USERNAME --set auth.password=$PASSWORD
  ```
If your `$NAME` contained the string `postgresql`, the database server will be available at `$NAME`, otherwise at `${NAME}-postgresql`.
Assuming the former case, the JDBC URL can be constructed as follows: `jdbc:postgresql://${NAME}:5432/$DATABASE`.

Vault has a "dev mode" that is perfect for this use case.
First, add the HashiCorp Helm repository:
  ```bash
  helm repo add hashicorp https://helm.releases.hashicorp.com
  ```
Then, as with PostgreSQL, decide on a name for your Vault instance.
You can then roll it out using:
  ```bash
  helm upgrade --install $NAME hashicorp/vault --set server.dev.enabled=true --set injector.enabled=false
  ```
Again, similar to PostgreSQL, the Vault URL is `http://${NAME}:8200` if `$NAME` contains the string `vault`, and `http://${NAME}-vault:8200` if not.
The token to use in dev mode is simply the string `root`.
For reasons of simplicity, we disable the injector, as making use of it would require further configuration of the Vault instance.

The current version of the chart requires a static Vault token that needs to be valid for the lifetime of the deployment.
In a more advanced deployment, Vault could be configured with the Kubernetes auth method and a role granting access to the relevant secrets to the deployment's service account.
Assuming the injector is enabled in this case, the deployment could be annotated as follows:

- `vault.hashicorp.com/agent-cache-enabled`: `true`
- `vault.hashicorp.com/agent-cache-use-auto-auth-token`: `force`

The deployment should then be modified to expect the Vault API at `http://localhost:8200`.
The token should be set to a garbage value, as the agent will overwrite it with the real token.

Setup
------------
1. Following mandatory properties will be required to fully configure and run the Connector in step 4 for use with Hashicorp Vault and PostgreSQL:
    - `daps.url`: the URL of the DAPS service (e.g. https://daps1.int.demo.catena-x.net)
    - `daps.clientId`: the client ID of the Connector (syntax: SKI:AKI)
    - `vault.hashicorp.enabled` to `true`
    - `vault.hashicorp.url`: the URL of the Hashicorp Vault service
    - `vault.hashicorp.token`: the token of the Hashicorp Vault service to access the secrets
    - `postgresql.enabled` to `true`
    - `postgresql.jdbcUrl`: JDBC URL of the PostgreSQL database
    - `postgresql.username`: username of the PostgreSQL database
    - `postgresql.password`: password of the PostgreSQL database
    - `controlplane.endpoints.authKey`: auth key of the control plane
    - `backendService.httpProxyTokenReceiverUrl`: URL of the HTTP proxy token receiver service
2. Further Connector specific properties can be configured:
    - `internationalDataSpaces.id`: the id of the IDS connector
    - `internationalDataSpaces.description`: the description of the IDS connector
    - `internationalDataSpaces.title`: the title of the IDS connector
    - `internationalDataSpaces.maintainer`: the maintainer of the IDS connector
    - `internationalDataSpaces.curator`: the curator profile of the IDS connector
3. Setup secrets in [Hashicorp](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/hashicorp-vault/README.md) Vault, add the following secrets:
    - `transfer-proxy-token-signer-public-key`: key `content`, key-value `-----BEGIN PUBLIC KEY-----...` (2048 bits+)
    - `transfer-proxy-token-signer-private-key`: key `content`, key-value `-----BEGIN RSA PRIVATE KEY-----...` (2048 bits+)
    - Connector certificate PEM as `daps-public-key`: key `content`, key-value `-----BEGIN CERTIFICATE-----...`
    - private-key PEM as `daps-private-key`: key `content`, key-value `-----BEGIN PRIVATE KEY----- ...`
    - `transfer-proxy-token-encryption-aes-key`: key `content`, aes-key as key-value
4. Install the Tractus-X EDC (please replace the angle brackets data with the information from step 1):
   ```bash
   helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
   helm install edc tractusx-edc/tractusx-connector --version 0.3.3 --set vault.hashicorp.enabled=true --set postgresql.enabled=true --set vault.hashicorp.url=<YOUR URL> --set vault.hashicorp.token=<YOUR TOKEN> --set daps.clientId=<YOUR ID> --set controlplane.endpoints.management.authKey=<YOUR AUTHKEY> --set postgresql.username=<YOUR USERNAME> --set postgresql.password=<YOUR PW> --set postgresql.jdbcUrl=<YOUR JDBC URL> --set backendService.httpProxyTokenReceiverUrl=<YOUR URL> --set daps.url=<YOUR URL>
   ```
5. Check if the pods are running:
   ```bash
   kubectl get pods
   ```
6. To see logs of the pods, run the following command:
   ```bash
   kubectl logs -f <pod-name>
   ```
7. Following endpoints are now available in default setup:
   - controlplane: 
     - default: port `8080` `/api` 
     - management: `8081` `/management`
     - control: port `8083` `/control` 
     - protocol: port `8084` `/api/v1/ids`
     - observability: port `8085` `/observability`
     - metrics: port `9090` `/metrics`
   - dataplane:
     - default: port `8080` `/api`
     - public: port `8081` `/api/public`
     - control: port `8083` `/api/dataplane/control`
     - observability: port `8085` `/observability`
     - metrics: port `9090` `/metrics`
    
8. How to continue?  
You could register your connector in the Catena-X portal, for more information see [here](https://portal.int.demo.catena-x.net/documentation/?path=docs%2F02.+Technical+Integration).