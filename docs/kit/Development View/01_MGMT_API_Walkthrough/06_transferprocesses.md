# Initiation a Transfer Process

Despite the naming, the Transfer Process is not the step that transmits the backend's data from the Provider to the
Consumer. What this API does instead is trigger the Transfer of a Data Plane token from the Provider Control Plane to
the Consumer Control Plane and in turn to a location specified by the Data Consumer.

To trigger this process, the Consumer app makes a request to its EDC's Control Plane:
```http
POST /v2/transferprocesses HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "assetId": "<ASSET-ID>",
  "connectorAddress": "<CONNECTOR-ADDRESS>",
  "contractId": "<CONTRACT-AGREEMENT-ID>",
  "dataDestination": {
    "type": "<SUPPORTED-TYPE>"
  },
  "managedResources": false,
  "privateProperties": {
    "receiverHttpEndpoint": "<RECEIVER-HTTP-ENDPOINT>"
  },
  "protocol": "dataspace-protocol-http",
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
  ]
}
```

- `assetId` is the id of the [asset](01_assets.md) that a transfer process should be triggered for.
- `connectorAddress` is the DSP-endpoint of the Data Provider (usually ending on /api/v1/dsp).
- `contractId` represents the Contract Agreement that the Provider and Consumer agreed on during the [Contract Negotiation](05_contractnegotiations.md)
  phase.
- `dataDestination` will in the case of an HTTP-based transfer of the Token be a `DataAddress` object, holding exclusively
  the `edc:type` property that must be set to `"HttpProxy"`.
- `managedResources` is a boolean (not a string like in the [assets-API](01_assets.md#http-data-plane)).
- `privateProperties` can be filled with arbitrary data (like in the [assets-API](01_assets.md)). However, there is one property
  that will cause changes in EDC behavior:
    - `receiverHttpEndpoint` is interpreted by the EDC as the URL that it shall write the Data Plane token to. These messages
      will be transmitted via HTTP POST holding plain JSON where the Data Plane Token is written to the property `authCode`.
      The Consumer Control Plane will use authentication headers that must be configured during deployment.
- `protocol` describes the protocol between the EDCs and will always be `dataspace-protocol-http`.
- `callbackAddresses`: Like the [Contract Negotiation API](05_contractnegotiations.md), an application can also register
  a callback listener to get updates on the Transfer Process state. The relevant signal is `contract.negotiation`.

This call also returns an id, that can be used to monitor the progress.

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "177aba51-52d7-44dc-beab-fd6151147024",
  "createdAt": 1688465655
}
```

## Checking for Completion

### Token Transfer

As described in the `receiverHttpEndpoint` section, on a successful Transfer Process, a message will be transmitted to
the specified address holding the Data Plane Token. That's not only the goal of EDC-interaction but also the sign for
a completed process. Here's an example for such a message:

```json
{
  "id": "177aba51-52d7-44dc-beab-fd6151147024",
  "endpoint": "https://provider-data.plane/",
  "authKey": "Authorization",
  "authCode": "<DATA-PLANE-TOKEN>",
  "properties": {}
}
```

### Polling

The state of a given Transfer Process can be requested like this:

```http
GET /v2/transferprocesses/177aba51-52d7-44dc-beab-fd6151147024 HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

yielding

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "https://w3id.org/edc/v0.0.1/ns/TransferProcess",
  "@id": "process-id",
  "correlationId": "correlation-id",
  "type": "PROVIDER",
  "state": "STARTED",
  "stateTimestamp": 1688465655,
  "assetId": "asset-id",
  "connectorId": "connectorId",
  "contractId": "contractId",
  "dataDestination": {
    "type": "HttpProxy"
  },
  "privateProperties": {
    "receiverHttpEndpoint": "private-value"
  },
  "errorDetail": "eventual-error-detail",
  "createdAt": 1688465655,
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
  ]
}

```
Note that the property `errorDetails` will only be returned in certain states and may contain hints to where the communication
between the Data Planes failed. The state-machine for the Transfer Process is [documented here](https://eclipse-edc.github.io/docs/#/submodule/Connector/docs/developer/data-transfer?id=transfer-process-state-machine).

### Callbacks

The callback process works similarly to the known one from the Contract Negotiation. The `"TransferProcessStarted"` message
will already contain the `authCode`.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)