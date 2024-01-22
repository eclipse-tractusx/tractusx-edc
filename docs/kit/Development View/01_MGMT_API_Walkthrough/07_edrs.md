# Fetching a Data Plane Token via EDR

EDR is short for Endpoint Data Reference. It describes a more ergonomic process for the Data Consumer
to negotiate access to a Data Offer and receive the corresponding token for the HTTP Data Plane. Unlike the process via
[Contract-Negotiation](6-contract-negotiation.md)- and [Transfer-Process](06_transferprocesses.md)-APIs, the EDR-process does not require a Consumer to operate a
separate service that receives the Tokens from the Provider Control Plane. Instead, the Consumer Control Plane receives
and stores the Data Plane Tokens. Consumer Applications query it for valid tokens and use these for Data Plane calls.
As a consumer-side abstraction, the checks of the Contract Negotiation and Transfer Process phases are still executed
between the Business Partners' EDCs.

## Initiate Negotiation & Token Transfer

```http
POST /edrs HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": {
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "NegotiationInitiateRequest",
  "edc:connectorAddress": "https://provider-control.plane/api/v1/dsp",
  "edc:protocol": "dataspace-protocol-http",
  "edc:connectorId": "<PROVIDER_BPN>",
  "edc:providerId": "<PROVIDER_BPN>",
  "edc:offer": {
    "edc:offerId": "<OFFER_ID>",
    "edc:assetId": "<ASSET_ID>",
    "edc:policy": {
      "@type": "odrl:Set",
      "odrl:target": "<ASSET_ID>",
      "odrl:permission": {
        "odrl:target": "<ASSET_ID>",
        "odrl:action": {
          "odrl:type": "USE"
        },
        "odrl:constraint": {
          "odrl:and": {
            "odrl:leftOperand": "BusinessPartnerNumber",
            "odrl:operator": {
              "@id": "odrl:eq"
            },
            "odrl:rightOperand": "<BPN_CONSUMER>"
          }
        }
      },
      "odrl:prohibition": [],
      "odrl:obligation": []
    }
  }
}
```
- `edc:connectorAddress` sets the coordinates for the connector that the Consumer-EDC shall negotiate with (Provider EDC).
  It will usually end in `/api/v1/dsp`
- `edc:protocol` must be `dataspace-protocol-http`
- `edc:assetId` and all `odrl:target` properties must be the id of the EDC-Asset/dcat:DataSet that the offer was made for.
- `edc:connectorId` and `edc:providerId` must both hold the correct BPN for the `edc:connectorAddress`.
- In the `edc:offer` section, the Data Consumer specifies the Data Offer for the negotiation. As there may be multiple
  Data Offers for the same DataSet, the Data Consumer must choose one.
    - `edc:offerId` is the id of the entry in the [catalog-response](04_catalog.md) that the Consumer wants to negotiate for.
      It will usually be a concatenation of three base64-encoded ids.
    - `edc:policy` must hold an identical copy of the Data Offer's contract policy as provided via the catalog-API in the
      `odrl:hasPolicy` field.

This request synchronously returns a server-generated `negotiationId` that could be used to get the state of the negotiation.

```http
POST /v2/contractnegotiations HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```
When the `edc:state` in the response is `"FINALIZED"`, the Consumer can proceed.

## Retrieving the Data Plane Token from the Consumer Control Plane

