# Initiating a Contract Negotiation

Contract Negotiation is the second check a Data Consumer has to pass before getting access rights to a backend resource.
It includes

- a check of the Consumer's VC against the Offer's `contractPolicy`.
- a check of the `contractPolicy` against the policy the Data Consumer signals in the negotiation request to.

## Creating a new Contract Negotiation

To trigger the process, the Data Consumer POSTs against their own Control Plane.

```http request
POST /v3/contractnegotiations HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractRequest",
  "counterPartyAddress": "https://provider-control.plane/api/v1/dsp",
  "protocol": "dataspace-protocol-http",
  "policy": {
    "@type": "Offer",
    "@id": "{{OFFER_ID}}",
    "target": "{{ASSET_ID}}",
    "assigner": "{{PROVIDER_BPN}}",
    "permission": [],
    "prohibition": [],
    "obligation": []
  },
  "callbackAddresses": [
    {
      "transactional": false,
      "uri": "http://callback/url",
      "events": [
        "contract.negotiation"
      ],
      "authKey": "auth-key",
      "authCodeId": "auth-code-id"
    }
  ]
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
- `callbackAddresses` is a list of Consumer-side endpoints that the Provider's Data Plane writes events to.
    - `uri` is the http endpoint of the token repository. Mandatory.
    - `events` is a list of the strings, signifying for what callbacks the specified API shall be used. They are
      structured hierarchically, so if a Consumer is interested in all events about status changes, the
      `contract.negotiation` marker can be added. If only events about the `requested` stage of a transfer are relevant,
      they can be subscribed via `transfer.process.requested`. This enables the consumer to wait for arrival of a
      relevant event instead of having to poll for transition into a desired state.
    - `transactional` Optional, default false.
    - `authCodeId` is the key of a secret stored in the Consumer's vault that can be used to unlock the callback API if
      it is protected. Optional.
    - `authKey` Key of the HTTP-header that will be sent to the callbackAddress for authentication. Optional. If
      `authCodeId` is set and `authKey` isn't, it defaults to `Authorization`.

This call does not yet return a negotiation result but rather a server-side generated id for the contract negotiation in
the `@id` property.

```json
{
	  "@type": "IdResponse",
	  "@id": "773b8795-45f2-4c57-a020-dc04e639baf3",
	  "edc:createdAt": 1701289079455,
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

### Polling

```http request
GET /v3/contractnegotiation/773b8795-45f2-4c57-a020-dc04e639baf3 HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

This request (holding the previously returned `contractNegotiationId` in its path) returns details on the negotiation
that will look like this:

```json
{
  "@type": "ContractNegotiation",
  "@id": "50bf14b9-8f6e-4975-8ada-6f24379a58a2",
  "type": "CONSUMER",
  "protocol": "dataspace-protocol-http",
  "state": "REQUESTING",
  "counterPartyId": "{{PROVIDER_BPN}}",
  "counterPartyAddress": "https://provider-control.plane/api/v1/dsp",
  "callbackAddresses": [
    {
      "transactional": false,
      "uri": "http://call.back/url",
      "events": [
        "contract.negotiation"
      ],
      "authKey": "auth-key",
      "authCodeId": "auth-code-id"
    }
  ],
  "createdAt": 1701351116766,
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

The Contract Negotiation was successful when `edc:state == FINALIZED`.

### Callbacks

As shown in the example above, state transitions can also be subscribed to by adding a `callbackAddress`. A typical
callback message will hold the relevant information in the `type` property. The value of the `type` property will always
hold a string following the schema `ContractNegotiation` appended by the new state like `Verified` yielding `ContractNegotiationVerified`
The state-machine for the Contract Negotiation process is [visualized in the documentation](https://eclipse-edc.github.io/documentation/for-contributors/control-plane/entities/#4-contract-negotiations)
of the eclipse-edc/connector. The diagram only visualizes the transitions while the callbacks are fired when such a
transition is done yielding a new state.

Be aware that (unlike most other messages) this is not JSON-LD and thus does not require prefix/context handling.
Here's an example:

```json
{
  "id": "5e6b1a66-c0a8-4189-bbb8-305e3bdbeddd",
  "at": 1701441001897,
  "payload": {
    "contractNegotiationId": "019488e0-f242-4c12-8314-610927b09e96",
    "counterPartyAddress": "{{PROVIDER_CONTROLPLANE_DSP_ENDPOINT}}",
    "counterPartyId": "{{PROVIDER_BPN}}",
    "callbackAddresses": [
      {
        "transactional": false,
        "uri": "http://call.back/url",
        "events": [
          "contract.negotiation"
        ],
        "authKey": "auth-key",
        "authCodeId": "auth-code-id"
      }
    ],
    "contractOffers": [
      {
        "id": "{{OFFER_ID}}",
        "policy": {
          "permissions": [
            {
              "edctype": "dataspaceconnector:permission",
              "target": "{{ASSET_ID}}",
              "action": {
                "type": "http://www.w3.org/ns/odrl/2/use",
                "includedIn": null,
                "constraint": null
              },
              "assignee": null,
              "assigner": null,
              "constraints": [],
              "duties": []
            }
          ],
          "prohibitions": [],
          "obligations": [],
          "extensibleProperties": {},
          "inheritsFrom": null,
          "assigner": null,
          "assignee": null,
          "target": "{{ASSET_ID}}",
          "@type": {
            "@policytype": "set"
          }
        },
        "assetId": "{{ASSET_ID}}"
      }
    ],
    "protocol": "dataspace-protocol-http",
    "lastContractOffer": {
      "id": "{{OFFER_ID}}",
      "policy": {
        "permissions": [
          {
            "edctype": "dataspaceconnector:permission",
            "target": "{{ASSET_ID}}",
            "action": {
              "type": "http://www.w3.org/ns/odrl/2/use",
              "includedIn": null,
              "constraint": null
            },
            "assignee": null,
            "assigner": null,
            "constraints": [],
            "duties": []
          }
        ],
        "prohibitions": [],
        "obligations": [],
        "extensibleProperties": {},
        "inheritsFrom": null,
        "assigner": null,
        "assignee": null,
        "target": "{{ASSET_ID}}",
        "@type": {
          "@policytype": "set"
        }
      },
      "assetId": "{{ASSET_ID}}"
    }
  },
  "type": "ContractNegotiationRequested"
}
```

As soon as `"type": "ContractNegotiationFinalized"`, a `contractAgreement` will be added holding additional context
like the `contractSigningDate`.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
