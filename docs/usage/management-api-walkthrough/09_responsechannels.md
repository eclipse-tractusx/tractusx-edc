# Using transfer process response channels

In certain cases, a dataspace consumer might need to send feedback messages to a transfer process counterpart.
This bidirectional exchange of information requires two separate channels: one forward channel where data flows from the
provider to the consumer, and a response channel (back channel) where data flows in the reverse
direction.
The connector component permits such bidirectional flow of information to be realized. It allows dataspace providers
to set a response channel endpoint for their offered assets. When this response channel is defined by the provider,
a consumer can request the endpoint information during a transfer process request. The information will be sent to
consumer as part of the requested EDR in case of a PULL transfer scenario, or as new EDR in the case of PUSH scenario.



## HTTP response channels

The HTTP wire protocol is the only supported protocol for response data communication as of now. Given the
below-mentioned considerations, and by following the demonstrated process, a dataspace consumer can send response
messages to a provider.

## Requirements

- Considering the connector operator has enabled the HTTP response channel for a certain connector installation, by
  setting the following configuration:

```
edc.dataplane.api.public.response.baseurl
# A common practice is to use <DATAPLANE_PUBLIC_ENDPOINT>/responseChannel
```

- A `HttpData` type asset is offered by the provider, exposing the base url of the backend application. This asset
  should enable the proxying of at least the request method, path, and body.
- The backend application exposes an `/responseChannel` endpoint, where messages will be wired to by the dataplane.
- A valid contract agreement exists for the mentioned offer.

## Getting an EDR with response channel properties

Given the above requirements are met, the consumer client is able to start a transfer process and indicate the intention
to receive response channel information in an EDR. For this to happen, the transferType attribute of the transfer
process request needs to include the type of the response channel protocol. Since HTTP is the only supported protocol,
`HttpData` is the response type to be used.

As an example, for this transfer process request:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "{{ASSET_ID}}",
  "contractId": "{{CONTRACT_AGREEMENT_ID}}",
  "counterPartyAddress": "{{CONNECTOR_ADDRESS}}",
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PULL",
  "callbackAddresses": []
}
```

Changing the `transferType` from `HttpData-PULL` to `HttpData-PULL-HttpData`, indicates the intention to receive
response channel details. Other possible `transferType` combinations can be discovered within `dcat:distribution`
when requesting a providers' catalog.

## Sending messages to the response channel

Once the right EDR has been identified using the EDR Management API, it should contain information about the forward
channel endpoint (as it usually has for a PULL transfer) and additional details for the response channel endpoint.
The forward channel still works the as if the response channel wasn't requested.
To use the response channel the same process as for any other transfer using an [EDR](07_edrs.md) can be used, but
`responseChannel-endpoint` should be used instead of `endpoint` and `responseChannel-authorization` instead of
`authorization`.

```json
{
  "@type": "DataAddress",
  (...)
  "responseChannel-endpoint": "<DATAPLANE_HOST>/public/responseChannel",
  "responseChannel-authorization": "<RESPONSE_CHANNEL_AUTH_TOKEN>",
  (...)
  "endpoint": "<DATAPLANE_HOST>/public",
  "authorization": "<AUTH_TOKEN>",
  (...)
  "@context": {
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "tx-auth": "https://w3id.org/tractusx/auth/",
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  }
}
```

## Current limitations

- The same `baseUrl` of the provider data offer will be used as the response channel `baseUrl`. The same backend
  application should be able to handle this request.
- If the provider data offer has a data address of any type other than `HttpData`, the response channel won't work.
- There is no manner to specify if and which assets have response channels associated. If the functionality is
  enabled by the connector operator, an `HttpData` response channel distribution type will be made available for every
  asset offered by the dataspace provider, even if the underlying asset data address type is not `HttpData`.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2025 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)