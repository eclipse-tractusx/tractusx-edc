# How-To run two connectors and a DAPS

## 1. Prepare environment

This guide will bring up two connectors named "Sokrates" and "Plato", each alongside their dependencies (Hashicorp
Vault, PostgreSQL) and a DAPS instance that both share.

We've tested this setup with [KinD](https://kind.sigs.k8s.io/), but other runtimes such
as [Minikube](https://minikube.sigs.k8s.io/docs/start/) may work as well, we just haven't tested them.

This version of Tractus-X EDC _requires_ a running instance of the Managed Identity Wallet and KeyCloak, a connector
will not be able to communicate to another connector without it.

Installation instructions for those are beyond the scope of this document, please refer to the respective manuals and
guides for information on how to set them up.

Furthermore, this guide assumes:

- the Tractus-X EDC repository is checked out, the working directory for this guide is `docs/samples/example-dataspace`
- a Kubernetes runtime (e.g. KinD) is already installed and ready-to-use
- basic knowledge about `helm` and Kubernetes
- the following tools are available: `yq`, `openssl`, `base64`
- a POSIX-compliant shell, e.g. `bash` or `zsh` unless stated otherwise

### 1.1 Create secrets for both runtimes

We'll need a x509 certificate in order to communicate with DAPS, as well as a private key and a Data Encryption signing
key.

```shell
# SOKRATES aes encryption key
echo "aes_enckey_test" | base64 > sokrates.aes.key

# PLATO aes encryption key
echo "aes_enckey_test" | base64 > plato.aes.key
```

Any arbitrary string can be used for the AES key, but it has to be 16, 24, or 32 characters in length, assuming UTF-8
encoding.

### 1.2 Obtain configuration for MiW and KeyCloak

> The following information is _required_, your connectors will **not** work properly unless you
> modify the `ssi:` section of `sokrates-values.yaml` and `plato-values.yaml` accordingly!

For communication with KeyCloak we need the following information

- the `tokenurl`: URL where access tokens can be obtained
- the `client.id`: KeyCloak identifier of the connector

Note that the OAuth2 client secret will be stored in the vault under the alias `client-secret`.

In order to use MiW as credential backend we need the following information:

- `url`: a URL where MiW is reachable
- `authorityId`: this is the `issuerIdentifier` for MiW REST requests, please refer to the respective documentation.

Furthermore, we need the `endpoint.audience`, which is used to verify the `aud` claim of incoming requests. This does **not** have to be set explicitly, it defaults to each connector's callback address.

## 2. Prepare Connectors

Next, the certificates and private keys we created previously must be stored in each connector's vault by injecting
a `postStart` element to the chart's configuration file:

```shell
# for sokrates
CONFIG_FILE=sokrates-values.yaml
CLIENT_SECRET=<sokrates-oauth-client-secret>

yq -i ".vault.server.postStart |= [\"sh\",\"-c\",\"{\nsleep 5\n
/bin/vault kv put secret/client-secret content=$CLIENT_SECRET\n
/bin/vault kv put secret/aes-keys content=$AES_KEY\n}\"]" "$VALUES_FILE"

# for plato
CONFIG_FILE=plato-values.yaml
CLIENT_SECRET=<plato-oauth-client-secret>

yq -i ".vault.server.postStart |= [\"sh\",\"-c\",\"{\nsleep 5\n
/bin/vault kv put secret/client-secret content=$CLIENT_SECRET\n
/bin/vault kv put secret/aes-keys content=$AES_KEY\n}\"]" "$VALUES_FILE"
```

## 3 Install the connectors

Use `helm` to install the Tractus-X EDC Helm charts. In this example we are using the _local_ charts, assuming you have
Tractus-X EDC checked out in your local filesystem at `<YOUR_PATH>`.

```shell
# install sokrates
helm install tx-sokrates <YOUR_PATH>/charts/tractusx-connector \
            -f sokrates-values.yaml \
            --dependency-update
            
# install plato 
helm install tx-plato <YOUR_PATH>/charts/tractusx-connector \
            -f plato-values.yaml \
            --dependency-update
```

_Note: if you prefer to use the published version of the `tractusx-connector` chart, please add the Tractus-X Helm repo
first:_

```shell
helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
helm install tx-[sokrates|plato] tractusx-edc/tractusx-connector \
     -f [sokrates|plato]-values.yaml \
     --dependency-update
```

## 3.1 [Optional] Verify the correct installation

There is several ways of making sure everything worked out well:

- simply look at the logs of the Helm releases, e.g. with a tool
  like [stern](https://kubernetes.io/blog/2016/10/tail-kubernetes-with-stern/) and look out for a log line similar to:

  ```shell
  stern tx-sokrates
  ```

  then look out for something similar to:

  ```shell
  tx-sokrates-controlplane-b9456f97b-s5jts tractusx-connector INFO 2023-05-31T07:24:53.020975888 tx-sokrates-controlplane ready
  ```

- wait for the Kubernetes rollout to be successful, e.g. `kubectl rollout status deployment tx-plato-controlplane`
- use `helm test` to execute tests: `helm test tx-plato`
