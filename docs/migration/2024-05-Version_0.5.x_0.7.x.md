# Migration Guide `0.5.x/0.6.0 -> 0.7.x`

<!-- TOC -->

* [Migration Guide `0.5.x/0.6.0 -> 0.7.x`](#migration-guide-05x060---07x)
    * [1. Dataplane Signaling (DPS)](#1-dataplane-signaling-dps)
        * [1.1 Overview](#11-overview)
        * [1.2 Automatic token renewal](#12-automatic-token-renewal)
        * [1.3 New data format for `DataAddress`](#13-new-data-format-for-dataaddress)
        * [1.4 Dataplane Signaling API](#14-dataplane-signaling-api)
        * [1.5 Configuration values](#15-configuration-values)
        * [1.6 Further references](#16-further-references)
    * [2. Changes to EDR handling and APIs](#2-changes-to-edr-handling-and-apis)
        * [2.1 Example of dynamic callback](#21-example-of-dynamic-callback)
        * [2.2 Example of static callbacks configuration](#22-example-of-static-callbacks-configuration)
        * [2.3 References](#23-references)
    * [3. Identity And Trust Protocols (IATP)](#3-identity-and-trust-protocols-iatp)
        * [3.1 Preconditions](#31-preconditions)
        * [3.2 Configuration values](#32-configuration-values)
        * [3.3 DIDs and BPNs](#33-dids-and-bpns)
        * [3.4 References](#34-references)
    * [4. BPN/DID Resolution Service](#4-bpndid-resolution-service)
        * [4.1 Config values](#41-config-values)
        * [4.2 References](#42-references)
    * [5. EDC Management API](#5-edc-management-api)
        * [5.1 Catalog](#51-catalog)
        * [5.2 Transfer Process](#52-transfer-process)
        * [5.3 EDRs](#53-edrs)
    * [6. Other changes and noteworthy items](#6-other-changes-and-noteworthy-items)

<!-- TOC -->

## 1. Dataplane Signaling (DPS)

### 1.1 Overview

Dataplane Signaling effectively replaces the previous data plane public API. It is a standardized way for the control
plane to communicate with the data plane, e.g. when suspending or resuming data transfers.

New features like token renewal build on top of it.

> This replaces the Control API, the Public API and the EDR callbacks

### 1.2 Automatic token renewal

When a `TransferProcess` enters the `STARTED` state, the consumer connector receives the Endpoint Data Reference (EDR)
that contains the URL to the public API, an access token, a refresh token and endpoint where the token can be refreshed.

EDRs are now cached automatically within the consumer connector's control plane, and the [EDR API](#2-new-edr-api) will
auto-refresh the token if it is nearing expiry.

### 1.3 New data format for `DataAddress`

In order for Dataplane Signaling to become aligned with the Dataspace Protocol (DSP), `DataAddress` objects now conform
to
the `dspace` [data format](https://github.com/eclipse-edc/Connector/blob/v0.7.2/docs/developer/data-plane-signaling/data-plane-signaling-token-handling.md#2-updates-to-thedataaddress-format).

### 1.4 Dataplane Signaling API

This is a new REST api that replaces the old Control API for a more consolidated and streamlined communication between a
participant's control plane and data plane.

### 1.5 Configuration values

| Helm value                                         | Environment value                                   | required | default value        | description                                               |
|----------------------------------------------------|-----------------------------------------------------|----------|----------------------|-----------------------------------------------------------|
| `dataplane.token.refresh.expiry_seconds`           | `TX_EDC_DATAPLANE_TOKEN_EXPIRY`                     |          | 30                   | TTL for access tokens                                     |
| `dataplane.token.refresh.expiry_tolerance_seconds` | `TX_EDC_DATAPLANE_TOKEN_EXPIRY_TOLERANCE`           |          | 10                   | tolerance for token expiry                                |
| `dataplane.token.refresh.refresh_endpoint`         | `TX_EDC_DATAPLANE_TOKEN_REFRESH_ENDPOINT`           |          | `<PUBLIC_API>/token` | endpoint for an OAuth2 token refresh request              |
| `dataplane.token.signer.privatekey_alias`          | `EDC_TRANSFER_PROXY_TOKEN_SIGNER_PRIVATEKEY_ALIAS`  | x        |                      | alias, under which the private key is stored in the vault |
| `dataplane.token.verifier.publickey_alias`         | `EDC_TRANSFER_PROXY_TOKEN_VERIFIER_PUBLICKEY_ALIAS` | x        |                      | alias, under which the public key is stored in the vault  |

### 1.6 Further references

- Dataplane Signaling
  [documentation](https://github.com/eclipse-edc/Connector/blob/v0.7.2/docs/developer/data-plane-signaling/data-plane-signaling.md)
  and [token
  handling](https://github.com/eclipse-edc/Connector/blob/v0.7.2/docs/developer/data-plane-signaling/data-plane-signaling-token-handling.md)
- [Tractus-X Specification for Token
  Renewal](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md)
- [Tractus-X EDC Implementation
  Documentation](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)

## 2. Changes to EDR handling and APIs

The setting `backendService.httpProxyTokenReceiverUrl`, which configured an EDR receiver backend using the upstream
EDC [extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/transfer/transfer-pull-http-dynamic-receiver)
is no longer available as well as the extension is not shipped in the Tractus-X EDC 0.7.x distributions.
Likewise, the option to dynamically register a consumer-side HTTP-callback via `receiverHttpEndpoint` in the POST
/transferprocesses call was removed. Consumer apps should use the /v2/edrs API instead.

The main reason is that EDC/Tractus-X-EDC switched to Dataplane Signaling for handling transfers and the EDR format
specified in
DPS [spec](https://github.com/eclipse-edc/Connector/blob/v0.7.2/docs/developer/data-plane-signaling/data-plane-signaling-token-handling.md)
was not handled properly by the receiver extension.

Now all the EDRs are automatically stored by the consumer's EDR cache and made available via the new
EDRs API. If users still need to receive the EDR on their backend and want to process this manually, EDC can handle this
natively by registering an HTTP callback for the `transfer.process.start` event.

This can be done in two ways:

- dynamic callbacks (associated to each transfer process)
- static callbacks (configured at boot time)

### 2.1 Example of dynamic callback

When starting a new transfer process `callbackAddresses` can be attached to the transfer request:

```json
{
  "callbackAddresses": [
    {
      "events": [
        "transfer.process.started"
      ],
      "uri": "https://mybackend/edr"
    }
  ]
}
```

### 2.2 Example of static callbacks configuration

```
EDC_CALLBACK_CB1_URI="https://mybackend/edr"
EDC_CALLBACK_CB1_EVENTS="transfer.process.started"
```

> When configuring static callbacks like above, users will receive notifications about transfer process start events of
> both sides consumer/provider if the connector acts as both. This can be checked by the `type` property in the event
> itself.

> Users need to be aware that if they handle the receiver manually, they also have to handle EDR token refresh using
> either the EDR API or by manually calling the refresh endpoint (not recommended).

### 2.3 References

- [OpenAPI Spec](https://app.swaggerhub.com/apis/eclipse-tractusx-bot/tractusx-edc/0.7.0), "Control Plane EDR Api"
- More info
  about [callbacks](https://github.com/eclipse-edc/Connector/tree/v0.7.2/docs/developer/decision-records/2023-02-28-processing-callbacks)

## 3. Identity And Trust Protocols (IATP)

IATP defines the message flow between a consumer connector and a provider connector when they want to exchange DSP
messages. On every message exchange, the recipient asks the sender for certain VerifiableCredentials, which are accessed
from the sender's credential storage (= "wallet").

In this release, the only IATP-compliant wallet is a centrally hosted commercial solution provided by SAP called "DIM".
Here, the DIM wallet acts as both a Secure Token Service (STS), a CredentialService (CS) and a server for DID documents.

This setup replaces the Managed Identity Wallet.

### 3.1 Preconditions

During participant onboarding the following information is generated/disclosed and must be configured before launching
the connector.

- DIM client ID: during onboarding a Client ID is created, which represents the participant's tenant within DIM
- DIM client secret: similarly, the Client Secret authenticates the tenant with DIM. **The actual secret must be stored
  under this alias in the participant's vault (Hashicorp Vault: `secrets/<ALIAS>`)!**
- DIM token URL: connectors must use this URL to request access tokens using their client-ID and client secret to obtain
  an access token for DIM
- DIM STS URL: this is the URL where connectors can request self-issued ID tokens. This is needed for every DSP
  request, and when querying the [BDRS](#4-bpndid-resolution-service).
- Decentralized Identifier (DID): the DID is a unique identifier that exposes public information such as service
  endpoints or public key material. This DID is generated by DIM upon registration.

### 3.2 Configuration values

| Helm value                           | Environment value                             | required | default value | description                                                   |
|--------------------------------------|-----------------------------------------------|----------|---------------|---------------------------------------------------------------|
| `iatp.id`                            | `EDC_IAM_ISSUER_ID`                           | x        |               | DID, e.g. `did:web:your connector`                            |
| `iatp.trustedIssuers`                | `EDC_IAM_TRUSTED-ISSUER_{{$index}}-ISSUER_ID` | x        |               | a list of DIDs, each representing an issuer of VCs            |
| `iatp.sts.dim.url`                   | `TX_EDC_IAM_STS_DIM_URL`                      | x        |               | URL where connectors can request SI tokens                    |
| `iatp.sts.oauth.token_url`           | `EDC_IAM_STS_OAUTH_TOKEN_URL`                 | x        |               | URL where connectors can request OAuth2 access tokens for DIM |
| `iatp.sts.oauth.client.id`           | `EDC_IAM_STS_OAUTH_CLIENT_ID`                 | x        |               | Client ID issued by DIM                                       |
| `iatp.sts.oauth.client.secret_alias` | `EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS`       | x        |               | alias under which the client secret is stored in the vault    |

### 3.3 DIDs and BPNs

In Tractus-X, BusinessPartnerNumbers (BPNs) are unique, stable identifiers that identify participants within the
network. IATP relies on Decentralized Identifiers (DIDs), which are used to dereference information about the
participant such as service endpoints, public key material, etc.

Since there is no way to implicitly map BPN <> DID, Tractus-X relies on a new core service called ["BPN/DID Resolution
Service" ("BDRS")](#4-bpndid-resolution-service), which provides that mapping information to authenticated parties.

### 3.4 References

- [IATP Specification](https://github.com/eclipse-tractusx/identity-trust/tree/main/specifications)
- [Tractus-X Credentials](https://github.com/eclipse-tractusx/tractusx-profiles/tree/main/cx/credentials)
- [Tractus-X Policies](https://github.com/eclipse-tractusx/tractusx-profiles/tree/main/cx/policy)

### 3.5 Limitations

- It is not possible to define an access policy that would require the evaluation of a VerifiableCredential

## 4. BPN/DID Resolution Service

The BPN/DID Resolution Service (BDRS) is a new core service that became necessary due to the dual use of identifiers (
BPNs and DIDs) in Catena-X.

In short, it exposes a GZipped list of BPN<>DID tuples, which the connector caches locally and updates periodically.
This API is called the _Directory API_.

These requests are authenticated by putting the participant's `MembershipCredential` in the `Authorization` header.

### 4.1 Config values

| Helm value                                 | Environment value                 | required | default value | description                               |
|--------------------------------------------|-----------------------------------|----------|---------------|-------------------------------------------|
| `controlplane.bdrs.cache_validity_seconds` | `TX_IAM_IATP_BDRS_CACHE_VALIDITY` |          | 600           | time (sec) that the client cache is valid |
| `controlplane.bdrs.server.url`             | `TX_IAM_IATP_BDRS_SERVER_URL`     | x        |               | base URL of the BDRS server               |

### 4.2 References

- [BDRS Server on GitHub](https://github.com/eclipse-tractusx/bpn-did-resolution-service)
- [BDRS Server Directory API](https://eclipse-tractusx.github.io/bpn-did-resolution-service/openapi/directory-api/)
- [BDRS Server Management API](https://eclipse-tractusx.github.io/bpn-did-resolution-service/openapi/management-api/) (
  not for public consumption)

Further, the BDRS server is available as Helm chart from the
official [Tractus-X Chart Repo](https://eclipse-tractusx.github.io/charts/dev):

- `bdrs-server`: official distribution using Postgres
- `bdrs-server-memory`: variant, that uses pure in-mem storage, useful for testing

To see how BDRS could be used in testing/dev etc. check out
this [test class](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/test/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImplComponentTest.java)

> Note that consuming the Directory API with tools like Postman or Insomnia is possible, but not straight forward and it
> is out-of-scope for this document.

## 5. EDC Management API

### 5.0 Policies

Due to [#4192](https://github.com/eclipse-edc/Connector/issues/4192)
and [#4179](https://github.com/eclipse-edc/Connector/issues/4179)

The `action` and the `leftOperand` field are now represented as `@id` instead of `value` in JSON-LD.

`action` from:

```json
 {
  "odrl:permission": {
    "odrl:action": {
      "odrl:type": "http://www.w3.org/ns/odrl/2/use"
    }
  }
}
```

to:

```json
{
  "odrl:action": {
    "@id": "odrl:use"
  }
}
```

`leftOperand` from:

```json
{
  "odrl:leftOperand": "https://w3id.org/catenax/policy/FrameworkAgreement"
}
```

to:

```json
{
  "odrl:leftOperand": {
    "@id": "cx-policy:FrameworkAgreement"
  }
}
```

This is reflected in the `Catalog` as well as in `PolicyDefinition` API.

### 5.1 Catalog

- `counterPartyId` is a **required** property for the catalog request in Tractus-X EDC. It's not enforced currently (
  backward compatibility) but if not provided, the IATP flows won't work and thus the catalog request will fail.

### 5.2 Transfer Process

- `transferType`  is a needed property for the transfer request to be working in tx-edc. `transferTypes` are the
  supported transfer format in tx-edc. They return as part of the Catalog response as part of distribution format for an
  asset. In TX-EDC we currently supports `HttpData-PULL`, `AzureStorage-PUSH` and `AmazonS3-PUSH`. The `transferType`
  is not mandatory (backward compatibility) but if not provided the transfer request will fail.

### 5.3 EDRs

The 0.5.4 EDRs API was removed due the adoption of DPS (data plane signaling) and reborn under a different shape
in `/v2/edrs`

The main difference between 0.5.4 and 0.7.0 is how the token refresh is handled. In 0.5.x when an EDR was about to
expire, the users had to manually trigger another transfer process, or use the EDR API which did that
automatically. This approach had major drawbacks like proactively creating transfer process on EDR expiration. In 0.7.0
this changed radically by adopting DPS and overlaying on top of it
a [refresh](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)
mechanism for EDRs.

For a detailed explanation, please refer to
the [EDR API documentation](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/usage/management-api-walkthrough/07_edrs.md).

## 6. Other changes and noteworthy items

- Changes in the policy definition properties: A policy definition now adheres to ODRL and the `@type` has changed
  to `Set`
- An `Offer` does not contain a `target` property anymore, it has to be injected in the ContractRequest message.
- The `consumerId` and `providerId` were replaced by the ODRL `assignee `and `assigner`, respectively. The latter also
  has to be injected in the ContractRequest Message.
- A transfer process has now a new transfer type property that should adhere to the following
  mapping: https://github.com/eclipse-edc/Connector/blob/v0.7.2/docs/developer/data-plane-signaling/data-plane-signaling-mapping.md
- The EDR API changed, and it's no longer possible to query for EDRs by contract agreement directly. A `QuerySpec` with
  a filter has to be used for this.
- The AWS S3 Extension also has been improved and now enables the user to specify S3 object prefixes in the data
  address. This enables the transfer of multiple files within on single transfer process. For more information, visit
  the
  extension [readme](https://github.com/eclipse-edc/Technology-Aws/tree/main/extensions/data-plane/data-plane-aws-s3).
- contrary to previous deprecation warnings, the Business Partner Number evaluation function will remain in place and
  can continue to be used in parallel to the Business Partner Group evaluation function.
- changes to environment variables in the Azure KeyVault variant of Tractus-X EDC. Note that this does not affect the
  Helm config values.
    - `EDC_VAULT_CLIENTID` was replaced by `AZURE_CLIENT_ID`
    - `EDC_VAULT_TENANTID` was replaced by `AZURE_TENANT_ID`
    - `EDC_VAULT_CLIENTSECRET` was replaced by `AZURE_CLIENT_SECRET`
    - `EDC_VAULT_CERTIFICATE` was replaced by `AZURE_CLIENT_CERTIFICATE_PATH`
