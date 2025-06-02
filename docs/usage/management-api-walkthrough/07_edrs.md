# Fetching a Data Plane Token via EDR

EDR is short for Endpoint Data Reference and describes how a Data Consumer can fetch data with PULL mechanism.
It contains information such as the `endpoint` where to fetch the data and additional information like
authorization. The EDR is conveyed with the Transfer start message of the DSP protocol from the Provider to the Consumer,
after receiving and accepting a [Transfer](06_transferprocesses.md) request made by a Consumer.

For sending a Transfer Request a [Contract Agreement](05_contractnegotiations.md) should be already in place.

Previously TractusX-EDC provided a set extensions for caching EDRs on Consumer side and exposing them via EDRs management APIs.
The renewal was handled proactively by firing another transfer process with the same contract agreement near expiry and caching
the newly transmitted EDR.

Starting from TractusX-EDC 0.7.0, the high level concepts are the same, but with the advent of [DPS](https://eclipse-edc.github.io/documentation/for-contributors/data-plane/data-plane-signaling/) (Data plane signaling)
the way EDRs are managed and renewed has been changed.

Since the default implementation of DPS does not support token refresh, the TractusX-EDC project extends it by introducing refresh capabilities. More info [here](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)

## Receiving the EDR

An EDR can be received on Consumer side by:

- Negotiating a contract for an asset
- Sending a transfer request for the contract agreement previously negotiated

Once the Transfer process reaches the state of `STARTED`, the provider will send the EDR to the consumer, which will
automatically cache it for later usage.

Alternatively TractusX-EDC provides a single API to collapse those two processes in a single request.

Example of negotiating a contract for an asset with a framework agreement policy:

```http request
POST /v3/edrs HTTP/1.1
Host: https://consumer-control.plane/management
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
        "@id": "{{OFFER_ID}}",
        "@type": "Offer",
        "assigner": "{{PROVIDER_BPN}}",
        "permission": [
            {
                "action": "use",
                "constraint": {
                    "or": {
                        "leftOperand": "FrameworkAgreement",
                        "operator": "eq",
                        "rightOperand": "Pcf:<version>"
                    }
                }
            }
        ],
        "prohibition": [],
        "obligation": [],
        "target": "{{ASSET_ID}}"
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

Additional callbacks can be provided in both cases for being notified about the start of a transfer process (containing the EDR).

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

Callback can be also configured statically at boot time with this [extension](https://github.com/eclipse-edc/Connector/blob/main/extensions/control-plane/callback/callback-static-endpoint/REAME.md)
that is bundled in the TractusX-EDC distribution.

> When configuring static callbacks like above, users will receive notifications about transfer process start events of both sides consumer/provider if the connector acts as both. 
> This can be checked by the type property in the event itself.

But in any case the EDR will be cached upon arrival on the consumer side.


## Retrieving EDR entries from the Consumer Control Plane (Management API)

The Consumer Control Plane can be queried for EDRs by the 
- id of the [Assets](01_assets.md) and/or 
- id of the relevant Contract Agreement (given there is one)
- id of the Contract Negotiation (as obtained [previously](#receiving-the-edr))
- id of the Data Provider


Here's an example of querying with `assetId`:

```http request
POST /v3/edrs/request HTTP/1.1
Host: https://consumer-control.plane/management
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

It returns a set of EDR entries holding meta-data including:
- `transferProcessId`: The ID of the [Transfer Process](06_transferprocesses.md) that was implicitly initiated
  by the POST `/v3/edrs` request.
- `agreementId`: The ID of the agreement that the two EDCs have made in the [Contract Negotiation](05_contractnegotiations.md)
  phase of their EDR-interaction.
- `providerId`: The ID of the provider.
- `assetId`: The ID of the asset.
- `contractNegotiationId`: The ID of contract negotiation.

The EDR itself contain also authentication information is stored in the secure vault of the Consumer. 

Finally, after first obtaining them from the Provider Control Plane and
then locating in the Consumer Control Plane's cache, they can be retrieved using the `transferProcessId`.

```http request
GET /v3/edrs/myTransferProcessId/dataaddress HTTP/1.1
Host: https://consumer-control.plane/management
X-Api-Key: password
Content-Type: application/json
```

This will pull out the EDR directly from the vault if present.

The interesting field is `authorization`. It holds a short-lived token that the Consumer can use to unlock the HTTP Data Plane
that is located at `endpoint`.

```json
{
  "@type": "DataAddress",
  "endpointType": "https://w3id.org/idsa/v4.1/HTTP",
  "tx-auth:refreshEndpoint": "http://provider.dataplane/api/public/token",
  "type": "https://w3id.org/idsa/v4.1/HTTP",
  "endpoint": "http://provider.dataplane/api/public",
  "tx-auth:refreshToken": "{{REFRESH_TOKEN}}",
  "tx-auth:expiresIn": "300",
  "authorization": "{{TOKEN}}",
  "tx-auth:refreshAudience": "{{REFRESH_AUDIENCE}}",
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "tx-auth": "https://w3id.org/tractusx/auth/",
    "cx-policy": "https://w3id.org/catenax/policy/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  }
}
```

Since tokens expire after `tx-auth:expiresIn` property, they need to be refreshed.

Three ways are currently supported depending on the use case:

- Auto refresh on fetch.
- Explicit refresh.
- Auto refresh on request [see](#consumer-data-plane-proxy).

### Auto Renew on fetch

By using the same API described above and passing a query parameter `auto_refresh=true`, the renewal
will be done automatically if necessary transparently.

```http request
GET /v3/edrs/myTransferProcessId/dataaddress?auto_refresh=true HTTP/1.1
Host: https://consumer-control.plane/management
X-Api-Key: password
Content-Type: application/json
```

In this way, always a valid token is returned.

### Explicit Refresh

A explicit refresh API is available for users; 

```http
POST /v3/edrs/myTransferProcessId/refresh HTTP/1.1
Host: https://consumer-control.plane/management
X-Api-Key: password
Content-Type: application/json
```

and it will store the refreshed EDR in the cache return it in the response body.


## EDR Management | Deleting a cached EDR

Normally there is no need to delete manually an EDR, and it's metadata from the cache. The EDR follows the state of
the transfer process. When the transfer process is `STARTED` the EDR is available in the cache.
If/When the transfer process transition to `SUSPENDED`/`TERMINATED` the EDR is automatically deleted

However, if needed this endpoint will delete the EDR entry associated with the `transfer-process-id` and it will remove the EDR itself
from the vault.

```http request
DELETE /v3/edrs/myTransferProcessId HTTP/1.1
Host: https://consumer-control.plane/management
X-Api-Key: password
Content-Type: application/json
```
# Using the EDR for Data Access

Once the EDR has been negotiated and stored, the data can be fetched in two ways depending on the use-case:

- Provider data-plane ("EDC way")
- Consumer proxy (Tractus-X EDC simplified)

## Provider Data Plane

Once the right EDR has been identified using the EDR Management API via the correct asset/agreement/transfer-process that
you want to transfer, we can use the `endpoint` and `authorization` information from the EDR to make the request. 
In the example below, the `endpoint` is used as the Host to which the request is sent and the `authorization` is used as the 
token in the authorization header.

Note: If the HTTP [Asset](01_assets.md) has been configured to proxy also HTTP verb, query parameters and path segments, 
they will be forwarded to the backend by the Provider Data Plane:

```http request
GET /subroute?foo=bar HTTP/1.1
Host: https://provider-data.plane/api/public
Authorization: {{TOKEN}}
Content-Type: application/json
```

## Consumer Data Plane Proxy

The Consumer Data Plane Proxy is an extension available in `tractusx-edc` that will use the EDR store to simplify
the data request on Consumer side.

The API fetches the data according to the input body. The body should contain the `assetId` plus `providerId` or the 
`transferProcessId` which identifies the EDR to use for fetching data.

Please note that the path-segment `/aas/` is not configurable. Still, this feature is not specific to the Asset Administration
Shell APIs but can be used to connect to any Http-based Asset.

Example:

```http request
POST /aas/request HTTP/1.1
Host: https://consumer-data.plane/proxy
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "assetId": "1",
  "providerId": "BPNL000000000001"
}
```

Users can provide additional properties to the request for composing the final
url:

- `pathSegments` sub path to append to the base url
- `queryParams` query parameters to add to the url

So if the `Edr#endpoint` is `http://provider-data.plane:8080/test`, the following request
```http request
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

will be routed to `GET|http://provider-data.plane:8080/test/sub?foo=bar` and then resolved to the backend as described in
the docs for the [`baseUrl` of the Asset](01_assets.md#http-data-plane).

When using this API for fetching data the refresh of the token is done automatically if it's expired

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