The Consumer Control Plane can be queried for EDRs by the 
- id of the [Assets](01_assets.md) and/or 
- id of the relevant Contract Agreement (given there is one)
- id of the Contract Negotiation (as obtained [previously](#initiate-negotiation--token-transfer))
- id of the Data Provider

```http
GET /edrs?assetId=myAssetId&agreementId=myAgreementId HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

It returns a set of EDR entries holding meta-data including two IDs per entry:
- `edc:transferProcessId`: This is the ID of the [Transfer Process](06_transferprocesses.md) that was implicitly initiated
  by the POST `/edrs` request.
- `edc:agreementId`: This is the ID of the agreement that the two EDCs have made in the [Contract Negotiation](05_contractnegotiations.md)
  phase of their EDR-interaction.

One of the essential features of the EDR-API is the automatic retrieval of new short-lived Data Plane tokens for an
agreed Contract Agreement. When choosing the route via [the negotiation-](05_contractnegotiations.md) and [transfer-APIs](06_transferprocesses.md),
the transfer process would have to be initiated for every new token.

The EDR mechanism stores the Data Plane tokens in the secure vault of the Consumer's Control Plane. 
Finally, after first obtaining them from the Provider Control Plane and
then locating in the Consumer Control Plane's cache, they can be retrieved using the `transferProcessId`.

```http
GET /edrs/myTransferProcessId HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

The interesting field is `edc:authCode`. It holds a short-lived token that the Consumer can use to unlock the HTTP Data Plane
that is located at `edc:endpoint`.

```json
{
  "@type": "edc:DataAddress",
  "edc:type": "EDR",
  "edc:authCode": "myAuthCode",
  "edc:endpoint": "https://provider-data.plane/api/public",
  "edc:id": "someServer-GeneratedId",
  "edc:authKey": "Authorization",
  "@context": {
    "dct": "https://purl.org/dc/terms/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "https://www.w3.org/ns/dcat/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/"
  }
}
```

## Automatic Renewal

Since `tractusx-edc` [v0.5.1](https://github.com/eclipse-tractusx/tractusx-edc/releases/tag/0.5.1) the cached EDRs also come with a state machine that will manage the lifecycle of an EDR
on the consumer side. That means that it will auto-renew it is nearing its expiration date by firing another transfer 
process request with the same parameters as the original one. Once renewed, the old EDR will transition to the `EXPIRED` 
state, and it will be removed from the database and the vault according to the [configuration](../../core/edr-core/README.md).

## EDR Management | Deleting a cached EDR

This endpoint will delete the EDR entry associated with the `transfer-process-id` and it will remove the EDR itself
from the vault.

```http
DELETE /edrs/myTransferProcessId HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```
# Using the EDR for Data Access

Once the EDR has been negotiated and stored, the data can be fetched in two ways depending on the use-case:

- Provider data-plane ("EDC way")
- Consumer proxy (Tractus-X EDC simplified)

## Provider Data Plane

Once the right EDR has been identified using the EDR Management API the current asset/agreement/transfer-process that
you want to transfer, we can use the `endpoint`, `authCode` and `authKey` to make the request. If the HTTP [Asset](01_assets.md) 
has been configured to proxy also HTTP verb, query parameters and path segments, they will be forwarded to the backend from the 
Provider Data Plane:

```http
GET /subroute?foo=bar HTTP/1.1
Host: https://consumer-data.plane/api/public
X-Api-Key: password
Content-Type: application/json
```

## Consumer Data Plane Proxy

The Consumer Data Plane Proxy is an extension available in `tractusx-edc` that will use the EDR store to simplify
the data request on Consumer side. The documentation is available [here](../../edc-extensions/dataplane-proxy/edc-dataplane-proxy-consumer-api/README.md).

The API fetches the data according to the input body. The body should contain the `assetId` plus `providerId` or the 
`transferProcessId` which identifies the EDR to use for fetching data and an `endpointUrl` which is the [provider gateway](../../edc-extensions/dataplane-proxy/edc-dataplane-proxy-provider-api/README.md)
on which the data is available. 

Please note that the path-segment `/aas/` is not configurable. Still, this feature is not specific to the Asset Administration
Shell APIs but can be used to connect to any Http-based Asset.

Example:

```http
POST /aas/request HTTP/1.1
Host: https://consumer-data.plane/proxy
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "assetId": "1",
  "providerId": "BPNL000000000001",
  "endpointUrl": "http://provider-data.plane/api/gateway/aas/test"
}
```

Alternatively if the `endpointUrl` is not known or the gateway on the provider side is not configured, it can be omitted and the `Edr#endpointUrl`
will be used as base url. In this scenario if needed, users can provide additional properties to the request for composing the final
url:

- `pathSegments` sub path to append to the base url
- `queryParams` query parameters to add to the url

So if the `Edr#endpointUrl` is `http://provider-data.plane:8080/test`, the following request
```http
POST /aas/request HTTP/1.1
Host: https://consumer-data.plane/proxy
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "assetId": "1",
  "providerId": "BPNL000000000001",
  "pathSegments": "/sub",
  "queryParams": "foo=bar"
}
```

will be routed to `http://provider-data.plane:8080/test/sub?foo=bar` and then resolved to the backend as described in
the docs for the [`baseUrl` of the Asset](01_assets.md#http-data-plane).

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)