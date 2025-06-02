# Initiation a Transfer Process

Transfer process is a set of interactions between Consumer and Provider that gives 
the consumer access to a `Dataset` under the terms negotiated the in [Contract Negotiation](05_contractnegotiations.md) phase.

The processes are handled in the state machine of the Control Planes, but the real data transfer (if necessary)
happens in the provider Data Plane. 
TractusX 0.7.0 follows the spec of [DPS](https://eclipse-edc.github.io/documentation/for-contributors/data-plane/data-plane-signaling/) (Data plane signaling) for Control Plane -> Data Plane communication

Currently, transfer processes are divided in two modes:

- Push
- Pull

## Push

If the mode is push, for example `AmazonS3-PUSH` mode, once the transfer request has been validated and accepted
by the Provider Control Plane, the transfer switches to the `STARTED` state, and it delegates the data transfer 
to the Data Plane via (DPS). The transfer process transition to `COMPLETED` state once the actual transfer 
has been completed by the data plane.

## Pull

If the mode is pull, for example `HttpData-PULL` mode, once the transfer request has been validated and accepted
by the Provider Control Plane, the transfer switches to the `STARTED` state, and it delegates [EDR](07_edrs.md) creation
to the Data Plane via (DPS). The transfer will stay in `STARTED` state until the transfer process gets manually 
terminated/suspended or terminated by the policy monitor depending on the configured policy on the [Contract Agreement](08_contractagreements.md).

## Transfer Request

To trigger this process, the Consumer app makes a request to its EDC's Control Plane:
```http request
POST /v3/transferprocesses HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "{{ASSET_ID}}",
  "contractId": "{{CONTRACT_AGREEMENT_ID}}",
  "counterPartyAddress": "{{CONNECTOR_ADDRESS}}",
  "dataDestination": {
    "type": "<supported-transfer-type>"
  },
  "privateProperties": {},
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PULL",
  "callbackAddresses": [
    {
      "transactional": false,
      "uri": "http://call.back/url",
      "events": [
        "transfer.process"
      ],
      "authKey": "auth-key",
      "authCodeId": "auth-code-id"
    }
  ],
  "privateProperties": {
    "receiverHttpEndpoint": "{{RECEIVER_HTTP_ENDPOINT}}"
  }
}
```

- `assetId` is the id of the [asset](01_assets.md) that a transfer process should be triggered for.
- `counterPartyAddress` is the DSP-endpoint of the Data Provider (usually ending on /api/v1/dsp).
- `contractId` represents the Contract Agreement that the Provider and Consumer agreed on during the [Contract Negotiation](05_contractnegotiations.md)
  phase.
- `dataDestination` will in the case of an HTTP PULL-based transfer of the Token be a `DataAddress` object, holding exclusively
  the `type` property that must be set to `"HttpProxy"`.
- `privateProperties` can be filled with arbitrary data (like in the [assets-API](01_assets.md)).
- `protocol` describes the protocol between the EDCs and will always be `dataspace-protocol-http`.
- `transferType` should be one of the returned format in the [Catalog](04_catalog.md)
- `callbackAddresses`: Like the [Contract Negotiation API](05_contractnegotiations.md), an application can also register
  a callback listener to get updates on the Transfer Process state. The relevant signal is `transfer.process`.

This call also returns an id, that can be used to monitor the progress.

```json
{
  "@type": "IdResponse",
  "@id": "927c9712-b270-47ee-b391-9e92a4c55a5d",
  "createdAt": 1713439560709,
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

## Checking for Completion

### Token Transfer HTTP PULL-BASED

As outlined in PULL-BASED scenario the transfer process will remain in the `STARTED` state once the EDR reaches
the Consumer Control Plane. How to handle EDRs consumption check [here](07_edrs.md). 

### Polling

The state of a given Transfer Process can be requested like this:

```http request
GET /v3/transferprocesses/177aba51-52d7-44dc-beab-fd6151147024 HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

yielding

```json
{
  "@id": "432b242d-3795-403f-8fac-f7bd41d9cef5",
  "@type": "TransferProcess",
  "state": "TERMINATED",
  "stateTimestamp": 1713439434962,
  "type": "CONSUMER",
  "assetId": "1",
  "contractId": "47e05ca6-c373-4345-9ba8-e4915e02e99e",
  "transferType": "HttpData-PULL",
  "errorDetail": "Policy not found for contract: 47e05ca6-c373-4345-9ba8-e4915e02e99e",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpProxy"
  },
  "callbackAddresses": [
    {
      "transactional": false,
      "uri": "http://call.back/url",
      "events": [
        "transfer.process"
      ],
      "authKey": "auth-key",
      "authCodeId": "auth-code-id"
    }
  ],
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
Note that the property `errorDetails` will only be returned in certain states and may contain hints to where the communication
between the Data Planes failed. The state-machine for the Transfer Process is [documented here](https://eclipse-edc.github.io/documentation/for-contributors/control-plane/entities/#7-transfer-processes).

### Callbacks

The callback process works similarly to the known one from the Contract Negotiation. The `"TransferProcessStarted"` message
will already contain the `authCode`.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
