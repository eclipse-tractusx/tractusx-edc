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

The Consumer Control Plane can be queried for EDRs by the ids of the [Assets](01_assets.md).

```http
GET /edrs?assetId=myAssetId HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

It returns a set of EDR entries holding meta-data including two new IDs per entry:
- `edc:transferProcessId`: This is the ID of the [Transfer Process](06_transferprocesses.md) that was implicitly initiated
  by the POST `/edrs` request.
- `edc:agreementId`: This is the ID of the agreement that the two EDCs have made in the [Contract Negotiation](05_contractnegotiations.md)
  phase of their EDR-interaction.

One of the essential features of the EDR-API is the automatic retrieval of new short-lived Data Plane tokens for an
agreed Contract Agreement. When choosing the route via [the negotiation-](05_contractnegotiations.md) and [transfer-APIs](06_transferprocesses.md),
the transfer process would have to be initiated for every new token.

The EDR mechanism stores the Data Plane tokens. Finally, after first obtaining them from the Provider Control Plane and
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

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)