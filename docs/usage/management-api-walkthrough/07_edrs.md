# Fetching a Data Plane Token via EDR

EDR is short for Endpoint Data Reference and describes how a Data Consumer can fetch data with PULL mechanism.
It contains information such as the `endpoint` where to fetch the data, additional information like
authentication and so on. The EDR is conveyed with the Transfer start message of the DSP protocol from the Provider to the Consumer,
after receiving and accepting a [Transfer](06_transferprocesses.md) request made by a Consumer.

For sending a Transfer Request a [Contract Agreement](05_contractnegotiations.md) should be already in place.

Previously TractusX-EDC provided a set extension for caching EDRs on Consumer side and exposing them via EDRs management APIs.
The renewal was handled proactively by firing another transfer process with the same contract agreement near expiry and caching
the newly transmitted EDR.

Starting from TractusX-EDC 0.7.0, the high level concepts are the same, but with the advent of [DPS](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling.md) (Data plane signaling)
the way EDRs are managed and renewed has been changed.

Since the default implementation of DPS does not support token renewals, the TractusX-EDC project extends it by introducing renewals capabilities. More info [here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)

## Receiving the EDR

An EDR can be received on Consumer side by:

- Negotiating a contract for an asset
- Sending a transfer request for the contract agreement previously negotiated

Once the Transfer process reaches the state of `STARTED`, the provider will send the EDR to the consumer, which will
automatically cache it for later usage.

Alternatively TractusX-EDC provides a single API to collapse those two processes in a single request.

Example of negotiating a contract for an asset with a framework agreement policy:

```http
POST /v2/edrs HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
    "@context": [
        "https://w3id.org/tractusx/policy/v1.0.0",
        "http://www.w3.org/ns/odrl.jsonld",
        {
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
        }
    ],
    "@type": "ContractRequest",
    "counterPartyAddress": "https://provider-control.plane/api/v1/dsp",
    "protocol": "dataspace-protocol-http",
    "policy": {
        "@id": "<OFFER_ID>",
        "@type": "Offer",
        "assigner": "<PROVIDER_BPN}>",
        "permission": [
            {
                "action": "use",
                "constraint": {
                    "or": {
                        "leftOperand": "FrameworkAgreement",
                        "operator": "eq",
                        "rightOperand": "pcf"
                    }
                }
            }
        ],
        "prohibition": [],
        "obligation": [],
        "target": "<ASSET_ID>"
    },
    "callbackAddresses": []
}
```

- `counterPartyAddress` sets the coordinates for the connector that the Consumer-EDC shall negotiate with (Provider EDC).
  It will usually end in `/api/v1/dsp`
- `protocol` must be `dataspace-protocol-http`
- In the `policy` section, the Data Consumer specifies the Data Offer for the negotiation. As there may be multiple
  Data Offers for the same DataSet, the Data Consumer must choose one. 
  It must hold an identical copy of the Data Offer's contract policy as provided via the catalog-API in the `odrl:hasPolicy` field plus:
    - `assigner` must hold the BPN of the Provider
    - `target` must be the id of the EDC-Asset/dcat:DataSet that the offer was made for.

This request synchronously returns a server-generated `negotiationId` that could be used to get the state of the negotiation.
Once the negotiation reaches the `FINALIZED` state, using this API, the transfer process will be automatically fired off
by sending a transfer request for the `PULL` scenario.

Additional callbacks can be provided in both cases for being notified about the start of a transfer process.

```json
{
   ...
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

But in any case the EDR will be cached upon arrival on the consumer side.


## Retrieving EDR entries from the Consumer Control Plane (Management API)

The Consumer Control Plane can be queried for EDRs by the 
- id of the [Assets](01_assets.md) and/or 
- id of the relevant Contract Agreement (given there is one)
- id of the Contract Negotiation (as obtained [previously](#receiving-the-edr))
- id of the Data Provider

```http
POST /v2/edrs HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type": "QuerySpec",
    "filterExpression": [
        {
            "operandLeft": "assetId",
            "operator": "=",
            "operandRight": "1"
        }
        
    ]
}
```

It returns a set of EDR entries holding meta-data including two IDs per entry:
- `transferProcessId`: The ID of the [Transfer Process](06_transferprocesses.md) that was implicitly initiated
  by the POST `/v2/edrs` request.
- `agreementId`: The ID of the agreement that the two EDCs have made in the [Contract Negotiation](05_contractnegotiations.md)
  phase of their EDR-interaction.
- `providerId`: The ID of the provider.
- `assetId`: The ID of the asset.
- `contractNegotiationId`: The ID of contract negotiation.

The EDR itself contain also authentication information are stored in the secure vault of the Consumer. 

Finally, after first obtaining them from the Provider Control Plane and
then locating in the Consumer Control Plane's cache, they can be retrieved using the `transferProcessId`.

```http
GET /v2/edrs/myTransferProcessId/dataaddress HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

The interesting field is `edc:authCode`. It holds a short-lived token that the Consumer can use to unlock the HTTP Data Plane
that is located at `edc:endpoint`.

```json
{
  "@context": {},
  "@type": "DataAddress",
  "endpointType": "https://w3id.org/idsa/v4.1/HTTP",
  "refreshEndpoint": "http://alice-tractusx-connector-dataplane:8081/api/public/token",
  "audience": "did:web:dim-static-prod.dis-cloud-prod.cfapps.eu10-004.hana.ondemand.com:dim-hosted:3ecba91c-cc4f-4e07-b11c-2cc2af28c248:holder-iatp",
  "endpoint": "http://alice-tractusx-connector-dataplane:8081/api/public",
  "refreshToken": "eyJraWQiOiJ0cmFuc2ZlclByb3h5VG9rZW5TaWduZXJQdWJsaWNLZXkiLCJhbGciOiJFZERTQSJ9.eyJleHAiOjE3MTMzNTE0NDQsImlhdCI6MTcxMzM1MTE0NCwianRpIjoiZjYxOWFmMTItOWNhMS00OTliLTg5MmEtZWE3ZjNkYmQxNjI4In0.igyKMywf1eTBSWaZB3799NRFmGU9jBqOo5sZ-EPRRuEeueZz2seYBMq2aPCFHcQ1kJh-G_ylPb5OXWxIv4ITDw",
  "expiresIn": "300",
  "authorization": "eyJraWQiOiJ0cmFuc2ZlclByb3h5VG9rZW5TaWduZXJQdWJsaWNLZXkiLCJhbGciOiJFZERTQSJ9.eyJpc3MiOiJCUE5MMDAwMDAwMDAxSU5UIiwiYXVkIjoiQlBOTDAwMDAwMDAwMUROUyIsInN1YiI6IkJQTkwwMDAwMDAwMDFJTlQiLCJleHAiOjE3MTMzNTE0NDQsImlhdCI6MTcxMzM1MTE0NCwianRpIjoiMWNkNzU3NjktNDIxZS00ZTQ0LTkxOGMtMDVhMDZkZTA0OTVkIn0.LxfCU3UfyAnaoUym_ZD-97kcoiLvOIF1nBaL4oH-VLwisnxzkwaMFqeyW0r28rSRCKagZr0UkyoC_Hfq38ldCQ",
  "refreshAudience": "did:web:dim-static-prod.dis-cloud-prod.cfapps.eu10-004.hana.ondemand.com:dim-hosted:3ecba91c-cc4f-4e07-b11c-2cc2af28c248:holder-iatp",
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
the data request on Consumer side. The documentation is available [here](../../../edc-extensions/dataplane-proxy/edc-dataplane-proxy-consumer-api/README.md).

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